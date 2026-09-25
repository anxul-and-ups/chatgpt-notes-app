package com.example.ui.screens

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.launch

@Composable
fun SecurityAreaScreen(
    repository: NoteRepository,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allNotes by repository.allActiveNotes.collectAsState(initial = emptyList())
    val storedPin by preferences.lockPin.collectAsState()
    val secQuestion by preferences.securityQuestion.collectAsState()
    val secAnswer by preferences.securityAnswer.collectAsState()
    val lockedFolders by preferences.lockedFolders.collectAsState()
    val hiddenFolders by preferences.hiddenFolders.collectAsState()
    val folderOrder by preferences.folderOrder.collectAsState()
    val hiddenNotesFromDb by repository.hiddenNotes.collectAsState(initial = emptyList())

    var isVaultUnlocked by remember { mutableStateOf(false) }
    var showAuthPrompt by remember { mutableStateOf(true) }
    var showHidePicker by remember { mutableStateOf(false) }
    var showRestorePicker by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val securityAutoLock by preferences.securityAutoLock.collectAsState()
    DisposableEffect(lifecycleOwner, securityAutoLock) {
        val autoLock = securityAutoLock
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // Screen-off/lock must immediately relock the vault.
                Lifecycle.Event.ON_PAUSE -> if (autoLock == "screen") {
                    isVaultUnlocked = false
                    showAuthPrompt = true
                }
                // App-level background/closure policy.
                Lifecycle.Event.ON_STOP -> if (autoLock == "closed") {
                    isVaultUnlocked = false
                    showAuthPrompt = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // "When leaving Security Area" relocks before navigation. "Immediately"
    // follows the same safe boundary so the vault is never left exposed.
    BackHandler(enabled = !showAuthPrompt) {
        if (isVaultUnlocked && (securityAutoLock == "leaving" || securityAutoLock == "immediately")) {
            isVaultUnlocked = false
            showAuthPrompt = true
        }
        onBack()
    }

    if (showAuthPrompt && !isVaultUnlocked) {
        com.example.ui.components.PinLockDialog(
            correctPin = storedPin,
            title = "Security Vault Locked",
            subtitle = "Enter 4-digit PIN or use Fingerprint to unlock",
            securityQuestion = secQuestion,
            securityAnswer = secAnswer,
            isDarkMode = isDarkMode,
            onDismiss = {
                showAuthPrompt = false
                if (!isVaultUnlocked) {
                    onBack()
                }
            },
            onUnlocked = {
                isVaultUnlocked = true
                showAuthPrompt = false
            },
            onPinReset = { newPin ->
                preferences.setLockPin(newPin)
                isVaultUnlocked = true
                showAuthPrompt = false
                Toast.makeText(context, "PIN Reset Successfully!", Toast.LENGTH_SHORT).show()
            },
            onSecuritySetup = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
            }
        )
    }

    val hiddenNotes = hiddenNotesFromDb.filter { !it.isTrash }
    val protectedNotes = allNotes.filter { !it.isTrash && it.isLocked && !it.isHidden }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 Hidden Notes, 1 Protected Notes, 2 Hidden Folders, 3 Protected Folders, 4 Change Lock

    // Change Lock fields
    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var newSecQuestionInput by remember { mutableStateOf(secQuestion) }
    var newSecAnswerInput by remember { mutableStateOf("") }
    var isOldPinVerified by remember { mutableStateOf(false) }

    GlassBackground(isDarkMode = isDarkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeuIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            isDarkMode = isDarkMode,
                            size = 38.dp,
                            iconSize = 18.dp,
                            tint = if (isDarkMode) Color.White else Color.Black,
                            onClick = onBack
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Security Area",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF111111)
                            )
                            Text(
                                text = "PIN & Biometric Protected Vault",
                                fontSize = 11.sp,
                                color = CrimsonPrimary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showHidePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Hide items", tint = CrimsonPrimary)
                        }
                        IconButton(onClick = { showRestorePicker = true }) {
                            Icon(Icons.Default.Remove, contentDescription = "Unhide items", tint = CrimsonPrimary)
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_svg_lock),
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isVaultUnlocked) {
                    // Vault Locked Placeholder UI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_svg_lock),
                                contentDescription = "Locked",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Security Vault is Locked",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Authentication required to access protected files",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            GlassCard(
                                modifier = Modifier.padding(horizontal = 32.dp),
                                shape = RoundedCornerShape(14.dp),
                                isDarkMode = isDarkMode,
                                onClick = { showAuthPrompt = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF5E7E))))
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Unlock with PIN / Biometrics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Tab Selector: Hidden Notes | Hidden Folders | Protected Folders | Change Lock
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = CrimsonPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = CrimsonPrimary
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Hidden Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Protected Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Hidden Folders", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Protected", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = { Text("Change Lock", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab 0: Hidden Notes List
                if (selectedTab == 0) {
                    if (hiddenNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_svg_lock),
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No locked or hidden notes found.",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Long-press any note on Home Screen and select 'Lock'.",
                                    color = CrimsonPrimary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(hiddenNotes) { note ->
                                var isUnlockingAnim by remember { mutableStateOf(false) }
                                val lockRotation by animateFloatAsState(
                                    targetValue = if (isUnlockingAnim) -35f else 0f,
                                    animationSpec = tween(350),
                                    label = "lockAnim"
                                )

                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    isDarkMode = isDarkMode,
                                    elevation = 3.dp,
                                    onClick = { onOpenNote(note) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(if (isUnlockingAnim) R.drawable.ic_svg_unlock else R.drawable.ic_svg_lock),
                                                    contentDescription = null,
                                                    tint = CrimsonPrimary,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .rotate(lockRotation)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = note.title,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkMode) Color.White else Color(0xFF111111),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = note.content.take(60),
                                                fontSize = 12.sp,
                                                color = if (isDarkMode) Color.White.copy(0.6f) else Color.DarkGray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Unhide / Unlock Button with smooth animation
                                            GlassCard(
                                                shape = RoundedCornerShape(10.dp),
                                                isDarkMode = isDarkMode,
                                                onClick = {
                                                    isUnlockingAnim = true
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(250)
                                                        repository.unhideNote(note)
                                                        Toast.makeText(context, "Note restored to ${note.originalFolder}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_svg_unlock),
                                                        contentDescription = "Unlock",
                                                        tint = Color(0xFF4CAF50),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Unhide",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF4CAF50)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 1: Protected Notes
                else if (selectedTab == 1) {
                    if (protectedNotes.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No protected notes", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(protectedNotes) { note ->
                                GlassCard(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), isDarkMode, elevation = 3.dp, onClick = { onOpenNote(note) }) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(painterResource(R.drawable.ic_svg_lock), "Protected note", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(note.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("Protected note", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                        Icon(painter = painterResource(R.drawable.ic_svg_edit), contentDescription = null, tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 2: Hidden Folders
                else if (selectedTab == 2) {
                    if (hiddenFolders.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No hidden folders", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(hiddenFolders.toList().sorted()) { folder ->
                                GlassCard(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), isDarkMode, elevation = 3.dp) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painterResource(R.drawable.ic_svg_folder), "Hidden Folder", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(folder, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = { preferences.setFolderHidden(folder, false); Toast.makeText(context, "Folder restored", Toast.LENGTH_SHORT).show() }) { Text("Unhide", color = Color(0xFF4CAF50)) }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 3: Protected / Locked Folders
                else if (selectedTab == 3) {
                    val allFolders = folderOrder
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allFolders) { folder ->
                            val isFolderLocked = lockedFolders.contains(folder)
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                isDarkMode = isDarkMode,
                                elevation = 2.dp,
                                onClick = {
                                    preferences.toggleFolderLock(folder)
                                    Toast.makeText(
                                        context,
                                        if (!isFolderLocked) "Locked $folder" else "Unlocked $folder",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_svg_folder),
                                            contentDescription = null,
                                            tint = if (isFolderLocked) CrimsonPrimary else Color(0xFFFFCA28),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = folder,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else Color.Black
                                            )
                                            Text(
                                                text = if (isFolderLocked) "Protected with PIN" else "Normal folder",
                                                fontSize = 11.sp,
                                                color = if (isFolderLocked) CrimsonPrimary else Color.Gray
                                            )
                                        }
                                    }

                                    Icon(
                                        painter = painterResource(if (isFolderLocked) R.drawable.ic_svg_lock else R.drawable.ic_svg_unlock),
                                        contentDescription = null,
                                        tint = if (isFolderLocked) CrimsonPrimary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tab 4: Change Lock with Strict Existing PIN Guard
                else if (selectedTab == 4) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), isDarkMode = isDarkMode) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Security Auto Lock", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Choose when the Security Area requires PIN again.", fontSize = 11.sp, color = Color.Gray)
                                listOf("immediately" to "Immediately", "leaving" to "When leaving Security Area", "closed" to "When app is closed", "screen" to "When screen is locked / turned off").forEach { (key,label) ->
                                    Row(Modifier.fillMaxWidth().clickable { preferences.setSecurityAutoLock(key) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.RadioButton(selected = securityAutoLock == key, onClick = { preferences.setSecurityAutoLock(key) })
                                        Text(label, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        if (!isOldPinVerified) {
                            Text(
                                text = "Verify Existing Passcode First",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                            Text(
                                text = "You must enter your current 4-digit PIN before setting a new passcode.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = oldPinInput,
                                onValueChange = { if (it.length <= 4) oldPinInput = it },
                                label = { Text("Current 4-Digit PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CrimsonPrimary,
                                    focusedLabelColor = CrimsonPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                isDarkMode = isDarkMode,
                                onClick = {
                                    if (oldPinInput == storedPin) {
                                        isOldPinVerified = true
                                        Toast.makeText(context, "Old PIN verified! Enter new details below.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Incorrect current PIN!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF5E7E))))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Verify & Continue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Text(
                                text = "Set New Security Credentials",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { if (it.length <= 4) newPinInput = it },
                                label = { Text("New 4-Digit PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CrimsonPrimary,
                                    focusedLabelColor = CrimsonPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = newSecQuestionInput,
                                onValueChange = { newSecQuestionInput = it },
                                label = { Text("Security Recovery Question") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CrimsonPrimary,
                                    focusedLabelColor = CrimsonPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = newSecAnswerInput,
                                onValueChange = { newSecAnswerInput = it },
                                label = { Text("Security Answer") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CrimsonPrimary,
                                    focusedLabelColor = CrimsonPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                isDarkMode = isDarkMode,
                                onClick = {
                                    if (newPinInput.length == 4 && newSecAnswerInput.isNotBlank()) {
                                        preferences.setSecurityDetails(newPinInput, newSecQuestionInput, newSecAnswerInput)
                                        Toast.makeText(context, "Passcode & Security Question Updated!", Toast.LENGTH_SHORT).show()
                                        isOldPinVerified = false
                                        oldPinInput = ""
                                        newPinInput = ""
                                        newSecAnswerInput = ""
                                    } else {
                                        Toast.makeText(context, "Please enter 4 digits PIN and an answer.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Save New Security Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

    if (showRestorePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRestorePicker = false },
            title = { Text("Restore Hidden Items") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tap an item to restore it to its original workspace location.", fontSize = 12.sp, color = Color.Gray)
                    hiddenNotes.filter { !it.isTrash }.forEach { note ->
                        TextButton(onClick = {
                            coroutineScope.launch { repository.unhideNote(note) }
                            Toast.makeText(context, "Note restored", Toast.LENGTH_SHORT).show()
                        }) { Text("Note • ${note.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    hiddenFolders.toList().sorted().forEach { folder ->
                        TextButton(onClick = {
                            preferences.setFolderHidden(folder, false)
                            Toast.makeText(context, "Folder restored", Toast.LENGTH_SHORT).show()
                        }) { Text("Folder • $folder", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRestorePicker = false }) { Text("Done") } }
        )
    }

    if (showHidePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHidePicker = false },
            title = { Text("Hide Notes & Folders") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tap an item to hide it in Security Area.", fontSize = 12.sp, color = Color.Gray)
                    allNotes.filter { !it.isTrash && !it.isHidden }.take(12).forEach { note ->
                        TextButton(onClick = {
                            coroutineScope.launch { repository.hideNote(note) }
                            Toast.makeText(context, "Note hidden", Toast.LENGTH_SHORT).show()
                        }) { Text("Note • ${note.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    hiddenFolders.toList().let { }
                    preferences.folderOrder.value.filter { it != "All Notes" && !preferences.isFolderHidden(it) }.forEach { folder ->
                        TextButton(onClick = {
                            preferences.setFolderHidden(folder, true)
                            Toast.makeText(context, "Folder hidden", Toast.LENGTH_SHORT).show()
                        }) { Text("Folder • $folder") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHidePicker = false }) { Text("Done") } }
        )
    }

    }
}
}
