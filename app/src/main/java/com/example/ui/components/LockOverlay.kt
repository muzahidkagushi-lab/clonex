package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.model.AppSettings
import com.example.domain.model.LockMethod
import com.example.util.SecurityHelper
import kotlin.math.hypot

@Composable
fun SecurityLockOverlay(
    subtitle: String,
    settings: AppSettings,
    canDismiss: Boolean,
    onUnlockSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var activeMethod by remember(settings.lockMethod) {
        mutableStateOf(
            when (settings.lockMethod) {
                LockMethod.BIOMETRIC ->
                    if (SecurityHelper.isBiometricAvailable(context)) LockMethod.BIOMETRIC
                    else if (settings.patternHash.isNotEmpty()) LockMethod.PATTERN
                    else LockMethod.PIN
                LockMethod.PATTERN ->
                    if (settings.patternHash.isNotEmpty()) LockMethod.PATTERN else LockMethod.PIN
                LockMethod.PIN -> LockMethod.PIN
            }
        )
    }

    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val biometricTitle = stringResource(R.string.lock_biometric_title)
    val biometricSubtitle = stringResource(R.string.lock_biometric_subtitle)
    val biometricCancel = stringResource(R.string.lock_biometric_cancel)
    val wrongPinText = stringResource(R.string.lock_wrong_pin)
    val wrongPatternText = stringResource(R.string.lock_wrong_pattern)

    fun triggerBiometric() {
        SecurityHelper.showBiometricPrompt(
            context = context,
            title = biometricTitle,
            subtitle = biometricSubtitle,
            negativeText = biometricCancel,
            onSuccess = onUnlockSuccess,
            onFallbackOrError = {
                activeMethod = if (settings.patternHash.isNotEmpty()) LockMethod.PATTERN else LockMethod.PIN
            }
        )
    }

    LaunchedEffect(activeMethod) {
        if (activeMethod == LockMethod.BIOMETRIC) {
            triggerBiometric()
        }
    }

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.lock_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (activeMethod == LockMethod.PATTERN && settings.patternHash.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.lock_draw_pattern),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PatternLockCanvas(
                        onPatternComplete = { pattern ->
                            val hash = SecurityHelper.hashSecret(pattern)
                            if (hash == settings.patternHash) {
                                errorMessage = null
                                onUnlockSuccess()
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                errorMessage = wrongPatternText
                            }
                        }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.lock_enter_pin),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(4) { index ->
                            val filled = index < pinInput.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    PinKeypad(
                        onDigit = { digit ->
                            if (pinInput.length < 4) {
                                errorMessage = null
                                val next = pinInput + digit
                                pinInput = next
                                if (next.length == 4) {
                                    val expected = settings.pinHash
                                    val matches = expected.isEmpty() ||
                                        SecurityHelper.hashSecret(next) == expected
                                    if (matches) {
                                        onUnlockSuccess()
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        errorMessage = wrongPinText
                                        pinInput = ""
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (pinInput.isNotEmpty()) {
                                pinInput = pinInput.dropLast(1)
                            }
                        }
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (SecurityHelper.isBiometricAvailable(context)) {
                        FilledTonalButton(
                            onClick = {
                                activeMethod = LockMethod.BIOMETRIC
                                triggerBiometric()
                            }
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.lock_use_biometric))
                        }
                    }
                    if (canDismiss) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.clone_cancel_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(64.dp))
                        "DEL" -> IconButton(
                            onClick = onBackspace,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .testTag("pin_key_backspace")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Backspace"
                            )
                        }
                        else -> Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onDigit(key) }
                                .testTag("pin_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatternLockCanvas(
    modifier: Modifier = Modifier,
    onPatternComplete: (String) -> Unit
) {
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var dragPoint by remember { mutableStateOf<Offset?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = modifier
            .size(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedNodes.clear()
                        dragPoint = offset
                        val hit = hitTestNode(offset, size.width.toFloat(), size.height.toFloat())
                        if (hit != null) selectedNodes.add(hit)
                    },
                    onDrag = { change, _ ->
                        dragPoint = change.position
                        val hit = hitTestNode(change.position, size.width.toFloat(), size.height.toFloat())
                        if (hit != null && hit !in selectedNodes) {
                            selectedNodes.add(hit)
                        }
                    },
                    onDragEnd = {
                        dragPoint = null
                        if (selectedNodes.isNotEmpty()) {
                            onPatternComplete(selectedNodes.joinToString("-"))
                        }
                        selectedNodes.clear()
                    },
                    onDragCancel = {
                        dragPoint = null
                        selectedNodes.clear()
                    }
                )
            }
    ) {
        val centers = nodeCenters(size.width, size.height)

        for (i in 0 until selectedNodes.size - 1) {
            val from = centers[selectedNodes[i]]
            val to = centers[selectedNodes[i + 1]]
            drawLine(
                color = primaryColor,
                start = from,
                end = to,
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
        }
        val activeDrag = dragPoint
        if (selectedNodes.isNotEmpty() && activeDrag != null) {
            drawLine(
                color = primaryColor.copy(alpha = 0.55f),
                start = centers[selectedNodes.last()],
                end = activeDrag,
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        centers.forEachIndexed { index, center ->
            val isSelected = index in selectedNodes
            drawCircle(
                color = if (isSelected) primaryColor.copy(alpha = 0.25f) else inactiveColor.copy(alpha = 0.2f),
                radius = if (isSelected) 28f else 20f,
                center = center
            )
            drawCircle(
                color = if (isSelected) primaryColor else inactiveColor,
                radius = 10f,
                center = center
            )
        }
    }
}

private fun nodeCenters(width: Float, height: Float): List<Offset> {
    val centers = ArrayList<Offset>(9)
    val stepX = width / 3f
    val stepY = height / 3f
    for (row in 0..2) {
        for (col in 0..2) {
            centers.add(Offset(col * stepX + stepX / 2f, row * stepY + stepY / 2f))
        }
    }
    return centers
}

private fun hitTestNode(point: Offset, width: Float, height: Float): Int? {
    val centers = nodeCenters(width, height)
    val threshold = (width / 3f) * 0.36f
    centers.forEachIndexed { index, center ->
        if (hypot(point.x - center.x, point.y - center.y) <= threshold) {
            return index
        }
    }
    return null
}

@Composable
fun CredentialSetupDialog(
    initialMethod: LockMethod,
    onSavePin: (String) -> Unit,
    onSavePattern: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember {
        mutableStateOf(if (initialMethod == LockMethod.PATTERN) LockMethod.PATTERN else LockMethod.PIN)
    }
    var pin by remember { mutableStateOf("") }
    var patternError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (mode == LockMethod.PIN) stringResource(R.string.lock_setup_pin_title)
                else stringResource(R.string.lock_setup_pattern_title)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { mode = LockMethod.PIN }) {
                        Text(stringResource(R.string.lock_method_pin))
                    }
                    FilledTonalButton(onClick = { mode = LockMethod.PATTERN }) {
                        Text(stringResource(R.string.lock_method_pattern))
                    }
                }

                if (mode == LockMethod.PIN) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                        label = { Text(stringResource(R.string.lock_setup_pin_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_pin_field")
                    )
                } else {
                    PatternLockCanvas(
                        onPatternComplete = { pattern ->
                            val count = pattern.split("-").size
                            if (count >= 4) {
                                patternError = false
                                onSavePattern(pattern)
                                onDismiss()
                            } else {
                                patternError = true
                            }
                        }
                    )
                    if (patternError) {
                        Text(
                            text = stringResource(R.string.lock_setup_pattern_too_short),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (mode == LockMethod.PIN) {
                Button(
                    onClick = {
                        if (pin.length == 4) {
                            onSavePin(pin)
                            onDismiss()
                        }
                    },
                    enabled = pin.length == 4,
                    modifier = Modifier.testTag("save_pin_btn")
                ) {
                    Text(stringResource(R.string.clone_save_button))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.clone_cancel_button))
            }
        }
    )
}
