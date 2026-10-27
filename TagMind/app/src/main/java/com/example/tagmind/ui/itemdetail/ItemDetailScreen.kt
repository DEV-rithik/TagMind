package com.example.tagmind.ui.itemdetail

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import com.example.tagmind.ui.common.CustomTimerDialog
import com.example.tagmind.ui.register.EMOJI_OPTIONS
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tagmind.MainActivity
import com.example.tagmind.data.db.entity.ReminderStatus
import com.example.tagmind.data.db.entity.ScanEventEntity
import com.example.tagmind.data.db.entity.ScanType
import com.example.tagmind.theme.AmberWarning
import com.example.tagmind.theme.CyanAccent
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    itemId: String,
    onBack: () -> Unit,
    viewModel: ItemDetailViewModel = viewModel(
        key = "itemDetail:$itemId",
        factory = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                handle["itemId"] = itemId
                ItemDetailViewModel(
                    app = (this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])!!,
                    savedStateHandle = handle,
                )
            }
        }
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? MainActivity

    // Register NFC handler to confirm reminder when tag is scanned
    DisposableEffect(Unit) {
        activity?.onNfcTagScanned = { scannedUid ->
            viewModel.onTagScanned(scannedUid)
        }
        onDispose { activity?.onNfcTagScanned = null }
    }

    val item = state.item
    val reminder = state.activeReminder

    var showCustomTimer by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showCustomTimer) {
        CustomTimerDialog(
            onDismiss = { showCustomTimer = false },
            onConfirm = { ms ->
                viewModel.setReminder(ms)
                showCustomTimer = false
            },
        )
    }

    if (showEditDialog && item != null) {
        EditItemDialog(
            initialName = item.name,
            initialEmoji = item.iconEmoji,
            onDismiss = { showEditDialog = false },
            onSave = { name, emoji ->
                viewModel.editItem(name, emoji)
                showEditDialog = false
            },
        )
    }

    if (showDeleteDialog && item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkSurface,
            title = { Text("Delete ${item.name}?", color = Color.White) },
            text = {
                Text(
                    "This removes the item, its reminders and its scan history. " +
                        "The NFC tag itself isn't changed, so you can register it again.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteItem(onDeleted = onBack)
                }) { Text("Delete", color = RedDanger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        // Top bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Back",
                        tint = Color.White,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (item != null) {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit item", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete item", tint = RedDanger)
                    }
                }
            }
        }

        if (item == null) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Loading…", color = TextMuted)
                }
            }
        } else {
            // Hero card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    CyanAccent.copy(alpha = 0.4f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = item.iconEmoji, fontSize = 72.sp)
                }
            }

            // Name + status
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val (statusColor, statusLabel) = when (reminder?.status) {
                        ReminderStatus.PENDING -> {
                            val mins = TimeUnit.MILLISECONDS.toMinutes(
                                reminder.fireAt - System.currentTimeMillis()
                            ).coerceAtLeast(0)
                            AmberWarning to "⏰ Reminder fires in ${mins}m"
                        }
                        ReminderStatus.FIRED -> RedDanger to "🔔 Scan your tag to confirm!"
                        ReminderStatus.CONFIRMED -> EmeraldSuccess to "✓ Confirmed"
                        else -> TextSecondary to "No active reminder"
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Timer chips
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Set Reminder",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        listOf(
                            "15m" to { viewModel.setReminder(TimeUnit.MINUTES.toMillis(15)) },
                            "30m" to { viewModel.setReminder(TimeUnit.MINUTES.toMillis(30)) },
                            "1h" to { viewModel.setReminder(TimeUnit.HOURS.toMillis(1)) },
                            "2h" to { viewModel.setReminder(TimeUnit.HOURS.toMillis(2)) },
                            "Custom" to { showCustomTimer = true },
                        ).forEach { (label, action) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                                    .clickable(onClick = action)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            // Cancel reminder button
            if (reminder != null && (reminder.status == ReminderStatus.PENDING || reminder.status == ReminderStatus.FIRED)) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = viewModel::cancelReminder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedDanger),
                    ) {
                        Text("Cancel Reminder", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // If alarm fired, show confirm button
            if (reminder?.status == ReminderStatus.FIRED) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::confirmReminder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    ) {
                        Text("✓ Confirm I Have It", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Scan history header
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Scan History",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // History items
            items(state.scanHistory) { event ->
                ScanHistoryRow(event = event)
            }

            if (state.scanHistory.isEmpty()) {
                item {
                    Text(
                        text = "No scan history yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditItemDialog(
    initialName: String,
    initialEmoji: String,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Edit item", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                Text("Icon", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val options = if (emoji in EMOJI_OPTIONS) EMOJI_OPTIONS else listOf(emoji) + EMOJI_OPTIONS
                    options.forEach { option ->
                        val selected = option == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else DarkBackground,
                                )
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else DarkCardBorder,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { emoji = option },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = option, fontSize = 18.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), emoji) }, enabled = name.isNotBlank()) {
                Text("Save changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
    )
}

@Composable
private fun ScanHistoryRow(event: ScanEventEntity) {
    val (dotColor, label) = when (event.type) {
        ScanType.REGISTER -> MaterialTheme.colorScheme.primary to "Registered"
        ScanType.REMIND -> AmberWarning to "Reminder set"
        ScanType.CONFIRM -> EmeraldSuccess to "Confirmed"
        else -> TextSecondary to event.type
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    .format(Date(event.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}
