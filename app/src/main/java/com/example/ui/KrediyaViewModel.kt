package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KrediyaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = KrediyaRepository(db)

    // --- ADVANCED LICENSE & SECURITY SUITE ---
    private val prefs = application.getSharedPreferences("krediya_security_prefs", Context.MODE_PRIVATE)

    var isLicensed by mutableStateOf(false)
    var licenseActivationDate by mutableStateOf("")
    var licenseExpirationDate by mutableStateOf("")
    var purchasedDuration by mutableStateOf("")
    var licenseStatusText by mutableStateOf("Non activée")

    // User authentication settings
    var appLockType by mutableStateOf("PIN") // "PIN", "PASSWORD", "FINGERPRINT"
    var userPinCode by mutableStateOf("1234")
    var enteredPassword by mutableStateOf("")
    var selectedAutoLockSeconds by mutableStateOf(60)

    // Hashed credentials
    var adminPasswordHash by mutableStateOf("")

    // Anti-fraud / Clock tamper status
    var isTimeFraudDetected by mutableStateOf(false)
    var lastRecordedSystemTime by mutableStateOf(0L)

    // Current session states
    var activeAdminSession by mutableStateOf(false)
    var lastUserInteractionTime by mutableStateOf(System.currentTimeMillis())

    // UI state
    var currentTab by mutableStateOf("accueil")
    var isUnlocked by mutableStateOf(false) // Sécurité d'accès
    var enteredPin by mutableStateOf("")
    var isDarkMode by mutableStateOf(false) // Mode clair/mode sombre
    var searchClientQuery by mutableStateOf("")
    var selectedClient by mutableStateOf<Client?>(null)
    var selectedLoan by mutableStateOf<Loan?>(null)

    // Calendrier filter
    var calendarFilter by mutableStateOf("Ce mois") // "Aujourd'hui", "Cette semaine", "Ce mois", "En retard"

    // Flows
    val clientsFlow = repository.allClients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val blacklistedFlow = repository.blacklistedClients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val loansFlow = repository.allLoans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val repaymentsFlow = repository.allRepayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val logsFlow = repository.allLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val agentsFlow = repository.allAgents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val capitalFlow = repository.capitalFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CapitalInfo(1, 5000000.0, 0.0, 5000000.0))

    // Form states - Client
    var clientLastName by mutableStateOf("")
    var clientFirstName by mutableStateOf("")
    var clientGender by mutableStateOf("F")
    var clientBirthDate by mutableStateOf("1995-01-01")
    var clientPhone by mutableStateOf("+242 06 ")
    var clientAddress by mutableStateOf("")
    var clientNeighborhood by mutableStateOf("")
    var clientProfession by mutableStateOf("")
    var showAddClientDialog by mutableStateOf(false)
    
    // Optional Identity Documents & Tutorial support
    var clientDocumentationType by mutableStateOf("CNI") // "CNI", "Passeport", "Aucun (Scoring Alternatif)"
    var clientIDCardNumber by mutableStateOf("")
    var clientPassportNumber by mutableStateOf("")
    var showTutorialDialog by mutableStateOf(false)
    var showFloatingGuideTip by mutableStateOf(true)

    // Interactive step-by-step tutorial overlay states
    var isInteractiveTutorialActive by mutableStateOf(false)
    var interactiveTutorialStep by mutableStateOf(0)

    fun startInteractiveTutorial() {
        showTutorialDialog = false
        isInteractiveTutorialActive = true
        interactiveTutorialStep = 0
        currentTab = "accueil"
    }

    fun nextInteractiveTutorialStep() {
        if (interactiveTutorialStep < 5) {
            interactiveTutorialStep++
            // Sync current tab with tutorial step to showcase the correct screen
            when (interactiveTutorialStep) {
                0, 1 -> currentTab = "accueil"
                2 -> currentTab = "clients"
                3 -> currentTab = "prets"
                4 -> currentTab = "echeances"
                5 -> currentTab = "plus"
            }
        } else {
            stopInteractiveTutorial()
            triggerStatus("Guide interactif terminé avec succès ! 🚀 Prêt pour le terrain.")
        }
    }

    fun prevInteractiveTutorialStep() {
        if (interactiveTutorialStep > 0) {
            interactiveTutorialStep--
            when (interactiveTutorialStep) {
                0, 1 -> currentTab = "accueil"
                2 -> currentTab = "clients"
                3 -> currentTab = "prets"
                4 -> currentTab = "echeances"
                5 -> currentTab = "plus"
            }
        }
    }

    fun stopInteractiveTutorial() {
        isInteractiveTutorialActive = false
        interactiveTutorialStep = 0
    }

    // Form states - Loan
    var selectedClientForLoan by mutableStateOf<Client?>(null)
    var loanAmount by mutableStateOf("")
    var loanInterestRate by mutableStateOf("20.0")
    var loanDurationDays by mutableStateOf("30")
    var loanStartDate by mutableStateOf("")
    var showAddLoanDialog by mutableStateOf(false)
    var showLoanConfirmationStep by mutableStateOf(false)

    // Repayment Form states
    var repaymentAmount by mutableStateOf("")
    var repaymentNote by mutableStateOf("")
    var showRepaymentDialog by mutableStateOf(false)

    // Add Agent states
    var agentName by mutableStateOf("")
    var agentRole by mutableStateOf("AGENT_COLLECTOR")
    var agentPhone by mutableStateOf("+242 ")
    var agentEmail by mutableStateOf("")
    var showAddAgentDialog by mutableStateOf(false)

    // Signature Pad
    var isSignatureCaptured by mutableStateOf(false)
    var signatureSignaturePoints by mutableStateOf<List<Pair<Float, Float>>>(emptyList())

    // Proof file path simulation
    var proofsForClient = MutableStateFlow<List<ClientProof>>(emptyList())

    // System status banner notification logs
    var statusMessage by mutableStateOf<String?>(null)

    // Report selection states
    var selectedReportType by mutableStateOf("journalier") // "journalier", "hebdomadaire", "mensuel", "annuel"
    var showReportDialog by mutableStateOf(false)

    // Backup trigger states
    var backupPathMessage by mutableStateOf<String?>(null)

    init {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        loanStartDate = dateFormat.format(Date())

        // Load Advanced Licence Settings offline
        isLicensed = prefs.getBoolean("is_licensed", false)
        licenseActivationDate = prefs.getString("lic_activation_date", "") ?: ""
        licenseExpirationDate = prefs.getString("lic_expiration_date", "") ?: ""
        purchasedDuration = prefs.getString("lic_purchased_duration", "") ?: ""
        updateLicenseStatusText()

        // Load Security profile
        appLockType = prefs.getString("app_lock_type", "PIN") ?: "PIN"
        userPinCode = prefs.getString("user_pin_code", "1234") ?: "1234"
        selectedAutoLockSeconds = prefs.getInt("auto_lock_seconds", 60)

        // Hashed administrators credentials: default code is "admin2026"
        val defaultHash = hashAdminCode("admin2026")
        adminPasswordHash = prefs.getString("admin_pwd_hash", defaultHash) ?: defaultHash
        if (!prefs.contains("admin_pwd_hash")) {
            prefs.edit().putString("admin_pwd_hash", defaultHash).apply()
        }

        // Fraud prevention checks
        lastRecordedSystemTime = prefs.getLong("last_system_time", System.currentTimeMillis())
        checkSystemClockFraud()

        // Start local inactivity checks monitor
        startInactivityTimer()
    }

    fun triggerStatus(msg: String) {
        statusMessage = msg
    }

    fun clearStatus() {
        statusMessage = null
    }

    // --- ENCRYPTION & HASH SECURITY SUITE ---
    fun hashAdminCode(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }

    // Clock fraud tamper guards
    fun checkSystemClockFraud() {
        val curTime = System.currentTimeMillis()
        if (curTime < lastRecordedSystemTime - 10 * 60 * 1000) { // 10 minutes grace window
            isTimeFraudDetected = true
            viewModelScope.launch {
                repository.logActivity(
                    action = "FRAUDE_DATE_DETECTEE",
                    details = "Alerte : Heure système reculée ($curTime) par rapport au dernier enregistrement ($lastRecordedSystemTime). Blocage actif.",
                    agentName = "Sécurité Système"
                )
            }
        } else {
            // Clock is moving forward, update last recorded timestamp safely 
            lastRecordedSystemTime = curTime
            prefs.edit().putLong("last_system_time", curTime).apply()
        }
    }

    // Interactive user interaction monitoring for auto lockouts
    fun recordInteraction() {
        lastUserInteractionTime = System.currentTimeMillis()
    }

    private fun startInactivityTimer() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // check system clock/inactivity every 5 seconds
                checkSystemClockFraud()

                if (isUnlocked && selectedAutoLockSeconds > 0) {
                    val inactiveMs = System.currentTimeMillis() - lastUserInteractionTime
                    if (inactiveMs > selectedAutoLockSeconds * 1000L) {
                        lockApp()
                        repository.logActivity(
                            action = "VERROUILLAGE_INACTIVITE",
                            details = "Déconnexion automatique de l'agent après $selectedAutoLockSeconds secondes d'inactivité.",
                            agentName = "Sécurité Automatique"
                        )
                        triggerStatus("Session déconnectée automatiquement pour inactivité.")
                    }
                }
            }
        }
    }

    // Authentications handlers
    fun handlePinInput(digit: String) {
        if (enteredPin.length < 4) {
            enteredPin += digit
        }
        if (enteredPin.length == 4) {
            if (enteredPin == userPinCode) {
                isUnlocked = true
                enteredPin = ""
                recordInteraction()
                viewModelScope.launch {
                    repository.logActivity("CONNEXION_PIN", "Authentification de l'agent réussie via code PIN.", "Agent Collecteur")
                }
                triggerStatus("Authentification réussie !")
            } else {
                enteredPin = ""
                viewModelScope.launch {
                    repository.logActivity("ECHEC_CONNEXION_PIN", "Tentative d'accès PIN invalide.", "Visiteur")
                }
                triggerStatus("Code PIN incorrect. Réessayez.")
            }
        }
    }

    fun handlePasswordUnlock(pwd: String): Boolean {
        val savedPwd = prefs.getString("user_password_code", "krediya2026") ?: "krediya2026"
        if (pwd == savedPwd) {
            isUnlocked = true
            enteredPassword = ""
            recordInteraction()
            viewModelScope.launch {
                repository.logActivity("CONNEXION_PASSWORD", "Authentification réussie via mot de passe.", "Agent Collecteur")
            }
            triggerStatus("Authentification réussie !")
            return true
        } else {
            enteredPassword = ""
            viewModelScope.launch {
                repository.logActivity("ECHEC_CONNEXION_PASSWORD", "Tentative d'accès mot de passe invalide.", "Visiteur")
            }
            triggerStatus("Mot de passe incorrect.")
            return false
        }
    }

    fun handleBiometricUnlock() {
        isUnlocked = true
        recordInteraction()
        viewModelScope.launch {
            repository.logActivity("CONNEXION_BIOMETRIQUE", "Empreinte digitale validée avec succès.", "Agent Collecteur")
        }
        triggerStatus("Authentification biométrique réussie !")
    }

    fun lockApp() {
        isUnlocked = false
        enteredPin = ""
        enteredPassword = ""
        activeAdminSession = false
    }

    // License operations inside administrator cabinet
    fun updateLicenseStatusText() {
        if (!isLicensed) {
            licenseStatusText = "Application non activée"
            return
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val expDate = sdf.parse(licenseExpirationDate) ?: return
            val curDate = Date()
            if (curDate.after(expDate)) {
                licenseStatusText = "Abonnement expiré depuis le $licenseExpirationDate"
            } else {
                val diffTime = expDate.time - curDate.time
                val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()
                licenseStatusText = "Active (Sera expiré le $licenseExpirationDate • reste $diffDays j)"
            }
        } catch (e: Exception) {
            licenseStatusText = "Licence Activée"
        }
    }

    fun checkLicenseValidity(): Boolean {
        if (!isLicensed) return false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val expDate = sdf.parse(licenseExpirationDate) ?: return false
            val curDate = Date()
            !curDate.after(expDate)
        } catch (e: Exception) {
            false
        }
    }

    fun activateLicense(code: String, months: Int, customDays: Int? = null): Boolean {
        if (hashAdminCode(code) != adminPasswordHash) {
            viewModelScope.launch {
                repository.logActivity(
                    action = "ECHEC_ADMINISTRATEUR",
                    details = "Saisie de clé d'administration incorrecte lors d'une tentative de renouvellement.",
                    agentName = "Inconnu"
                )
            }
            triggerStatus("Code administrateur incorrect.")
            return false
        }

        val cal = Calendar.getInstance()
        val activationDate = cal.time
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val activationStr = sdf.format(activationDate)

        val durationLabel: String
        if (customDays != null) {
            cal.add(Calendar.DAY_OF_YEAR, customDays)
            durationLabel = "Personnalisé ($customDays Jours)"
        } else {
            cal.add(Calendar.MONTH, months)
            durationLabel = "$months Mois"
        }
        val expirationStr = sdf.format(cal.time)

        isLicensed = true
        licenseActivationDate = activationStr
        licenseExpirationDate = expirationStr
        purchasedDuration = durationLabel
        updateLicenseStatusText()

        prefs.edit()
            .putBoolean("is_licensed", true)
            .putString("lic_activation_date", activationStr)
            .putString("lic_expiration_date", expirationStr)
            .putString("lic_purchased_duration", durationLabel)
            .apply()

        viewModelScope.launch {
            repository.logActivity(
                action = "LICENCE_ACTIVEE",
                details = "Licence validée pour $durationLabel (Fin de validité: $expirationStr) par l'administrateur d'agence.",
                agentName = "Administrateur"
            )
        }

        triggerStatus("Licence Krediya activée avec succès pour de nouvelles collectes !")
        return true
    }

    fun suspendLicense() {
        isLicensed = false
        prefs.edit().putBoolean("is_licensed", false).apply()
        updateLicenseStatusText()
        viewModelScope.launch {
            repository.logActivity(
                action = "LICENCE_SUSPENDUE",
                details = "La licence locale de l'application a été désactivée par mesure de contrôle d'agence.",
                agentName = "Administrateur"
            )
        }
        triggerStatus("Licence suspendue par l'administrateur.")
    }

    fun reactivateLicense() {
        isLicensed = true
        prefs.edit().putBoolean("is_licensed", true).apply()
        updateLicenseStatusText()
        viewModelScope.launch {
            repository.logActivity(
                action = "LICENCE_REACTIVEE",
                details = "Licence réactivée et rattachée avec succès.",
                agentName = "Administrateur"
            )
        }
        triggerStatus("Licence réactivée avec succès.")
    }

    fun modifyAdminCode(oldCode: String, newCode: String): Boolean {
        if (hashAdminCode(oldCode) != adminPasswordHash) {
            triggerStatus("Code actuel incorrect. Modification refusée.")
            return false
        }
        if (newCode.length < 4) {
            triggerStatus("Le nouveau code administrateur doit contenir au moins 4 caractères.")
            return false
        }
        val targetHash = hashAdminCode(newCode)
        adminPasswordHash = targetHash
        prefs.edit().putString("admin_pwd_hash", targetHash).apply()

        viewModelScope.launch {
            repository.logActivity(
                action = "HACK_ADMIN_DOUB_MODIF",
                details = "Code secret d'administration générale local réécrit et haché.",
                agentName = "Administrateur"
            )
        }
        triggerStatus("Code administrateur modifié avec succès.")
        return true
    }

    fun resetAdminCodeToDefault() {
        val defaultHash = hashAdminCode("admin2026")
        adminPasswordHash = defaultHash
        prefs.edit().putString("admin_pwd_hash", defaultHash).apply()
        viewModelScope.launch {
            repository.logActivity(
                action = "REINITIALISATION_ADMIN_CODE",
                details = "Le code d'administration principal a été réinitialisé à sa valeur usine ('admin2026').",
                agentName = "Sécurité Interne des Systèmes"
            )
        }
        triggerStatus("Code d'accès administrateur réinitialisé par défaut à 'admin2026'")
    }

    fun updateSecuritySettings(lockType: String, autoLockSec: Int) {
        appLockType = lockType
        selectedAutoLockSeconds = autoLockSec
        prefs.edit()
            .putString("app_lock_type", lockType)
            .putInt("auto_lock_seconds", autoLockSec)
            .apply()
        viewModelScope.launch {
            repository.logActivity(
                action = "PARAMETRE_SECURITE_MIS_A_JOUR",
                details = "Préférences d'accès modifiées : Verrouillage=$lockType, Timeout=$autoLockSec s",
                agentName = "Contrôleur d'Agence"
            )
        }
        triggerStatus("Paramètres de sécurité mis à jour !")
    }

    fun modifyUserPin(oldPin: String, newPin: String): Boolean {
        if (oldPin != userPinCode) {
            triggerStatus("Code PIN actuel invalide.")
            return false
        }
        if (newPin.length != 4 || newPin.toIntOrNull() == null) {
            triggerStatus("Le code PIN doit comporter précisément 4 chiffres.")
            return false
        }
        userPinCode = newPin
        prefs.edit().putString("user_pin_code", newPin).apply()
        viewModelScope.launch {
            repository.logActivity("MODIF_USER_PIN", "Code PIN d'accès de l'agent modifié.", "Agent Collecteur")
        }
        triggerStatus("Code PIN utilisateur mis à jour !")
        return true
    }

    fun modifyUserPassword(oldPwd: String, newPwd: String): Boolean {
        val savedPwd = prefs.getString("user_password_code", "krediya2026") ?: "krediya2026"
        if (oldPwd != savedPwd) {
            triggerStatus("Mot de passe actuel incorrect.")
            return false
        }
        if (newPwd.length < 4) {
            triggerStatus("Le mot de passe doit comporter au moins 4 caractères.")
            return false
        }
        prefs.edit().putString("user_password_code", newPwd).apply()
        viewModelScope.launch {
            repository.logActivity("MODIF_USER_PASSWORD", "Mot de passe principal utilisateur modifié.", "Agent Collecteur")
        }
        triggerStatus("Mot de passe utilisateur enregistré !")
        return true
    }

    fun adminResolveTimeFraud(code: String): Boolean {
        if (hashAdminCode(code) == adminPasswordHash) {
            isTimeFraudDetected = false
            lastRecordedSystemTime = System.currentTimeMillis()
            prefs.edit().putLong("last_system_time", lastRecordedSystemTime).apply()
            viewModelScope.launch {
                repository.logActivity(
                    action = "CONTESTATION_FRAUDE_APPROUVEE",
                    details = "Déblocage de la sécurité anti-fraude de date autorisé via mot de passe d'administration.",
                    agentName = "Administrateur"
                )
            }
            triggerStatus("Protection anti-fraude réactivée. Heure resynchronisée.")
            return true
        } else {
            triggerStatus("Code administrateur incorrect. Déblocage refusé.")
            return false
        }
    }

    // Add Client
    fun saveNewClient() {
        if (clientLastName.isBlank() || clientFirstName.isBlank() || clientPhone.isBlank()) {
            triggerStatus("Veuillez remplir les informations obligatoires.")
            return
        }
        viewModelScope.launch {
            val hasNoDocs = clientDocumentationType == "Aucun (Scoring Alternatif)"
            val newC = Client(
                lastName = clientLastName,
                firstName = clientFirstName,
                gender = clientGender,
                birthDate = clientBirthDate,
                phone = clientPhone,
                address = clientAddress,
                neighborhood = clientNeighborhood,
                profession = clientProfession,
                photoUri = if (hasNoDocs) "SANS_DOCUMENT" else "PIECE_FOURNIE",
                idCardUri = if (clientDocumentationType == "CNI" && clientIDCardNumber.isNotBlank()) clientIDCardNumber else null,
                passportUri = if (clientDocumentationType == "Passeport" && clientPassportNumber.isNotBlank()) clientPassportNumber else null,
                score = if (hasNoDocs) 82 else 95, // alternative scoring trust index slightly updated (82% vs 95%)
                agentId = 1
            )
            repository.insertClient(newC)
            
            // Add custom activity log details
            repository.logActivity(
                action = "CREATION_CLIENT",
                details = "Fiche client ${newC.fullName} enregistrée " + 
                         if (hasNoDocs) "(Scoring Alternatif activé, sans document)" 
                         else "avec pièce d'identité $clientDocumentationType",
                agentName = "Alphonse Ngolo"
            )

            triggerStatus(
                if (hasNoDocs) "Nouveau client ${newC.fullName} créé sans pièces (Scoring Alternatif activé) !"
                else "Nouveau client ${newC.fullName} enregistré avec pièce de type $clientDocumentationType !"
            )
            resetClientForm()
        }
    }

    private fun resetClientForm() {
        clientLastName = ""
        clientFirstName = ""
        clientGender = "F"
        clientBirthDate = "1995-01-01"
        clientPhone = "+242 06 "
        clientAddress = ""
        clientNeighborhood = ""
        clientProfession = ""
        clientDocumentationType = "CNI"
        clientIDCardNumber = ""
        clientPassportNumber = ""
        showAddClientDialog = false
    }

    // Add Agent
    fun saveNewAgent() {
        if (agentName.isBlank()) {
            triggerStatus("Veuillez saisir le nom de l'agent.")
            return
        }
        viewModelScope.launch {
            val agent = Agent(
                name = agentName,
                role = agentRole,
                phone = agentPhone,
                email = agentEmail.ifBlank { null }
            )
            repository.insertAgent(agent)
            agentName = ""
            agentPhone = "+242 "
            agentEmail = ""
            showAddAgentDialog = false
            triggerStatus("Nouvel agent $agentName enregistré avec succès !")
        }
    }

    fun toggleBlacklistClient(client: Client, reason: String?) {
        viewModelScope.launch {
            repository.toggleBlacklist(client.id, !client.isBlacklisted, reason)
            selectedClient = repository.getClientById(client.id)
            triggerStatus(if (!client.isBlacklisted) "${client.fullName} a été mis sur liste noire." else "${client.fullName} a été réhabilité.")
        }
    }

    // Loan processing & capital validation
    fun calculateLoanPreview(): LoanPreview {
        val amount = loanAmount.toDoubleOrNull() ?: 0.0
        val rate = loanInterestRate.toDoubleOrNull() ?: 0.0
        val days = loanDurationDays.toIntOrNull() ?: 30

        val interest = (amount * rate) / 100.0
        val total = amount + interest

        val calendar = Calendar.getInstance()
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = format.parse(loanStartDate) ?: Date()
            calendar.time = date
        } catch (e: Exception) {
            // fallback
        }
        calendar.add(Calendar.DAY_OF_YEAR, days)
        val formatOut = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val limitDateStr = formatOut.format(calendar.time)

        return LoanPreview(amount, interest, total, limitDateStr)
    }

    fun submitLoanDisbursement() {
        val client = selectedClientForLoan ?: return
        if (client.isBlacklisted) {
            triggerStatus("Action annulée : Ce client est sur la liste noire !")
            return
        }

        val amount = loanAmount.toDoubleOrNull() ?: 0.0
        val interestRate = loanInterestRate.toDoubleOrNull() ?: 20.0
        val durationDays = loanDurationDays.toIntOrNull() ?: 30

        if (amount <= 0.0) {
            triggerStatus("Montant invalide.")
            return
        }

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val curDate = format.format(Date())

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, durationDays)
        val limitDate = format.format(calendar.time)

        viewModelScope.launch {
            val success = repository.disburseLoan(
                clientId = client.id,
                amount = amount,
                interestRate = interestRate,
                durationDays = durationDays,
                startDate = curDate,
                dueDate = limitDate
            )

            if (success) {
                triggerStatus("Prêt de ${amount.toInt()} FCFA validé et déboursé !")
                showAddLoanDialog = false
                showLoanConfirmationStep = false
                loanAmount = ""
                selectedClientForLoan = null
            } else {
                triggerStatus("Échec du déboursement. Vérifiez le capital disponible.")
            }
        }
    }

    // Repayment logic
    fun submitRepayment() {
        val loan = selectedLoan ?: return
        val amount = repaymentAmount.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) {
            triggerStatus("Montant invalide.")
            return
        }

        viewModelScope.launch {
            val success = repository.recordRepayment(loan.id, amount, repaymentNote.ifBlank { null })
            if (success) {
                triggerStatus("Remboursement de ${amount.toInt()} FCFA validé avec succès !")
                showRepaymentDialog = false
                repaymentAmount = ""
                repaymentNote = ""
                // Refresh loan detail
                selectedLoan = repository.getLoanById(loan.id)
                selectedClient?.let {
                    selectedClient = repository.getClientById(it.id)
                }
            } else {
                triggerStatus("Erreur interne lors du remboursement.")
            }
        }
    }

    // Capital base modifications
    fun updateCapitalPool(total: Double) {
        viewModelScope.launch {
            repository.initCapital(total)
            triggerStatus("Capital d'exploitation Krediya configuré à ${total.toInt()} FCFA")
        }
    }

    // Proof file attachments
    fun fetchProofs(clientId: Int) {
        viewModelScope.launch {
            repository.getProofsForClient(clientId).collect {
                proofsForClient.value = it
            }
        }
    }

    fun addMockProof(clientId: Int, title: String) {
        viewModelScope.launch {
            repository.insertProof(clientId, title, "krediya://evidence_${title.lowercase()}_file")
            fetchProofs(clientId)
            triggerStatus("Document '$title' ajouté et sauvegardé en sécurité.")
        }
    }

    fun clearFingerSignature() {
        signatureSignaturePoints = emptyList()
        isSignatureCaptured = false
    }

    fun saveFingerSignature(loanId: Int) {
        viewModelScope.launch {
            val loan = repository.getLoanById(loanId)
            if (loan != null) {
                val updated = loan.copy(signatureData = "SIGNATURE_CAPTURED_${System.currentTimeMillis()}")
                db.loanDao().updateLoan(updated)
                selectedLoan = updated
                isSignatureCaptured = true
                triggerStatus("Contrat signé au doigt et archivé en preuve légale.")
            }
        }
    }

    // Backup
    fun executeLocalBackup(context: Context) {
        viewModelScope.launch {
            val path = repository.exportSampleBackup(context)
            backupPathMessage = "Fichier exporté: $path\nMode: Sauvegarde automatique prête."
            triggerStatus("Sauvegarde 'krediya_backup.db' exportée dans le stockage sécurisé.")
        }
    }

    // Analytics / Forecasting formulas
    fun getForecastingStats(loans: List<Loan>): ForecastingStats {
        val curDate = Date()
        val calendar = Calendar.getInstance()

        calendar.time = curDate
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val endOfThisWeek = calendar.time

        calendar.time = curDate
        calendar.add(Calendar.MONTH, 1)
        val endOfThisMonth = calendar.time

        calendar.time = curDate
        calendar.add(Calendar.MONTH, 3)
        val endOfThisQuarter = calendar.time

        var expectedThisWeek = 0.0
        var expectedThisMonth = 0.0
        var expectedThisQuarter = 0.0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (l in loans) {
            if (l.status != "REPAID") {
                try {
                    val due = sdf.parse(l.dueDate) ?: continue
                    val outstanding = l.remainingAmount

                    if (due.before(endOfThisWeek) || due.equals(endOfThisWeek)) {
                        expectedThisWeek += outstanding
                    }
                    if (due.before(endOfThisMonth) || due.equals(endOfThisMonth)) {
                        expectedThisMonth += outstanding
                    }
                    if (due.before(endOfThisQuarter) || due.equals(endOfThisQuarter)) {
                        expectedThisQuarter += outstanding
                    }
                } catch (e: Exception) {
                    // skip
                }
            }
        }

        return ForecastingStats(expectedThisWeek, expectedThisMonth, expectedThisQuarter)
    }

    data class LoanPreview(val principal: Double, val interest: Double, val total: Double, val limitDateString: String)
    data class ForecastingStats(val week: Double, val month: Double, val quarter: Double)
}
