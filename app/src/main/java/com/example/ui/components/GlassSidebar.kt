package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NeuDarkBorder
import com.example.ui.theme.NeuDarkHighlightBorder
import com.example.ui.theme.NeuDarkShadow
import com.example.ui.theme.NeuDarkSurface
import com.example.ui.theme.NeuDarkSurfaceDark
import com.example.ui.theme.NeuDarkSurfaceLight
import com.example.ui.theme.NeuLightBorder
import com.example.ui.theme.NeuLightHighlightBorder
import com.example.ui.theme.NeuLightShadow
import com.example.ui.theme.NeuLightSurface
import com.example.ui.theme.NeuLightSurfaceDark
import com.example.ui.theme.NeuLightSurfaceLight

@Composable
fun GlassSidebar(
    isOpen: Boolean,
    isDarkMode: Boolean,
    activeAlarm: Boolean = false,
    blinkAlarmIndicator: Boolean = true,
    onClose: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateFolder: (String) -> Unit = {},
    onOpenAlarm: () -> Unit = {},
    onOpenFileEditor: () -> Unit,
    onOpenNameGenerator: () -> Unit,
    onOpenCommandMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenFeaturesModal: () -> Unit
) {
    val context = LocalContext.current
    var showDeveloperModal by remember { mutableStateOf(false) }

    val alarmAlpha by if (activeAlarm && blinkAlarmIndicator) {
        rememberInfiniteTransition(label = "alarm-blink").animateFloat(0.25f, 1f, infiniteRepeatable(keyframes { durationMillis = 900 }))
    } else { androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(if (activeAlarm) 1f else 0f) } }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(350)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable(onClick = onClose)
        ) {
            val drawerBgBrush = if (isDarkMode) {
                Brush.linearGradient(
                    colors = listOf(NeuDarkSurfaceLight, NeuDarkSurface, NeuDarkSurfaceDark)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(NeuLightSurfaceLight, NeuLightSurface, NeuLightSurfaceDark)
                )
            }

            val drawerBorderBrush = if (isDarkMode) {
                Brush.verticalGradient(listOf(NeuDarkHighlightBorder, Color(0x1AFFFFFF), NeuDarkBorder))
            } else {
                Brush.verticalGradient(listOf(NeuLightHighlightBorder, Color(0x40FFFFFF), NeuLightBorder))
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                        ambientColor = if (isDarkMode) NeuDarkShadow else NeuLightShadow,
                        spotColor = if (isDarkMode) NeuDarkShadow else NeuLightShadow
                    )
                    .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                    .background(drawerBgBrush)
                    .border(1.dp, drawerBorderBrush, RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                    .clickable(enabled = false) {}
                    .statusBarsPadding()
                    .padding(vertical = 20.dp, horizontal = 18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WindowDots(size = 11.dp, spacing = 6.dp)

                        NeuIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Close Sidebar",
                            isDarkMode = isDarkMode,
                            size = 34.dp,
                            iconSize = 16.dp,
                            tint = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color(0xFF333333),
                            onClick = onClose
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Title Branding
                    Text(
                        text = "AU NOTES",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimsonPrimary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Your Premium Workspace",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x1FFFFFFF) else Color(0x1F718096))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Section: APPLICATION WORKSPACE
                    Text(
                        text = "APPLICATION WORKSPACE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Alarm
                    Row(Modifier.fillMaxWidth().clickable { onClose(); onOpenAlarm() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Alarm, contentDescription = "Alarm", tint = if (isDarkMode) Color.White else Color(0xFF222222), modifier = Modifier.size(21.dp))
                            if (activeAlarm) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Red.copy(alpha = alarmAlpha)).align(Alignment.TopEnd))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Alarm", color = if (isDarkMode) Color.White else Color(0xFF222222), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    // 2. File Editor (Renamed from Storage File Editor)
                    SidebarMenuItemPainter(
                        icon = painterResource(R.drawable.ic_svg_file_editor),
                        title = "File Editor",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenFileEditor()
                        }
                    )

                    // 3. Name Generator
                    SidebarMenuItemPainter(
                        icon = painterResource(R.drawable.ic_format_txt),
                        title = "Name Generator",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenNameGenerator()
                        }
                    )

                    // 4. Command Mode
                    SidebarMenuItem(
                        icon = Icons.Default.Mic,
                        title = "Command Mode",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenCommandMode()
                        }
                    )

                    // 5. Developer Contact
                    SidebarMenuItem(
                        icon = Icons.Default.ContactMail,
                        title = "Developer Contact",
                        isDarkMode = isDarkMode,
                        onClick = { showDeveloperModal = true }
                    )

                    // 6. About The App
                    SidebarMenuItem(
                        icon = Icons.Default.Info,
                        title = "About The App",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenFeaturesModal()
                        }
                    )

                    // 6. Recycle Bin
                    SidebarMenuItemPainter(
                        icon = painterResource(R.drawable.ic_svg_delete),
                        title = "Recycle Bin",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenRecycleBin()
                        }
                    )

                    // 7. Settings
                    SidebarMenuItemPainter(
                        icon = painterResource(R.drawable.ic_svg_settings),
                        title = "Settings",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenSettings()
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x1FFFFFFF) else Color(0x1F718096))
                    Spacer(modifier = Modifier.height(14.dp))

                    // User Profile Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        isDarkMode = isDarkMode,
                        elevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.anxul_pfp),
                                contentDescription = "ANXUL vfx profile picture",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ANXUL vfx",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                                )
                                Text(
                                    text = "Premium Workspace",
                                    fontSize = 11.sp,
                                    color = CrimsonPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2CF95F))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Version 2.0.0 • AU Workspace",
                        fontSize = 10.sp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    // Developer Contact Modal with Vertically Stacked Blue Tick Accounts (PART C)
    if (showDeveloperModal) {
        DeveloperContactModal(
            isDarkMode = isDarkMode,
            onDismiss = { showDeveloperModal = false },
            onOpenLink = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // fallback
                }
            }
        )
    }
}

@Composable
fun SidebarMenuItem(
    icon: ImageVector,
    title: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0F000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = CrimsonPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color(0xFF222222),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SidebarMenuItemPainter(
    icon: Painter,
    title: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0F000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = CrimsonPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color(0xFF222222),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun DeveloperContactModal(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            isDarkMode = isDarkMode,
            strong = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Developer Profiles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Official Developer & Social Handles",
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // PART C: Accounts stacked vertically with blue tick badge
                VerifiedContactItem(
                    platform = "GitHub",
                    handle = "anxul-and-ups",
                    url = "https://github.com/anxul-and-ups",
                    color = if (isDarkMode) Color(0xFFC9D1D9) else Color(0xFF24292E),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    platform = "YouTube",
                    handle = "http://lrx.anshul",
                    url = "https://youtube.com/@lrx.anshul",
                    color = Color(0xFFFF0000),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    platform = "Gmail (Primary)",
                    handle = "educationaltalks1@gmail.com",
                    url = "mailto:educationaltalks1@gmail.com",
                    color = Color(0xFFEA4335),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    platform = "Gmail (Secondary)",
                    handle = "animalsa154@gmail.com",
                    url = "mailto:animalsa154@gmail.com",
                    color = Color(0xFFEA4335),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    platform = "Telegram",
                    handle = "anxul_ydv",
                    url = "https://t.me/anxul_ydv",
                    color = Color(0xFF2CA5E0),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    platform = "Instagram",
                    handle = "anxul_ydv",
                    url = "https://instagram.com/anxul_ydv",
                    color = Color(0xFFE1306C),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )
            }
        }
    }
}

@Composable
fun VerifiedContactItem(
    platform: String,
    handle: String,
    url: String,
    color: Color,
    isDarkMode: Boolean,
    onOpenLink: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        isDarkMode = isDarkMode,
        elevation = 2.dp,
        onClick = { onOpenLink(url) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = platform,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = color
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Blue Tick Verified Badge
                    Icon(
                        painter = painterResource(R.drawable.ic_svg_verified),
                        contentDescription = "Verified",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = handle,
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.DarkGray
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
