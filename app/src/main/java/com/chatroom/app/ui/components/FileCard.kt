package com.chatroom.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chatroom.app.R
import com.chatroom.app.data.model.Message
import com.chatroom.app.ui.theme.Blue100
import com.chatroom.app.ui.theme.Blue500

@Composable
fun FileCard(
    message: Message,
    onDownload: (Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Blue100),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // File icon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = stringResource(R.string.file_icon),
                tint = Blue500,
                modifier = Modifier.size(40.dp),
            )

            Spacer(Modifier.width(12.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.filename ?: stringResource(R.string.file_icon),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatFileSize(message.fileSize ?: 0),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Download button
            IconButton(
                onClick = { message.fileId?.let(onDownload) },
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.file_download),
                    tint = Blue500,
                )
            }
        }
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / 1024 / 1024)} MB"
    else -> "${"%.1f".format(bytes.toDouble() / 1024 / 1024 / 1024)} GB"
}
