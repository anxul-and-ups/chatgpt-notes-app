package com.example.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ai.AiService
import com.example.data.preferences.AppPreferences
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onOpenSecurityArea: () -> Unit = {},
    onOpenApiRoom: () -> Unit = {},
    onOpenRecycleBin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val blurApis by preferences.blurApis.collectAsState()
    val syntaxHighlight by preferences.syntaxHighlight.collectAsState()
    val lockPin by preferences.lockPin.collectAsState()
    val securityQuestion by preferences.securityQuestion.collectAsState()
    val securityAnswer by preferences.securityAnswer.collectAsState()
    val securityAutoLock by preferences.securityAutoLock.collectAsState()
    val blinkAlarmIndicator by preferences.blinkAlarmIndicator.collectAsState()

    var showSecurityPinPrompt by remember { mutableStateOf(false) }

    if (showSecurityPinPrompt) {
        com.example.ui.components.PinLockDialog(
            correctPin = lockPin,
            title = "Security Vault Lock",
            subtitle = "Enter 4-digit PIN or Fingerprint to access Security Area",
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer,
            isDarkMode = isDarkMode,
            onDismiss = { showSecurityPinPrompt = false },
            onUnlocked = {
                showSecurityPinPrompt = false
                onOpenSecurityArea()
            },
            onPinReset = { newPin ->
                preferences.setLockPin(newPin)
                Toast.makeText(context, "PIN Reset Successfully!", Toast.LENGTH_SHORT).show()
                showSecurityPinPrompt = false
                onOpenSecurityArea()
            },
            onSecuritySetup = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
                showSecurityPinPrompt = false
                onOpenSecurityArea()
            }
        )
    }


    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar: Back arrow + "Settings" (PART A Item 6)
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: SECURITY AREA (PART I Item 2)
            Text(
                text = "SECURITY & PRIVACY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CrimsonPrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // New Security Area Card with PIN Lock Protection
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                elevation = 3.dp,
                onClick = { showSecurityPinPrompt = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_svg_lock),
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Security Area",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                            Text(
                                text = "PIN / Fingerprint Vault (Hidden Notes & Folders)",
                                fontSize = 11.sp,
                                color = CrimsonPrimary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Security Auto Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDarkMode) Color.White else Color.Black)
                    Text("Choose when the Security Area locks again.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    val autoLockOptions = listOf("immediately" to "Immediately", "leaving" to "When leaving Security Area", "closed" to "When app is closed", "screen" to "When screen is locked / turned off")
                    autoLockOptions.forEach { (value, label) ->
                        Row(Modifier.fillMaxWidth().clickable { preferences.setSecurityAutoLock(value) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = securityAutoLock == value, onClick = { preferences.setSecurityAutoLock(value) }, colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary))
                            Text(label, fontSize = 12.sp, color = if (isDarkMode) Color.White else Color.Black)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = if (isDarkMode) Color(0x1FFFFFFF) else Color(0x1F000000))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Blink Indicator When Alarm Is Active", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)
                            Text("Show the red active-alarm indicators in the sidebar.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(checked = blinkAlarmIndicator, onCheckedChange = { preferences.setBlinkAlarmIndicator(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: GENERAL PREFERENCES
            Text(
                text = "GENERAL PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Blur APIs Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mask API Keys in Feed", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (isDarkMode) Color.White else Color.Black)
                            Text("Masks sensitive strings like •••••••••", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = blurApis,
                            onCheckedChange = { preferences.setBlurApis(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x1FFFFFFF) else Color(0x1F000000))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Syntax Highlight Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Syntax Highlighting", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (isDarkMode) Color.White else Color.Black)
                            Text("Colorize code snippets and JSON", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = syntaxHighlight,
                            onCheckedChange = { preferences.setSyntaxHighlight(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // RECYCLE BIN ACCESS
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                onClick = onOpenRecycleBin
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
                            painter = painterResource(R.drawable.ic_svg_delete),
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Recycle Bin & Trash Management", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Red)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // API Room is the only API configuration surface. Provider keys are encrypted
            // with Android Keystore and the room has its own PIN gate.
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                onClick = onOpenApiRoom
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(R.drawable.ic_svg_vpn), contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("API Room", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDarkMode) Color.White else Color.Black)
                            Text("Providers, encrypted keys and Model Detector", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CrimsonPrimary)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
