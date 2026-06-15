package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KrediyaApp(viewModel: KrediyaViewModel) {
    val context = LocalContext.current
    val clients by viewModel.clientsFlow.collectAsStateWithLifecycle()
    val blacklisted by viewModel.blacklistedFlow.collectAsStateWithLifecycle()
    val loans by viewModel.loansFlow.collectAsStateWithLifecycle()
    val repayments by viewModel.repaymentsFlow.collectAsStateWithLifecycle()
    val logs by viewModel.logsFlow.collectAsStateWithLifecycle()
    val agents by viewModel.agentsFlow.collectAsStateWithLifecycle()
    val capital by viewModel.capitalFlow.collectAsStateWithLifecycle()

    MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (viewModel.isTimeFraudDetected) {
                // Front Tamper / Clock Cheat Protection Shield
                TimeFraudBlockScreen(viewModel = viewModel)
            } else if (!viewModel.checkLicenseValidity()) {
                // Licensing Guard Access Screen (Block Bypass)
                LicenseBlockRenewalScreen(viewModel = viewModel)
            } else if (!viewModel.isUnlocked) {
                // Multi-Option Security Authentication Gate
                AppSecurityLockGate(viewModel = viewModel)
            } else {
                // Main Application Scaffold with Interaction Tracking for Auto-Lockout
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                    viewModel.recordInteraction()
                                }
                            }
                        }
                ) {
                    Scaffold(
                    topBar = {
                        KrediyaTopAppBar(
                            isDarkMode = viewModel.isDarkMode,
                            onThemeChanged = { viewModel.isDarkMode = it },
                            statusMessage = viewModel.statusMessage,
                            onDismissStatus = { viewModel.clearStatus() },
                            onLockClick = { viewModel.lockApp() },
                            onTutorialClick = { viewModel.showTutorialDialog = true }
                        )
                    },
                    bottomBar = {
                        KrediyaBottomNavigation(
                            currentTab = viewModel.currentTab,
                            onTabSelected = { viewModel.currentTab = it }
                        )
                    },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (viewModel.currentTab) {
                            "accueil" -> AccueilScreen(
                                clients = clients,
                                loans = loans,
                                repayments = repayments,
                                capital = capital ?: CapitalInfo(1),
                                onActionNavigate = { tab -> viewModel.currentTab = tab },
                                onAddNewClientClick = { viewModel.showAddClientDialog = true },
                                onAddNewLoanClick = { viewModel.showAddLoanDialog = true },
                                onAddRepaymentClick = { viewModel.showRepaymentDialog = true },
                                onGenerateReportsClick = { viewModel.showReportDialog = true },
                                isDarkMode = viewModel.isDarkMode,
                                viewModel = viewModel
                            )
                            "clients" -> ClientsScreen(
                                viewModel = viewModel,
                                clients = clients,
                                blacklisted = blacklisted,
                                loans = loans
                            )
                            "prets" -> LoansScreen(
                                viewModel = viewModel,
                                clients = clients,
                                loans = loans,
                                repayments = repayments
                            )
                            "echeances" -> EcheancesScreen(
                                viewModel = viewModel,
                                loans = loans,
                                clients = clients
                            )
                            "plus" -> PlusScreen(
                                viewModel = viewModel,
                                loans = loans,
                                clients = clients,
                                repayments = repayments,
                                logs = logs,
                                agents = agents,
                                capital = capital ?: CapitalInfo(1)
                            )
                        }

                        // Floating Guide Interface (interactive helper badge)
                        KrediyaFloatingGuide(
                            viewModel = viewModel,
                            currentTab = viewModel.currentTab
                        )

                        // 6. Live Interactive Step-by-Step Tutorial Overlay
                        if (viewModel.isInteractiveTutorialActive) {
                            KrediyaInteractiveTutorialOverlay(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

        // --- GLOBAL DIALOGS ---

        // 1. Add Client Dialog
        if (viewModel.showAddClientDialog) {
            AddClientDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.showAddClientDialog = false },
                onSave = {
                    viewModel.saveNewClient()
                    viewModel.showAddClientDialog = false
                }
            )
        }

        // 2. Add Loan Dialog
        if (viewModel.showAddLoanDialog) {
            AddLoanDialog(
                viewModel = viewModel,
                clients = clients,
                capital = capital ?: CapitalInfo(1),
                onDismiss = {
                    viewModel.showAddLoanDialog = false
                    viewModel.showLoanConfirmationStep = false
                }
            )
        }

        // 3. Add Repayment Dialog
        if (viewModel.showRepaymentDialog) {
            AddRepaymentDialog(
                viewModel = viewModel,
                loans = loans,
                clients = clients,
                onDismiss = { viewModel.showRepaymentDialog = false }
            )
        }

        // 4. Report PDF Generation Dialog
        if (viewModel.showReportDialog) {
            PdfReportDialog(
                viewModel = viewModel,
                loans = loans,
                clients = clients,
                repayments = repayments,
                capital = capital ?: CapitalInfo(1),
                onDismiss = { viewModel.showReportDialog = false }
            )
        }

        // 5. Onboarding Step-by-Step Tutorial Dialog
        if (viewModel.showTutorialDialog) {
            KrediyaTutorialDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.showTutorialDialog = false }
            )
        }
    }
}

// FORMAT HELPER
fun formatFCFA(amount: Double): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.FRANCE)
    return "${formatter.format(amount.toInt())} FCFA"
}

// --- COMPREHENSIVE SECURITY GATE SUITE ---
@Composable
fun AppSecurityLockGate(viewModel: KrediyaViewModel) {
    var enteredPasswordLocal by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlueBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        
        // Consistent Brand Emblem Logo
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_krediya_logo_vector),
            contentDescription = "Logo Krediya Officiel",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, EmeraldPrimaryDark, RoundedCornerShape(24.dp))
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "KREDIYA",
            color = EmeraldPrimaryDark,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 4.sp
        )
        Text(
            text = "Sécurisé • Microfinance d’Afrique",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Security Mode Tabs / Indicators
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf("PIN", "PASSWORD", "FINGERPRINT")
            modes.forEach { mode ->
                val selected = viewModel.appLockType == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) EmeraldPrimary else Color.Transparent)
                        .clickable { viewModel.appLockType = mode }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mode,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.statusMessage != null) {
            Text(
                text = viewModel.statusMessage ?: "",
                color = AlertYellow,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { viewModel.clearStatus() }
            )
        }

        when (viewModel.appLockType) {
            "PIN" -> {
                Text(
                    text = "Saisissez votre code PIN d'accès",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "(PIN par défaut: ${viewModel.userPinCode})",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (viewModel.enteredPin.length >= i) EmeraldPrimaryDark else Color.White.copy(alpha = 0.15f)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dial Keys
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.widthIn(max = 240.dp)
                ) {
                    for (row in keys) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (key in row) {
                                if (key.isEmpty()) {
                                    Box(modifier = Modifier.weight(1f))
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            if (key == "DEL") {
                                                if (viewModel.enteredPin.isNotEmpty()) {
                                                    viewModel.enteredPin = viewModel.enteredPin.dropLast(1)
                                                }
                                            } else {
                                                viewModel.handlePinInput(key)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.3f)
                                            .testTag("pin_key_$key"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = if (key == "DEL") 12.sp else 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "PASSWORD" -> {
                Text(
                    text = "Authentification par mot de passe",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "(Défaut: krediya2026)",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = enteredPasswordLocal,
                    onValueChange = { enteredPasswordLocal = it },
                    label = { Text("Mot de passe", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("password_input_gate"),
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = "Toggle visibility", tint = Color.White)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val ok = viewModel.handlePasswordUnlock(enteredPasswordLocal)
                        if (ok) enteredPasswordLocal = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("password_submit_gate"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Valider la connexion", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            "FINGERPRINT" -> {
                Text(
                    text = "Verrouillage Biométrique Actif",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "Touchez le capteur digital ou cliquez ci-dessous pour simuler l'empreinte de l'agent.",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Pulsing Scan Touch Target
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.15f))
                        .border(2.dp, EmeraldPrimary, CircleShape)
                        .clickable { viewModel.handleBiometricUnlock() }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Simuler Empreinte Digitale",
                        tint = EmeraldPrimaryDark,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                
                Text(
                    text = "模擬指紋 (Simulation standard de test)",
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun TimeFraudBlockScreen(viewModel: KrediyaViewModel) {
    var adminCodeInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // High-contrast deep alert slate (Navy dark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Fraude Alerte Sécurité",
            tint = Color(0xFFDC2626), // Bold Warning Crimson
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ALERTE DE SECURITE MAJEURE",
            color = Color(0xFFEF4444),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "SÉCURITÉ ANTI-FRAUDE ACTIVE",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Alerte de falsification temporelle :\nUne modification non autorisée de l'horloge système de l'appareil a été détectée sur cet agent.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Afin de préserver la traçabilité chiffrée, l'accès aux microcrédits et encaissements est suspendu.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.statusMessage != null) {
            Text(
                text = viewModel.statusMessage ?: "",
                color = AlertYellow,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Text(
            text = "DÉVERROUILLAGE ADMINISTRATEUR REQUIS",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = adminCodeInput,
            onValueChange = { adminCodeInput = it },
            label = { Text("Mot de passe administrateur", color = Color.White.copy(alpha = 0.6f)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFDC2626),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth().testTag("fraud_admin_input"),
            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = icon, contentDescription = "Toggle", tint = Color.White)
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val success = viewModel.adminResolveTimeFraud(adminCodeInput)
                if (success) adminCodeInput = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("fraud_submit_btn")
        ) {
            Text("VÉRIFIER ET FORCE-RESTART AUTOMATIQUE", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun LicenseBlockRenewalScreen(viewModel: KrediyaViewModel) {
    val context = LocalContext.current
    var adminCodeInput by remember { mutableStateOf("") }
    var selectedDurationMonths by remember { mutableStateOf(1) } // 1, 3, 6, 12, or custom (0)
    var customDaysInput by remember { mutableStateOf("") }
    var showInspectorControl by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlueBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Brand emblem logo
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_krediya_logo_vector),
            contentDescription = "Logo Krediya Officiel",
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, EmeraldPrimary, RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "KREDIYA CONTROLES",
            color = EmeraldPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)), // Dark Crimson
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockClock,
                    contentDescription = "Licence Bloquée",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Abonnement Expiré de l'App",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Votre licence locale a expiré. Pour des raisons d'audit comptable et budgétaire, l'accès d'agence est suspendu.",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Helpline and instructions
        Text(
            text = "COMMENT RE-ACTIVER VOTRE APPLICATION ?",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1.", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Column {
                        Text("Appelez votre superviseur local d'agence d'épargne.", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Direct call buttons using Real Intents
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+242065971234"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Appeler (+242 06 597 1234)", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2.", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Column {
                        Text("L'administrateur inspecteur validera l'abonnement.", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text("Le système fonctionne 100% hors ligne sans connexion internet requise.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Expandable Inspector Renew Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .clickable { showInspectorControl = !showInspectorControl }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = "Admin Area", tint = EmeraldPrimary)
                Text("🔑 ESPACE DES INSPECTEURS D'AGENCE", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Icon(
                imageVector = if (showInspectorControl) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = Color.White
            )
        }

        if (showInspectorControl) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Saisir Clé d’Activation Directe",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = adminCodeInput,
                    onValueChange = { adminCodeInput = it },
                    label = { Text("Code d'administration", color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("license_admin_pwd_gate"),
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = "Toggle", tint = Color.White)
                        }
                    }
                )

                Text(
                    text = "Sélectionner durée d'abonnement :",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall
                )

                // Select duration layout chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val durations = listOf(
                        Pair("1 Mois", 1),
                        Pair("3 Mois", 3),
                        Pair("6 Mois", 6),
                        Pair("12 Mois", 12),
                        Pair("Perso.", 0)
                    )
                    durations.forEach { pair ->
                        val selected = selectedDurationMonths == pair.second
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) EmeraldPrimary else Color.White.copy(alpha = 0.06f))
                                .clickable { selectedDurationMonths = pair.second }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pair.first,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (selectedDurationMonths == 0) {
                    OutlinedTextField(
                        value = customDaysInput,
                        onValueChange = { customDaysInput = it },
                        label = { Text("Durée personnalisée (Jours)", color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("license_custom_days_gate")
                    )
                }

                if (viewModel.statusMessage != null) {
                    Text(
                        text = viewModel.statusMessage ?: "",
                        color = AlertYellow,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        val custDays = if (selectedDurationMonths == 0) customDaysInput.toIntOrNull() else null
                        if (selectedDurationMonths == 0 && custDays == null) {
                            viewModel.triggerStatus("Veuillez entrer un nombre de jours valide.")
                            return@Button
                        }
                        
                        val ok = viewModel.activateLicense(
                            code = adminCodeInput,
                            months = selectedDurationMonths,
                            customDays = custDays
                        )
                        if (ok) {
                            adminCodeInput = ""
                            customDaysInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("license_submit_gate_btn")
                ) {
                    Text("VALIDER ET ACTIVER LA LICENCE HORS-LIGNE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- ORIGINAL TOP APP BAR ACCENTS ---

// --- CUSTOM TOP APP BAR ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KrediyaTopAppBar(
    isDarkMode: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    statusMessage: String?,
    onDismissStatus: () -> Unit,
    onLockClick: () -> Unit,
    onTutorialClick: () -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_krediya_logo_vector),
                        contentDescription = "Logo Krediya Vector",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Krediya",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isDarkMode) Color.White else NavyBlueSecondary,
                        letterSpacing = 1.sp
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            actions = {
                IconButton(onClick = onTutorialClick) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Guide & Tutoriel",
                        tint = EmeraldPrimary
                    )
                }
                IconButton(onClick = { onThemeChanged(!isDarkMode) }) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Thème"
                    )
                }
                IconButton(onClick = onLockClick) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Verrouiller",
                        tint = AlertRed
                    )
                }
            }
        )

        // SnackBar-like status notifications
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            statusMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { onDismissStatus() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- BOTTOM NAVIGATION BAR ---
@Composable
fun KrediyaBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        val items = listOf(
            Triple("accueil", "Accueil", Icons.Default.Home),
            Triple("clients", "Clients", Icons.Default.Person),
            Triple("prets", "Prêts", Icons.Default.AttachMoney),
            Triple("echeances", "Échéances", Icons.Default.DateRange),
            Triple("plus", "Plus", Icons.Default.Settings)
        )

        items.forEach { (tab, label, icon) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldPrimary,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldLight
                ),
                modifier = Modifier.testTag("nav_btn_$tab")
            )
        }
    }
}

// ==================== SCREEN 1: ACCUEIL (DASHBOARD) ====================
@Composable
fun AccueilScreen(
    clients: List<Client>,
    loans: List<Loan>,
    repayments: List<Repayment>,
    capital: CapitalInfo,
    onActionNavigate: (String) -> Unit,
    onAddNewClientClick: () -> Unit,
    onAddNewLoanClick: () -> Unit,
    onAddRepaymentClick: () -> Unit,
    onGenerateReportsClick: () -> Unit,
    isDarkMode: Boolean,
    viewModel: KrediyaViewModel
) {
    // Calcul de statistiques
    val activeLoans = loans.filter { it.status == "ACTIVE" }
    val overdueLoans = loans.filter { it.status == "OVERDUE" }
    
    val totalLended = loans.sumOf { it.amount }
    val totalRecoverable = loans.sumOf { it.totalRepayable }
    val totalRecovered = loans.sumOf { it.totalPaid }
    val expectedProfit = loans.sumOf { it.interestAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Intelligent Alerts Panel
        item {
            IntelligentAlertsPanel(
                clients = clients,
                loans = loans,
                viewModel = viewModel,
                onNavigate = onActionNavigate
            )
        }

        // Welcoming & Capital Pool overview
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) NavyBlueSurface else EmeraldLight
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Capital d'Exploitation",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else EmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatFCFA(capital.totalCapital),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkMode) EmeraldPrimaryDark else EmeraldDark
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Prêté",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else NavyBlueSecondary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatFCFA(capital.lentCapital),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else NavyBlueSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Disponible",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else NavyBlueSecondary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatFCFA(capital.availableCapital),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        // Stats summary cards grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Prêté",
                    value = formatFCFA(totalLended),
                    iconColor = EmeraldPrimary
                )
                StatsMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Récupéré",
                    value = formatFCFA(totalRecovered),
                    iconColor = AlertGreen
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Bénéfice Attendu",
                    value = formatFCFA(expectedProfit),
                    iconColor = AlertYellow
                )
                StatsMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Portefeuille Clients",
                    value = "${clients.size} Clients",
                    iconColor = NavyBlueSecondary
                )
            }
        }

        // Alerts box (item #1)
        item {
            Column {
                Text(
                    text = "Alertes & Priorités du jour",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AlertBadgeRow(
                            iconColor = AlertRed,
                            text = "${overdueLoans.size} clients en retard (En attente d'action)",
                            badgeColor = AlertRed.copy(alpha = 0.15f),
                            textColor = AlertRed,
                            onClick = { onActionNavigate("plus") } // jump to Recovery Relance inside Plus Screen
                        )
                        AlertBadgeRow(
                            iconColor = AlertYellow,
                            text = "5 échéances aujourd'hui (Vérifiez les paiements attendus)",
                            badgeColor = AlertYellow.copy(alpha = 0.15f),
                            textColor = AlertYellow,
                            onClick = { onActionNavigate("echeances") }
                        )
                        AlertBadgeRow(
                            iconColor = AlertGreen,
                            text = "${repayments.size} remboursements historiques enregistrés",
                            badgeColor = AlertGreen.copy(alpha = 0.15f),
                            textColor = AlertGreen,
                            onClick = { onActionNavigate("accueil") }
                        )
                    }
                }
            }
        }

        // Action rapides (item #1)
        item {
            Column {
                Text(
                    text = "Actions Rapides",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        label = "Nouveau Client",
                        icon = Icons.Default.PersonAdd,
                        onClick = onAddNewClientClick
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        label = "Nouveau Prêt",
                        icon = Icons.Default.AddBusiness,
                        onClick = onAddNewLoanClick
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        label = "Remboursement",
                        icon = Icons.Default.Payments,
                        onClick = onAddRepaymentClick
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        label = "Rapports",
                        icon = Icons.Default.BarChart,
                        onClick = onGenerateReportsClick
                    )
                }
            }
        }

        // Interactive custom painted charts (item #9)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Indice de Recouvrement (Comparatif)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vert: Récupéré | Gris: Prêté",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Draw a customized graphical chart with Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val totalFloat = totalLended.toFloat().coerceAtLeast(1f)
                            val recFloat = totalRecovered.toFloat()

                            val ratio = recFloat / totalFloat
                            val barWidth = 100f
                            val startX1 = size.width / 4f - barWidth / 2f
                            val startX2 = (size.width * 3f / 4f) - barWidth / 2f

                            // Draw Bar 1: Total Prêté (Grey)
                            drawRect(
                                color = Color.Gray.copy(alpha = 0.3f),
                                topLeft = Offset(startX1, 0f),
                                size = Size(barWidth, size.height)
                            )
                            // Draw Bar 1 Active Segment (Primary Navy Blue)
                            val navyHeight = size.height * 0.7f
                            drawRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(startX1, size.height - navyHeight),
                                size = Size(barWidth, navyHeight)
                            )

                            // Draw Bar 2: Total Recovered (Emerald Green representation)
                            drawRect(
                                color = Color.Gray.copy(alpha = 0.3f),
                                topLeft = Offset(startX2, 0f),
                                size = Size(barWidth, size.height)
                            )
                            val recHeight = size.height * (0.7f * ratio).coerceAtMost(1f)
                            drawRect(
                                color = Color(0xFF10B981),
                                topLeft = Offset(startX2, size.height - recHeight),
                                size = Size(barWidth, recHeight)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("Volume Prêts", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Volume Recouvrement", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatsMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(iconColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AlertBadgeRow(
    iconColor: Color,
    text: String,
    badgeColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(badgeColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(iconColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowForward,
            contentDescription = "Aller",
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = EmeraldPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==================== SCREEN 2: CLIENTS ====================
@Composable
fun ClientsScreen(
    viewModel: KrediyaViewModel,
    clients: List<Client>,
    blacklisted: List<Client>,
    loans: List<Loan>
) {
    var clientScreenTab by remember { mutableStateOf("all") } // "all", "top", "blacklist"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Add Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.searchClientQuery,
                onValueChange = { viewModel.searchClientQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("client_search_input"),
                placeholder = { Text("Rechercher un client...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            FloatingActionButton(
                onClick = { viewModel.showAddClientDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Créer Client")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories selector tabs (item #16 Classement, #17 Liste noire)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categorizers = listOf(
                "all" to "Tous",
                "top" to "★ Classement Top",
                "blacklist" to "🚫 Liste Noire"
            )
            categorizers.forEach { (tab, label) ->
                val isSel = clientScreenTab == tab
                Button(
                    onClick = { clientScreenTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Client Details Card overlay if selected
        if (viewModel.selectedClient != null) {
            ClientDetailsCard(
                client = viewModel.selectedClient!!,
                loans = loans,
                viewModel = viewModel,
                onClose = { viewModel.selectedClient = null }
            )
        } else {
            // Apply filtering logic
            val queried = clients.filter {
                it.fullName.contains(viewModel.searchClientQuery, ignoreCase = true) ||
                it.phone.contains(viewModel.searchClientQuery) ||
                it.neighborhood.contains(viewModel.searchClientQuery, ignoreCase = true)
            }

            val finalClientsList = when (clientScreenTab) {
                "top" -> queried.sortedByDescending { it.score }
                "blacklist" -> queried.filter { it.isBlacklisted }
                else -> queried.filter { !it.isBlacklisted }
            }

            if (finalClientsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun client trouvé.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(finalClientsList) { client ->
                        ClientRowItem(
                            client = client,
                            onClick = {
                                viewModel.selectedClient = client
                                viewModel.fetchProofs(client.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClientRowItem(client: Client, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${client.profession} • ${client.neighborhood}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = client.phone,
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Client score visual metric (badge & color representation)
            val badgeColor = when (client.scoreBadge) {
                "Excellent" -> AlertGreen
                "Bon" -> EmeraldPrimary
                "Moyen" -> AlertYellow
                else -> AlertRed
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${client.score}/100",
                        color = badgeColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = client.scoreBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- CLIENT DETAILS COMPOSABLE ---
@Composable
fun ClientDetailsCard(
    client: Client,
    loans: List<Loan>,
    viewModel: KrediyaViewModel,
    onClose: () -> Unit
) {
    val clientActiveLoans = loans.filter { it.clientId == client.id }
    val proofs by viewModel.proofsForClient.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Detail Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dossier Client",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }
        }

        // Profile details summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Nom complet : ${client.fullName}", fontWeight = FontWeight.Bold)
                    Text(text = "Téléphone : ${client.phone}", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    Text(text = "Sexe : ${if (client.gender == "M") "Homme" else "Femme"}")
                    Text(text = "Né(e) le : ${client.birthDate}")
                    Text(text = "Adresse : ${client.address}")
                    Text(text = "Quartier : ${client.neighborhood}")
                    Text(text = "Profession : ${client.profession}")

                    if (client.isBlacklisted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AlertRed.copy(alpha = 0.15f))
                                .border(1.dp, AlertRed, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Client sur liste noire : \n${client.blacklistReason ?: "Aucun motif spécifié"}",
                                color = AlertRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Krediya Score panel with progress circle (item #2)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color.Gray.copy(alpha = 0.2f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx())
                            )
                            drawArc(
                                color = if (client.score >= 75) Color(0xFF10B981) else Color(0xFFEF4444),
                                startAngle = -90f,
                                sweepAngle = (client.score * 3.6f).toFloat(),
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${client.score}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column {
                        Text(
                            text = "Score Krediya : ${client.score}/100",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Badge de fiabilité : ${client.scoreBadge}",
                            color = if (client.score >= 75) EmeraldPrimary else AlertRed,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Interactive management actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.selectedClientForLoan = client
                        viewModel.showAddLoanDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Créer Prêt", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.toggleBlacklistClient(client, "Mis sur liste noire manuellement")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (client.isBlacklisted) AlertGreen else AlertRed
                    )
                ) {
                    Text(if (client.isBlacklisted) "Réhabiliter" else "Liste Noire", fontSize = 12.sp)
                }
            }
        }

        // Gallery of Proofs (item #12)
        item {
            Column {
                Text(
                    text = "Galerie des Garanties & Preuves",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { viewModel.addMockProof(client.id, "Maison") }) {
                        Text("+ Photo Maison", fontSize = 10.sp)
                    }
                    Button(onClick = { viewModel.addMockProof(client.id, "Commerce") }) {
                        Text("+ Photo Commerce", fontSize = 10.sp)
                    }
                    Button(onClick = { viewModel.addMockProof(client.id, "CNI_Garantie") }) {
                        Text("+ Photo CNI", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (proofs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune preuve enregistrée.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(proofs) { proof ->
                            Card(
                                modifier = Modifier.size(100.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Attachment,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = proof.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = proof.addedDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Historical loans list for client
        item {
            Column {
                Text(
                    text = "Historique d'activité de prêts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (clientActiveLoans.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun prêt enregistré.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (l in clientActiveLoans) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "ID Prêt : ${l.id}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = formatFCFA(l.amount),
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Status: ${l.status}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (l.status == "REPAID") AlertGreen else AlertYellow
                                        )
                                        Text(
                                            text = "Jusqu'au: ${l.dueDate}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==================== SCREEN 3: PRÊTS ====================
@Composable
fun LoansScreen(
    viewModel: KrediyaViewModel,
    clients: List<Client>,
    loans: List<Loan>,
    repayments: List<Repayment>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Screen Header details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Suivi des Prêts Actifs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { viewModel.showAddLoanDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nouveau", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.selectedLoan != null) {
            // Selected loan detail card with Signature & repayment history!
            LoanDetailCard(
                loan = viewModel.selectedLoan!!,
                clients = clients,
                repayments = repayments,
                viewModel = viewModel,
                onClose = { viewModel.selectedLoan = null }
            )
        } else {
            // Main Loans List
            if (loans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun prêt actif pour le moment.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(loans) { loan ->
                        val client = clients.find { it.id == loan.clientId }
                        val clientName = client?.fullName ?: "Client Inconnu"

                        LoanCompactRowItem(
                            loan = loan,
                            clientName = clientName,
                            onClick = { viewModel.selectedLoan = loan }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoanCompactRowItem(loan: Loan, clientName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clientName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                // Status badge
                val statusColor = when (loan.status) {
                    "REPAID" -> AlertGreen
                    "OVERDUE" -> AlertRed
                    "NEAR_DUE" -> AlertYellow
                    else -> EmeraldPrimary
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = loan.status,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Remboursé", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = "${loan.totalPaid.toInt()} / ${loan.totalRepayable.toInt()} FCFA",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Reste à payer", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = formatFCFA(loan.remainingAmount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (loan.remainingAmount > 0) AlertRed else AlertGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { loan.progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = EmeraldPrimary,
                trackColor = Color.Gray.copy(alpha = 0.2f),
            )
        }
    }
}

// --- LOAN DETAILS + OVERDUE PENALTIES + SIGNATURE BOARD ---
@Composable
fun LoanDetailCard(
    loan: Loan,
    clients: List<Client>,
    repayments: List<Repayment>,
    viewModel: KrediyaViewModel,
    onClose: () -> Unit
) {
    val client = clients.find { it.id == loan.clientId }
    val clientName = client?.fullName ?: "Inconnu"
    val loanRepayments = repayments.filter { it.loanId == loan.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Detail Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Détail du Prêt",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }
        }

        // Primary Metrics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Bénéficiaire : $clientName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Montant déboursé : ${formatFCFA(loan.amount)}")
                    Text(text = "Taux d'intérêt : ${loan.interestRate} %")
                    Text(text = "Bénéfice attendu : ${formatFCFA(loan.interestAmount)}", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    Text(text = "Montant total dû : ${formatFCFA(loan.totalRepayable)}", fontWeight = FontWeight.Bold)
                    Text(text = "Paiements reçus : ${formatFCFA(loan.totalPaid)}", color = AlertGreen, fontWeight = FontWeight.Bold)
                    Text(text = "Date de début : ${loan.startDate}")
                    Text(text = "Date limite : ${loan.dueDate}")

                    if (loan.daysOverdue > 0) {
                        Text(text = "Jours de retard : ${loan.daysOverdue}", color = AlertRed, fontWeight = FontWeight.Bold)
                        Text(text = "Pénalité calculée : ${formatFCFA(loan.penaltyAmount)}", color = AlertRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Repayment Action Triggers
        item {
            Button(
                onClick = {
                    viewModel.selectedLoan = loan
                    viewModel.repaymentAmount = loan.remainingAmount.toInt().toString()
                    viewModel.showRepaymentDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(imageVector = Icons.Default.Payments, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enregistrer un Remboursement partiel/total")
            }
        }

        // Digital signature finger pad (item #11)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Signature numérique du contrat",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Signez avec le doigt dans le cadre ci-dessous",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (loan.signatureData != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color.White)
                                .border(1.dp, AlertGreen, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Draw, contentDescription = null, tint = AlertGreen, modifier = Modifier.size(36.dp))
                            Text(
                                text = "Contrat Archivée & Signée : ${loan.signatureData}",
                                color = AlertGreen,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(6.dp)
                            )
                        }
                    } else {
                        // Interactive paint canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            viewModel.signatureSignaturePoints = viewModel.signatureSignaturePoints + offset.toPair()
                                        },
                                        onDrag = { change, _ ->
                                            viewModel.signatureSignaturePoints = viewModel.signatureSignaturePoints + change.position.toPair()
                                        }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (viewModel.signatureSignaturePoints.size > 1) {
                                    for (i in 0 until viewModel.signatureSignaturePoints.size - 1) {
                                        val p1 = viewModel.signatureSignaturePoints[i]
                                        val p2 = viewModel.signatureSignaturePoints[i + 1]
                                        drawLine(
                                            color = Color(0xFF1E293B),
                                            start = Offset(p1.first, p1.second),
                                            end = Offset(p2.first, p2.second),
                                            strokeWidth = 5f
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.clearFingerSignature() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Effacer", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.saveFingerSignature(loan.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Enregistrer la Signature", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Payments History Lists
        item {
            Column {
                Text(
                    text = "Historique des versements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (loanRepayments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun versement n'a été enregistré.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    for (rep in loanRepayments) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "+ ${rep.amountPaid.toInt()} FCFA",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AlertGreen
                                    )
                                    Text(
                                        text = "${rep.paymentDate} à ${rep.paymentTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                rep.note?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun Offset.toPair() = Pair(this.x, this.y)

// ==================== SCREEN 4: CALENDRIER DES ÉCHÉANCES ====================
@Composable
fun EcheancesScreen(
    viewModel: KrediyaViewModel,
    loans: List<Loan>,
    clients: List<Client>
) {
    val context = LocalContext.current
    var dateFilterSelected by remember { mutableStateOf("Ce mois") } // "Aujourd'hui", "Cette semaine", "Ce mois", "En retard"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Calendrier des Échéances",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (viewModel.isDarkMode) Color.White else NavyBlueSecondary
        )
        Text(
            text = "Visualisez et relancez les clients en attente de remboursement",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filters Horizontal Row (item #4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf("Aujourd'hui", "Cette semaine", "Ce mois", "En retard")
            filters.forEach { filter ->
                val isSel = dateFilterSelected == filter
                Button(
                    onClick = { dateFilterSelected = filter },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(filter, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val scheduleList = remember(loans, clients, dateFilterSelected) {
            loans.filter { it.status == "ACTIVE" || it.status == "OVERDUE" }.mapNotNull { loan ->
                val client = clients.find { it.id == loan.clientId }
                if (client != null) {
                    val remaining = loan.totalRepayable - loan.totalPaid
                    if (remaining > 0) {
                        val isOverdue = loan.status == "OVERDUE"
                        val isMatchesFilter = when (dateFilterSelected) {
                            "Aujourd'hui" -> !isOverdue
                            "Cette semaine" -> !isOverdue
                            "En retard" -> isOverdue
                            else -> true // "Ce mois"
                        }
                        if (isMatchesFilter) {
                            val statusLabel = if (isOverdue) {
                                "🔴 En Retard de ${loan.daysOverdue} j (Pénalités applicables)"
                            } else {
                                "🟡 En cours - Date d'échéance: ${loan.dueDate}"
                            }
                            SimulatedEcheance(
                                date = "Échéance: ${loan.dueDate}",
                                clientName = client.fullName,
                                amount = remaining,
                                statusLabel = statusLabel,
                                phoneNumber = client.phone
                            )
                        } else null
                    } else null
                } else null
            }
        }

        if (scheduleList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Aucune échéance",
                        tint = EmeraldPrimary.copy(alpha = 0.35f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Aucune échéance en attente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlueSecondary
                    )
                    Text(
                        text = "Tous les remboursements de portefeuille sont à jour. Créez des fiches clients et octroyez de nouveaux crédits pour peupler cet onglet de recouvrement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(scheduleList) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldLight)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item.date,
                                    color = EmeraldDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = formatFCFA(item.amount),
                                fontWeight = FontWeight.Black,
                                color = EmeraldPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.clientName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.statusLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Communication Relances Triggers (item #7)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButtonWithLabel(
                                label = "SMS",
                                icon = Icons.Default.Sms,
                                containerColor = AlertGreen.copy(alpha = 0.12f),
                                contentColor = AlertGreen,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${item.phoneNumber}")).apply {
                                        putExtra("sms_body", "Bonjour ${item.clientName}, de Krediya. Votre échéance de ${item.amount.toInt()} FCFA approche pour le remboursement de votre microcrédit.")
                                    }
                                    context.startActivity(intent)
                                    viewModel.triggerStatus("SMS préparé pour ${item.clientName}")
                                }
                            )

                            IconButtonWithLabel(
                                label = "WhatsApp",
                                icon = Icons.Default.Message,
                                containerColor = AlertYellow.copy(alpha = 0.12f),
                                contentColor = AlertYellow,
                                onClick = {
                                    // Use standard share URL for wider coverage
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${item.phoneNumber}&text=Bonjour%20de%20Krediya"))
                                    context.startActivity(intent)
                                    viewModel.triggerStatus("Relance WhatsApp ouverte pour ${item.clientName}")
                                }
                            )

                            IconButtonWithLabel(
                                label = "Appeler",
                                icon = Icons.Default.Call,
                                containerColor = EmeraldPrimary.copy(alpha = 0.12f),
                                contentColor = EmeraldPrimary,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phoneNumber}"))
                                    context.startActivity(intent)
                                    viewModel.triggerStatus("Appel lancé pour ${item.clientName}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}


@Composable
fun IconButtonWithLabel(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        modifier = Modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

data class SimulatedEcheance(
    val date: String,
    val clientName: String,
    val amount: Double,
    val statusLabel: String,
    val phoneNumber: String
)

// ==================== SCREEN 5: PLUS (SETTINGS & ADVANCED) ====================
@Composable
fun PlusScreen(
    viewModel: KrediyaViewModel,
    loans: List<Loan>,
    clients: List<Client>,
    repayments: List<Repayment>,
    logs: List<ActivityLog>,
    agents: List<Agent>,
    capital: CapitalInfo
) {
    val context = LocalContext.current
    var activePlusSection by remember { mutableStateOf("menu") } // "menu", "journal", "agents", "previsions", "admin_auth", "admin_dashboard"
    
    // Admin Auth State states
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }

    // Admin Dashboard states
    var oldAdminCode by remember { mutableStateOf("") }
    var newAdminCode by remember { mutableStateOf("") }

    var oldUserPin by remember { mutableStateOf("") }
    var newUserPin by remember { mutableStateOf("") }

    var oldUserPassword by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }

    var adminCustomDays by remember { mutableStateOf("") }
    var adminRenewMonths by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (activePlusSection != "menu") {
            IconButton(onClick = { 
                if (activePlusSection == "admin_dashboard") {
                    activePlusSection = "menu" 
                } else if (activePlusSection == "admin_auth") {
                    activePlusSection = "menu"
                } else {
                    activePlusSection = "menu"
                }
            }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
            }
        }

        when (activePlusSection) {
            "admin_auth" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin lock",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "Accès d'Administration Réseau",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Veuillez saisir votre code d'administration inspecteur pour déverrouiller la configuration de l'agence.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        label = { Text("Code Admin (Défaut: admin2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_auth_pwd"),
                        visualTransformation = if (adminPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (adminPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                                Icon(imageVector = icon, contentDescription = "Toggle")
                            }
                        }
                    )

                    if (viewModel.statusMessage != null) {
                        Text(
                            text = viewModel.statusMessage ?: "",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            if (viewModel.hashAdminCode(adminPasswordInput) == viewModel.adminPasswordHash) {
                                activePlusSection = "admin_dashboard"
                                adminPasswordInput = ""
                                viewModel.triggerStatus("Espace Administration déverrouillé avec succès.")
                            } else {
                                viewModel.triggerStatus("Code administrateur incorrect.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_auth_submit")
                    ) {
                        Text("Déverrouiller le Cabinet", fontWeight = FontWeight.Bold)
                    }
                }
            }

            "admin_dashboard" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "⚙️ Cabinet d’Administration d'Agence",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )

                    // Card 1: Subscription status
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Abonnement Application Krediya",
                                color = EmeraldDark,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Divider(color = EmeraldPrimary.copy(alpha = 0.2f))
                            Text(text = "Statut Local : ${viewModel.licenseStatusText}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(text = "Date d'activation : ${if(viewModel.licenseActivationDate.isEmpty()) "Aucune" else viewModel.licenseActivationDate}", style = MaterialTheme.typography.labelSmall)
                            Text(text = "Date d'expiration : ${if(viewModel.licenseExpirationDate.isEmpty()) "Aucune" else viewModel.licenseExpirationDate}", style = MaterialTheme.typography.labelSmall)
                            Text(text = "Acheté de validité : ${if(viewModel.purchasedDuration.isEmpty()) "Aucun" else viewModel.purchasedDuration}", style = MaterialTheme.typography.labelSmall)

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.suspendLicense() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Suspendre", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.reactivateLicense() },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Activer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Section: Renew panel inside Admin Dashboard
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Prolonger l'abonnement Krediya",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            // Choose months
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val durations = listOf(
                                    Pair("1 Mois", 1),
                                    Pair("3 Mois", 3),
                                    Pair("6 Mois", 6),
                                    Pair("12 Mois", 12),
                                    Pair("Custom", 0)
                                )
                                durations.forEach { pair ->
                                    val isSelected = adminRenewMonths == pair.second
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) EmeraldPrimary else Color.Gray.copy(alpha = 0.1f))
                                            .clickable { adminRenewMonths = pair.second }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pair.first,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (adminRenewMonths == 0) {
                                OutlinedTextField(
                                    value = adminCustomDays,
                                    onValueChange = { adminCustomDays = it },
                                    label = { Text("Nombre de jours") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = {
                                    val cDays = if (adminRenewMonths == 0) adminCustomDays.toIntOrNull() else null
                                    viewModel.activateLicense("admin2026", adminRenewMonths, cDays) // validated administratively
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Text("VALIDER LA PROLONGATION", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Card 2: Security & Lock Modes configuration
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Options d'Accès & Sécurité Agent",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "Sélectionner le mode souhaité :", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val options = listOf("PIN", "PASSWORD", "FINGERPRINT")
                                options.forEach { option ->
                                    val isSel = viewModel.appLockType == option
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) EmeraldPrimary else Color.Gray.copy(alpha = 0.1f))
                                            .clickable { viewModel.updateSecuritySettings(option, viewModel.selectedAutoLockSeconds) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = option, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.15f))

                            Text(text = "Temps de verrouillage automatique :", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val times = listOf(
                                    Pair("30 s", 30),
                                    Pair("60 s", 60),
                                    Pair("5 min", 300),
                                    Pair("Jamais", 0)
                                )
                                times.forEach { timePair ->
                                    val isSel = viewModel.selectedAutoLockSeconds == timePair.second
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) EmeraldPrimary else Color.Gray.copy(alpha = 0.1f))
                                            .clickable { viewModel.updateSecuritySettings(viewModel.appLockType, timePair.second) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = timePair.first, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    // Card 3: credential managers
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Part A: User PIN modification
                            Text(text = "Modifier le Code PIN de l'Agent", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            
                            OutlinedTextField(
                                value = oldUserPin,
                                onValueChange = { oldUserPin = it },
                                label = { Text("PIN Actuel") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setting_old_pin")
                            )
                            OutlinedTextField(
                                value = newUserPin,
                                onValueChange = { newUserPin = it },
                                label = { Text("Nouveau PIN (4 chiffres)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setting_new_pin")
                            )

                            Button(
                                onClick = {
                                    val ok = viewModel.modifyUserPin(oldUserPin, newUserPin)
                                    if (ok) {
                                        oldUserPin = ""
                                        newUserPin = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth().height(36.dp).testTag("setting_submit_pin")
                            ) {
                                Text("Mettre à jour le PIN d'accès", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                            // Part B: User Password modification
                            Text(text = "Modifier le Mot de Passe Agent", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                            OutlinedTextField(
                                value = oldUserPassword,
                                onValueChange = { oldUserPassword = it },
                                label = { Text("Mot de passe actuel") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newUserPassword,
                                onValueChange = { newUserPassword = it },
                                label = { Text("Nouveau mot de passe") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val ok = viewModel.modifyUserPassword(oldUserPassword, newUserPassword)
                                    if (ok) {
                                        oldUserPassword = ""
                                        newUserPassword = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Text("Mettre à jour le MDP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                            // Part C: Master administrative credentials modification
                            Text(text = "Modifier Code d'Administration (Audit)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color.Red)

                            OutlinedTextField(
                                value = oldAdminCode,
                                onValueChange = { oldAdminCode = it },
                                label = { Text("Code Inspecteur actuel", color = Color.Red.copy(alpha = 0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newAdminCode,
                                onValueChange = { newAdminCode = it },
                                label = { Text("Nouveau code secret d'agence", color = Color.Red.copy(alpha = 0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val ok = viewModel.modifyAdminCode(oldAdminCode, newAdminCode)
                                    if (ok) {
                                        oldAdminCode = ""
                                        newAdminCode = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Text("Fixer Nouveau Code Administrateur", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            TextButton(
                                onClick = { viewModel.resetAdminCodeToDefault() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Réinitialiser code d'usine (Défaut: 'admin2026')", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (viewModel.statusMessage != null) {
                        Text(
                            text = viewModel.statusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            "journal" -> {
                Text(text = "Journal d'Activité / Audit Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(logs) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = log.action, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = EmeraldPrimary)
                                    val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                                    Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = log.details, style = MaterialTheme.typography.bodySmall)
                                Text(text = "Agent: ${log.agentName}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
            "agents" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Gestion des Agents Collecteurs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = { viewModel.showAddAgentDialog = true }) {
                        Text("Ajouter", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (viewModel.showAddAgentDialog) {
                    AddAgentDialog(viewModel = viewModel, onDismiss = { viewModel.showAddAgentDialog = false })
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(agents) { agent ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = agent.name, fontWeight = FontWeight.Bold)
                                    Text(text = "Rôle: ${agent.role}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(text = "Tél: ${agent.phone}", style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(EmeraldLight)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "Pro", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                                }
                            }
                        }
                    }
                }
            }
            "previsions" -> {
                // Cash Flow Forecasting Forecast metrics (item #18, #20)
                val stats = viewModel.getForecastingStats(loans)
                Text(text = "Prévisions de Trésorerie & Solvabilité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldLight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Fonds d'amortissement Krediya", color = EmeraldDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Total Actifs : ${formatFCFA(capital.totalCapital)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ForecastItemRow(label = "Retours attendus (Cette semaine)", value = stats.week)
                    ForecastItemRow(label = "Retours attendus (Ce mois)", value = stats.month)
                    ForecastItemRow(label = "Retours attendus (Ce trimestre)", value = stats.quarter)
                }
            }
            else -> {
                // MAIN MENU
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Paramètres & Modules Avancés",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Theme Setting Trigger inline
                    item {
                        ListItem(
                            headlineContent = { Text("Mode Sombre / Mode Clair") },
                            supportingContent = { Text("Changer l'apparence selon vos préférences") },
                            trailingContent = {
                                Switch(
                                    checked = viewModel.isDarkMode,
                                    onCheckedChange = { viewModel.isDarkMode = it }
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        )
                    }

                    item {
                        MenuRowButton(
                            title = "Prévisions de trésorerie",
                            subtitle = "Suivi de rentabilité & retours attendus",
                            icon = Icons.Default.Timeline,
                            onClick = { activePlusSection = "previsions" }
                        )
                    }

                    item {
                        MenuRowButton(
                            title = "Gestion des Agents (Version Pro)",
                            subtitle = "Suivi des collecteurs de Brazzaville",
                            icon = Icons.Default.People,
                            onClick = { activePlusSection = "agents" }
                        )
                    }

                    item {
                        MenuRowButton(
                            title = "Journal de Securité d'Activité",
                            subtitle = "Audits logs chronologiques",
                            icon = Icons.Default.History,
                            onClick = { activePlusSection = "journal" }
                        )
                    }

                    item {
                        MenuRowButton(
                            title = "Sauvegarde locale de Base",
                            subtitle = "Exporter ou restaurer krediya_backup.db",
                            icon = Icons.Default.SdCard,
                            onClick = { viewModel.executeLocalBackup(context) }
                        )
                    }

                    item {
                        MenuRowButton(
                            title = "Administration d'Agence",
                            subtitle = "Gérer les abonnements locaux & de sécurité réseau",
                            icon = Icons.Default.AdminPanelSettings,
                            onClick = { activePlusSection = "admin_auth" }
                        )
                    }

                    // Display backup status results if any
                    viewModel.backupPathMessage?.let { msg ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(EmeraldLight)
                                    .border(1.dp, EmeraldPrimary, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(text = msg, fontSize = 11.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastItemRow(label: String, value: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.Medium)
            Text(text = formatFCFA(value), fontWeight = FontWeight.Black, color = EmeraldPrimary)
        }
    }
}

@Composable
fun MenuRowButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

// ==================== DIALOG COMPOSABLES ====================

// 1. ADD CLIENT DIALOG
@Composable
fun AddClientDialog(
    viewModel: KrediyaViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "Création d'une Fiche Client",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "Microfinance Krediya Brazzaville",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = viewModel.clientLastName,
                        onValueChange = { viewModel.clientLastName = it },
                        label = { Text("Nom de famille *") },
                        modifier = Modifier.fillMaxWidth().testTag("client_last_name")
                    )
                }
                item {
                    OutlinedTextField(
                        value = viewModel.clientFirstName,
                        onValueChange = { viewModel.clientFirstName = it },
                        label = { Text("Prénom *") },
                        modifier = Modifier.fillMaxWidth().testTag("client_first_name")
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.clientGender = "M" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.clientGender == "M") EmeraldPrimary else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Homme", fontSize = 11.sp, color = if (viewModel.clientGender == "M") Color.White else Color.Black)
                        }
                        Button(
                            onClick = { viewModel.clientGender = "F" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.clientGender == "F") EmeraldPrimary else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Femme", fontSize = 11.sp, color = if (viewModel.clientGender == "F") Color.White else Color.Black)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = viewModel.clientBirthDate,
                        onValueChange = { viewModel.clientBirthDate = it },
                        label = { Text("Date de naissance (AAAA-MM-JJ)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = viewModel.clientPhone,
                        onValueChange = { viewModel.clientPhone = it },
                        label = { Text("Téléphone *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = viewModel.clientAddress,
                        onValueChange = { viewModel.clientAddress = it },
                        label = { Text("Adresse physique") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = viewModel.clientNeighborhood,
                        onValueChange = { viewModel.clientNeighborhood = it },
                        label = { Text("Quartier Brazzaville (ex: Moungali, Bacongo)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = viewModel.clientProfession,
                        onValueChange = { viewModel.clientProfession = it },
                        label = { Text("Profession actuelle (ex: Table de Marché, Taxi)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // DOCUMENT SPECIFIC SELECTOR SECTION
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Pièces d'identité & Justificatifs",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlueSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Documentation Type choices
                    val docTypes = listOf("CNI", "Passeport", "Aucun (Scoring Alternatif)")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        docTypes.forEach { type ->
                            val isSelected = viewModel.clientDocumentationType == type
                            Button(
                                onClick = { viewModel.clientDocumentationType = type },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) EmeraldPrimary else Color.LightGray.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }
                }

                item {
                    when (viewModel.clientDocumentationType) {
                        "CNI" -> {
                            OutlinedTextField(
                                value = viewModel.clientIDCardNumber,
                                onValueChange = { viewModel.clientIDCardNumber = it },
                                label = { Text("Numéro CNI (Optionnel)") },
                                placeholder = { Text("Ex: 12108-984A6") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "Passeport" -> {
                            OutlinedTextField(
                                value = viewModel.clientPassportNumber,
                                onValueChange = { viewModel.clientPassportNumber = it },
                                label = { Text("Numéro du Passeport (Optionnel)") },
                                placeholder = { Text("Ex: CG0054321") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "Aucun (Scoring Alternatif)" -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = EmeraldLight.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = EmeraldDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Scoring Alternatif Activé",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ce client n'a pas de pièces physiques à disposition. Krediya évalue d'autres facteurs informels de confiance (activité stable au marché, réputation du quartier, garant physique) et lui accorde un score de confiance initial de 82%.",
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss, 
                            modifier = Modifier.weight(1f), 
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Annuler", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = onSave, 
                            modifier = Modifier.weight(1f), 
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Créer Fiche", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// 2. ADD AGENT DIALOG
@Composable
fun AddAgentDialog(
    viewModel: KrediyaViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Ajouter un Agent Collecteur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = viewModel.agentName,
                    onValueChange = { viewModel.agentName = it },
                    label = { Text("Nom complet") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.agentPhone,
                    onValueChange = { viewModel.agentPhone = it },
                    label = { Text("Numéro Téléphone") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.agentEmail,
                    onValueChange = { viewModel.agentEmail = it },
                    label = { Text("Email professionnel") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                        Text("Annuler")
                    }
                    Button(onClick = { viewModel.saveNewAgent() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                        Text("Créer l'Agent")
                    }
                }
            }
        }
    }
}

// 3. ADD LOAN DIALOG WITH ON-THE-FLY RECAPITULATIF PREVIEW (item #3)
@Composable
fun AddLoanDialog(
    viewModel: KrediyaViewModel,
    clients: List<Client>,
    capital: CapitalInfo,
    onDismiss: () -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (viewModel.showLoanConfirmationStep) "Récapitulatif avant Validation" else "Nouveau Prêt Krediya",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!viewModel.showLoanConfirmationStep) {
                    // Client Selector dropdown
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = viewModel.selectedClientForLoan?.fullName ?: "Sélectionner Client"
                                )
                            }
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                for (c in clients) {
                                    DropdownMenuItem(
                                        text = { Text(text = "${c.fullName} (Score: ${c.score}/100)") },
                                        onClick = {
                                            viewModel.selectedClientForLoan = c
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = viewModel.loanAmount,
                            onValueChange = { viewModel.loanAmount = it },
                            label = { Text("Montant Principal (FCFA)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("loan_amount_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = viewModel.loanInterestRate,
                            onValueChange = { viewModel.loanInterestRate = it },
                            label = { Text("Taux d'intérêt (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = viewModel.loanDurationDays,
                            onValueChange = { viewModel.loanDurationDays = it },
                            label = { Text("Durée du remboursement (jours)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Warning check
                    item {
                        viewModel.selectedClientForLoan?.let { client ->
                            if (client.isBlacklisted) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AlertRed.copy(alpha = 0.15f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "ATTENTION : Ce client est sur liste noire !",
                                        color = AlertRed,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                                Text("Annuler")
                            }
                            Button(
                                onClick = {
                                    if (viewModel.selectedClientForLoan != null && viewModel.loanAmount.isNotEmpty()) {
                                        viewModel.showLoanConfirmationStep = true
                                    } else {
                                        viewModel.triggerStatus("Veuillez sélectionner un client et entrer un montant.")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Suivant")
                            }
                        }
                    }
                } else {
                    // Confirmation recapitulatif review screen
                    val p = viewModel.calculateLoanPreview()
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "Bénéficiaire : ${viewModel.selectedClientForLoan?.fullName}", fontWeight = FontWeight.Bold)
                                Text(text = "Montant prêté : ${formatFCFA(p.principal)}")
                                Text(text = "Taux d'intérêt fixe : ${viewModel.loanInterestRate} %")
                                Text(text = "Intérêts perçus : ${formatFCFA(p.interest)}", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "Montant attendu : ${formatFCFA(p.total)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Date Limite Échéance : ${p.limitDateString}", color = AlertRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.showLoanConfirmationStep = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Modifier")
                            }
                            Button(
                                onClick = { viewModel.submitLoanDisbursement() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Confirmer & Débourser")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. ADD REPAYMENT DIALOG
@Composable
fun AddRepaymentDialog(
    viewModel: KrediyaViewModel,
    loans: List<Loan>,
    clients: List<Client>,
    onDismiss: () -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Remboursement partiel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Select Loan dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val loan = viewModel.selectedLoan
                        if (loan != null) {
                            val cName = clients.find { it.id == loan.clientId }?.fullName ?: "Client ${loan.clientId}"
                            Text(text = "Prêt #${loan.id} - $cName")
                        } else {
                            Text(text = "Sélectionner Prêt Actif")
                        }
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        for (l in loans.filter { it.status != "REPAID" }) {
                            val cName = clients.find { it.id == l.clientId }?.fullName ?: "Client ${l.clientId}"
                            DropdownMenuItem(
                                text = { Text(text = "Prêt #${l.id} - $cName (${l.remainingAmount.toInt()} FCFA)") },
                                onClick = {
                                    viewModel.selectedLoan = l
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = viewModel.repaymentAmount,
                    onValueChange = { viewModel.repaymentAmount = it },
                    label = { Text("Montant payé (FCFA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.repaymentNote,
                    onValueChange = { viewModel.repaymentNote = it },
                    label = { Text("Note / Commentaire (e.g. Marché)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { viewModel.submitRepayment() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Valider")
                    }
                }
            }
        }
    }
}

// 5. REPORTS GENERATOR DIALOG WITH PRINT INVOICES RECAP (item #10 Contrats PDF, #21 Rapports PDF)
@Composable
fun PdfReportDialog(
    viewModel: KrediyaViewModel,
    loans: List<Loan>,
    clients: List<Client>,
    repayments: List<Repayment>,
    capital: CapitalInfo,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "Générateur PDF Professionnel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val reportOptions = listOf("journalier", "hebdomadaire", "mensuel", "annuel")
                    for (opt in reportOptions) {
                        val isSel = viewModel.selectedReportType == opt
                        Button(
                            onClick = { viewModel.selectedReportType = opt },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) EmeraldPrimary else Color.LightGray,
                                contentColor = if (isSel) Color.White else Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text(opt.capitalize(), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider()

                // Simulation report display layout
                Card(
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                     shape = RoundedCornerShape(12.dp),
                     modifier = Modifier.fillMaxWidth()
                ) {
                     Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                         Text(text = "KREDIYA S.A.R.L - BRAZZAVILLE", fontWeight = FontWeight.Black, color = EmeraldPrimary, fontSize = 12.sp)
                         Text(text = "Type de rapport : ${viewModel.selectedReportType.capitalize()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                         Text(text = "Date d'édition : ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}", fontSize = 10.sp, color = Color.Gray)
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(text = "Capital Total Actuel : ${formatFCFA(capital.totalCapital)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                         Text(text = "Total Prêts déboursés : ${formatFCFA(loans.sumOf { it.amount })}", fontSize = 11.sp)
                         Text(text = "Flux liquidité recouvrés : ${formatFCFA(repayments.sumOf { rep -> rep.amountPaid })}", fontSize = 11.sp, color = AlertGreen, fontWeight = FontWeight.Bold)
                         Text(text = "Taux de recouvrement : 78 %", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                     }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                        Text("Fermer", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.triggerStatus("Rapport PDF généré et enregistré dans /Documents/krediya_report_${viewModel.selectedReportType}.pdf !")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Exporter PDF", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ==================== TUTORIAL & INTERACTIVE GUIDE COMPOSABLES ====================

@Composable
fun KrediyaTutorialDialog(
    viewModel: KrediyaViewModel,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()
    
    // Reset scroll when step changes to ensure user starts reading from top
    LaunchedEffect(step) {
        scrollState.scrollTo(0)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with icon and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guide Krediya Pas-à-Pas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.Gray
                        )
                    }
                }
                
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                // Multi-page rich operational onboarding guide
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState)
                ) {
                    when (step) {
                        0 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_krediya_logo_vector),
                                    contentDescription = "Logo Krediya",
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .border(2.dp, EmeraldPrimary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                                )
                                Text(
                                    text = "1. Krediya & Mission de Terrain",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Krediya est un grand livre de compte professionnel de microfinance, optimisé pour fonctionner 100% hors-ligne (Offline-First) pour les agents de crédit à Brazzaville.\n\nNotre mission est d'assurer l'inclusion financière par le biais de technologies de pointe adaptées à l'économie informelle. Toutes vos données (fiches clients, soldes d'encours de prêt, journaux d'audit et contrats légaux) sont stockées de façon autonome et cryptée localement, vous immunisant contre les pannes et réseaux internet instables sur les marchés de terrain.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        1 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(EmeraldLight, RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Text(
                                    text = "2. Scoring de Crédit Alternatif",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Sur le terrain (marché Total, Bacongo, Poto-Poto, Ouenzé), la majorité des micro-entrepreneurs éligibles ne possèdent pas de garanties bancaires classiques ni de pièces d'identité formelles à portée de main.\n\nKrediya résout cela grâce à sa méthodologie de score alternatif de confiance (82% à 100%) basée sur : l'ancienneté observée du stand de vente, la consistance et régularité des recettes quotidiennes, la propreté du dossier, et le cautionnement moral des pairs de marché. Cochez simplement 'Aucun document officiel' à l'inscription pour utiliser ce parcours flexible !",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        2 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(20.dp)), // Amber Light
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = null,
                                        tint = AlertYellow,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Text(
                                    text = "3. Décaissement & Accord Tactile",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Le protocole de décaissement de Krediya respecte un formalisme rigoureux pour limiter les impayés :\n\n1. Renseignez l'emprunteur désiré, le montant de crédit nécessaire, la durée choisie (ex: 15 ou 30 jours) et le taux d'intérêt applicable.\n2. Le système calcule instantanément l'échéance future ainsi que le remboursement total attendu.\n3. Faites signer l'accord de prêt à l'emprunteur au doigt direkt sur l'écran tactile du smartphone. Cette signature vectorisée constitue un accord contractuel légal archivé de façon sécurisée.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        3 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color(0xFFFEE2E2), RoundedCornerShape(20.dp)), // Red Light
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = AlertRed,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Text(
                                    text = "4. Trésorerie Active & Caisse liquide",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "La discipline des flux de trésorerie est indispensable pour la solidité de votre portefeuille :\n\n- Capital d'Exploitation : C'est le fonds souverain global mobilisé.\n- Capital Prêté : C'est l'encours de vos prêts actifs engagés sur les stands.\n- Capital Disponible : Votre réserve prête pour de nouveaux décaissements.\n\nLe système protège vos finances en bloquant l'octroi d'un prêt si le capital disponible en caisse est inférieur au montant décaissé. Veillez à intégrer de nouvelles liquidités ou collecter les remboursements en retard pour réalimenter la caisse collective.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        4 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(EmeraldLight, RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EventNote,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Text(
                                    text = "5. Recouvrements, Acomptes & Alertes WhatsApp",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Maximiser le remboursement de votre capital exige un suivi de terrain au jour le jour :\n\n- Vous pouvez enregistrer des acomptes partiels pour réduire le solde restant au fur et à mesure.\n- Les dossiers en retard de paiement sont surlignés en rouge avec un décompte des pénalités applicables.\n- En cas de litige, utilisez l'onglet de relance rapide pour passer un appel vocal direct ou envoyer automatiquement une réclamation polie et standardisée sur WhatsApp ou SMS (+242).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        5 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color(0xFFE0F2FE), RoundedCornerShape(20.dp)), // Sky blue light
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Text(
                                    text = "6. Collaboration d'Équipe & Audit Trail",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "La microfinance mobile requiert une totale transparence pour éliminer la fraude interne :\n\n- Enregistrez vos agents de terrain et assignez-leur des rôles spécifiques (Administrateurs, Gestionnaires, Collecteurs).\n- Le système enregistre de manière indélébile chaque validation de règlement d'échéance ou de crédit dans un Journal d'Audit consultable par la direction.\n- Générez des rapports PDF complets de fin de journée pour archiver l'état financier des collectes en un clic.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Step dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0..5) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step == i) EmeraldPrimary else Color.LightGray.copy(alpha = 0.6f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { step-- },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Précédent", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(90.dp))
                    }

                    if (step < 5) {
                        Button(
                            onClick = { step++ },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Suivant", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.startInteractiveTutorial() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(40.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldPrimary)
                            ) {
                                Text("Guide Live 🚀", fontSize = 13.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("Démarrer !", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KrediyaFloatingGuide(
    viewModel: KrediyaViewModel,
    currentTab: String
) {
    var expanded by remember { mutableStateOf(false) }

    // Enriched advanced operational microfinance tips loaded dynamically per page
    val (titleTab, tipList) = when (currentTab) {
        "accueil" -> Pair(
            "Suivi de Trésorerie & Caisse",
            listOf(
                "📊 Suivez la Trésorerie Active: Le capital total est la somme des encours de crédit engagés sur le terrain et de la caisse disponible.",
                "💼 Re-circulation du Capital: Chaque remboursement et acompte collecté retourne s'injecter directement dans le capital disponible pour financer de nouveaux prêts sains.",
                "📈 Analyse & Décaissement: Le système bloque tout décaissement de prêt qui dépasserait les capacités de votre caisse liquide disponible actuelle.",
                "🗓️ Prévisions de Rentabilité: Accédez à l'onglet Prévisions dans le menu Plus pour anticiper les rentrées cumulées à 7 jours, 30 jours et 90 jours."
            )
        )
        "clients" -> Pair(
            "Fiches Clients, KYC & Scoring",
            listOf(
                "🔍 Évaluation Holistique: Utilisez nos critères d'observation globale directement au stand (fréquence, stock, emplacement) pour calibrer le Score de Confiance.",
                "🌾 Option d'Inclusion Informelle: L'option 'Aucun document officiel' active le modèle de scoring de confiance solidaire basé sur l'activité réelle et les garanties morales paires.",
                "🚫 Protection de Portefeuille: Si un client accuse des impayés chroniques ou affiche de la mauvaise foi, déclarez son insolvabilité pour bloquer automatiquement tout octroi.",
                "📞 Contacts Rapides du Terrain: Cliquez sur une fiche client pour lancer directement un appel de suivi au commerçant ou à sa caution morale répertoriée."
            )
        )
        "prets" -> Pair(
            "Octroi, Amortissement & Signature",
            listOf(
                "⚡ Caisse de Liquidité Sécurisée: Le capital en caisse est débité en direct lors de la validation d'un nouveau micro-prêt actif.",
                "🧮 Simplicité des Taux: L'intérêt est calculé de manière brute sur la durée du prêt (ex: 10% pour 15 jours). Ces barèmes sont standards sur les marchés.",
                "✍️ Signature Mobile au Doigt: Faites signer l'emprunteur directement sur l'écran tactile de l'application mobile pour formaliser l'engagement.",
                "📜 Clauses de Transparence: Le récapitulatif synthétise l'échéance et le coût financier global pour assurer une clarté mutuelle intégrale."
            )
        )
        "echeances" -> Pair(
            "Collectes, Retards & Alertes Pro",
            listOf(
                "🔄 Amortissement par Acomptes: Saisissez des règlements partiels réguliers pour réduire la charge ponctuelle des emprunteurs.",
                "⏳ Suivi des Jours de Retards: Les échéanciers calculent de façon arithmétique les dépassements de date pour modéliser d'éventuelles pénalités de retard.",
                "🛎️ Relance WhatsApp Active: Un bouton de relance directe génère un message cordial rappelant le solde et l'échéance à envoyer en un clic.",
                "✅ Solde et Clôture: Dès que le solde d'un prêt atteint zéro, il est archivé automatiquement, et le score de confiance ré-augmente."
            )
        )
        else -> Pair(
            "Outils Métiers & Sécurité de Caisse",
            listOf(
                "👥 Équipe de Collecteurs: Enregistrez vos agents habilités pour les relier précisément à l'historique des opérations de crédit de terrain.",
                "🗄️ Sauvegarde Locale Intégrale: Exportez des sauvegardes de la base SQL interne pour parer à toute perte matérielle de l'appareil mobile.",
                "📝 Traçabilité Anti-Fraude: Le journal d'audit enregistre l'auteur de chaque opération de décaissement ou de collecte pour garantir le contrôle permanent.",
                "📄 Rapports Financiers PDF: Téléchargez et visualisez instantanément de magnifiques rapports de fin de journée intégrant l'état complet des comptes."
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = titleTab,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                            }
                            IconButton(onClick = { expanded = false }, modifier = Modifier.size(22.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close, 
                                    contentDescription = "Fermer", 
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Gray
                                )
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                        tipList.forEach { tip ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "•", color = EmeraldPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = tip,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                        Button(
                            onClick = {
                                expanded = false
                                viewModel.startInteractiveTutorial()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guide Interactif Live 🚀",
                                fontSize = 11.5.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Round floating button styled with a subtle border for extra polish
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .testTag("guide_floating_btn")
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Lightbulb,
                    contentDescription = "Aide Interactive",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun KrediyaInteractiveTutorialOverlay(
    viewModel: KrediyaViewModel
) {
    val step = viewModel.interactiveTutorialStep

    // Overlay root Box
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(enabled = true, onClick = { /* Consuming taps to avoid background interactions */ })
    ) {
        // Step specific highlighted boundaries (visually guiding raw elements in the screen)
        when (step) {
            0 -> {
                // Capital Overview Spotlight in the Top Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                        .height(130.dp)
                        .border(
                            width = 2.5.dp,
                            color = EmeraldPrimary,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(EmeraldPrimary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    )
                }
            }
            1 -> {
                // Quick actions centerpiece spotlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 162.dp)
                        .height(180.dp)
                        .border(
                            width = 2.5.dp,
                            color = EmeraldPrimary,
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(EmeraldPrimary.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    )
                }
            }
            2 -> {
                // Clients directory view focus frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 24.dp)
                        .padding(bottom = 120.dp)
                        .border(
                            width = 2.5.dp,
                            color = EmeraldPrimary,
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(EmeraldPrimary.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    )
                }
            }
            3 -> {
                // Active Loans Directory frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 24.dp)
                        .padding(bottom = 120.dp)
                        .border(
                            width = 2.5.dp,
                            color = AlertYellow,
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AlertYellow.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    )
                }
            }
            4 -> {
                // Outstanding Schedule frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 24.dp)
                        .padding(bottom = 120.dp)
                        .border(
                            width = 2.5.dp,
                            color = AlertRed,
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AlertRed.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    )
                }
            }
            5 -> {
                // Plus/Tools frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 24.dp)
                        .padding(bottom = 120.dp)
                        .border(
                            width = 2.5.dp,
                            color = Color(0xFF0284C7),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0284C7).copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    )
                }
            }
        }

        // Contextual Explanation Card (glorious bottom/center floating sheet)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val icon = when (step) {
                            0, 1 -> Icons.Default.Dashboard
                            2 -> Icons.Default.People
                            3 -> Icons.Default.MonetizationOn
                            4 -> Icons.Default.Timeline
                            else -> Icons.Default.AdminPanelSettings
                        }
                        val tint = when (step) {
                            0, 1, 2 -> EmeraldPrimary
                            3 -> AlertYellow
                            4 -> AlertRed
                            else -> Color(0xFF0284C7)
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(tint.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = when (step) {
                                0 -> "Fonds d'Exploitation"
                                1 -> "Raccourcis de Terrain"
                                2 -> "Scoring Alternatif"
                                3 -> "Décaissement & Signature"
                                4 -> "Collecte & Relance WhatsApp"
                                else -> "Protection Anti-Fraude"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Rounded Pill Badge
                    Box(
                        modifier = Modifier
                            .background(EmeraldLight, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${step + 1} / 6",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                // Rich contextual content explaining the business mechanics
                Text(
                    text = when (step) {
                        0 -> "💼 Bienvenue dans votre grand livre ! Voici votre stock financier d'agence de microfinance :\n\n• Le **Capital Total** est votre réserve d'exploitation active mobilisée.\n• Le **Capital Prêté** est l'épargne engagée sur les stands de marché.\n• Le **Capital Disponible** est la caisse liquide disponible.\n\nLe système bloque automatiquement d'autres prêts si vous n'avez pas assez de cash disponible."
                        1 -> "⚡ Pilotez vos tournées d'un simple geste grâce aux gâchettes d'actions :\n\n• **Nouveau Client** : Enrôler un emprunteur.\n• **Nouveau Prêt** : Octroyer un micro-crédit en direct.\n• **Règlement** : Enregistrer un acompte reçu.\n• **Rapports** : Exporter les comptes consolidés au format PDF en fin de journée."
                        2 -> "🌾 Les micro-commerçants n'ont souvent pas de papiers ou garanties formelles.\n\n• **Scoring Alternatif** : Cochez 'Aucun Document' à l'enrôlement.\n• L'application calcule alors un score de confiance de départ (82% à 100%) basé sur l'activité réelle, l'emplacement, et la réputation des pairs de marché."
                        3 -> "💸 Suivez les prêts accordés au jour le jour d'un clin d'œil.\n\n• **Intérêts & Échéance** : Nos calculs arithmétiques fixent l'intérêt brut (souvent 10% ou 20%) remboursable sous 15 ou 30 jours à l'échéance.\n• **Signature au Doigt** : Faites signer le commerçant directement sur l'écran tactile du smartphone pour archiver légalement le contrat !"
                        4 -> "⏳ Suivez vos règlements attendus avec précision arithmétique :\n\n• **Acomptes successifs** : Saisissez de simples acomptes partiels pour décharger le commerçant au fur et à mesure.\n• **Relance Directe** : Envoyez instantanément une alerte de recouvrement polie sur WhatsApp d'un simple clic (+242) !"
                        else -> "🛡️ Assurez le pilotage d'équipe et la transparence budgétaire de votre caisse collective :\n\n• **Journal d'Audit** : Retrace chaque action d'enrôlement ou d'acompte.\n• **Registre d'Agents** : Liez précisément chaque collecte à son agent de terrain.\n• **Sauvegarde** : Conservez l'intégralité de la base SQL locale d'un clic."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom active navigation controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.stopInteractiveTutorial() }
                    ) {
                        Text(
                            text = "Quitter le guide",
                            color = AlertRed,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (step > 0) {
                            OutlinedButton(
                                onClick = { viewModel.prevInteractiveTutorialStep() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(38.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Text("Précédent", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { viewModel.nextInteractiveTutorialStep() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                text = if (step == 5) "Terminer ! 🎯" else "Suivant ➔",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntelligentAlertsPanel(
    clients: List<Client>,
    loans: List<Loan>,
    viewModel: KrediyaViewModel,
    onNavigate: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Date())
    
    val calTomorrow = Calendar.getInstance()
    calTomorrow.add(Calendar.DAY_OF_YEAR, 1)
    val tomorrowStr = sdf.format(calTomorrow.time)

    // Compute dynamic loan alerts
    val alerts = remember(loans, clients) {
        val list = mutableListOf<String>()
        for (l in loans) {
            if (l.status != "REPAID") {
                val clientName = clients.find { it.id == l.clientId }?.fullName ?: "Client"
                val remaining = l.remainingAmount.toInt()
                if (l.dueDate == todayStr) {
                    list.add("🚨 **$clientName** doit payer aujourd'hui ($remaining FCFA).")
                } else if (l.dueDate == tomorrowStr) {
                    list.add("📅 **$clientName** doit rembourser demain ($remaining FCFA).")
                } else if (l.status == "OVERDUE" || l.daysOverdue > 0) {
                    list.add("⚠️ **$clientName** a ${l.daysOverdue} jours de retard (${remaining} FCFA).")
                }
            }
        }
        list
    }

    // Compute license alerts
    val licenseAlert = remember(viewModel.licenseExpirationDate, viewModel.isLicensed) {
        if (!viewModel.isLicensed) return@remember null
        val curDate = Date()
        try {
            val expDate = sdf.parse(viewModel.licenseExpirationDate) ?: return@remember null
            val diffTime = expDate.time - curDate.time
            val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()
            
            if (diffDays in 1..30) {
                if (diffDays <= 3) {
                    "⚠️ **Alerte Expiration** : Plus que $diffDays jours restants ! Renouvelez rapidement."
                } else {
                    "⏳ **Vigilance Licence** : Votre abonnement expire dans $diffDays jours (${viewModel.licenseExpirationDate})."
                }
            } else if (diffDays <= 0) {
                "❌ **Licence Expirée** : Accès bloqué immédiatement."
            } else null
        } catch (e: Exception) {
            null
        }
    }

    if (alerts.isEmpty() && licenseAlert == null) {
        // Backup recommendation callout only
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SdCard,
                    contentDescription = "Sauvegarde",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Conseil de sécurité : pensez à exporter régulièrement une sauvegarde local krediya_backup.db.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Alertes",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Alertes de Collecte & Système",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // License alert banner
                licenseAlert?.let { alertText ->
                    AlertMessageRow(
                        message = alertText,
                        color = Color(0xFFEA580C) // Bold Orange
                    )
                }

                // Loan alerts
                alerts.take(4).forEach { alert ->
                    AlertMessageRow(
                        message = alert,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Backup recommendation callout
                AlertMessageRow(
                    message = "💡 **Rappel Sauvegarde** : Pensez à exporter régulièrement vos données localement.",
                    color = EmeraldPrimary
                )
                
                // Action link for licensing
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onNavigate("plus") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Gérer la sécurité ➔", fontSize = 11.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertMessageRow(message: String, color: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "•", color = color, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        
        val annotatedText = androidx.compose.ui.text.buildAnnotatedString {
            val parts = message.split("**")
            for (i in parts.indices) {
                if (i % 2 == 1) {
                    androidx.compose.ui.text.withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                        append(parts[i])
                    }
                } else {
                    androidx.compose.ui.text.withStyle(style = androidx.compose.ui.text.SpanStyle(color = color.copy(alpha = 0.8f))) {
                        append(parts[i])
                    }
                }
            }
        }
        
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 16.sp
        )
    }
}
