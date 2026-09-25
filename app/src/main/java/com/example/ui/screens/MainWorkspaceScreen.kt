package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.ExportDialog
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.PinLockDialog
import com.example.ui.theme.CategoryApiColor
import com.example.ui.theme.CategoryCodeColor
import com.example.ui.theme.CategoryGeneralColor
import com.example.ui.theme.CategoryMediaColor
import com.example.ui.theme.CategoryPersonalColor
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainWorkspaceScreen(
    repository: NoteRepository,
    preferences: AppPreferences,
    initialFolder: String = "All Notes",
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenSidebar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onCreateNote: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val blurApis by preferences.blurApis.collectAsState()
    val lockPin by preferences.lockPin.collectAsState()
    val lockedFolders by preferences.lockedFolders.collectAsState()
    val hiddenFolders by preferences.hiddenFolders.collectAsState()
    val customFolders by preferences.customFolders.collectAsState()
    val folderOrder by preferences.folderOrder.collectAsState()
    var showAddFolderDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isDeepSearch by remember { mutableStateOf(false) } // PART A Item 5: Deep Search toggle
    var selectedFolder by remember(initialFolder) { mutableStateOf(initialFolder) }

    // Telegram Long-Press Action Sheet state (PART A Item 2)
    var longPressedNote by remember { mutableStateOf<NoteEntity?>(null) }
    var longPressedFolder by remember { mutableStateOf<String?>(null) }
    var noteToExport by remember { mutableStateOf<NoteEntity?>(null) }

    // PIN lock prompt for protected notes
    var pendingLockedNote by remember { mutableStateOf<NoteEntity?>(null) }
    var pendingUnlockNote by remember { mutableStateOf<NoteEntity?>(null) }
    var pendingSetupLockNote by remember { mutableStateOf<NoteEntity?>(null) }
    var pendingSetupLockFolder by remember { mutableStateOf<String?>(null) }
    var unlockedFoldersThisSession by remember { mutableStateOf(setOf<String>()) }
    var pendingFolderToOpen by remember { mutableStateOf<String?>(null) }

    fun openFolder(folderName: String) {
        val requiresLock = preferences.isFolderLocked(folderName)
        if (requiresLock && folderName !in unlockedFoldersThisSession) {
            pendingFolderToOpen = folderName
        } else {
            selectedFolder = folderName
        }
    }

    // Observe active notes
    val allNotes by repository.allActiveNotes.collectAsState(initial = emptyList())

    val filteredNotes = remember(allNotes, selectedFolder, searchQuery, isDeepSearch, lockedFolders, unlockedFoldersThisSession, hiddenFolders) {
        allNotes.filter { note ->
            if (note.isTrash) return@filter false
            // Protected notes never expose their title/preview on the normal workspace.
            // They are available only from Security Area after PIN verification.
            if (note.isLocked) return@filter false
            // Hidden folders hide the entire folder from Home, including its notes.
            if (note.folder in hiddenFolders) return@filter false
            // A locked folder's notes stay inaccessible until that folder is unlocked.
            if (note.folder in lockedFolders && note.folder !in unlockedFoldersThisSession) return@filter false
            if (note.isHidden) return@filter false

            // Folder Filter
            val matchesFolder = when (selectedFolder) {
                "All Notes" -> true
                "Favorites" -> note.isFavorite
                "APIs Keys" -> note.category == "API"
                "Code" -> note.category == "Code"
                "Media" -> note.category == "Media"
                "Personal" -> note.category == "Personal"
                else -> note.folder == selectedFolder
            }

            if (!matchesFolder) return@filter false

            // Search Query Filter
            if (searchQuery.isBlank()) {
                true
            } else {
                if (isDeepSearch) {
                    // Deep search = Title or Content
                    note.title.contains(searchQuery, ignoreCase = true) ||
                            note.content.contains(searchQuery, ignoreCase = true)
                } else {
                    // Normal search = Title only
                    note.title.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    val folderChips = folderOrder.filter { it == "All Notes" || it == "Favorites" || it == "APIs Keys" || it == "Code" || it == "Media" || it == "Personal" || it in customFolders }

    fun exportFolderAsZip(folderName: String) {
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val notesInFolder = allNotes.filter {
                    when (folderName) {
                        "All Notes" -> true
                        "Favorites" -> it.isFavorite
                        "APIs Keys" -> it.category == "API"
                        "Code" -> it.category == "Code"
                        "Media" -> it.category == "Media"
                        "Personal" -> it.category == "Personal"
                        else -> it.folder == folderName
                    }
                }.filter { !it.isLocked && !it.isHidden }

                val tempDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val safeName = folderName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                val zipFile = File(tempDir, "${safeName}_notes.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    notesInFolder.forEachIndexed { index, note ->
                        val fileName = "${index + 1}_${note.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}.txt"
                        zos.putNextEntry(ZipEntry(fileName))
                        val noteBody = "TITLE: ${note.title}\nCATEGORY: ${note.category}\nDATE: ${Date(note.updatedAt)}\n\n${note.content}"
                        zos.write(noteBody.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }

                val exportedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, zipFile.name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AU Notes")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("Could not create device storage file")
                    try {
                        resolver.openOutputStream(uri)?.use { out -> zipFile.inputStream().use { it.copyTo(out) } }
                            ?: throw IllegalStateException("Could not write device storage file")
                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        "Documents/AU Notes/${zipFile.name}"
                    } catch (e: Exception) {
                        resolver.delete(uri, null, null)
                        throw e
                    }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AU Notes").apply { mkdirs() }
                    val out = File(dir, zipFile.name)
                    zipFile.copyTo(out, overwrite = true)
                    out.absolutePath
                }

                launch(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Exported ${notesInFolder.size} notes to $exportedPath", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeuIconButton(
                            icon = Icons.Default.Menu,
                            contentDescription = "Open Sidebar",
                            isDarkMode = isDarkMode,
                            size = 40.dp,
                            iconSize = 20.dp,
                            tint = if (isDarkMode) Color.White else Color(0xFF222222),
                            onClick = onOpenSidebar
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "AU NOTES",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimsonPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${filteredNotes.size} notes in $selectedFolder",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Dark/Light Mode Switcher using SVG Icons (PART J)
                        NeuIconButton(
                            painter = painterResource(if (isDarkMode) R.drawable.ic_svg_sun else R.drawable.ic_svg_moon),
                            contentDescription = "Toggle Day/Night Mode",
                            isDarkMode = isDarkMode,
                            size = 38.dp,
                            iconSize = 18.dp,
                            tint = if (isDarkMode) Color(0xFFFFCA72) else Color(0xFF37474F),
                            onClick = onToggleDarkMode
                        )

                        NeuIconButton(
                            painter = painterResource(R.drawable.ic_svg_settings),
                            contentDescription = "Settings",
                            isDarkMode = isDarkMode,
                            size = 38.dp,
                            iconSize = 18.dp,
                            tint = CrimsonPrimary,
                            onClick = onOpenSettings
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar with Deep Search Toggle (PART A Item 5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = if (isDeepSearch) "Deep Search (content & title)..." else "Search title...",
                                fontSize = 13.sp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            // Deep Search Toggle Chip
                            Row(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDeepSearch) CrimsonPrimary else (if (isDarkMode) Color(0x33FFFFFF) else Color(0x1F000000)))
                                    .clickable { isDeepSearch = !isDeepSearch }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Deep",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDeepSearch) Color.White else (if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray)
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F718096),
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Folder Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    folderChips.filterNot { it != "All Notes" && hiddenFolders.contains(it) }.forEach { folder ->
                        val isSelected = selectedFolder == folder
                        val isFolderLocked = lockedFolders.contains(folder)
                        val folderIconRes = when (folder) {
                            "All Notes" -> R.drawable.ic_custom_folder
                            "Favorites" -> R.drawable.ic_svg_favorite
                            "APIs Keys" -> R.drawable.ic_svg_vpn
                            "Code" -> R.drawable.ic_code_snippet
                            "Media" -> R.drawable.ic_svg_media
                            "Personal" -> R.drawable.ic_fingerprint_attachment
                            else -> R.drawable.ic_custom_folder
                        }
                        val iconTint = when (folder) {
                            "All Notes" -> Color(0xFFFFB300)
                            "Favorites" -> Color(0xFFFF5252)
                            "APIs Keys" -> CategoryApiColor
                            "Code" -> CategoryCodeColor
                            "Media" -> CategoryMediaColor
                            "Personal" -> CategoryPersonalColor
                            else -> CrimsonPrimary
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CrimsonPrimary else if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0F000000))
                                .border(1.dp, if (isSelected) CrimsonPrimary else if (isDarkMode) Color(0x26FFFFFF) else Color(0x1F718096), RoundedCornerShape(12.dp))
                                .combinedClickable(onClick = { openFolder(folder) }, onLongClick = { longPressedFolder = folder })
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(folderIconRes), contentDescription = folder, tint = if (isSelected) Color.White else iconTint, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                if (isFolderLocked) { Icon(painterResource(R.drawable.ic_svg_lock), "Locked Folder", tint = if (isSelected) Color.White else CrimsonPrimary, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)) }
                                Text(folder, fontSize = 12.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(.85f) else Color(0xFF333333))
                            }
                        }
                    }
                    GlassCard(shape = RoundedCornerShape(12.dp), isDarkMode = isDarkMode, onClick = { showAddFolderDialog = true }) {
                        Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_svg_folder), "Add Folder", tint = CrimsonPrimary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Folder", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes List with Compact Balanced Size (PART A Item 1)
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_svg_note),
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No notes found in $selectedFolder",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+' to create your first note",
                                color = CrimsonPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            CompactNoteCard(
                                note = note,
                                isDarkMode = isDarkMode,
                                blurApis = blurApis,
                                onOpen = {
                                    if (note.isLocked) {
                                        pendingLockedNote = note
                                    } else {
                                        onOpenNote(note)
                                    }
                                },
                                onLongPress = {
                                    longPressedNote = note
                                }
                            )
                        }
                    }
                }
            }

            // Floating Buttons Row: Floating AU Bot + Create Note FAB
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Floating AU AI Bot with custom sphere mascot icon (PART F Item 2)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .clickable { onOpenAiChat() }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.au_bot_icon),
                            contentDescription = "AU AI Assistant",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Create Note FAB
                    FloatingActionButton(
                        onClick = onCreateNote,
                        containerColor = CrimsonPrimary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Note", modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }

    // Telegram-Style Long-Press Bottom Action Sheet for Notes (PART A Item 2)
    longPressedNote?.let { note ->
        ModalBottomSheet(
            onDismissRequest = { longPressedNote = null },
            containerColor = if (isDarkMode) Color(0xFF1E222B) else Color(0xFFF6F8FB),
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = note.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Quick Actions (Telegram Style)",
                    fontSize = 11.5.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000))
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Reorder / Pin Note
                TelegramActionItem(
                    icon = painterResource(R.drawable.ic_svg_edit),
                    title = if (note.isPinned) "Unpin Note" else "Reorder / Pin to Top",
                    isDarkMode = isDarkMode,
                    onClick = {
                        coroutineScope.launch {
                            repository.togglePin(note.id, note.isPinned)
                            longPressedNote = null
                            Toast.makeText(context, if (note.isPinned) "Unpinned" else "Pinned to top", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // 2. Lock / Unlock Note with SVG
                TelegramActionItem(
                    icon = painterResource(if (note.isLocked) R.drawable.ic_svg_unlock else R.drawable.ic_svg_lock),
                    title = if (note.isLocked) "Unlock Note" else "Lock Note with Passcode",
                    isDarkMode = isDarkMode,
                    onClick = {
                        longPressedNote = null
                        if (note.isLocked) {
                            pendingUnlockNote = note
                        } else if (!preferences.hasSecuritySetup()) {
                            pendingSetupLockNote = note
                        } else {
                            // Locking an already-configured note must persist TRUE.
                            coroutineScope.launch {
                                repository.toggleLock(note.id, true)
                                Toast.makeText(context, "Note Locked", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                // 3. Hide Note
                if (!note.isHidden) {
                    TelegramActionItem(
                        icon = painterResource(R.drawable.ic_svg_eye_off),
                        title = "Hide Note",
                        isDarkMode = isDarkMode,
                        onClick = {
                            coroutineScope.launch {
                                repository.hideNote(note)
                                longPressedNote = null
                                Toast.makeText(context, "Note hidden in Security Area", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // 4. Export as ZIP / File
                TelegramActionItem(
                    icon = painterResource(R.drawable.ic_svg_download),
                    title = "Export Note",
                    isDarkMode = isDarkMode,
                    onClick = {
                        noteToExport = note
                        longPressedNote = null
                    }
                )

                // 4. Delete Note
                TelegramActionItem(
                    icon = painterResource(R.drawable.ic_svg_delete),
                    title = "Delete (Move to Recycle Bin)",
                    isDarkMode = isDarkMode,
                    isDestructive = true,
                    onClick = {
                        coroutineScope.launch {
                            repository.moveToTrash(note.id)
                            longPressedNote = null
                            Toast.makeText(context, "Moved '${note.title}' to Recycle Bin", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Telegram-Style Long-Press Bottom Action Sheet for Folders (PART A Item 2)
    longPressedFolder?.let { folder ->
        ModalBottomSheet(
            onDismissRequest = { longPressedFolder = null },
            containerColor = if (isDarkMode) Color(0xFF1E222B) else Color(0xFFF6F8FB),
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Folder: $folder",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonPrimary
                )
                Text(
                    text = "Folder Management Options",
                    fontSize = 11.5.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000))
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Lock / Unlock Folder
                val isLocked = preferences.isFolderLocked(folder)
                val folderProtected = isLocked && folder !in unlockedFoldersThisSession
                TelegramActionItem(
                    icon = painterResource(if (isLocked) R.drawable.ic_svg_unlock else R.drawable.ic_svg_lock),
                    title = if (isLocked) "Unlock Folder" else "Lock Folder with PIN",
                    isDarkMode = isDarkMode,
                    onClick = {
                        longPressedFolder = null
                        if (isLocked) {
                            // Unlocking always requires the shared PIN.
                            pendingSetupLockFolder = folder
                        } else if (!preferences.hasSecuritySetup()) {
                            pendingSetupLockFolder = folder
                        } else {
                            preferences.toggleFolderLock(folder)
                            Toast.makeText(context, "Folder Locked with PIN", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // 2. Hide Folder -> Security Area. Notes remain attached to the same folder.
                if (folder != "All Notes") {
                    TelegramActionItem(
                        icon = painterResource(R.drawable.ic_svg_eye_off),
                        title = "Hide Folder",
                        isDarkMode = isDarkMode,
                        onClick = {
                            if (folderProtected) {
                                Toast.makeText(context, "Unlock this folder before changing it.", Toast.LENGTH_SHORT).show()
                            } else {
                                preferences.setFolderHidden(folder, true)
                                if (selectedFolder == folder) selectedFolder = "All Notes"
                            }
                            longPressedFolder = null
                        }
                    )
                }

                // 2. Reorder / Delete custom folders. Default folders cannot be deleted.
                val order = folderOrder
                val index = order.indexOf(folder)
                if (index > 0) {
                    TelegramActionItem(
                        icon = painterResource(R.drawable.ic_svg_edit),
                        title = "Move Folder Left",
                        isDarkMode = isDarkMode,
                        onClick = {
                            if (folderProtected) { Toast.makeText(context, "Unlock this folder before changing it.", Toast.LENGTH_SHORT).show(); longPressedFolder = null; return@TelegramActionItem }
                            val next = order.toMutableList()
                            val item = next.removeAt(index); next.add(index - 1, item)
                            preferences.setFolderOrder(next); longPressedFolder = null
                        }
                    )
                }
                if (index >= 0 && index < order.lastIndex) {
                    TelegramActionItem(
                        icon = painterResource(R.drawable.ic_svg_edit),
                        title = "Move Folder Right",
                        isDarkMode = isDarkMode,
                        onClick = {
                            if (folderProtected) { Toast.makeText(context, "Unlock this folder before changing it.", Toast.LENGTH_SHORT).show(); longPressedFolder = null; return@TelegramActionItem }
                            val next = order.toMutableList()
                            val item = next.removeAt(index); next.add(index + 1, item)
                            preferences.setFolderOrder(next); longPressedFolder = null
                        }
                    )
                }
                if (folder in customFolders) {
                    TelegramActionItem(
                        icon = painterResource(R.drawable.ic_svg_delete),
                        title = "Delete Folder",
                        isDarkMode = isDarkMode,
                        isDestructive = true,
                        onClick = {
                            if (folderProtected) {
                                Toast.makeText(context, "Unlock this folder before deleting it.", Toast.LENGTH_SHORT).show()
                                longPressedFolder = null
                                return@TelegramActionItem
                            }
                            // Keep notes intact when a custom folder is deleted: move them to All Notes.
                            coroutineScope.launch { repository.moveFolderNotes(folder, "All Notes") }
                            preferences.removeCustomFolder(folder)
                            preferences.setFolderHidden(folder, false)
                            preferences.setFolderLocked(folder, false)
                            if (selectedFolder == folder) selectedFolder = "All Notes"
                            longPressedFolder = null
                            Toast.makeText(context, "Folder deleted; notes moved to All Notes", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 3. Export Folder as a real ZIP file
                TelegramActionItem(
                    icon = painterResource(R.drawable.ic_svg_download),
                    title = "Export as ZIP (${folder})",
                    isDarkMode = isDarkMode,
                    onClick = {
                        if (isLocked && folder !in unlockedFoldersThisSession) {
                            Toast.makeText(context, "Unlock this folder before exporting it.", Toast.LENGTH_SHORT).show()
                        } else {
                            exportFolderAsZip(folder)
                        }
                        longPressedFolder = null
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showAddFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("Add Folder") },
            text = { OutlinedTextField(value = newFolderName, onValueChange = { newFolderName = it }, singleLine = true, label = { Text("Folder name") }) },
            confirmButton = { TextButton(onClick = {
                if (preferences.addCustomFolder(newFolderName)) Toast.makeText(context, "Folder added", Toast.LENGTH_SHORT).show()
                else Toast.makeText(context, "Enter a unique folder name", Toast.LENGTH_SHORT).show()
                showAddFolderDialog = false
            }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddFolderDialog = false }) { Text("Cancel") } }
        )
    }

    // Export Dialog (PART G)
    noteToExport?.let { note ->
        ExportDialog(
            note = note,
            isDarkMode = isDarkMode,
            onDismiss = { noteToExport = null }
        )
    }

    // PIN dialog for unlocking an already protected note
    pendingUnlockNote?.let { note ->
        PinLockDialog(
            correctPin = lockPin,
            title = "Unlock Note",
            subtitle = "Enter your security PIN to unlock this note",
            securityQuestion = preferences.securityQuestion.value,
            securityAnswer = preferences.securityAnswer.value,
            isDarkMode = isDarkMode,
            onDismiss = { pendingUnlockNote = null },
            onUnlocked = {
                coroutineScope.launch {
                    repository.toggleLock(note.id, true)
                    pendingUnlockNote = null
                    Toast.makeText(context, "Note Unlocked", Toast.LENGTH_SHORT).show()
                }
            },
            onSecuritySetup = { pin, question, answer -> preferences.setSecurityDetails(pin, question, answer) }
        )
    }

    // PIN/security setup is required before a note can be locked.
    pendingSetupLockNote?.let { note ->
        PinLockDialog(
            correctPin = lockPin,
            title = "Set Security PIN",
            subtitle = "Create a PIN before locking this note",
            securityQuestion = preferences.securityQuestion.value,
            securityAnswer = preferences.securityAnswer.value,
            isDarkMode = isDarkMode,
            onDismiss = { pendingSetupLockNote = null },
            onUnlocked = {
                coroutineScope.launch {
                    repository.toggleLock(note.id, false)
                    pendingSetupLockNote = null
                    Toast.makeText(context, "Note Locked", Toast.LENGTH_SHORT).show()
                }
            },
            onSecuritySetup = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
                coroutineScope.launch { repository.toggleLock(note.id, false) }
            }
        )
    }

    // Folder locking also requires the shared security setup. Unlocking a folder is
    // gated by the same PIN instead of silently toggling the preference.
    pendingSetupLockFolder?.let { folder ->
        val folderIsLocked = preferences.isFolderLocked(folder)
        PinLockDialog(
            correctPin = lockPin,
            title = if (folderIsLocked) "Unlock Folder" else "Set Security PIN",
            subtitle = if (folderIsLocked) "Enter your security PIN to unlock this folder" else "Create a PIN before locking this folder",
            securityQuestion = preferences.securityQuestion.value,
            securityAnswer = preferences.securityAnswer.value,
            isDarkMode = isDarkMode,
            onDismiss = { pendingSetupLockFolder = null },
            onUnlocked = {
                preferences.toggleFolderLock(folder)
                pendingSetupLockFolder = null
                Toast.makeText(context, if (folderIsLocked) "Folder Unlocked" else "Folder Locked with PIN", Toast.LENGTH_SHORT).show()
            },
            onSecuritySetup = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
                preferences.toggleFolderLock(folder)
                pendingSetupLockFolder = null
            }
        )
    }

    // PIN dialog for opening a protected note
    pendingLockedNote?.let { note ->
        PinLockDialog(
            correctPin = lockPin,
            isDarkMode = isDarkMode,
            onDismiss = { pendingLockedNote = null },
            onUnlocked = {
                pendingLockedNote = null
                onOpenNote(note)
            },
            onSecuritySetup = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
            }
        )
    }

    // PIN dialog for unlocking folder
    pendingFolderToOpen?.let { folder ->
        PinLockDialog(
            correctPin = lockPin,
            isDarkMode = isDarkMode,
            onDismiss = { pendingFolderToOpen = null },
            onUnlocked = {
                unlockedFoldersThisSession = unlockedFoldersThisSession + folder
                selectedFolder = folder
                pendingFolderToOpen = null
            },
            onSecuritySetup = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CompactNoteCard(
    note: NoteEntity,
    isDarkMode: Boolean,
    blurApis: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    val categoryColor = when (note.category) {
        "API" -> CategoryApiColor
        "Code" -> CategoryCodeColor
        "Media" -> CategoryMediaColor
        "Personal" -> CategoryPersonalColor
        else -> CategoryGeneralColor
    }

    val displayContent = if (note.category == "API" && blurApis) {
        "•••••••••••••••••••• (API Key Blurred)"
    } else {
        note.content.lines().firstOrNull { it.isNotBlank() } ?: ""
    }

    val formattedTime = remember(note.updatedAt) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
    }

    // Compact Card with height ~75dp
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(14.dp),
        isDarkMode = isDarkMode,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Main Title & Subtitle Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (note.isLocked) {
                        Icon(
                            painter = painterResource(R.drawable.ic_svg_lock),
                            contentDescription = "Locked",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = note.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF111111),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = displayContent,
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF555555),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${note.category}",
                        fontSize = 10.sp,
                        color = categoryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Copy action replaces the old Favourite action on the home card.
            val clipboardManager = LocalClipboardManager.current
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(note.content))
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_svg_copy),
                    contentDescription = "Copy note content",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TelegramActionItem(
    icon: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    isDarkMode: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = title,
            tint = if (isDestructive) Color.Red else CrimsonPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDestructive) Color.Red else (if (isDarkMode) Color.White else Color(0xFF222222))
        )
    }
}
