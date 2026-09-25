package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.SirenAudioPlayer

@Composable
fun PinLockDialog(
    correctPin: String,
    title: String = "Security Passcode",
    subtitle: String = "Enter 4-digit PIN or use Fingerprint to unlock",
    securityQuestion: String = "",
    securityAnswer: String = "",
    isDarkMode: Boolean = true,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
    onPinReset: ((String) -> Unit)? = null,
    onSecuritySetup: ((pin: String, question: String, answer: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val setupMode = correctPin.isBlank() || securityQuestion.isBlank() || securityAnswer.isBlank()
    var setupStep by remember(setupMode) { mutableIntStateOf(if (setupMode) 0 else -1) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var enteredAnswer by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }

    fun wrongPin() {
        val prefs = context.getSharedPreferences("au_notes_prefs", Context.MODE_PRIVATE)
        val attempts = prefs.getInt("wrong_pin_attempts", 0) + 1
        prefs.edit().putInt("wrong_pin_attempts", attempts).apply()
        errorMessage = if (attempts >= 3) "Incorrect PIN. Access denied." else "Incorrect PIN. Attempt $attempts/3."
        if (attempts >= 3) {
            SirenAudioPlayer.playWrongPin(context)
            prefs.edit().putInt("wrong_pin_attempts", 0).apply()
        }
        enteredPin = ""
    }

    fun unlockSuccess() {
        context.getSharedPreferences("au_notes_prefs", Context.MODE_PRIVATE).edit().putInt("wrong_pin_attempts", 0).apply()
        onUnlocked()
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            isDarkMode = isDarkMode,
            strong = true
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(com.example.R.drawable.ic_svg_lock), null, tint = CrimsonPrimary)
                        Text(
                            if (setupMode) "Create Security PIN" else title,
                            fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Close") }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        setupStep == 0 -> "Choose a new 4-digit PIN."
                        setupStep == 1 -> "Confirm your 4-digit PIN."
                        setupStep == 2 -> "Set a security question and answer for recovery."
                        else -> subtitle
                    }, fontSize = 12.sp,
                    color = if (isDarkMode) Color.White.copy(.65f) else Color.DarkGray
                )
                Spacer(Modifier.height(18.dp))

                if (setupMode && setupStep >= 0) {
                    when (setupStep) {
                        0, 1 -> {
                            val value = if (setupStep == 0) enteredPin else confirmPin
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                repeat(4) { i ->
                                    GlassCard(Modifier.size(18.dp), CircleShape, isDarkMode, isInset = i >= value.length) {
                                        Box(Modifier.fillMaxSize().then(if (i < value.length) Modifier.background(Brush.radialGradient(listOf(CrimsonPrimary, Color(0xFFCC1F41)))) else Modifier))
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            val digits = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","DEL"))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                digits.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    row.forEach { digit ->
                                        GlassCard(Modifier.size(58.dp), CircleShape, isDarkMode, elevation = 5.dp, onClick = {
                                            when (digit) {
                                                "DEL" -> if (value.isNotEmpty()) { if (setupStep == 0) enteredPin = value.dropLast(1) else confirmPin = value.dropLast(1); errorMessage = null }
                                                "" -> Unit
                                                else -> if (value.length < 4) {
                                                    val next = value + digit
                                                    if (setupStep == 0) enteredPin = next else confirmPin = next
                                                    if (next.length == 4 && setupStep == 0) setupStep = 1
                                                    else if (next.length == 4 && setupStep == 1) {
                                                        if (confirmPin == enteredPin) setupStep = 2 else { errorMessage = "PINs do not match."; confirmPin = "" }
                                                    }
                                                }
                                            }
                                        }) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text(if (digit == "DEL") "⌫" else digit, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) } }
                                    }
                                }}
                            }
                        }
                        2 -> {
                            OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Security Question") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Answer") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (question.isBlank() || answer.isBlank()) errorMessage = "Enter both question and answer."
                                    else { onSecuritySetup?.invoke(enteredPin, question.trim(), answer.trim()); unlockSuccess() }
                                }, colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary), modifier = Modifier.fillMaxWidth()
                            ) { Text("Complete Security Setup") }
                        }
                    }
                    if (errorMessage != null) { Spacer(Modifier.height(10.dp)); Text(errorMessage!!, color = Color(0xFFFF5252), fontSize = 12.sp) }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        repeat(4) { i ->
                            GlassCard(Modifier.size(18.dp), CircleShape, isDarkMode, isInset = i >= enteredPin.length) {
                                Box(Modifier.fillMaxSize().then(if (i < enteredPin.length) Modifier.background(Brush.radialGradient(listOf(CrimsonPrimary, Color(0xFFCC1F41)))) else Modifier))
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    val digits = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("FP","0","DEL"))
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        digits.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            row.forEach { digit ->
                                GlassCard(Modifier.size(58.dp), CircleShape, isDarkMode, elevation = 5.dp, onClick = {
                                    when (digit) {
                                        "DEL" -> { enteredPin = enteredPin.dropLast(1); errorMessage = null }
                                        "FP" -> {
                                            val activity = context as? androidx.fragment.app.FragmentActivity
                                            if (activity == null) Toast.makeText(context, "Use your PIN", Toast.LENGTH_SHORT).show()
                                            else com.example.ui.util.BiometricAuthHelper.authenticate(activity) { success, error -> if (success) unlockSuccess() else if (error != null) errorMessage = error }
                                        }
                                        else -> if (enteredPin.length < 4) { enteredPin += digit; if (enteredPin.length == 4) { if (enteredPin == correctPin) unlockSuccess() else wrongPin() } }
                                    }
                                }) { Box(Modifier.fillMaxSize(), Alignment.Center) { if (digit == "FP") Icon(painterResource(com.example.R.drawable.ic_svg_fingerprint), "Fingerprint", tint = CrimsonPrimary, modifier = Modifier.size(26.dp)) else Text(if (digit == "DEL") "⌫" else digit, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) } }
                            }
                        }}
                    }
                    if (errorMessage != null) { Spacer(Modifier.height(10.dp)); Text(errorMessage!!, color = Color(0xFFFF5252), fontSize = 12.sp) }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { showForgotDialog = true }) { Text("Forgot PIN? Reset with Security Question", fontSize = 11.sp, color = CrimsonPrimary) }
                }
            }
        }
    }

    if (showForgotDialog && !setupMode) {
        Dialog(onDismissRequest = { showForgotDialog = false }) {
            GlassCard(Modifier.fillMaxWidth().padding(16.dp), isDarkMode = isDarkMode, strong = true) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reset PIN via Security Question", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                    Text("Question: $securityQuestion", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = enteredAnswer, onValueChange = { enteredAnswer = it }, label = { Text("Answer") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newPinInput, onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPinInput = it }, label = { Text("New 4-digit PIN") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (enteredAnswer.trim().equals(securityAnswer.trim(), true) && newPinInput.length == 4) {
                            onPinReset?.invoke(newPinInput); showForgotDialog = false; unlockSuccess()
                        } else Toast.makeText(context, "Incorrect answer or invalid PIN", Toast.LENGTH_SHORT).show()
                    }, colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary), modifier = Modifier.fillMaxWidth()) { Text("Verify & Unlock") }
                }
            }
        }
    }
}
