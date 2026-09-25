package com.example.ui.screens

import androidx.compose.ui.res.painterResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppPreferences
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlarmScreen(preferences: AppPreferences, isDarkMode: Boolean, onBack: () -> Unit, onEdit: () -> Unit) {
    val active by preferences.activeAlarm.collectAsState()
    val title by preferences.alarmTitle.collectAsState()
    val time by preferences.alarmTime.collectAsState()
    val ringtone by preferences.alarmRingtone.collectAsState()
    var remaining by remember(time, active) { mutableLongStateOf(maxOf(0L, time - System.currentTimeMillis())) }
    LaunchedEffect(time, active) {
        while (active) {
            remaining = maxOf(0L, time - System.currentTimeMillis())
            delay(1000L)
        }
    }
    val hours = remaining / 3_600_000L
    val minutes = (remaining / 60_000L) % 60L
    GlassBackground(isDarkMode) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeuIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", isDarkMode, 38.dp, 18.dp, if (isDarkMode) Color.White else Color.Black, onClick = onBack)
                Spacer(Modifier.width(12.dp)); Icon(Icons.Default.Alarm, null, tint = CrimsonPrimary); Spacer(Modifier.width(8.dp)); Text("Alarm", fontSize = 20.sp, color = if (isDarkMode) Color.White else Color.Black)
            }
            GlassCard(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), isDarkMode) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (!active) {
                        Text("No active alarm", color = Color.Gray)
                        Text("Set an alarm from a note's Read Mode.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        Text(title.ifBlank { "Note Reminder" }, style = MaterialTheme.typography.titleMedium, color = if (isDarkMode) Color.White else Color.Black)
                        Text(SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(time)), color = CrimsonPrimary)
                        Text("Rings in ${hours}h ${minutes}m", fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)
                        Text("Ringtone: ${ringtone.substringAfterLast('/').ifBlank { "System alarm sound" }}", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onEdit) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(4.dp)); Text("Edit") }
                            OutlinedButton(onClick = { preferences.clearActiveAlarm() }) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}
