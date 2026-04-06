package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.ui.theme.Primary
import com.reeled.quizoverlay.util.PermissionChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOverlayScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(PermissionChecker.hasOverlayPermission(context)) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = PermissionChecker.hasOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isGranted) {
        if (isGranted) {
            onNext()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.permission_overlay_title), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.common_back), modifier = Modifier.size(20.dp))
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
                Text(stringResource(R.string.setup_progress), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(stringResource(R.string.permission_overlay_progress_counter), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Illustration Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    // Mockup Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFE0F2FE), Color(0xFFFAE8FF), Color(0xFFFCE7F3))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Social App Mockup
                        Surface(
                            modifier = Modifier
                                .size(width = 140.dp, height = 100.dp)
                                .offset(x = (-20).dp)
                                .rotate((-5).dp.value),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)))
                                    Box(modifier = Modifier.width(48.dp).height(8.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)))
                                }
                                Box(modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                            }
                        }

                        // Quiz Overlay Mockup
                        Surface(
                            modifier = Modifier
                                .size(width = 100.dp, height = 140.dp)
                                .offset(x = 40.dp)
                                .rotate(10.dp.value),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(2.dp, Primary),
                            shadowElevation = 16.dp
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = Color(0xFFFFD700), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(24.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text("?", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White) }
                                }
                                Box(modifier = Modifier.width(60.dp).height(6.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)))
                                Box(modifier = Modifier.width(40.dp).height(6.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)))
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(Primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                                Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(Primary, RoundedCornerShape(4.dp)))
                            }
                        }
                    }

                    // Content Area
                    Column(
                        modifier = Modifier
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.permission_overlay_enable_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.permission_overlay_enable_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isGranted) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFD7F5E1),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.permission_granted), color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = onOpenSettings,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text(stringResource(R.string.permission_open_settings), fontWeight = FontWeight.Bold)
                            }
                        }
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
                    Text(stringResource(R.string.common_back), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onNext,
                    enabled = isGranted,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(stringResource(R.string.common_next), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
