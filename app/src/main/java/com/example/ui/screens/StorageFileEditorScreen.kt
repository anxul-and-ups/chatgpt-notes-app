package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.Date

private fun extractZipItem(context: Context, item: RealStorageItem): Int {
    return try {
        val baseDir = item.file?.parentFile ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AU Notes/Extracted")
        val destination = File(baseDir, item.name.substringBeforeLast('.', item.name) + "_extracted").apply { mkdirs() }
        val input = item.file?.inputStream() ?: item.uri?.let { context.contentResolver.openInputStream(it) } ?: return 0
        var count = 0
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = File(destination, entry.name)
                val canonicalBase = destination.canonicalPath + File.separator
                if (target.canonicalPath != destination.canonicalPath && !target.canonicalPath.startsWith(canonicalBase)) continue
                if (entry.isDirectory) target.mkdirs() else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { out -> zip.copyTo(out) }
                    count++
                }
                zip.closeEntry()
            }
        }
        count
    } catch (_: Exception) { 0 }
}

data class RealStorageItem(
    val name: String,
    val file: File? = null,
    val uri: Uri? = null,
    val sizeString: String,
    val path: String,
    val lastModified: Long,
    val isPdf: Boolean = false,
    val isDirectory: Boolean = false
)

@Composable
fun StorageFileEditorScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onOpenPdf: (Uri) -> Unit = {},
    onOpenFileInEditor: (title: String, content: String, category: String) -> Unit
) {
    val context = LocalContext.current
    var hasUnderstoodIntro by remember {
        val prefs = context.getSharedPreferences("au_file_editor_prefs", Context.MODE_PRIVATE)
        mutableStateOf(prefs.getBoolean("intro_understood", false))
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFileItem by remember { mutableStateOf<RealStorageItem?>(null) }
    var fileContentEdit by remember { mutableStateOf("") }
    var isEditingInPlace by remember { mutableStateOf(false) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    val storageRoot = remember { Environment.getExternalStorageDirectory() }
    var currentDirectory by remember { mutableStateOf(storageRoot) }
    var fileList by remember { mutableStateOf<List<RealStorageItem>>(emptyList()) }
    var showDetailsDialog by remember { mutableStateOf<RealStorageItem?>(null) }

    fun refreshFiles() {
        val list = mutableListOf<RealStorageItem>()
        val directory = currentDirectory
        try {
            directory.listFiles()?.filter { !it.name.startsWith(".") }?.sortedWith(
                compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
            )?.forEach { f ->
                list.add(
                    RealStorageItem(
                        name = f.name,
                        file = f,
                        sizeString = if (f.isDirectory) "Folder" else "${(f.length() / 1024).coerceAtLeast(1)} KB",
                        path = f.absolutePath,
                        lastModified = f.lastModified(),
                        isPdf = f.name.endsWith(".pdf", ignoreCase = true),
                        isDirectory = f.isDirectory
                    )
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Storage access unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        fileList = list.sortedWith(compareByDescending<RealStorageItem> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    LaunchedEffect(currentDirectory) {
        refreshFiles()
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var displayName = "Document"
                var sizeBytes = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                        if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                    }
                }

                val isPdf = displayName.endsWith(".pdf", ignoreCase = true)
                if (isPdf) {
                    onOpenPdf(uri)
                } else {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    val item = RealStorageItem(
                        name = displayName,
                        uri = uri,
                        sizeString = "${(sizeBytes / 1024).coerceAtLeast(1)} KB",
                        path = uri.toString(),
                        lastModified = System.currentTimeMillis(),
                        isPdf = false
                    )
                    selectedFileItem = item
                    fileContentEdit = content
                    isEditingInPlace = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredList = fileList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.path.contains(searchQuery, ignoreCase = true)
    }

    GlassBackground(isDarkMode = isDarkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top App Bar
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
                                text = "File Editor",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF111111)
                            )
                            Text(
                                text = "Device Storage & File Extractor",
                                fontSize = 11.sp,
                                color = CrimsonPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (currentDirectory != storageRoot) currentDirectory = currentDirectory.parentFile ?: storageRoot },
                            enabled = currentDirectory != storageRoot
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Parent folder", tint = if (currentDirectory != storageRoot) CrimsonPrimary else Color.Gray)
                        }
                        IconButton(onClick = { openFileLauncher.launch(arrayOf("*/*")) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_svg_folder),
                                contentDescription = "Import/Extract File",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create New File",
                                tint = if (isDarkMode) Color.White else Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PART B Item 4: Stylish Intro Card (Explaining what File Editor does)
                if (!hasUnderstoodIntro) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        isDarkMode = isDarkMode,
                        elevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_file_editor),
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Welcome to File Editor",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "File Editor allows you to directly explore device files, preview PDFs, edit text & code files (.txt, .py, .html, .json), and extract content seamlessly into AU Notes.",
                                fontSize = 13.sp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color(0xFF444444),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                GlassCard(
                                    shape = RoundedCornerShape(10.dp),
                                    isDarkMode = isDarkMode,
                                    onClick = {
                                        hasUnderstoodIntro = true
                                        context.getSharedPreferences("au_file_editor_prefs", Context.MODE_PRIVATE)
                                            .edit().putBoolean("intro_understood", true).apply()
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF5E7E))))
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Understood", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // In-Place Editor Mode
                if (isEditingInPlace && selectedFileItem != null) {
                    val item = selectedFileItem!!
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        isDarkMode = isDarkMode
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CrimsonPrimary
                                    )
                                    Text(
                                        text = item.sizeString,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Row {
                                    IconButton(onClick = {
                                        // Save back to file
                                        try {
                                            if (item.file != null) {
                                                item.file.writeText(fileContentEdit)
                                                Toast.makeText(context, "File Saved!", Toast.LENGTH_SHORT).show()
                                            } else if (item.uri != null) {
                                                context.contentResolver.openOutputStream(item.uri, "wt")?.use { out ->
                                                    out.write(fileContentEdit.toByteArray())
                                                }
                                                Toast.makeText(context, "File Saved to Storage!", Toast.LENGTH_SHORT).show()
                                            }
                                            refreshFiles()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF4CAF50))
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            val ext = item.name.substringAfterLast(".", "")
                                            val category = when (ext.lowercase()) {
                                                "py", "kt", "js", "html", "css", "java", "json" -> "Code"
                                                else -> "General"
                                            }
                                            onOpenFileInEditor(item.name, fileContentEdit, category)
                                        }
                                    ) {
                                        Text("Open in Note Editor", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = fileContentEdit,
                                onValueChange = { fileContentEdit = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = if (isDarkMode) Color.White else Color.Black
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CrimsonPrimary,
                                    unfocusedBorderColor = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(onClick = { isEditingInPlace = false }) {
                                Text("Close In-Place Editor", color = CrimsonPrimary)
                            }
                        }
                    }
                } else {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search storage files...", fontSize = 13.sp, color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CrimsonPrimary) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // File List
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_file_editor),
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No files found", color = Color.Gray, fontSize = 14.sp)
                                Text("Tap '+' to create a new file or folder icon to import.", color = CrimsonPrimary, fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredList) { item ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    isDarkMode = isDarkMode,
                                    elevation = 2.dp,
                                    onClick = {
                                        if (item.isDirectory && item.file != null) {
                                            currentDirectory = item.file
                                        } else if (item.name.lowercase().endsWith(".zip")) {
                                            val extracted = extractZipItem(context, item)
                                            Toast.makeText(context, if (extracted > 0) "Extracted $extracted file(s)" else "No files extracted", Toast.LENGTH_SHORT).show()
                                            refreshFiles()
                                        } else if (item.isPdf) {
                                            if (item.file != null) {
                                                onOpenPdf(Uri.fromFile(item.file))
                                            } else if (item.uri != null) {
                                                onOpenPdf(item.uri)
                                            }
                                        } else {
                                            try {
                                                val text = if (item.file != null) {
                                                    item.file.readText()
                                                } else if (item.uri != null) {
                                                    context.contentResolver.openInputStream(item.uri)?.bufferedReader()?.use { it.readText() } ?: ""
                                                } else ""

                                                selectedFileItem = item
                                                fileContentEdit = text
                                                isEditingInPlace = true
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot read file: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                painter = if (item.isDirectory) painterResource(R.drawable.ic_svg_folder) else painterResource(R.drawable.ic_file_editor),
                                                contentDescription = null,
                                                tint = if (item.isDirectory) Color(0xFFFFCA28) else if (item.isPdf) Color(0xFFFF5252) else CrimsonPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = item.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                                                )
                                                Text(
                                                    text = "${item.sizeString} • ${Date(item.lastModified).toLocaleString()}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(
                                                onClick = { showDetailsDialog = item },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            }

                                            if (item.file != null) {
                                                IconButton(
                                                    onClick = {
                                                        item.file.delete()
                                                        refreshFiles()
                                                        Toast.makeText(context, "Deleted ${item.name}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(painterResource(R.drawable.ic_svg_delete), contentDescription = "Delete", tint = Color.Red.copy(0.7f), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create File Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create File in Storage", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Filename (e.g. script.py, notes.txt)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            val targetDir = currentDirectory.apply { if (!exists()) mkdirs() }
                            val newFile = File(targetDir, newFileName.trim())
                            newFile.writeText("")
                            showCreateDialog = false
                            newFileName = ""
                            refreshFiles()
                            Toast.makeText(context, "Created ${newFile.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Details Dialog
    showDetailsDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDetailsDialog = null },
            title = { Text("File Details", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Name: ${item.name}", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Path: ${item.path}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Size: ${item.sizeString}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Modified: ${Date(item.lastModified).toLocaleString()}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = null }) { Text("Close") }
            }
        )
    }
}
