package com.example.ui.screens

import android.widget.Toast
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.SecureApiStore
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private val builtInProviders = listOf("Gemini", "OpenAI", "Anthropic", "DeepSeek", "Kimi", "OpenCode", "Hugging Face")

@Composable
fun ApiRoomScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onRequestUnlock: (onUnlocked: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val secure = remember { SecureApiStore(context) }
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    var unlocked by remember { mutableStateOf(false) }
    var provider by remember { mutableStateOf(secure.get("provider").ifBlank { "Gemini" }) }
    var key by remember { mutableStateOf(secure.get("key")) }
    var baseUrl by remember { mutableStateOf(secure.get("baseUrl")) }
    var model by remember { mutableStateOf(secure.get("model")) }
    var showKey by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(secure.get("locked") == "1") }
    var status by remember { mutableStateOf("Not tested") }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var testing by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(provider !in builtInProviders) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (locked || !preferences.hasSecuritySetup()) {
            onRequestUnlock { unlocked = true }
        } else unlocked = true
    }

    if (!unlocked) {
        GlassBackground(isDarkMode) { Box(Modifier.fillMaxSize()) }
        return
    }

    fun save() {
        secure.put("provider", provider)
        secure.put("key", key)
        secure.put("baseUrl", baseUrl)
        secure.put("model", model)
        secure.put("locked", if (locked) "1" else "0")
        Toast.makeText(context, "API configuration saved securely", Toast.LENGTH_SHORT).show()
    }

    GlassBackground(isDarkMode) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeuIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", isDarkMode, 38.dp, 18.dp,
                    if (isDarkMode) Color.White else Color.Black, onBack)
                Spacer(Modifier.width(12.dp))
                Icon(painterResource(R.drawable.ic_svg_vpn), null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("API Room", fontSize = 20.sp, color = if (isDarkMode) Color.White else Color.Black)
            }

            GlassCard(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), isDarkMode) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Provider", color = if (isDarkMode) Color.White else Color.Black)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        builtInProviders.take(4).forEach { p ->
                            FilterChip(selected = provider == p, onClick = { provider = p; showCustom = false }, label = { Text(p, fontSize = 11.sp) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        builtInProviders.drop(4).forEach { p ->
                            FilterChip(selected = provider == p, onClick = { provider = p; showCustom = false }, label = { Text(p, fontSize = 11.sp) })
                        }
                        FilterChip(selected = showCustom, onClick = { showCustom = true; provider = "Custom" }, label = { Text("Custom") }, leadingIcon = { Icon(Icons.Default.Add, null) })
                    }
                    if (showCustom) {
                        OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Provider name") }, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API key") }, modifier = Modifier.fillMaxWidth(), visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton({ showKey = !showKey }) { Icon(painterResource(if (showKey) R.drawable.ic_svg_eye_off else R.drawable.ic_svg_eye), null) } })
                    OutlinedButton(
                        onClick = {
                            val clip = clipboard.primaryClip
                            val pasted = clip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (pasted.isNotBlank()) { key = pasted.trim(); Toast.makeText(context, "API key pasted", Toast.LENGTH_SHORT).show() }
                            else Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Paste API Key from Clipboard") }
                    OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL / endpoint (optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model (optional)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = locked, onCheckedChange = { locked = it })
                        Spacer(Modifier.width(8.dp)); Text("Lock API Room", color = if (isDarkMode) Color.White else Color.Black)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { save() }, colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)) { Text("Save / Update") }
                        OutlinedButton(onClick = {
                            testing = true; status = "Testing connection…"; models = emptyList()
                            scope.launch(Dispatchers.IO) {
                                val result = testProvider(provider, key, baseUrl)
                                withContext(Dispatchers.Main) { testing = false; status = result.first; models = result.second }
                            }
                        }) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Model Detector") }
                    }
                    Text(status, color = if (status.startsWith("ACTIVE")) Color(0xFF35C759) else Color.Gray, fontSize = 12.sp)
                    if (testing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (models.isNotEmpty()) Text("Available models:\n" + models.joinToString("\n") { "• $it" }, color = if (isDarkMode) Color.White else Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}


private fun testProvider(provider: String, key: String, baseUrl: String): Pair<String, List<String>> {
    if (key.isBlank()) return "INACTIVE — API key is empty" to emptyList()
    val client = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    val p = provider.trim()
    val url = if (baseUrl.isNotBlank()) baseUrl.trimEnd('/') else when {
        p.equals("Gemini", true) -> "https://generativelanguage.googleapis.com/v1beta/models"
        p.equals("Anthropic", true) -> "https://api.anthropic.com/v1/models"
        p.equals("DeepSeek", true) -> "https://api.deepseek.com/models"
        p.equals("Kimi", true) -> "https://api.moonshot.cn/v1/models"
        p.equals("OpenCode", true) -> "https://opencode.ai/zen/v1/models"
        p.equals("Hugging Face", true) -> "https://router.huggingface.co/v1/models"
        else -> "https://api.openai.com/v1/models"
    }
    return try {
        val requestBuilder = Request.Builder().url(if (p.equals("Gemini", true)) "$url?key=$key" else url)
        if (p.equals("Anthropic", true)) {
            requestBuilder.header("x-api-key", key).header("anthropic-version", "2023-06-01")
        } else if (!p.equals("Gemini", true)) {
            requestBuilder.header("Authorization", "Bearer $key")
        }
        client.newCall(requestBuilder.get().build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) "INACTIVE — HTTP ${response.code}" to emptyList()
            else {
                val names = Regex("\"(?:name|id)\"\\s*:\\s*\"([^\"]+)\"").findAll(body)
                    .map { it.groupValues[1] }.filter { it.isNotBlank() }.distinct().take(50).toList()
                "ACTIVE — HTTP ${response.code}" to names
            }
        }
    } catch (e: Exception) { "INACTIVE — ${e.message ?: "connection failed"}" to emptyList() }
}
