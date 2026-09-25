package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.NoteEntity
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.RichTextFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportDialog(
    note: NoteEntity,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedExtension by remember {
        mutableStateOf(
            when (note.category) {
                "Code" -> ".py"
                "API" -> ".txt"
                else -> ".txt"
            }
        )
    }
    var showAttachmentWarning by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<(() -> Unit)?>(null) }

    var fileNameWithoutExt by remember {
        mutableStateOf(
            note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "AU_Note_${note.id}" }
        )
    }

    val extensions = listOf(".txt", ".pdf", ".docx", ".html", ".py", ".xml", ".json")

    val attachments: List<RichTextFormatter.AttachmentInfo> = remember(note.attachmentsJson) {
        RichTextFormatter.deserializeAttachments(note.attachmentsJson)
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            isDarkMode = isDarkMode,
            strong = true
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_svg_download),
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Export System",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF111111)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Export Extension:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    extensions.forEach { ext ->
                        val isSelected = selectedExtension == ext
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) CrimsonPrimary else if (isDarkMode) Color(0x33FFFFFF) else Color(0x14000000)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CrimsonPrimary else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedExtension = ext }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ext.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "File Name (Editable Extension):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fileNameWithoutExt,
                        onValueChange = { input ->
                            fileNameWithoutExt = input.replace(Regex("[/\\\\:*?\"<>|]"), "")
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x44FF2D55)
                        )
                    )
                    Text(
                        text = selectedExtension,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // PART G: TWO EXPORT BUTTONS (1st Export to Storage, 2nd Share)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: Save / Export to Internal Storage (Documents)
                    Button(
                        onClick = {
                            val action = {
                                val safeName = fileNameWithoutExt.trim().ifBlank { "AU_Note_${note.id}" }
                                val fullFileName = "$safeName$selectedExtension"
                                val savedFile = saveFileToInternalStorage(context, note, fullFileName, selectedExtension, attachments)
                                if (savedFile != null) Toast.makeText(context, "Saved to device: $savedFile", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                            if (selectedExtension != ".pdf" && attachments.isNotEmpty()) { pendingExport = action; showAttachmentWarning = true } else action()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("1. Export", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    // Option 2: Share via Android Share Sheet
                    Button(
                        onClick = {
                            val action = {
                                val safeName = fileNameWithoutExt.trim().ifBlank { "AU_Note_${note.id}" }
                                val fullFileName = "$safeName$selectedExtension"
                                exportAndShareFile(context, note, fullFileName, selectedExtension, attachments)
                                onDismiss()
                            }
                            if (selectedExtension != ".pdf" && attachments.isNotEmpty()) { pendingExport = action; showAttachmentWarning = true } else action()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("2. Share", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }

    if (showAttachmentWarning) {
        AlertDialog(
            onDismissRequest = { showAttachmentWarning = false; pendingExport = null },
            title = { Text("Attachment compatibility") },
            text = { Text("This format may not support images or attachments. Some content may not be included in the exported file.") },
            confirmButton = { TextButton(onClick = { showAttachmentWarning = false; val action = pendingExport; pendingExport = null; action?.invoke() }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { showAttachmentWarning = false; pendingExport = null }) { Text("Cancel") } }
        )
    }
}

private fun saveFileToInternalStorage(
    context: Context,
    note: NoteEntity,
    fileName: String,
    extension: String,
    attachments: List<RichTextFormatter.AttachmentInfo>
): String? {
    return try {
        val tempDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val temp = File(tempDir, fileName)
        writeNoteContentToFile(context, note, temp, extension, attachments)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeFor(extension))
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AU Notes")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create device storage file")
            resolver.openOutputStream(uri)?.use { output -> temp.inputStream().use { it.copyTo(output) } }
                ?: throw IllegalStateException("Could not write device storage file")
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Documents/AU Notes/$fileName"
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).apply { mkdirs() }
            val out = File(dir, fileName)
            temp.copyTo(out, overwrite = true)
            out.absolutePath
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        null
    }
}

private fun mimeFor(extension: String): String = when (extension.lowercase()) {
    ".pdf" -> "application/pdf"
    ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ".html" -> "text/html"
    ".xml" -> "application/xml"
    ".py" -> "text/x-python"
    ".json" -> "application/json"
    else -> "text/plain"
}

private fun exportAndShareFile(
    context: Context,
    note: NoteEntity,
    fileName: String,
    extension: String,
    attachments: List<RichTextFormatter.AttachmentInfo>
) {
    try {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val targetFile = File(exportDir, fileName)
        writeNoteContentToFile(context, note, targetFile, extension, attachments)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", targetFile)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeFor(extension)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, targetFile.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share ${targetFile.name}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun openAttachmentBitmap(context: Context, uriString: String): Bitmap? {
    return try {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: File(uriString).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
    } catch (_: Exception) { null }
}

private fun writePdf(
    context: Context,
    note: NoteEntity,
    targetFile: File,
    attachments: List<RichTextFormatter.AttachmentInfo>
) {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val left = 40f
    val right = 555f
    val top = 46f
    val bottom = 790f
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = AndroidColor.rgb(30,30,30) }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; color = AndroidColor.rgb(255,45,85); typeface = Typeface.DEFAULT_BOLD }
    val lineHeight = 18f
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var canvas: android.graphics.Canvas? = null
    var y = top

    fun newPage() {
        page?.let { document.finishPage(it) }
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page!!.canvas
        y = top
    }
    fun ensureSpace(h: Float) { if (page == null || y + h > bottom) newPage() }

    newPage()
    canvas!!.drawText(note.title, left, y + 4f, titlePaint); y += 34f

    // Render text with basic per-range styles without collapsing the note to one page.
    val spans = RichTextFormatter.deserializeSpans(note.styleSpansJson)
    val lines = note.content.split("\n")
    var offset = 0
    for (line in lines) {
        var pos = 0
        val lineEnd = offset + line.length
        val cuts = mutableSetOf(0, line.length)
        spans.filter { it.end > offset && it.start < lineEnd }.forEach {
            cuts += (it.start - offset).coerceIn(0, line.length)
            cuts += (it.end - offset).coerceIn(0, line.length)
        }
        val sorted = cuts.toList().sorted()
        for (i in 0 until sorted.lastIndex) {
            val a = sorted[i]; val b = sorted[i+1]
            if (a >= b) continue
            val segment = line.substring(a,b)
            val active = spans.filter { it.start <= offset+a && it.end >= offset+b }
            val paint = Paint(bodyPaint).apply {
                typeface = when {
                    active.any { it.type == "bold" } && active.any { it.type == "italic" } -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                    active.any { it.type == "bold" } -> Typeface.DEFAULT_BOLD
                    active.any { it.type == "italic" } -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    else -> Typeface.DEFAULT
                }
                isUnderlineText = active.any { it.type == "underline" }
                isStrikeThruText = active.any { it.type == "strikethrough" }
                active.firstOrNull { it.type == "color" }?.value?.let { runCatching { color = AndroidColor.parseColor(it) } }
            }
            // Wrap long lines by measured width.
            var remaining = segment
            while (remaining.isNotEmpty()) {
                ensureSpace(lineHeight)
                val available = right - left
                var count = remaining.length
                while (count > 1 && paint.measureText(remaining, 0, count) > available) count--
                canvas!!.drawText(remaining, 0, count, left, y, paint)
                y += lineHeight
                remaining = remaining.substring(count)
            }
        }
        if (line.isEmpty()) { ensureSpace(lineHeight); y += lineHeight }
        offset = lineEnd + 1
    }

    // Render embedded tables as real PDF grids instead of dropping them.
    if (note.tableData.isNotBlank()) {
        val rows = note.tableData.lines().filter { it.trim().startsWith("|") && !it.contains("---") }.map {
            it.trim().trim('|').split('|').map { cell -> cell.trim() }
        }.filter { it.isNotEmpty() }
        if (rows.isNotEmpty()) {
            val cols = rows.maxOf { it.size }.coerceAtLeast(1)
            val cellW = (right-left) / cols
            val cellH = 26f
            rows.forEach { row ->
                ensureSpace(cellH)
                row.forEachIndexed { c, value ->
                    val x = left + c * cellW
                    canvas!!.drawRect(x, y, x + cellW, y + cellH, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = AndroidColor.LTGRAY })
                    canvas!!.drawText(value.take(28), x + 4f, y + 17f, Paint(bodyPaint))
                }
                y += cellH
            }
            y += 12f
        }
    }

    for (attachment in attachments) {
        if (!attachment.mimeType.startsWith("image/")) continue
        val bitmap = openAttachmentBitmap(context, attachment.uri) ?: continue
        val maxW = right-left
        val scale = minOf(1f, maxW / bitmap.width.toFloat())
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        ensureSpace(minOf(drawH, bottom-top) + 12f)
        if (drawH > bottom-top) {
            // Scale very tall images to fit one page rather than cropping them.
            val s = (bottom-top) / bitmap.height.toFloat()
            canvas!!.drawBitmap(bitmap, null, RectF(left, y, left + bitmap.width*s, y + bitmap.height*s), null)
            y += bitmap.height*s + 12f
        } else {
            canvas!!.drawBitmap(bitmap, null, RectF(left, y, left+drawW, y+drawH), null)
            y += drawH + 12f
        }
        bitmap.recycle()
    }

    page?.let { document.finishPage(it) }
    FileOutputStream(targetFile).use { document.writeTo(it) }
    document.close()
}

private fun writeMinimalDocx(note: NoteEntity, targetFile: File) {
    val esc: (String) -> String = { it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;") }
    ZipOutputStream(FileOutputStream(targetFile)).use { zip ->
        fun entry(name: String, data: String) { zip.putNextEntry(ZipEntry(name)); zip.write(data.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
        entry("[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>")
        entry("_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>")
        val paragraphs = buildString {
            append("<w:p><w:r><w:rPr><w:b/></w:rPr><w:t xml:space=\"preserve\">").append(esc(note.title)).append("</w:t></w:r></w:p>")
            note.content.split("\n").forEach { line -> append("<w:p><w:r><w:t xml:space=\"preserve\">").append(esc(line)).append("</w:t></w:r></w:p>") }
        }
        entry("word/document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>$paragraphs<w:sectPr/></w:body></w:document>")
    }
}

private fun writeNoteContentToFile(
    context: Context,
    note: NoteEntity,
    targetFile: File,
    extension: String,
    attachments: List<RichTextFormatter.AttachmentInfo>
) {
    when (extension.lowercase()) {
        ".pdf" -> writePdf(context, note, targetFile, attachments)
        ".docx" -> writeMinimalDocx(note, targetFile)
        ".html" -> {
            val escaped = note.content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            targetFile.writeText("<html><head><title>${note.title}</title></head><body><h1>${note.title}</h1><pre>$escaped</pre></body></html>", Charsets.UTF_8)
        }
        ".json" -> {
            val obj = org.json.JSONObject().put("title", note.title).put("category", note.category).put("content", note.content)
            targetFile.writeText(obj.toString(2), Charsets.UTF_8)
        }
        else -> {
            val attachmentNote = if (attachments.any { !it.mimeType.startsWith("image/") })
                "\n\n[Attachments]\n" + attachments.joinToString("\n") { "- ${it.fileName}" } else ""
            targetFile.writeText("${note.title}\n\n${note.content}$attachmentNote", Charsets.UTF_8)
        }
    }
}
