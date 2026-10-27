package com.example.tagmind.ui.leaving

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tagmind.MainActivity
import com.example.tagmind.theme.DarkBackground
import com.example.tagmind.theme.DarkCardBorder
import com.example.tagmind.theme.DarkSurface
import com.example.tagmind.theme.EmeraldSuccess
import com.example.tagmind.theme.TextMuted
import com.example.tagmind.theme.TextSecondary

@Composable
fun LeavingCheckScreen(
    onBack: () -> Unit,
    viewModel: LeavingCheckViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? MainActivity

    // Tapping a registered tag ticks off its item
    DisposableEffect(Unit) {
        activity?.onNfcTagScanned = { uid -> viewModel.onTagScanned(uid) }
        onDispose { activity?.onNfcTagScanned = null }
    }

    val packedCount = state.packedIds.size
    val total = state.items.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (packedCount > 0) {
                TextButton(onClick = viewModel::reset) {
                    Text("Clear ticks", color = TextSecondary)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = if (state.allPacked) "You're all set" else "Got everything?",
                style = MaterialTheme.typography.headlineMedium,
                color = if (state.allPacked) EmeraldSuccess else Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    total == 0 -> "Register an item first, then it will show up here."
                    state.lastScannedName != null -> "${state.lastScannedName} ticked. $packedCount of $total packed."
                    else -> "Tap an item, or tap its NFC tag with your phone. $packedCount of $total packed."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else packedCount / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = EmeraldSuccess,
                trackColor = DarkSurface,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            items(state.items, key = { it.id }) { item ->
                val packed = item.id in state.packedIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(
                            1.5.dp,
                            if (packed) EmeraldSuccess else DarkCardBorder,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { viewModel.toggle(item.id) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(DarkBackground, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = item.iconEmoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (packed) TextMuted else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (packed) TextDecoration.LineThrough else null,
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(if (packed) EmeraldSuccess else DarkBackground, CircleShape)
                            .border(1.5.dp, if (packed) EmeraldSuccess else DarkCardBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (packed) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Packed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
