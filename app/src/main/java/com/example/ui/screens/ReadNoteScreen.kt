package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AutoClassifier
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.ui.components.ExportDialog
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveChecklistView
import com.example.ui.components.NeuIconButton
import com.example.ui.components.PinLockDialog
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AttachmentStorage
import com.example.ui.util.RichTextFormatter
import com.example.ui.util.AlarmScheduler
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

@Composable
fun ReadNoteScreen(
    note: NoteEntity,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onEditNote: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onToggleLock: (Long, Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var lockingAfterSetup by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var revealApi by remember { mutableStateOf(false) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var selectedRingtone by remember { mutableStateOf<Uri?>(null) }
    var selectedRingtoneTitle by remember { mutableStateOf("System alarm sound") }
    var pdfAttachmentToView by remember { mutableStateOf<Uri?>(null) }

    val attachments = remember(note.attachmentsJson) {
        RichTextFormatter.deserializeAttachments(note.attachmentsJson)
    }

    val spans = remember(note.styleSpansJson) {
        RichTextFormatter.deserializeSpans(note.styleSpansJson)
    }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("EEE, MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        isDarkMode = isDarkMode,
                        size = 38.dp,
                        iconSize = 18.dp,
                        tint = if (isDarkMode) Color.White else Color.Black,
                        onClick = onBack
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassCard(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            isDarkMode = isDarkMode,
                            onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(note.content)); Toast.makeText(context, "Note copied", Toast.LENGTH_SHORT).show() }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_svg_copy), "Copy note", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Edit Floating Action Button
                        GlassCard(
                        shape = RoundedCornerShape(12.dp),
                        isDarkMode = isDarkMode,
                        onClick = { onEditNote(note) }
                    ) {
                        Row(
                            modifier = Modifier
                                .background(CrimsonPrimary)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_svg_edit),
                                contentDescription = "Edit Note",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Note Display Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    isDarkMode = isDarkMode,
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Title
                        Text(
                            text = note.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color.White else Color(0xFF111111),
                            lineHeight = 28.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Category Badge & Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CrimsonPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = note.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonPrimary
                                )
                            }
                            Text(
                                text = dateFormatter.format(Date(note.updatedAt)),
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(0.55f) else Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        GlassCard(
                            modifier = Modifier.fillMaxWidth().clickable { showAlarmDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            isDarkMode = isDarkMode
                        ) {
                            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Alarm, contentDescription = "Alarm", tint = CrimsonPrimary, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Set Note Alarm", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)
                                    Text("Reminder, time and ringtone", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("SET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                            }
                        }

                        // Action Toolbar Card (Lock | Export | Delete | TTS | Copy)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            isDarkMode = isDarkMode,
                            isInset = true,
                            elevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Lock Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            if (note.isLocked) {
                                                lockingAfterSetup = false
                                                showPinDialog = true
                                            } else if (!preferences.hasSecuritySetup()) {
                                                lockingAfterSetup = true
                                                showPinDialog = true
                                            } else {
                                                onToggleLock(note.id, false)
                                                Toast.makeText(context, "Note Locked", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (note.isLocked) R.drawable.ic_svg_lock else R.drawable.ic_svg_unlock),
                                        contentDescription = "Lock",
                                        tint = if (note.isLocked) CrimsonPrimary else if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (note.isLocked) "Locked" else "Lock",
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // Export Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showExportDialog = true }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_svg_download),
                                        contentDescription = "Export",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Export",
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // Delete Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            onDeleteNote(note.id)
                                            Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_svg_delete),
                                        contentDescription = "Delete",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Delete",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF5252)
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // TTS Button (Speaker)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            if (isSpeaking) {
                                                tts?.stop()
                                                isSpeaking = false
                                            } else {
                                                val toSpeak = "${note.title}. ${note.content}"
                                                tts?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "NoteTTS")
                                                isSpeaking = true
                                            }
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (isSpeaking) R.drawable.ic_svg_mute else R.drawable.ic_svg_speaker),
                                        contentDescription = "TTS",
                                        tint = Color(0xFF2CF95F),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSpeaking) "Stop" else "Listen",
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // Copy Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            clipboard.setText(AnnotatedString(note.content))
                                            Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_svg_copy),
                                        contentDescription = "Copy",
                                        tint = Color(0xFFFFD93D),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Copy",
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render Attached Images and Files in Read Mode (PART F Item 6 Fix)
                        if (attachments.isNotEmpty()) {
                            Text(
                                text = "Attached Media & Files (${attachments.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                attachments.forEach { attachment ->
                                    val isImage = attachment.mimeType.startsWith("image")
                                    if (isImage) {
                                        // Real Image Rendering in Read Mode
                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            isDarkMode = isDarkMode,
                                            elevation = 2.dp
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                AsyncImage(
                                                    model = if (attachment.uri.startsWith("content://") || attachment.uri.startsWith("file://")) Uri.parse(attachment.uri) else File(attachment.uri),
                                                    contentDescription = attachment.fileName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 240.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = attachment.fileName,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray
                                                )
                                            }
                                        }
                                    } else {
                                        ReadAttachmentCard(
                                            attachment = attachment,
                                            isDarkMode = isDarkMode,
                                            onClick = {
                                                try {
                                                    val shareUri = AttachmentStorage.getShareableUri(context, attachment.uri)
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(shareUri, attachment.mimeType)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Could not open attachment", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Content Area with DAY MODE CONTRAST FIX
                        val displayContent = if (note.category == "API" && preferences.blurApis.value && !revealApi) {
                            AutoClassifier.maskApiKey(note.content)
                        } else {
                            note.content
                        }

                        // Day mode contrast fix: Ensure text is dark and readable
                        val currentFontColor = if (!isDarkMode && (note.fontColorHex.equals("#FFFFFF", ignoreCase = true) || note.fontColorHex.equals("#FFF", ignoreCase = true))) {
                            Color(0xFF111111)
                        } else {
                            try {
                                Color(android.graphics.Color.parseColor(note.fontColorHex))
                            } catch (e: Exception) {
                                if (isDarkMode) Color.White.copy(0.92f) else Color(0xFF111111)
                            }
                        }

                        if (note.category == "Code" || note.isCodeFormat) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDarkMode) Color(0xFF090B10) else Color(0xFF1E1E1E))
                                    .border(1.dp, Color(0x3326C6DA), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = displayContent,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF80D8FF),
                                    lineHeight = 20.sp
                                )
                            }
                        } else {
                            val styledText = remember(displayContent, spans, currentFontColor, note.fontSize) {
                                if (spans.isNotEmpty()) {
                                    RichTextFormatter.buildStyledText(
                                        text = displayContent,
                                        spans = spans,
                                        defaultColor = currentFontColor,
                                        fontSize = note.fontSize.toFloat()
                                    )
                                } else {
                                    AnnotatedString(displayContent)
                                }
                            }

                            Text(
                                text = styledText,
                                fontSize = note.fontSize.sp,
                                fontWeight = if (spans.isEmpty() && note.isBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (spans.isEmpty() && note.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                textDecoration = if (spans.isEmpty() && note.isUnderline) androidx.compose.ui.text.style.TextDecoration.Underline else if (spans.isEmpty() && note.isStrikethrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                                color = currentFontColor,
                                lineHeight = (note.fontSize + 6).sp
                            )
                        }

                        // Render Table if table data exists
                        if (note.tableData.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Embedded Table:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            RenderTable(note.tableData, isDarkMode)
                        }
                    }
                }
            }
        }
    }

    if (showAlarmDialog) {
        val calendar = remember { Calendar.getInstance() }
        var alarmHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
        var alarmMinute by remember { mutableIntStateOf((calendar.get(Calendar.MINUTE) + 1) % 60) }
        var alarmTitle by remember { mutableStateOf(note.title) }
        val ringtoneItems = remember { AlarmScheduler.getSystemRingtones(context) + AlarmScheduler.getDeviceMusicFiles(context).map { com.example.ui.util.SystemRingtoneItem(it.title, it.uri) } }
        AlertDialog(
            onDismissRequest = { showAlarmDialog = false },
            title = { Text("Set Note Alarm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Note: ${note.title}", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = alarmTitle,
                        onValueChange = { alarmTitle = it },
                        label = { Text("Alarm title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, h, m -> alarmHour = h; alarmMinute = m }, alarmHour, alarmMinute, false).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text(String.format(Locale.getDefault(), "%02d:%02d", alarmHour, alarmMinute)) }
                    Text("Ringtone", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    ringtoneItems.take(40).forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { selectedRingtone = item.uri; selectedRingtoneTitle = item.title }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedRingtone == item.uri, onClick = { selectedRingtone = item.uri; selectedRingtoneTitle = item.title })
                            Text(item.title, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    if (ringtoneItems.isEmpty()) Text("No device audio found. Default alarm sound will be used.", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = { TextButton(onClick = {
                val target = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, alarmHour); set(Calendar.MINUTE, alarmMinute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
                val ok = AlarmScheduler.scheduleAlarm(context, target.timeInMillis, alarmTitle.trim().ifBlank { note.title }, note.id, selectedRingtone)
                if (ok) { preferences.setActiveAlarm(alarmTitle.trim().ifBlank { note.title }, target.timeInMillis, selectedRingtone?.toString() ?: selectedRingtoneTitle); Toast.makeText(context, "Alarm saved", Toast.LENGTH_SHORT).show(); showAlarmDialog = false }
                else Toast.makeText(context, "Could not schedule alarm", Toast.LENGTH_SHORT).show()
            }) { Text("Save Alarm", color = CrimsonPrimary) } },
            dismissButton = { TextButton(onClick = { showAlarmDialog = false }) { Text("Cancel") } }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            note = note,
            isDarkMode = isDarkMode,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showPinDialog) {
        val storedPin by preferences.lockPin.collectAsState()
        PinLockDialog(
            correctPin = storedPin,
            title = if (lockingAfterSetup) "Set Security PIN" else "Unlock Note",
            subtitle = if (lockingAfterSetup) "Create your shared 4-digit PIN before locking this note" else "Enter your security PIN to unlock this note",
            securityQuestion = preferences.securityQuestion.value,
            securityAnswer = preferences.securityAnswer.value,
            isDarkMode = isDarkMode,
            onDismiss = { showPinDialog = false },
            onUnlocked = {
                showPinDialog = false
                if (lockingAfterSetup) {
                    onToggleLock(note.id, false)
                    Toast.makeText(context, "Note Locked", Toast.LENGTH_SHORT).show()
                } else {
                    onToggleLock(note.id, true)
                    Toast.makeText(context, "Note Unlocked", Toast.LENGTH_SHORT).show()
                }
                lockingAfterSetup = false
            },
            onSecuritySetup = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
            }
        )
    }
}

@Composable
fun ReadAttachmentCard(
    attachment: RichTextFormatter.AttachmentInfo,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        isDarkMode = isDarkMode,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CrimsonPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (attachment.mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile,
                    contentDescription = "File",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.White else Color.Black,
                    maxLines = 1
                )
                Text(
                    text = "${(attachment.sizeBytes / 1024).coerceAtLeast(1)} KB • Tap to open",
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color.White.copy(0.5f) else Color.Gray
                )
            }
        }
    }
}

@Composable
fun RenderTable(tableData: String, isDarkMode: Boolean) {
    val rows = tableData.lines().filter { it.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), RoundedCornerShape(8.dp))
    ) {
        rows.forEachIndexed { rowIndex, row ->
            val cells = row.split("|").filter { it.isNotBlank() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (rowIndex == 0) {
                            if (isDarkMode) Color(0x33FF2D55) else Color(0x14FF2D55)
                        } else if (rowIndex % 2 == 0) {
                            if (isDarkMode) Color(0x14FFFFFF) else Color(0x0A000000)
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                cells.forEach { cell ->
                    Text(
                        text = cell.trim(),
                        fontSize = 12.sp,
                        fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (isDarkMode) Color.White else Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (rowIndex < rows.size - 1) {
                HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1A000000))
            }
        }
    }
}
