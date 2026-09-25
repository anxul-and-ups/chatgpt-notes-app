package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.PinLockDialog
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.SirenAudioPlayer
import kotlinx.coroutines.launch
import java.util.Locale

data class CommandLog(
    val sender: String, // "USER" or "AU_VOICE"
    val message: String,
    val isError: Boolean = false
)

@Composable
fun CommandModeScreen(
    repository: NoteRepository,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onCreateNewNote: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onOpenFolder: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allNotes by repository.allActiveNotes.collectAsState(initial = emptyList())
    val lockedFolders by preferences.lockedFolders.collectAsState()
    val customFolders by preferences.customFolders.collectAsState()

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    val logs = remember {
        mutableStateListOf(
            CommandLog("AU_VOICE", "Voice Command Mode Active. Say commands like 'create new note', 'summary notes', 'gemini api', 'delete notes' or 'open [note title]'.")
        )
    }

    var showPinDialogForApi by remember { mutableStateOf(false) }
    var pendingActionOnPinSuccess by remember { mutableStateOf<(() -> Unit)?>(null) }

    // TTS Engine
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    fun speak(text: String) {
        if (isTtsReady && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AU_COMMAND_TTS")
        }
    }

    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsReady = true
            }
        }
        tts = ttsInstance

        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
            SirenAudioPlayer.stop()
        }
    }

    // Speech Recognizer
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }

    fun processVoiceCommand(raw: String) {
        val cmd = raw.trim().lowercase(Locale.ROOT)
        logs.add(CommandLog("USER", raw))

        when {
            cmd.contains("create new note") || cmd.contains("new note") || cmd.contains("make note") -> {
                speak("Opening note editor to create a new note.")
                logs.add(CommandLog("AU_VOICE", "Creating new note..."))
                onCreateNewNote()
            }

            cmd.startsWith("search note ") || cmd.startsWith("find note ") -> {
                val query = cmd.removePrefix("search note ").removePrefix("find note ").trim()
                val matches = allNotes.filter {
                    !it.isLocked && !it.isHidden && it.folder !in lockedFolders &&
                        (it.title.contains(query, true) || it.content.contains(query, true))
                }
                if (matches.isEmpty()) {
                    speak("No notes matched $query.")
                    logs.add(CommandLog("AU_VOICE", "No notes found for '$query'."))
                } else {
                    val titles = matches.take(5).joinToString(", ") { it.title }
                    speak("Found ${matches.size} notes. $titles")
                    logs.add(CommandLog("AU_VOICE", "Found ${matches.size} notes: $titles"))
                }
            }

            cmd.contains("summary") || cmd.contains("summarize") -> {
                val accessibleNotes = allNotes.filter { !it.isLocked && !it.isHidden && it.folder !in lockedFolders }
                val count = accessibleNotes.size
                val titles = accessibleNotes.take(3).joinToString(", ") { it.title }
                val response = "You have $count active notes. Recent notes include: $titles."
                speak(response)
                logs.add(CommandLog("AU_VOICE", response))
            }

            cmd.contains("delete note") || cmd.contains("delete notes") -> {
                val requested = if (cmd.contains("delete notes")) "" else cmd.substringAfter("delete note", "").trim()
                val latest = if (requested.isNotBlank()) allNotes.find { it.title.contains(requested, true) } else allNotes.firstOrNull()
                if (latest != null) {
                    if (latest.isLocked || latest.isHidden || latest.folder in lockedFolders) {
                        speak("Access Denied. This note is protected.")
                        SirenAudioPlayer.playWrongPin(context)
                        pendingActionOnPinSuccess = { coroutineScope.launch { repository.moveToTrash(latest.id); speak("Moved ${latest.title} to Recycle Bin.") } }
                        showPinDialogForApi = true
                    } else {
                        coroutineScope.launch {
                            repository.moveToTrash(latest.id)
                            speak("Moved ${latest.title} to Recycle Bin.")
                            logs.add(CommandLog("AU_VOICE", "Moved note '${latest.title}' to Recycle Bin."))
                        }
                    }
                } else {
                    speak("No notes available to delete.")
                    logs.add(CommandLog("AU_VOICE", "No notes found in workspace."))
                }
            }

            cmd.contains("open note") || cmd.contains("open") -> {
                val query = cmd.replace("open note", "").replace("open", "").trim()
                val match = allNotes.find { it.title.lowercase().contains(query) } ?: allNotes.firstOrNull()
                if (match != null) {
                    if (match.isLocked || match.isHidden || match.folder in lockedFolders) {
                        speak("Access Denied. This note is protected.")
                        SirenAudioPlayer.playWrongPin(context)
                        pendingActionOnPinSuccess = { onOpenNote(match) }
                        showPinDialogForApi = true
                    } else {
                        speak("Opening ${match.title}.")
                        logs.add(CommandLog("AU_VOICE", "Opening note: ${match.title}"))
                        onOpenNote(match)
                    }
                } else {
                    speak("Could not find matching note.")
                    logs.add(CommandLog("AU_VOICE", "No matching note found for '$query'."))
                }
            }

            cmd.contains("gemini api") || cmd.contains("api key") || cmd.contains("api do mujhe") || cmd.contains("api previews") -> {
                // Sensitive request: Prompt for PIN
                speak("Security verification required. Please confirm your PIN.")
                logs.add(CommandLog("AU_VOICE", "Security check: PIN required for API key access."))
                pendingActionOnPinSuccess = {
                    val key = preferences.getEffectiveApiKey()
                    val masked = if (key.length > 8) key.take(4) + "••••••••" + key.takeLast(4) else "••••••••"
                    val msg = "Access Granted. Effective Gemini Key: $masked"
                    speak("Access Granted. Showing your Gemini API credentials.")
                    logs.add(CommandLog("AU_VOICE", msg))
                }
                showPinDialogForApi = true
            }

            cmd.startsWith("create folder ") || cmd.startsWith("new folder ") -> {
                val name = cmd.removePrefix("create folder ").removePrefix("new folder ").trim()
                if (preferences.addCustomFolder(name)) { speak("Created folder $name."); logs.add(CommandLog("AU_VOICE", "Created folder: $name")) }
                else { speak("I could not create that folder."); logs.add(CommandLog("AU_VOICE", "Folder creation failed.")) }
            }

            cmd.startsWith("delete folder ") -> {
                val name = cmd.removePrefix("delete folder ").trim()
                val actual = customFolders.firstOrNull { it.equals(name, true) }
                if (actual == null) {
                    speak("That custom folder was not found.")
                } else if (preferences.isFolderLocked(actual)) {
                    speak("Access Denied. This folder is protected.")
                    SirenAudioPlayer.playWrongPin(context)
                    pendingActionOnPinSuccess = {
                        coroutineScope.launch {
                            repository.moveFolderNotes(actual, "All Notes")
                            preferences.removeCustomFolder(actual)
                            preferences.setFolderLocked(actual, false)
                            preferences.setFolderHidden(actual, false)
                            speak("Deleted folder $actual and moved its notes to All Notes.")
                        }
                    }
                    showPinDialogForApi = true
                } else {
                    coroutineScope.launch { repository.moveFolderNotes(actual, "All Notes"); preferences.removeCustomFolder(actual); speak("Deleted folder $actual and moved its notes to All Notes.") }
                }
            }

            cmd.startsWith("open folder ") -> {
                val name = cmd.removePrefix("open folder ").trim()
                if (name in customFolders || name in listOf("all notes", "favorites", "apis keys", "code", "media", "personal")) {
                    val canonical = when (name.lowercase(Locale.ROOT)) {
                        "all notes" -> "All Notes"
                        "favorites" -> "Favorites"
                        "apis keys", "api keys" -> "APIs Keys"
                        "code" -> "Code"
                        "media" -> "Media"
                        "personal" -> "Personal"
                        else -> customFolders.firstOrNull { it.equals(name, true) } ?: name
                    }
                    if (preferences.isFolderLocked(canonical)) {
                        speak("Access Denied. This folder is protected.")
                        SirenAudioPlayer.playWrongPin(context)
                        pendingActionOnPinSuccess = { onOpenFolder(canonical) }
                        showPinDialogForApi = true
                    } else {
                        speak("Opening folder $canonical.")
                        logs.add(CommandLog("AU_VOICE", "Opening folder: $canonical"))
                        onOpenFolder(canonical)
                    }
                } else speak("Folder $name was not found.")
            }

            cmd.contains("folder") -> {
                val builtIns = listOf("All Notes", "Favorites", "APIs Keys", "Code", "Media", "Personal")
                val names = (builtIns + customFolders).joinToString(", ")
                speak("You have folders: $names.")
                logs.add(CommandLog("AU_VOICE", "Folders active: $names"))
            }

            else -> {
                val defaultMsg = "Command received: '$raw'. Try saying 'create new note', 'summary notes', 'open note [title]' or 'gemini api'."
                speak(defaultMsg)
                logs.add(CommandLog("AU_VOICE", defaultMsg))
            }
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(error: Int) {
                    isListening = false
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        recognizedText = text
                        processVoiceCommand(text)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer = recognizer
            recognizer.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "Could not start mic: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for Voice Control", Toast.LENGTH_SHORT).show()
        }
    }

    // Glowing Pulse Animation for Mic
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    GlassBackground(isDarkMode = isDarkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
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
                                text = "Command Mode",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF111111)
                            )
                            Text(
                                text = "AI Voice Assistant & Navigation",
                                fontSize = 11.sp,
                                color = CrimsonPrimary
                            )
                        }
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_svg_speaker),
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Glowing Mic Hero Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer pulsating glow
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                if (isListening) CrimsonPrimary.copy(alpha = 0.35f)
                                else Color(0xFF7C3AED).copy(alpha = 0.2f)
                            )
                    )

                    // Main mic button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (isListening) listOf(CrimsonPrimary, Color(0xFFFF5E7E))
                                    else listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                                )
                            )
                            .clickable {
                                if (isListening) {
                                    speechRecognizer?.stopListening()
                                    isListening = false
                                } else {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        startListening()
                                    } else {
                                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = if (isListening) "Listening... Speak your command now" else "Tap microphone to speak",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isListening) CrimsonPrimary else (if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Console Command Logs
                Text(
                    text = "VOICE CONSOLE FEED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        val isUser = log.sender == "USER"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                shape = RoundedCornerShape(14.dp),
                                isDarkMode = isDarkMode,
                                elevation = 2.dp
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isUser) "You" else "AU Assistant",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.isError) Color.Red else (if (isUser) Color(0xFF38BDF8) else CrimsonPrimary)
                                        )
                                        if (log.isError) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.message,
                                        fontSize = 13.sp,
                                        color = if (isDarkMode) Color.White else Color(0xFF111111)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PIN Guard Dialog for sensitive voice commands (e.g. "gemini api do mujhe")
    if (showPinDialogForApi) {
        val storedPin by preferences.lockPin.collectAsState()
        PinLockDialog(
            correctPin = storedPin,
            isDarkMode = isDarkMode,
            onDismiss = {
                showPinDialogForApi = false
                speak("Security verification cancelled.")
            },
            onUnlocked = {
                showPinDialogForApi = false
                pendingActionOnPinSuccess?.invoke()
            },
            onSecuritySetup = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
            }
        )
    }
}
