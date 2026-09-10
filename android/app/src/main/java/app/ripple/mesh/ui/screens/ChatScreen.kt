package app.ripple.mesh.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ripple.mesh.R
import app.ripple.mesh.core.NodeId
import app.ripple.mesh.data.MessageEntity
import app.ripple.mesh.data.MessageStatus
import app.ripple.mesh.service.MeshService
import app.ripple.mesh.ui.MeshViewModel
import kotlinx.coroutines.flow.flowOf
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MeshViewModel, conversation: String, onBack: () -> Unit) {
    val isBroadcast = conversation == MeshService.BROADCAST_CONVERSATION
    val messages by remember(conversation) { vm.messages(conversation) }.collectAsStateWithLifecycle(emptyList())
    val peer by remember(conversation) { if (isBroadcast) flowOf(null) else vm.peer(conversation) }.collectAsStateWithLifecycle(null)
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    DisposableEffect(conversation) {
        vm.setVisibleConversation(conversation)
        onDispose { vm.setVisibleConversation(null) }
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
                title = {
                    Column {
                        Text(if (isBroadcast) stringResource(R.string.broadcast_channel) else peer?.name ?: NodeId.fromHex(conversation).display)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!isBroadcast) Icon(Icons.Default.Lock, contentDescription = null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                if (isBroadcast) stringResource(R.string.broadcast_subtitle) else stringResource(R.string.direct_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                stringResource(R.string.message_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(
                        enabled = draft.isNotBlank(),
                        onClick = {
                            vm.send(conversation, draft.trim())
                            draft = ""
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (draft.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send),
                            tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages, key = { it.messageId }) { MessageBubble(it, showSender = isBroadcast) }
        }
    }
}

@Composable
private fun MessageBubble(m: MessageEntity, showSender: Boolean) {
    val mine = m.outgoing
    val bg = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val sender = if (showSender && !mine) m.fromName ?: NodeId.fromHex(m.fromNodeId).display else null
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(m.timestamp))
    val statusWord = if (mine) when (m.status) {
        MessageStatus.PENDING -> stringResource(R.string.status_pending_short)
        MessageStatus.SENT -> stringResource(R.string.status_sent)
        MessageStatus.DELIVERED -> stringResource(R.string.status_delivered)
        MessageStatus.FAILED -> stringResource(R.string.status_failed)
        MessageStatus.RECEIVED -> null
        MessageStatus.READ -> stringResource(R.string.status_delivered)
    } else null
    val verification = if (!mine && !m.verified) stringResource(R.string.unverified) else null
    // Compute localized strings before entering the non-composable semantics lambda.
    val summary = listOfNotNull(sender, m.text, time, statusWord, verification).joinToString(", ")

    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 300.dp)
                .background(bg, RoundedCornerShape(16.dp, 16.dp, if (mine) 4.dp else 16.dp, if (mine) 16.dp else 4.dp))
                .clearAndSetSemantics { contentDescription = summary }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (showSender && !mine) {
                Text(m.fromName ?: NodeId.fromHex(m.fromNodeId).display, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(m.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(m.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
                if (mine) {
                    DeliveryStatusIcon(status = m.status)
                } else if (!m.verified) {
                    Text(stringResource(R.string.unverified), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DeliveryStatusIcon(status: MessageStatus, isRead: Boolean = false) {
    when (status) {
        MessageStatus.PENDING -> Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = stringResource(R.string.status_pending_icon),
            modifier = Modifier.size(14.dp),
            tint = colorResource(R.color.status_pending)
        )
        MessageStatus.SENT -> Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(R.string.status_sent_icon),
            modifier = Modifier.size(14.dp),
            tint = colorResource(R.color.status_sent)
        )
        MessageStatus.DELIVERED -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = stringResource(R.string.status_delivered_icon),
            modifier = Modifier.size(16.dp),
            tint = colorResource(R.color.status_delivered)
        )
        MessageStatus.READ -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = stringResource(R.string.status_read_icon),
            modifier = Modifier.size(16.dp),
            tint = colorResource(R.color.status_read)
        )
        MessageStatus.FAILED -> Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = stringResource(R.string.status_failed_icon),
            modifier = Modifier.size(14.dp),
            tint = colorResource(R.color.status_failed)
        )
        MessageStatus.RECEIVED -> { /* No status icon for incoming messages */ }
    }
}
