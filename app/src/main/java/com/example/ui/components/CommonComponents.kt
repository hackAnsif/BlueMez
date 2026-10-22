package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.BluetoothConnectionState
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusReconnecting
import com.example.ui.theme.StatusTransferring

@Composable
fun ConnectionStatusBadge(
    connectionState: BluetoothConnectionState,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when (connectionState) {
        is BluetoothConnectionState.Connected -> Quadruple(
            StatusConnected.copy(alpha = 0.15f),
            StatusConnected,
            "Connected: ${connectionState.peerDisplayName}",
            Icons.Default.BluetoothConnected
        )
        is BluetoothConnectionState.Connecting,
        is BluetoothConnectionState.Handshaking -> Quadruple(
            StatusTransferring.copy(alpha = 0.15f),
            StatusTransferring,
            "Connecting...",
            Icons.Default.BluetoothSearching
        )
        is BluetoothConnectionState.Reconnecting -> Quadruple(
            StatusReconnecting.copy(alpha = 0.15f),
            StatusReconnecting,
            "Reconnecting...",
            Icons.Default.BluetoothSearching
        )
        is BluetoothConnectionState.Scanning -> Quadruple(
            StatusTransferring.copy(alpha = 0.15f),
            StatusTransferring,
            "Scanning...",
            Icons.Default.BluetoothSearching
        )
        is BluetoothConnectionState.Disconnected -> Quadruple(
            StatusDisconnected.copy(alpha = 0.15f),
            StatusDisconnected,
            connectionState.reason ?: "Disconnected",
            Icons.Default.Close
        )
        BluetoothConnectionState.Idle -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Offline / Ready",
            Icons.Default.Bluetooth
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun UserAvatar(
    name: String,
    size: Int = 40,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { "U" }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Text(
            text = initials,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.4).sp
        )
    }
}

@Composable
fun EditFriendNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Custom Friend Name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Friend's Custom Name") },
                singleLine = true,
                modifier = Modifier.testTag("friend_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                },
                modifier = Modifier.testTag("save_friend_name_button")
            ) {
                Text("Save Name")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
