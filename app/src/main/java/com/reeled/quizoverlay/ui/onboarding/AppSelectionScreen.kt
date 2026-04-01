package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    initialMonitoredApps: Set<String>,
    onSaveSelection: (Set<String>) -> Unit
) {
    var selectedApps by remember { mutableStateOf(initialMonitoredApps) }

    val appOptions = listOf(
        AppOption("YouTube", "com.google.android.youtube", "Video"),
        AppOption("Instagram", "com.instagram.android", "Social Media"),
        AppOption("Instagram Lite", "com.instagram.lite", "Social Media"),
        AppOption("X", "com.twitter.android", "Social Media"),
        AppOption("Facebook", "com.facebook.katana", "Social Media")
    )

    fun toggleApp(packageName: String) {
        val newSet = selectedApps.toMutableSet()
        if (newSet.contains(packageName)) {
            newSet.remove(packageName)
        } else {
            newSet.add(packageName)
        }
        selectedApps = newSet
        onSaveSelection(newSet)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        CenterAlignedTopAppBar(
            title = { Text("Detect Apps", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", modifier = Modifier.size(20.dp))
                }
            }
        )

        // Progress
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Setup Progress", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Step 6 of 8", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.75f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Choose apps to detect", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pick social apps that should trigger monitoring and quiz overlays.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            appOptions.forEach { app ->
                val isSelected = selectedApps.contains(app.packageName)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { toggleApp(app.packageName) },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, if (isSelected) Primary else Primary.copy(alpha = 0.1f)),
                    color = if (isSelected) Primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Initials circle
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = app.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = app.category, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { toggleApp(app.packageName) },
                            colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                    }
                }
            }
        }

        // Bottom Navigation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Back", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onNext,
                    enabled = selectedApps.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Next", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class AppOption(val name: String, val packageName: String, val category: String)
