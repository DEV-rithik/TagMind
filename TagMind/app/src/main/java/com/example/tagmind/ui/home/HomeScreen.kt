package com.example.tagmind.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.tagmind.ui.common.CustomTimerDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tagmind.data.db.entity.ReminderEntity
import com.example.tagmind.data.db.entity.ReminderStatus
import com.example.tagmind.theme.AmberWarning
import com.example.tagmind.theme.DarkBackground
import com.example.tagmind.theme.DarkCardBorder
import com.example.tagmind.theme.DarkSurface
import com.example.tagmind.theme.EmeraldSuccess
import com.example.tagmind.theme.RedDanger
import com.example.tagmind.theme.TextMuted
import com.example.tagmind.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    onItemClick: (String) -> Unit,
    onScanClick: () -> Unit,
    onLeavingCheckClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var customTimerItemId by remember { mutableStateOf<String?>(null) }

    customTimerItemId?.let { itemId ->
        CustomTimerDialog(
            onDismiss = { customTimerItemId = null },
            onConfirm = { ms ->
                viewModel.setReminder(itemId, ms)
                customTimerItemId = null
            },
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = {
                    Text(text = "📡", fontSize = 18.sp)
                },
                text = { Text("Scan Item", fontWeight = FontWeight.SemiBold) },
            )
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "TagMind",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Your NFC Item Reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                Button(
                    onClick = onLeavingCheckClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChecklistRtl,
                        contentDescription = "Leaving Check",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Leaving?", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val s = state) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading…", color = TextMuted)
                    }
                }
                is HomeUiState.Success -> {
                    if (s.items.isEmpty()) {
                        EmptyState(onScanClick = onScanClick)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 100.dp),
                        ) {
                            items(s.items, key = { it.item.id }) { itemWithReminder ->
                                ItemCard(
                                    itemWithReminder = itemWithReminder,
                                    onClick = { onItemClick(itemWithReminder.item.id) },
                                    onSetReminder = { delayMs ->
                                        viewModel.setReminder(itemWithReminder.item.id, delayMs)
                                    },
                                    onCustomReminder = { customTimerItemId = itemWithReminder.item.id },
                                    onCancelReminder = { reminder ->
                                        viewModel.cancelReminder(reminder)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onScanClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📡", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No items yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap an NFC tag to register your first item",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Scan First Item", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemCard(
    itemWithReminder: ItemWithReminder,
    onClick: () -> Unit,
    onSetReminder: (Long) -> Unit,
    onCustomReminder: () -> Unit,
    onCancelReminder: (ReminderEntity) -> Unit,
) {
    val reminder = itemWithReminder.activeReminder
    val item = itemWithReminder.item

    val (glowColor, statusText) = when (reminder?.status) {
        ReminderStatus.PENDING -> {
            val remaining = reminder.fireAt - System.currentTimeMillis()
            val mins = TimeUnit.MILLISECONDS.toMinutes(remaining).coerceAtLeast(0)
            AmberWarning to "⏰ Reminder in ${mins}m"
        }
        ReminderStatus.FIRED -> RedDanger to "🔔 Tap to confirm!"
        ReminderStatus.CONFIRMED -> EmeraldSuccess to "✓ Confirmed"
        else -> DarkCardBorder to null
    }

    val borderColor by animateColorAsState(
        targetValue = glowColor,
        animationSpec = tween(durationMillis = 600),
        label = "border",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Emoji icon in a circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkBackground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = item.iconEmoji, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (statusText != null) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = glowColor,
                            fontWeight = FontWeight.Medium,
                        )
                    } else {
                        Text(
                            text = "Last scanned ${formatRelativeTime(item.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
                // Status dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(glowColor, CircleShape)
                )
            }

            // Quick timer chips (only when no active pending reminder)
            if (reminder == null || reminder.status == ReminderStatus.CONFIRMED || reminder.status == ReminderStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "15m" to TimeUnit.MINUTES.toMillis(15),
                        "30m" to TimeUnit.MINUTES.toMillis(30),
                        "1h" to TimeUnit.HOURS.toMillis(1),
                        "2h" to TimeUnit.HOURS.toMillis(2),
                    ).forEach { (label, ms) ->
                        TimerChip(label = "+$label", onClick = { onSetReminder(ms) })
                    }
                    TimerChip(label = "Custom", onClick = onCustomReminder)
                }
            }
        }
    }
}

@Composable
private fun TimerChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatRelativeTime(timestampMs: Long): String {
    val diff = System.currentTimeMillis() - timestampMs
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMs))
    }
}
