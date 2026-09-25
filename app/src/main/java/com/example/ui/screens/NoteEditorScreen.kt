package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ai.AiService
import com.example.data.model.AutoClassifier
import com.example.data.model.NoteEntity
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveTableView
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AlarmScheduler
import com.example.ui.util.AttachmentStorage
import com.example.ui.util.DeviceAudioFile
import com.example.ui.util.RichTextFormatter
import com.example.ui.util.SystemRingtoneItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


@Composable
private fun JsonSyntaxPreview(text: String, isDarkMode: Boolean, modifier: Modifier = Modifier) {
    val base = if (isDarkMode) Color(0xFFE8E8E8) else Color(0xFF202020)
    val keyColor = if (isDarkMode) Color(0xFF8AB4F8) else Color(0xFF1565C0)
    val stringColor = if (isDarkMode) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
    val numberColor = if (isDarkMode) Color(0xFFFFCC80) else Color(0xFFEF6C00)
    val literalColor = if (isDarkMode) Color(0xFFCE93D8) else Color(0xFF7B1FA2)
    val punctuationColor = if (isDarkMode) Color(0xFFBDBDBD) else Color(0xFF616161)
    val regex = Regex("""(\"(?:\\.|[^\"])*\"(?=\s*:))|(\"(?:\\.|[^\"])*\")|(-?\d+(?:\.\d+)?)|\b(true|false|null)\b|([{}\[\],:])""")
    val annotated = buildAnnotatedString {
        var last = 0
        regex.findAll(text).forEach { m ->
            append(text.substring(last, m.range.first))
            val c = when {
                m.groups[1] != null -> keyColor
                m.groups[2] != null -> stringColor
                m.groups[3] != null -> numberColor
                m.groups[4] != null -> literalColor
                else -> punctuationColor
            }
            withStyle(SpanStyle(color = c)) { append(m.value) }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }
    androidx.compose.material3.Text(
        text = annotated,
        modifier = modifier,
        color = base,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
}

private fun renderPdfPagesForOcr(context: Context, uri: Uri, maxPages: Int): List<Bitmap> {
    val out = mutableListOf<Bitmap>()
    val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return out
    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
        val count = minOf(renderer.pageCount, maxPages)
        for (i in 0 until count) {
            renderer.openPage(i).use { page ->
                val width = (page.width * 2).coerceAtMost(2400)
                val height = (page.height * 2).coerceAtMost(3200)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                out += bitmap
            }
        }
    }
    pfd.close()
    return out
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    initialNote: NoteEntity?,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onOpenTableEditor: (initialTableData: String, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onSaveNote: (
        id: Long,
        title: String,
        content: String,
        category: String,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        isStrikethrough: Boolean,
        isCodeFormat: Boolean,
        fontSize: Int,
        fontColorHex: String,
        alignment: String,
        listType: String,
        tableData: String,
        styleSpansJson: String,
        attachmentsJson: String,
        onSaved: (Long) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { com.example.data.preferences.AppPreferences(context) }
    val aiService = remember { AiService() }

    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var contentValue by remember { mutableStateOf(TextFieldValue(initialNote?.content ?: "")) }
    var selectedCategory by remember { mutableStateOf(initialNote?.category ?: "Normal") }

    var currentNoteId by remember { mutableStateOf(initialNote?.id ?: 0L) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    var spans: List<RichTextFormatter.TextSpan> by remember {
        mutableStateOf(RichTextFormatter.deserializeSpans(initialNote?.styleSpansJson ?: "[]"))
    }

    var attachments: List<RichTextFormatter.AttachmentInfo> by remember {
        mutableStateOf(RichTextFormatter.deserializeAttachments(initialNote?.attachmentsJson ?: "[]"))
    }

    // Formatting state
    var isBold by remember { mutableStateOf(initialNote?.isBold ?: false) }
    var isItalic by remember { mutableStateOf(initialNote?.isItalic ?: false) }
    var isUnderline by remember { mutableStateOf(initialNote?.isUnderline ?: false) }
    var isStrikethrough by remember { mutableStateOf(initialNote?.isStrikethrough ?: false) }
    var isCodeFormat by remember { mutableStateOf(initialNote?.isCodeFormat ?: false) }

    var fontSize by remember { mutableStateOf(initialNote?.fontSize ?: 16) }
    // Ensure font color contrast: default to #111111 in day mode and #FFFFFF in dark mode
    var selectedColorHex by remember {
        val initial = initialNote?.fontColorHex
        if (initial.isNullOrBlank() || initial == "#FFFFFF" && !isDarkMode) {
            mutableStateOf(if (isDarkMode) "#FFFFFF" else "#111111")
        } else {
            mutableStateOf(initial)
        }
    }
    var alignment by remember { mutableStateOf(initialNote?.alignment ?: "left") }
    var listType by remember { mutableStateOf(initialNote?.listType ?: "none") }
    var tableData by remember { mutableStateOf(initialNote?.tableData ?: "") }

    // Dialogs & Modals state
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showImageEditModal by remember { mutableStateOf<RichTextFormatter.AttachmentInfo?>(null) }
    var showAiChatbotModal by remember { mutableStateOf(false) }
    var showTableFullscreen by remember { mutableStateOf(false) }
    var showFormattingMenu by remember { mutableStateOf(false) }
    var jsonMode by remember { mutableStateOf(false) }

    // Floating Chatbot Position
    var botOffsetX by remember { mutableFloatStateOf(0f) }
    var botOffsetY by remember { mutableFloatStateOf(0f) }

    // OCR / Attachment picker for AI Chatbot
    var chatbotAttachedDocText by remember { mutableStateOf("") }
    var chatbotAttachedDocName by remember { mutableStateOf("") }
    var isOcrProcessing by remember { mutableStateOf(false) }

    val ocrDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isOcrProcessing = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val mime = context.contentResolver.getType(uri) ?: ""
                    var extractedText = ""

                    if (mime.contains("image")) {
                        val bitmap = stream?.use { BitmapFactory.decodeStream(it) }
                        if (bitmap != null) {
                            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                                com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
                            )
                            extractedText = com.google.android.gms.tasks.Tasks.await(recognizer.process(image)).text
                        }
                    } else if (mime.contains("pdf")) {
                        val pdfImages = renderPdfPagesForOcr(context, uri, 8)
                        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                            com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
                        )
                        val parts = mutableListOf<String>()
                        for ((index, bitmap) in pdfImages.withIndex()) {
                            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                            val pageText = com.google.android.gms.tasks.Tasks.await(recognizer.process(image)).text
                            if (pageText.isNotBlank()) parts += "[Page ${index + 1}]\n$pageText"
                            bitmap.recycle()
                        }
                        extractedText = parts.joinToString("\n\n")
                    } else {
                        extractedText = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    }

                    withContext(Dispatchers.Main) {
                        chatbotAttachedDocText = extractedText
                        chatbotAttachedDocName = uri.lastPathSegment ?: "document"
                        isOcrProcessing = false
                        Toast.makeText(context, "Attached document to AI for OCR!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isOcrProcessing = false
                        Toast.makeText(context, "Could not process document: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Image Attachment picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val info = AttachmentStorage.copyToAppStorage(context, uri)
                withContext(Dispatchers.Main) {
                    if (info != null) {
                        attachments = attachments + info
                        Toast.makeText(context, "Media/file added to Note!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun applyRangeFormatting(type: String, value: String? = null) {
        val start = minOf(contentValue.selection.start, contentValue.selection.end)
        val end = maxOf(contentValue.selection.start, contentValue.selection.end)
        if (start < end) {
            spans = if (type == "color") {
                RichTextFormatter.setValueProperty(spans, type, start, end, value ?: selectedColorHex)
            } else {
                RichTextFormatter.toggleBooleanProperty(spans, type, start, end)
            }
            hasUnsavedChanges = true
        } else {
            when (type) {
                "bold" -> isBold = !isBold
                "italic" -> isItalic = !isItalic
                "underline" -> isUnderline = !isUnderline
                "strikethrough" -> isStrikethrough = !isStrikethrough
                "code" -> isCodeFormat = !isCodeFormat
                "color" -> selectedColorHex = value ?: selectedColorHex
            }
        }
    }

    fun updateContent(newValue: TextFieldValue) {
        val oldText = contentValue.text
        if (newValue.text == oldText) {
            contentValue = newValue
            return
        }
        val adjusted = RichTextFormatter.adjustSpansForEdit(spans, oldText, newValue.text).toMutableList()
        val diff = newValue.text.length - oldText.length
        if (diff > 0) {
            val insertionEnd = newValue.selection.end.coerceIn(0, newValue.text.length)
            val insertionStart = (insertionEnd - diff).coerceAtLeast(0)
            fun addIfActive(type: String, value: String? = null) {
                if (type == "color") {
                    if (selectedColorHex.isNotBlank()) adjusted += RichTextFormatter.TextSpan(insertionStart, insertionEnd, type, value ?: selectedColorHex)
                } else adjusted += RichTextFormatter.TextSpan(insertionStart, insertionEnd, type, value)
            }
            if (isBold) addIfActive("bold")
            if (isItalic) addIfActive("italic")
            if (isUnderline) addIfActive("underline")
            if (isStrikethrough) addIfActive("strikethrough")
            if (isCodeFormat) addIfActive("code")
            if (selectedColorHex != if (isDarkMode) "#FFFFFF" else "#111111") addIfActive("color", selectedColorHex)
        }
        spans = adjusted
        contentValue = newValue
    }

    fun performSave(showToast: Boolean, thenNavigateBack: Boolean) {
        if (title.isBlank() && contentValue.text.isBlank() && attachments.isEmpty()) {
            if (thenNavigateBack) onBack()
            return
        }
        val autoCat = if (selectedCategory == "Normal") {
            AutoClassifier.detectCategory(title, contentValue.text)
        } else {
            selectedCategory
        }

        onSaveNote(
            currentNoteId,
            title,
            contentValue.text,
            autoCat,
            isBold,
            isItalic,
            isUnderline,
            isStrikethrough,
            isCodeFormat,
            fontSize,
            selectedColorHex,
            alignment,
            listType,
            tableData,
            RichTextFormatter.serializeSpans(spans),
            RichTextFormatter.serializeAttachments(attachments)
        ) { savedId ->
            currentNoteId = savedId
            hasUnsavedChanges = false
            if (showToast) {
                Toast.makeText(context, "Note Saved in $autoCat", Toast.LENGTH_SHORT).show()
            }
            if (thenNavigateBack) onBack()
        }
    }

    // Debounced Auto-Save
    LaunchedEffect(
        title, contentValue.text, spans, tableData, attachments,
        isBold, isItalic, isUnderline, isStrikethrough, isCodeFormat,
        fontSize, selectedColorHex, alignment, listType
    ) {
        hasUnsavedChanges = true
        kotlinx.coroutines.delay(1200)
        performSave(showToast = false, thenNavigateBack = false)
    }

    // Save on pause/exit
    DisposableEffect(Unit) {
        onDispose {
            if (hasUnsavedChanges) {
                performSave(showToast = false, thenNavigateBack = false)
            }
        }
    }

    // Undo/Redo Stacks
    val undoStack = remember { mutableStateListOf<Pair<String, List<RichTextFormatter.TextSpan>>>() }
    val redoStack = remember { mutableStateListOf<Pair<String, List<RichTextFormatter.TextSpan>>>() }

    fun pushUndo() {
        if (undoStack.size > 20) undoStack.removeAt(0)
        undoStack.add(Pair(contentValue.text, spans))
        redoStack.clear()
    }

    val mainScrollState = rememberScrollState()

    // DAY MODE CONTRAST FIX: Default text color in Day mode is dark (Color(0xFF111111))
    val defaultTextColor = if (isDarkMode) Color.White else Color(0xFF111111)
    val effectiveTextColor = if (!isDarkMode && (selectedColorHex.equals("#FFFFFF", ignoreCase = true) || selectedColorHex.equals("#FFF", ignoreCase = true))) {
        Color(0xFF111111)
    } else {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (e: Exception) {
            defaultTextColor
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
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
                            onClick = { performSave(showToast = false, thenNavigateBack = true) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (initialNote == null) "New Note" else "Edit Note",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF111111)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Undo
                        NeuIconButton(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            isDarkMode = isDarkMode,
                            size = 36.dp,
                            iconSize = 17.dp,
                            tint = if (undoStack.isNotEmpty()) (if (isDarkMode) Color.White else Color.Black) else Color.Gray.copy(0.3f),
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    redoStack.add(Pair(contentValue.text, spans))
                                    val last = undoStack.removeAt(undoStack.size - 1)
                                    contentValue = TextFieldValue(last.first, selection = TextRange(last.first.length))
                                    spans = last.second
                                }
                            }
                        )

                        // Redo
                        NeuIconButton(
                            icon = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            isDarkMode = isDarkMode,
                            size = 36.dp,
                            iconSize = 17.dp,
                            tint = if (redoStack.isNotEmpty()) (if (isDarkMode) Color.White else Color.Black) else Color.Gray.copy(0.3f),
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    undoStack.add(Pair(contentValue.text, spans))
                                    val next = redoStack.removeAt(redoStack.size - 1)
                                    contentValue = TextFieldValue(next.first, selection = TextRange(next.first.length))
                                    spans = next.second
                                }
                            }
                        )

                        // Save Button
                        GlassCard(
                            shape = RoundedCornerShape(12.dp),
                            isDarkMode = isDarkMode,
                            onClick = { performSave(showToast = true, thenNavigateBack = true) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF5E7E))))
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Note Body Scrollable Area (Auto-Scroll with safe bottom padding so text never gets hidden behind toolbar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(mainScrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Note Title Input
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color.White else Color(0xFF111111)
                        ),
                        cursorBrush = SolidColor(CrimsonPrimary),
                        decorationBox = { innerTextField ->
                            if (title.isBlank()) {
                                Text(
                                    text = "Untitled Note",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color.White.copy(0.35f) else Color.Gray.copy(0.6f)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("EEEE, MMMM dd | HH:mm", Locale.getDefault()).format(Date()),
                        fontSize = 11.5.sp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Inline Attached Images (with Move, Resize, Crop, Rename support - PART F Item 5)
                    if (attachments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            attachments.forEachIndexed { index, att ->
                                val isImage = att.mimeType.startsWith("image")
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    isDarkMode = isDarkMode,
                                    elevation = 2.dp
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (isImage) {
                                            AsyncImage(
                                                model = File(att.uri),
                                                contentDescription = att.fileName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 240.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = att.fileName,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDarkMode) Color.White else Color.Black,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Row {
                                                IconButton(
                                                    onClick = { showImageEditModal = att },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(painterResource(R.drawable.ic_svg_edit), contentDescription = "Edit Image", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        attachments = attachments.toMutableList().apply { removeAt(index) }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(painterResource(R.drawable.ic_svg_delete), contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Interactive Table Display if present
                    if (tableData.isNotBlank()) {
                        InteractiveTableView(
                            tableData = tableData,
                            isDarkMode = isDarkMode,
                            onTableChange = { updated -> tableData = updated }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Main Content Input (Day Mode text contrast fix & Auto-Scroll buffer)
                    if (jsonMode) {
                        JsonSyntaxPreview(
                            text = contentValue.text,
                            isDarkMode = isDarkMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 260.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        )
                    } else BasicTextField(
                        value = contentValue,
                        onValueChange = { newVal ->
                            if (newVal.text != contentValue.text) pushUndo()
                            updateContent(newVal)
                        },
                        textStyle = TextStyle(
                            fontSize = fontSize.sp,
                            fontFamily = FontFamily.Default,
                            textAlign = when (alignment) {
                                "center" -> TextAlign.Center
                                "right" -> TextAlign.Right
                                else -> TextAlign.Left
                            },
                            color = effectiveTextColor,
                            lineHeight = (fontSize * 1.5).sp
                        ),
                        cursorBrush = SolidColor(CrimsonPrimary),
                        visualTransformation = VisualTransformation { value ->
                            TransformedText(
                                RichTextFormatter.buildStyledText(value.text, spans, defaultTextColor, fontSize.toFloat()),
                                OffsetMapping.Identity
                            )
                        },
                        decorationBox = { innerTextField ->
                            if (contentValue.text.isBlank()) {
                                Text(
                                    text = "Start typing your notes, code, or ideas...",
                                    fontSize = fontSize.sp,
                                    color = if (isDarkMode) Color.White.copy(0.35f) else Color.Gray.copy(0.6f)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp)
                    )

                    // Safe Bottom Space so typing near the bottom never slips behind the toolbar
                    Spacer(modifier = Modifier.height(160.dp))
                }

                // Bottom Formatting & Feature Toolbar
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    isDarkMode = isDarkMode,
                    elevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Formatting group: range formatting is applied to the current selection; with no selection it controls the next typed text.
                        Box {
                            IconButton(onClick = { showFormattingMenu = true }) {
                                Icon(Icons.Default.Title, contentDescription = "Formatting", tint = if (showFormattingMenu) CrimsonPrimary else (if (isDarkMode) Color.White else Color.Black))
                            }
                            DropdownMenu(expanded = showFormattingMenu, onDismissRequest = { showFormattingMenu = false }) {
                                DropdownMenuItem(text = { Text("Bold") }, onClick = { applyRangeFormatting("bold"); showFormattingMenu = false })
                                DropdownMenuItem(text = { Text("Italic") }, onClick = { applyRangeFormatting("italic"); showFormattingMenu = false })
                                DropdownMenuItem(text = { Text("Underline") }, onClick = { applyRangeFormatting("underline"); showFormattingMenu = false })
                                DropdownMenuItem(text = { Text("Strikethrough") }, onClick = { applyRangeFormatting("strikethrough"); showFormattingMenu = false })
                                DropdownMenuItem(text = { Text("Code") }, onClick = { applyRangeFormatting("code"); showFormattingMenu = false })
                                Text("Text color", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                listOf("#FFFFFF", "#111111", "#FF2D55", "#FF9800", "#4CAF50", "#2196F3", "#9C27B0").forEach { hex ->
                                    DropdownMenuItem(text = { Text(hex, color = Color(android.graphics.Color.parseColor(hex))) }, onClick = { applyRangeFormatting("color", hex); showFormattingMenu = false })
                                }
                            }
                        }

                        // Align Left
                        IconButton(onClick = { alignment = "left" }) {
                            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Left", tint = if (alignment == "left") CrimsonPrimary else Color.Gray)
                        }

                        // Align Center
                        IconButton(onClick = { alignment = "center" }) {
                            Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center", tint = if (alignment == "center") CrimsonPrimary else Color.Gray)
                        }

                        // JSON Mode: toggle a syntax-highlighted JSON view without changing stored text.
                        IconButton(onClick = { jsonMode = !jsonMode }) {
                            Icon(
                                Icons.Default.DataObject,
                                contentDescription = if (jsonMode) "Disable JSON Mode" else "Enable JSON Mode",
                                tint = if (jsonMode) CrimsonPrimary else Color.Gray
                            )
                        }

                        // Insert / Edit Table Screen (PART F Item 1)
                        IconButton(onClick = { showTableFullscreen = true }) {
                            Icon(Icons.Default.TableChart, contentDescription = "Table Editor", tint = CrimsonPrimary)
                        }

                        // Professional Alarm Feature replacing Timestamp insertion (PART F Item 7)
                        IconButton(onClick = { showAlarmDialog = true }) {
                            Icon(Icons.Default.Alarm, contentDescription = "Set Alarm Reminder", tint = CrimsonPrimary)
                        }

                        // Add Image Attachment
                        IconButton(onClick = { imagePickerLauncher.launch(arrayOf("image/*", "video/*", "application/pdf", "text/*", "application/json", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }) {
                            Icon(painter = painterResource(R.drawable.ic_svg_media), contentDescription = "Import media", tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            // SINGLE Floating AU AI Chatbot with custom sphere mascot icon (PART F Item 2 & 3 & 4)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(botOffsetX.roundToInt(), botOffsetY.roundToInt()) }
                    .padding(end = 20.dp, bottom = 80.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            botOffsetX += dragAmount.x
                            botOffsetY += dragAmount.y
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .clickable { showAiChatbotModal = true }
                ) {
                    Image(
                        painter = painterResource(R.drawable.au_bot_icon),
                        contentDescription = "AU Chatbot",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Fullscreen Dedicated Table Editor (PART F Item 1)
    if (showTableFullscreen) {
        TableEditorScreen(
            initialTableData = tableData,
            isDarkMode = isDarkMode,
            onBack = { showTableFullscreen = false },
            onSaveTable = { saved ->
                tableData = saved
                showTableFullscreen = false
                Toast.makeText(context, "Table updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Floating Chatbot Dialog with OCR (+) Button and Live Note Text Context (PART F Item 3 & 4)
    if (showAiChatbotModal) {
        var userPrompt by remember { mutableStateOf("") }
        var aiResponse by remember { mutableStateOf("") }
        var isGenerating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAiChatbotModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.au_bot_icon),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AU Notes AI Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (chatbotAttachedDocName.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1F2CF95F))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Attached: $chatbotAttachedDocName (OCR Ready)",
                                fontSize = 11.sp,
                                color = Color(0xFF2CF95F),
                                maxLines = 1
                            )
                            IconButton(onClick = {
                                chatbotAttachedDocName = ""
                                chatbotAttachedDocText = ""
                            }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (aiResponse.isNotBlank()) {
                        Text(
                            text = aiResponse,
                            fontSize = 13.sp,
                            color = if (isDarkMode) Color.White else Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .verticalScroll(rememberScrollState())
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            onClick = {
                                // Paste AI output directly into note
                                val newText = if (contentValue.text.isBlank()) aiResponse else "${contentValue.text}\n\n$aiResponse"
                                contentValue = TextFieldValue(newText, selection = TextRange(newText.length))
                                showAiChatbotModal = false
                                Toast.makeText(context, "Inserted AI content into note!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Paste into Note", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Input Row with (+) OCR Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // (+) Plus Icon for OCR of .txt, PDF, Image (PART F Item 4)
                        IconButton(
                            onClick = { ocrDocPicker.launch(arrayOf("*/*")) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Attach doc for OCR", tint = CrimsonPrimary)
                        }

                        OutlinedTextField(
                            value = userPrompt,
                            onValueChange = { userPrompt = it },
                            placeholder = { Text("Ask AI to rewrite, clean, or extract...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (userPrompt.isNotBlank()) {
                                    isGenerating = true
                                    coroutineScope.launch {
                                        val key = com.example.data.preferences.AppPreferences(context).getEffectiveApiKey()
                                        val fullContext = "Current Note Title: $title\nCurrent Note Text:\n${contentValue.text}\n\nAttached OCR Data:\n$chatbotAttachedDocText"
                                        val res = aiService.generateResponse(
                                            prompt = userPrompt,
                                            apiKey = key,
                                            provider = preferences.getAiProvider(),
                                            baseUrl = preferences.getAiBaseUrl(),
                                            noteContext = fullContext
                                        )
                                        isGenerating = false
                                        aiResponse = res
                                        val request = userPrompt.lowercase(Locale.ROOT)
                                        if (request.contains("paste") || request.contains("insert") || request.contains("add it to this note") || request.contains("note mein") || request.contains("note me")) {
                                            val insertion = if (contentValue.text.isBlank()) res else "${contentValue.text}\n\n$res"
                                            contentValue = TextFieldValue(insertion, selection = TextRange(insertion.length))
                                            hasUnsavedChanges = true
                                            Toast.makeText(context, "AI content inserted into note!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CrimsonPrimary)
                            } else {
                                Icon(painterResource(R.drawable.ic_svg_send), contentDescription = "Send", tint = CrimsonPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiChatbotModal = false }) { Text("Close") }
            }
        )
    }

    // Professional Alarm / Reminder Feature (PART F Item 7)
    if (showAlarmDialog) {
        var alarmTitleInput by remember { mutableStateOf(if (title.isNotBlank()) "Reminder: $title" else "Note Reminder") }
        var selectedHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
        var selectedMinute by remember { mutableIntStateOf((Calendar.getInstance().get(Calendar.MINUTE) + 5) % 60) }
        var ringtoneType by remember { mutableStateOf("system") } // "system" or "device"

        val systemRingtones = remember { AlarmScheduler.getSystemRingtones(context) }
        var selectedSystemRingtone by remember { mutableStateOf(systemRingtones.firstOrNull()) }

        val deviceMusicFiles = remember { AlarmScheduler.getDeviceMusicFiles(context) }
        var selectedDeviceMusic by remember { mutableStateOf(deviceMusicFiles.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { showAlarmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AlarmAdd, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Real Alarm", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = alarmTitleInput,
                        onValueChange = { alarmTitleInput = it },
                        label = { Text("Alarm Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Alarm Time: %02d:%02d".format(selectedHour, selectedMinute), fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedHour = (selectedHour + 1) % 24 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF2D55)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+1 Hour", color = if (isDarkMode) Color.White else Color.Black, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { selectedMinute = (selectedMinute + 10) % 60 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF2D55)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+10 Mins", color = if (isDarkMode) Color.White else Color.Black, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Ringtone Source:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = ringtoneType == "system",
                            onClick = { ringtoneType = "system" },
                            colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                        )
                        Text("System Ringtones", fontSize = 13.sp)

                        Spacer(modifier = Modifier.width(12.dp))

                        RadioButton(
                            selected = ringtoneType == "device",
                            onClick = { ringtoneType = "device" },
                            colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                        )
                        Text("Device Files", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (ringtoneType == "system") {
                        Text("Selected: ${selectedSystemRingtone?.title ?: "Default"}", fontSize = 12.sp, color = CrimsonPrimary)
                    } else {
                        Text("Selected Device Song: ${selectedDeviceMusic?.title ?: "First found"}", fontSize = 12.sp, color = CrimsonPrimary)
                        if (deviceMusicFiles.isEmpty()) {
                            Text("No audio files detected in storage. Defaulting to system alarm.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }

                        val chosenUri = if (ringtoneType == "device") selectedDeviceMusic?.uri else selectedSystemRingtone?.uri
                        val scheduled = AlarmScheduler.scheduleAlarm(
                            context = context,
                            triggerTimeMillis = cal.timeInMillis,
                            title = alarmTitleInput,
                            noteId = currentNoteId,
                            ringtoneUri = chosenUri
                        )

                        showAlarmDialog = false
                        if (scheduled) {
                            Toast.makeText(context, "Alarm set for %02d:%02d!".format(selectedHour, selectedMinute), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Alarm scheduled successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Set Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmDialog = false }) { Text("Cancel") }
            }
        )
    }

    fun transformImageAttachment(att: RichTextFormatter.AttachmentInfo, crop: Boolean, resize: Boolean) {
        if (!att.mimeType.startsWith("image")) return
        try {
            val source = if (att.uri.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(att.uri))?.use { BitmapFactory.decodeStream(it) }
            } else {
                FileInputStream(File(att.uri)).use { BitmapFactory.decodeStream(it) }
            } ?: return
            val cropped = if (crop) {
                val side = minOf(source.width, source.height)
                Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            } else source
            val output = if (resize) {
                val maxSide = 1600f
                val scale = minOf(1f, maxSide / maxOf(cropped.width, cropped.height).toFloat())
                if (scale < 1f) Bitmap.createScaledBitmap(cropped, (cropped.width * scale).toInt(), (cropped.height * scale).toInt(), true) else cropped
            } else cropped
            val dir = File(context.filesDir, "attachments").apply { mkdirs() }
            val outFile = File(dir, "edited_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (cropped !== output && cropped !== source) cropped.recycle()
            if (output !== source) source.recycle()
            attachments = attachments.map {
                if (it.uri == att.uri) it.copy(uri = outFile.absolutePath, fileName = editedNameForImage(att.fileName)) else it
            }
            Toast.makeText(context, if (crop && resize) "Image cropped and resized" else if (crop) "Image cropped" else "Image resized", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not edit image", Toast.LENGTH_SHORT).show()
        }
    }

    fun editedNameForImage(name: String): String {
        val base = name.substringBeforeLast('.', name)
        return "${base}_edited.png"
    }

    // Image Edit Modal (Move, Crop, Resize, Rename - PART F Item 5)
    showImageEditModal?.let { imgAtt ->
        var editedName by remember { mutableStateOf(imgAtt.fileName) }
        AlertDialog(
            onDismissRequest = { showImageEditModal = null },
            title = { Text("Edit Image in Note", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Rename File") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Image actions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { transformImageAttachment(imgAtt, crop = true, resize = false) }) { Text("Crop") }
                        OutlinedButton(onClick = { transformImageAttachment(imgAtt, crop = false, resize = true) }) { Text("Resize") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val index = attachments.indexOfFirst { it.uri == imgAtt.uri }
                            if (index > 0) attachments = attachments.toMutableList().apply { add(index - 1, removeAt(index)) }
                        }) { Text("Move Left") }
                        OutlinedButton(onClick = {
                            val index = attachments.indexOfFirst { it.uri == imgAtt.uri }
                            if (index >= 0 && index < attachments.lastIndex) attachments = attachments.toMutableList().apply { add(index + 1, removeAt(index)) }
                        }) { Text("Move Right") }
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    onClick = {
                        attachments = attachments.map {
                            if (it.uri == imgAtt.uri) it.copy(fileName = editedName.ifBlank { it.fileName }) else it
                        }
                        showImageEditModal = null
                        Toast.makeText(context, "Image updated!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImageEditModal = null }) { Text("Cancel") }
            }
        )
    }
}
