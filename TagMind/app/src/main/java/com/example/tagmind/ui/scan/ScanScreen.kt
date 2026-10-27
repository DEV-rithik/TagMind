package com.example.tagmind.ui.scan

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tagmind.MainActivity
import com.example.tagmind.theme.CyanAccent
import com.example.tagmind.theme.DarkBackground
import com.example.tagmind.theme.TextSecondary
import com.example.tagmind.ui.register.RegisterItemSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onItemRegistered: (String) -> Unit,
    viewModel: ScanViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? MainActivity
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var pendingUid by remember { mutableStateOf("") }

    // Register this screen as the NFC tag handler
    DisposableEffect(Unit) {
        activity?.onNfcTagScanned = { uid -> viewModel.onTagScanned(uid) }
        onDispose { activity?.onNfcTagScanned = null }
    }

    // React to state changes
    LaunchedEffect(state) {
        when (val s = state) {
            is ScanUiState.UnknownTag -> {
                pendingUid = s.uid
                showSheet = true
            }
            is ScanUiState.Registered -> {
                onItemRegistered(s.item.id)
            }
            is ScanUiState.KnownItem -> onItemRegistered(s.item.id)
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Pulsing NFC ring animation
            NfcPulseRings()

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = when (state) {
                    is ScanUiState.KnownItem -> "Item recognized!"
                    is ScanUiState.UnknownTag -> "New tag detected"
                    else -> "Hold near NFC tag"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (val s = state) {
                    is ScanUiState.KnownItem -> "${s.item.iconEmoji} ${s.item.name}"
                    is ScanUiState.UnknownTag -> "UID: ${s.uid.take(8)}…"
                    else -> "Point the back of your phone\nat the NFC sticker or tag"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }
    }

    // Register item bottom sheet
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                viewModel.resetToScanning()
            },
            sheetState = sheetState,
            containerColor = com.example.tagmind.theme.DarkSurface,
        ) {
            RegisterItemSheet(
                uid = pendingUid,
                onSave = { name, emoji, reminderDelayMs ->
                    scope.launch {
                        sheetState.hide()
                        showSheet = false
                        viewModel.registerItem(pendingUid, name, emoji, reminderDelayMs)
                    }
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showSheet = false
                        viewModel.resetToScanning()
                    }
                },
            )
        }
    }
}

@Composable
private fun NfcPulseRings() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")

    // Three staggered rings
    val scales = (0..2).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    delayMillis = i * 600,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "scale_$i",
        )
    }

    val alphas = (0..2).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    delayMillis = i * 600,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "alpha_$i",
        )
    }

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Pulse rings
        (0..2).forEach { i ->
            val scale by scales[i]
            val alpha by alphas[i]
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .border(2.dp, CyanAccent, CircleShape)
            )
        }

        // Center NFC icon circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(CyanAccent.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, CyanAccent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "📡", fontSize = 32.sp)
        }
    }
}
