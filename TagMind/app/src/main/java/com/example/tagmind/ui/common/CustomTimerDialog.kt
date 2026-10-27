package com.example.tagmind.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tagmind.theme.DarkCardBorder
import com.example.tagmind.theme.DarkSurface
import com.example.tagmind.theme.TextSecondary
import java.util.concurrent.TimeUnit

/**
 * Lets the user pick any reminder delay in hours and minutes.
 * [onConfirm] receives the delay in milliseconds.
 */
@Composable
fun CustomTimerDialog(
    onDismiss: () -> Unit,
    onConfirm: (delayMs: Long) -> Unit,
) {
    var hours by remember { mutableStateOf("0") }
    var minutes by remember { mutableStateOf("45") }

    val totalMs = TimeUnit.HOURS.toMillis(hours.toLongOrNull() ?: 0L) +
        TimeUnit.MINUTES.toMillis(minutes.toLongOrNull() ?: 0L)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = DarkCardBorder,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = TextSecondary,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Custom reminder", color = Color.White) },
        text = {
            Column {
                Text("Remind me in", color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { v -> if (v.length <= 2 && v.all(Char::isDigit)) hours = v },
                        label = { Text("Hours") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { v -> if (v.length <= 2 && v.all(Char::isDigit)) minutes = v },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(totalMs) }, enabled = totalMs > 0) {
                Text("Set reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
    )
}
