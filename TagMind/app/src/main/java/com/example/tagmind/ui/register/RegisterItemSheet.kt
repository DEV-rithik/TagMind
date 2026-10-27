package com.example.tagmind.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tagmind.theme.DarkBackground
import com.example.tagmind.theme.DarkCardBorder
import com.example.tagmind.theme.DarkSurface
import com.example.tagmind.theme.EmeraldSuccess
import com.example.tagmind.theme.TextMuted
import com.example.tagmind.theme.TextSecondary
import java.util.concurrent.TimeUnit

internal val EMOJI_OPTIONS = listOf(
    "🏍️", "🔑", "👜", "💊", "💼", "📱", "🎒", "🔧",
    "🚗", "🎓", "📷", "💻", "🏋️", "🎸", "🔐", "🌂",
    "👓", "🎧", "🏠", "✏️", "📚", "🍱", "💳", "🗝️",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterItemSheet(
    uid: String,
    onSave: (name: String, emoji: String, reminderDelayMs: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(EMOJI_OPTIONS.first()) }
    var setReminder by remember { mutableStateOf(true) }
    var selectedDelayMs by remember { mutableStateOf(TimeUnit.MINUTES.toMillis(30)) }
    var showCustomDialog by remember { mutableStateOf(false) }

    if (showCustomDialog) {
        com.example.tagmind.ui.common.CustomTimerDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { ms ->
                selectedDelayMs = ms
                showCustomDialog = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .imePadding(),
    ) {
        // Header
        Text(
            text = "New Tag Detected!",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "UID: ${uid.take(8)}…",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Emoji picker
        Text(
            text = "Choose an icon",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EMOJI_OPTIONS.forEach { emoji ->
                EmojiChip(
                    emoji = emoji,
                    selected = emoji == selectedEmoji,
                    onClick = { selectedEmoji = emoji },
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Item Name") },
            placeholder = { Text("e.g. Helmet, Keys, Backpack…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (name.isNotBlank()) {
                    onSave(name.trim(), selectedEmoji, if (setReminder) selectedDelayMs else null)
                }
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = DarkCardBorder,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Reminder toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Set a reminder after registering",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
            Switch(
                checked = setReminder,
                onCheckedChange = { setReminder = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkBackground,
                ),
            )
        }

        if (setReminder) {
            Spacer(modifier = Modifier.height(10.dp))
            val presets = listOf(
                "15m" to TimeUnit.MINUTES.toMillis(15),
                "30m" to TimeUnit.MINUTES.toMillis(30),
                "1h" to TimeUnit.HOURS.toMillis(1),
                "2h" to TimeUnit.HOURS.toMillis(2),
            )
            val isCustom = presets.none { it.second == selectedDelayMs }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { (label, ms) ->
                    DelayChip(
                        label = label,
                        selected = selectedDelayMs == ms,
                        onClick = { selectedDelayMs = ms },
                    )
                }
                DelayChip(
                    label = if (isCustom) formatDelay(selectedDelayMs) else "Custom",
                    selected = isCustom,
                    onClick = { showCustomDialog = true },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save button
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSave(name.trim(), selectedEmoji, if (setReminder) selectedDelayMs else null)
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldSuccess,
                contentColor = Color.White,
                disabledContainerColor = DarkSurface,
                disabledContentColor = TextMuted,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "Save Item",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancel", color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatDelay(ms: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

@Composable
private fun EmojiChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else DarkBackground)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else DarkCardBorder,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 20.sp)
    }
}

@Composable
private fun DelayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else DarkBackground)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else DarkCardBorder,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
