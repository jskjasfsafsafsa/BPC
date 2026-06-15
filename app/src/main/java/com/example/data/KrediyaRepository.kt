package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KrediyaRepository(private val db: AppDatabase) {

    // Clients
    val allClients: Flow<List<Client>> = db.clientDao().getAllClients()
    val blacklistedClients: Flow<List<Client>> = db.clientDao().getBlacklistedClients()

    suspend fun getClientById(id: Int): Client? = db.clientDao().getClientById(id)

    suspend fun insertClient(client: Client): Long {
        val id = db.clientDao().insertClient(client)
        logActivity("CREATION_CLIENT", "Création de la fiche de ${client.fullName}", "Alphonse Ngolo")
        return id
    }

    suspend fun updateClient(client: Client) {
        db.clientDao().updateClient(client)
        logActivity("MODIFICATION_CLIENT", "Informations de ${client.fullName} mises à jour", "Alphonse Ngolo")
    }

    suspend fun toggleBlacklist(clientId: Int, isBlacklisted: Boolean, reason: String?) {
        val client = getClientById(clientId)
        if (client != null) {
            val updated = client.copy(isBlacklisted = isBlacklisted, blacklistReason = reason, score = if (isBlacklisted) 0 else 50)
            db.clientDao().updateClient(updated)
            val action = if (isBlacklisted) "INSCRIPTION_LISTE_NOIRE" else "RETRAIT_LISTE_NOIRE"
            val detail = if (isBlacklisted) {
                "Client ${client.fullName} mis sur liste noire: $reason"
            } else {
                "Client ${client.fullName} retiré de la liste noire"
            }
            logActivity(action, detail, "Alphonse Ngolo")
        }
    }

    // Loans
    val allLoans: Flow<List<Loan>> = db.loanDao().getAllLoans()

    suspend fun getLoanById(id: Int): Loan? = db.loanDao().getLoanById(id)

    fun getLoansForClient(clientId: Int): Flow<List<Loan>> = db.loanDao().getLoansForClient(clientId)

    suspend fun disburseLoan(clientId: Int, amount: Double, interestRate: Double, durationDays: Int, startDate: String, dueDate: String): Boolean {
        val client = getClientById(clientId) ?: return false
        if (client.isBlacklisted) return false

        // Calculate expected values
        val interestAmount = (amount * interestRate) / 100.0
        val totalRepayable = amount + interestAmount

        // Check capital availability
        val capital = db.capitalInfoDao().getCapital() ?: CapitalInfo(id = 1)
        if (capital.availableCapital < amount) {
            return false // Solde insuffisant !
        }

        // Deduct capital
        val updatedCapital = capital.copy(
            lentCapital = capital.lentCapital + amount,
            availableCapital = capital.availableCapital - amount
        )
        db.capitalInfoDao().insertCapital(updatedCapital)

        // Insert Loan
        val newLoan = Loan(
            clientId = clientId,
            amount = amount,
            interestRate = interestRate,
            durationDays = durationDays,
            startDate = startDate,
            dueDate = dueDate,
            interestAmount = interestAmount,
            totalRepayable = totalRepayable,
            totalPaid = 0.0,
            status = "ACTIVE"
        )
        db.loanDao().insertLoan(newLoan)

        // Log
        logActivity("DEBOURSEMENT_PRET", "Prêt de ${amount.toInt()} FCFA déboursé pour ${client.fullName}", "Alphonse Ngolo")
        return true
    }

    suspend fun deleteLoan(loan: Loan) {
        val client = getClientById(loan.clientId)
        val clientName = client?.fullName ?: "Inconnu"
        db.loanDao().deleteLoan(loan)
        
        // Return money to capital on delete if not paid? To avoid breaking logic:
        val capital = db.capitalInfoDao().getCapital() ?: CapitalInfo(id = 1)
        db.capitalInfoDao().insertCapital(
            capital.copy(
                lentCapital = (capital.lentCapital - (loan.amount - loan.totalPaid)).coerceAtLeast(0.0),
                availableCapital = (capital.availableCapital + (loan.amount - loan.totalPaid)).coerceAtLeast(0.0)
            )
        )

        logActivity("SUPPRESSION_PRET", "Prêt ID ${loan.id} de $clientName supprimé", "Alphonse Ngolo")
    }

    // Repayments
    val allRepayments: Flow<List<Repayment>> = db.repaymentDao().getAllRepayments()

    fun getRepaymentsForLoan(loanId: Int): Flow<List<Repayment>> = db.repaymentDao().getRepaymentsForLoan(loanId)

    suspend fun recordRepayment(loanId: Int, amountPaid: Double, note: String?): Boolean {
        val loan = db.loanDao().getLoanById(loanId) ?: return false
        val client = db.clientDao().getClientById(loan.clientId) ?: return false

        // Update loan repayments
        val newPaid = loan.totalPaid + amountPaid
        val isCompleted = newPaid >= (loan.totalRepayable + loan.penaltyAmount)
        val updatedStatus = if (isCompleted) "REPAID" else loan.status

        val updatedLoan = loan.copy(
            totalPaid = newPaid,
            status = updatedStatus
        )
        db.loanDao().updateLoan(updatedLoan)

        // Insert repayment log
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        val repayment = Repayment(
            loanId = loanId,
            clientId = loan.clientId,
            amountPaid = amountPaid,
            paymentDate = dateFormat.format(now),
            paymentTime = timeFormat.format(now),
            note = note
        )
        db.repaymentDao().insertRepayment(repayment)

        // Update capital: return the principal recovered and net interest earned to the capital base
        val capital = db.capitalInfoDao().getCapital() ?: CapitalInfo(id = 1)
        // Principal ratio of the paid amount
        val principalRatio = loan.amount / loan.totalRepayable
        val principalPaid = amountPaid * principalRatio
        val interestPaid = amountPaid - principalPaid

        val updatedCapital = capital.copy(
            lentCapital = (capital.lentCapital - principalPaid).coerceAtLeast(0.0),
            availableCapital = (capital.availableCapital + amountPaid), // both principal and interest earned return to cash pool
            totalCapital = capital.totalCapital + interestPaid // interest increases overall portfolio value
        )
        db.capitalInfoDao().insertCapital(updatedCapital)

        // Recalculate client score!
        recalculateClientScore(client.id)

        // Log
        logActivity("REMBOURSEMENT", "Remboursement de ${amountPaid.toInt()} FCFA reçu de ${client.fullName}", "Alphonse Ngolo")
        return true
    }

    suspend fun recalculateClientScore(clientId: Int) {
        val client = db.clientDao().getClientById(clientId) ?: return
        val clientLoans = db.loanDao().getLoansForClient(clientId).firstOrNull() ?: emptyList()

        if (clientLoans.isEmpty()) {
            db.clientDao().updateClient(client.copy(score = 95))
            return
        }

        var penaltiesSum = 0.0
        var totalDelays = 0
        var totalRepaidOnTime = 0

        for (loan in clientLoans) {
            penaltiesSum += loan.penaltyAmount
            if (loan.daysOverdue > 0) {
                totalDelays++
            } else if (loan.status == "REPAID") {
                totalRepaidOnTime++
            }
        }

        // Calculation out of 100
        var calculatedScore = 100
        calculatedScore -= (totalDelays * 15) // Deduct for each late payment
        calculatedScore -= (penaltiesSum / 1000).toInt() // Deduct based on penalties accrued
        calculatedScore += (totalRepaidOnTime * 5) // Reward for on-time payments

        val finalScore = calculatedScore.coerceIn(5, 100)
        db.clientDao().updateClient(client.copy(score = finalScore))
    }

    // Activity Logs
    val allLogs: Flow<List<ActivityLog>> = db.activityLogDao().getAllLogs()

    suspend fun logActivity(action: String, details: String, agentName: String) {
        db.activityLogDao().insertLog(ActivityLog(action = action, details = details, agentName = agentName))
    }

    // Capital
    val capitalFlow: Flow<CapitalInfo?> = db.capitalInfoDao().getCapitalFlow()

    suspend fun initCapital(totalCapital: Double) {
        val existing = db.capitalInfoDao().getCapital()
        val lent = existing?.lentCapital ?: 0.0
        db.capitalInfoDao().insertCapital(
            CapitalInfo(id = 1, totalCapital = totalCapital, lentCapital = lent, availableCapital = totalCapital - lent)
        )
        logActivity("MODIFICATION_CAPITAL", "Niveau de capital d'exploitation ajusté à ${totalCapital.toInt()} FCFA", "Alphonse Ngolo")
    }

    // Client proofs
    fun getProofsForClient(clientId: Int): Flow<List<ClientProof>> = db.clientProofDao().getProofsForClient(clientId)

    suspend fun insertProof(clientId: Int, title: String, fileUri: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        db.clientProofDao().insertProof(ClientProof(clientId = clientId, title = title, fileUri = fileUri, addedDate = dateStr))
        val client = getClientById(clientId)
        logActivity("AJOUT_PREUVE", "Preuve '$title' ajoutée au dossier de ${client?.fullName ?: "Inconnu"}", "Alphonse Ngolo")
    }

    suspend fun deleteProof(proof: ClientProof) {
        db.clientProofDao().deleteProof(proof)
    }

    // Agents
    val allAgents: Flow<List<Agent>> = db.agentDao().getAllAgents()

    suspend fun insertAgent(agent: Agent) {
        db.agentDao().insertAgent(agent)
    }

    // Backups Export / Import simulated as local text file or string representation
    suspend fun exportSampleBackup(context: Context): String {
        val jsonDump = StringBuilder()
        val clients = db.clientDao().getAllClients().firstOrNull() ?: emptyList()
        val loans = db.loanDao().getAllLoans().firstOrNull() ?: emptyList()
        val capital = db.capitalInfoDao().getCapital()

        jsonDump.append("--- EXPORT KREDIYA SYSTEM BACKUP ---\n")
        jsonDump.append("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        jsonDump.append("Capital_Total: ${capital?.totalCapital ?: 5000000.0}\n")
        jsonDump.append("Clients_Count: ${clients.size}\n")
        jsonDump.append("Loans_Count: ${loans.size}\n\n")

        for (c in clients) {
            jsonDump.append("CLIENT|${c.id}|${c.lastName}|${c.firstName}|${c.phone}|${c.score}|${c.isBlacklisted}\n")
        }
        for (l in loans) {
            jsonDump.append("LOAN|${l.id}|${l.clientId}|${l.amount}|${l.totalPaid}|${l.status}\n")
        }

        // write file in local cache or files directory
        val file = File(context.filesDir, "krediya_backup.db")
        file.writeText(jsonDump.toString())
        return file.absolutePath
    }
}
