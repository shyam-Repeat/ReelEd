package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.ui.theme.Primary
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(
    onAccepted: (String) -> Unit,
    onBack: () -> Unit
) {
    var isChecked by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.consent_title), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.common_back))
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
                Text(stringResource(R.string.consent_step_label), color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(stringResource(R.string.consent_progress_counter), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.25f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.1f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .border(2.dp, Primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Surface(
                        color = Primary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(stringResource(R.string.consent_privacy_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.consent_privacy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Username field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.consent_display_name_label)) },
                placeholder = { Text(stringResource(R.string.consent_display_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isChecked && username.isNotBlank()) {
                            onAccepted(username)
                        }
                    }
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Checkbox Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isChecked = !isChecked },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, if (isChecked) Primary else Primary.copy(alpha = 0.2f)),
                color = if (isChecked) Primary.copy(alpha = 0.02f) else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = Primary)
                    )
                    Text(
                        text = stringResource(R.string.consent_checkbox_text),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.consent_security_note),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Footer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Button(
                    onClick = { onAccepted(username) },
                    enabled = isChecked && username.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(stringResource(R.string.common_next), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.common_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
