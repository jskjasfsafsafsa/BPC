package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lastName: String,
    val firstName: String,
    val gender: String, // "M" ou "F"
    val birthDate: String,
    val phone: String,
    val address: String,
    val neighborhood: String, // Quartier
    val profession: String,
    val photoUri: String? = null,
    val idCardUri: String? = null,
    val passportUri: String? = null,
    val contractUri: String? = null,
    val isBlacklisted: Boolean = false,
    val blacklistReason: String? = null,
    val score: Int = 95, // Score Krediya sur 100
    val agentId: Int = 1 // Support d'agents
) {
    val fullName: String get() = "$firstName $lastName".trim()

    val scoreBadge: String get() = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Bon"
        score >= 50 -> "Moyen"
        else -> "Risqué"
    }
}

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val amount: Double, // Montant prêté (e.g. 100 000 FCFA)
    val interestRate: Double, // Taux (e.g. 20%)
    val durationDays: Int,
    val startDate: String, // "2026-06-15"
    val dueDate: String, // "2026-06-30"
    val interestAmount: Double, // recalculé automatiquement (20 000)
    val totalRepayable: Double, // recalculé automatiquement (120 000)
    val totalPaid: Double = 0.0,
    val penaltyAmount: Double = 0.0,
    val status: String = "ACTIVE", // "ACTIVE", "NEAR_DUE", "OVERDUE", "REPAID"
    val daysOverdue: Int = 0,
    val agentId: Int = 1,
    val signatureData: String? = null // Contrat signé numériquement (coordonnées de signature ou bitmap en Base64)
) {
    val remainingAmount: Double get() = (totalRepayable + penaltyAmount - totalPaid).coerceAtLeast(0.0)
    val progressPercentage: Float get() = if (totalRepayable > 0) (totalPaid / totalRepayable).toFloat() else 0f
}

@Entity(tableName = "repayments")
data class Repayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loanId: Int,
    val clientId: Int,
    val amountPaid: Double,
    val paymentDate: String,
    val paymentTime: String,
    val agentId: Int = 1,
    val note: String? = null
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val details: String,
    val agentName: String
)

@Entity(tableName = "agents")
data class Agent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // "ADMIN", "AGENT_COLLECTOR", "MANAGER"
    val phone: String,
    val email: String? = null
)

@Entity(tableName = "capital_info")
data class CapitalInfo(
    @PrimaryKey val id: Int = 1,
    val totalCapital: Double = 5000000.0, // 5 000 000 FCFA par défaut
    val lentCapital: Double = 0.0,
    val availableCapital: Double = 5000000.0
)

@Entity(tableName = "client_proofs")
data class ClientProof(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val title: String, // "Maison", "Commerce", "Contrats", "Garanties"
    val fileUri: String, // Enregistré localement ou simulé
    val addedDate: String
)
