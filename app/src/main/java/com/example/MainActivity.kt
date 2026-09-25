package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassSidebar
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AlarmScreen
import com.example.ui.screens.ApiRoomScreen
import com.example.ui.screens.CommandModeScreen
import com.example.ui.screens.MainWorkspaceScreen
import com.example.ui.screens.NameGeneratorScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.ReadNoteScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.SecurityAreaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StorageFileEditorScreen
import com.example.ui.screens.TableEditorScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data class MainWorkspace(val initialFolder: String = "All Notes") : Screen()
    data class ReadNote(val note: NoteEntity) : Screen()
    data class EditNote(val note: NoteEntity?) : Screen()
    data object Settings : Screen()
    data object ApiRoom : Screen()
    data object Alarm : Screen()
    data object SecurityArea : Screen()
    data object NameGenerator : Screen()
    data object CommandMode : Screen()
    data class TableEditor(val initialTableData: String, val onSaved: (String) -> Unit) : Screen()
    data object RecycleBin : Screen()
    data object StorageEditor : Screen()
    data class PdfViewer(val uri: android.net.Uri, val returnTo: Screen) : Screen()
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = NoteRepository(database.noteDao())
        val preferences = AppPreferences(applicationContext)

        setContent {
            val isDarkMode by preferences.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuNotesApp(
                        activity = this,
                        repository = repository,
                        preferences = preferences,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = {
                            preferences.setDarkMode(!isDarkMode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AuNotesApp(
    activity: FragmentActivity,
    repository: NoteRepository,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var screenBeforeRecycleBin by remember { mutableStateOf<Screen>(Screen.MainWorkspace()) }
    var isSidebarOpen by remember { mutableStateOf(false) }
    var isAiChatOpen by remember { mutableStateOf(false) }

    // Request Storage and Notification Permissions on first launch (PART B Item 3)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
        // The File Editor is intended to behave like a native file manager. On
        // Android 11+, request the user's explicit all-files access once on first launch.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val prefs = activity.getSharedPreferences("au_notes_prefs", android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean("storage_access_prompted", false)) {
                prefs.edit().putBoolean("storage_access_prompted", true).apply()
                runCatching {
                    activity.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    })
                }
            }
        }
    }

    // Intercept Back Press
    BackHandler(enabled = isAiChatOpen || isSidebarOpen || currentScreen !is Screen.MainWorkspace) {
        val screenNow = currentScreen
        when {
            isAiChatOpen -> isAiChatOpen = false
            isSidebarOpen -> isSidebarOpen = false
            screenNow is Screen.RecycleBin -> currentScreen = screenBeforeRecycleBin
            screenNow is Screen.PdfViewer -> currentScreen = screenNow.returnTo
            screenNow is Screen.Splash -> { /* Let splash finish */ }
            currentScreen !is Screen.MainWorkspace -> currentScreen = Screen.MainWorkspace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is Screen.Splash -> {
                SplashScreen(
                    isDarkMode = isDarkMode,
                    onSplashFinished = {
                        currentScreen = Screen.MainWorkspace()
                    }
                )
            }

            is Screen.MainWorkspace -> {
                MainWorkspaceScreen(
                    repository = repository,
                    preferences = preferences,
                    initialFolder = screen.initialFolder,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    onOpenSidebar = { isSidebarOpen = true },
                    onOpenSettings = { currentScreen = Screen.Settings },
                    onOpenAiChat = { isAiChatOpen = true },
                    onOpenNote = { note ->
                        currentScreen = Screen.ReadNote(note)
                    },
                    onCreateNote = {
                        currentScreen = Screen.EditNote(null)
                    }
                )
            }

            is Screen.ReadNote -> {
                ReadNoteScreen(
                    note = screen.note,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace() },
                    onEditNote = { note ->
                        currentScreen = Screen.EditNote(note)
                    },
                    onDeleteNote = { noteId ->
                        coroutineScope.launch {
                            repository.moveToTrash(noteId)
                            currentScreen = Screen.MainWorkspace()
                        }
                    },
                    onToggleLock = { noteId, isUnlocked ->
                        coroutineScope.launch {
                            repository.toggleLock(noteId, !isUnlocked)
                        }
                    }
                )
            }

            is Screen.EditNote -> {
                NoteEditorScreen(
                    initialNote = screen.note,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace() },
                    onOpenTableEditor = { initialData, onResult ->
                        currentScreen = Screen.TableEditor(initialData, onResult)
                    },
                    onSaveNote = { id, title, content, category, isBold, isItalic, isUnderline, isStrikethrough, isCodeFormat, fontSize, fontColorHex, alignment, listType, tableData, styleSpansJson, attachmentsJson, onSaved ->
                        coroutineScope.launch {
                            val finalTitle = title.ifBlank { "Untitled Note" }
                            val existing = screen.note
                            val targetFolder = when (category) {
                                "API" -> "APIs Keys"
                                "Code" -> "Code"
                                "Media" -> "Media"
                                "Personal" -> "Personal"
                                else -> existing?.folder ?: "All Notes"
                            }
                            val entity = NoteEntity(
                                id = id,
                                title = finalTitle,
                                content = content,
                                category = category,
                                folder = targetFolder,
                                isFavorite = existing?.isFavorite ?: false,
                                isPinned = existing?.isPinned ?: false,
                                isLocked = existing?.isLocked ?: false,
                                isTrash = existing?.isTrash ?: false,
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                isBold = isBold,
                                isItalic = isItalic,
                                isUnderline = isUnderline,
                                isStrikethrough = isStrikethrough,
                                isCodeFormat = isCodeFormat,
                                fontSize = fontSize,
                                fontColorHex = fontColorHex,
                                alignment = alignment,
                                listType = listType,
                                tableData = tableData,
                                styleSpansJson = styleSpansJson,
                                attachmentsJson = attachmentsJson,
                                updatedAt = System.currentTimeMillis()
                            )
                            val savedId = repository.saveNote(entity)
                            onSaved(savedId)
                        }
                    }
                )
            }

            is Screen.TableEditor -> {
                TableEditorScreen(
                    initialTableData = screen.initialTableData,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.EditNote(null) },
                    onSaveTable = { savedData ->
                        screen.onSaved(savedData)
                        currentScreen = Screen.EditNote(null)
                    }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace() },
                    onOpenSecurityArea = { currentScreen = Screen.SecurityArea },
                    onOpenApiRoom = { currentScreen = Screen.ApiRoom },
                    onOpenRecycleBin = {
                        screenBeforeRecycleBin = Screen.Settings
                        currentScreen = Screen.RecycleBin
                    }
                )
            }

            is Screen.Alarm -> {
                AlarmScreen(preferences, isDarkMode, onBack = { currentScreen = Screen.MainWorkspace() }, onEdit = { currentScreen = Screen.MainWorkspace() })
            }

            is Screen.ApiRoom -> {
                ApiRoomScreen(
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.Settings },
                    onRequestUnlock = { afterUnlock ->
                        // Reuse the existing security dialog from Settings by requiring setup first.
                        // The screen itself remains inaccessible until the security gate succeeds.
                        if (!preferences.hasSecuritySetup()) {
                            currentScreen = Screen.SecurityArea
                        } else {
                            com.example.ui.components.PinLockDialog(
                                correctPin = preferences.lockPin.value,
                                title = "API Room Locked",
                                subtitle = "Enter your security PIN to continue",
                                securityQuestion = preferences.securityQuestion.value,
                                securityAnswer = preferences.securityAnswer.value,
                                isDarkMode = isDarkMode,
                                onDismiss = { currentScreen = Screen.Settings },
                                onUnlocked = afterUnlock,
                                onPinReset = { newPin -> preferences.setLockPin(newPin); afterUnlock() },
                                onSecuritySetup = { pin, question, answer -> preferences.setSecurityDetails(pin, question, answer); afterUnlock() }
                            )
                        }
                    }
                )
            }

            is Screen.SecurityArea -> {
                SecurityAreaScreen(
                    repository = repository,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.Settings },
                    onOpenNote = { note -> currentScreen = Screen.ReadNote(note) }
                )
            }

            is Screen.NameGenerator -> {
                NameGeneratorScreen(
                    repository = repository,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace() }
                )
            }

            is Screen.CommandMode -> {
                CommandModeScreen(
                    repository = repository,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace() },
                    onCreateNewNote = { currentScreen = Screen.EditNote(null) },
                    onOpenNote = { note -> currentScreen = Screen.ReadNote(note) },
                    onOpenFolder = { folder -> currentScreen = Screen.MainWorkspace(folder) }
                )
            }

            is Screen.RecycleBin -> {
                RecycleBinScreen(
                    repository = repository,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = screenBeforeRecycleBin }
                )
            }

            is Screen.StorageEditor -> {
                StorageFileEditorScreen(
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace() },
                    onOpenPdf = { uri ->
                        currentScreen = Screen.PdfViewer(uri, returnTo = Screen.StorageEditor)
                    },
                    onOpenFileInEditor = { fileTitle, fileContent, category ->
                        val virtualNote = NoteEntity(
                            id = 0L,
                            title = fileTitle,
                            content = fileContent,
                            category = category,
                            isCodeFormat = category == "Code"
                        )
                        currentScreen = Screen.EditNote(virtualNote)
                    }
                )
            }

            is Screen.PdfViewer -> {
                PdfViewerScreen(
                    pdfUri = screen.uri,
                    isDarkMode = isDarkMode,
                    onClose = { currentScreen = screen.returnTo }
                )
            }
        }

        // Navigation Drawer / Sidebar
        GlassSidebar(
            isOpen = isSidebarOpen,
            isDarkMode = isDarkMode,
            activeAlarm = preferences.activeAlarm.collectAsState().value,
            blinkAlarmIndicator = preferences.blinkAlarmIndicator.collectAsState().value,
            onClose = { isSidebarOpen = false },
            onNavigateHome = {
                isSidebarOpen = false
                currentScreen = Screen.MainWorkspace("All Notes")
            },
            onNavigateFolder = { folder ->
                isSidebarOpen = false
                val targetFolder = when (folder) {
                    "Code Snippets" -> "Code"
                    else -> folder
                }
                currentScreen = Screen.MainWorkspace(targetFolder)
            },
            onOpenAlarm = {
                isSidebarOpen = false
                currentScreen = Screen.Alarm
            },
            onOpenFileEditor = {
                isSidebarOpen = false
                currentScreen = Screen.StorageEditor
            },
            onOpenNameGenerator = {
                isSidebarOpen = false
                currentScreen = Screen.NameGenerator
            },
            onOpenCommandMode = {
                isSidebarOpen = false
                currentScreen = Screen.CommandMode
            },
            onOpenSettings = {
                isSidebarOpen = false
                currentScreen = Screen.Settings
            },
            onOpenRecycleBin = {
                isSidebarOpen = false
                screenBeforeRecycleBin = Screen.MainWorkspace()
                currentScreen = Screen.RecycleBin
            },
            onOpenFeaturesModal = {
                isSidebarOpen = false
                isAiChatOpen = true
            }
        )

        // AI Chat Engine Drawer
        AiChatScreen(
            isOpen = isAiChatOpen,
            isDarkMode = isDarkMode,
            preferences = preferences,
            repository = repository,
            currentNoteContent = (currentScreen as? Screen.ReadNote)?.note?.content
                ?: (currentScreen as? Screen.EditNote)?.note?.content,
            onClose = { isAiChatOpen = false },
            onCreateNoteFromAi = { title, content, category ->
                coroutineScope.launch {
                    val newEntity = NoteEntity(
                        id = 0L,
                        title = title,
                        content = content,
                        category = category,
                        isCodeFormat = category == "Code"
                    )
                    repository.saveNote(newEntity)
                }
            },
            onOpenNoteFromAi = { note ->
                isAiChatOpen = false
                currentScreen = Screen.ReadNote(note)
            }
        )
    }
}
