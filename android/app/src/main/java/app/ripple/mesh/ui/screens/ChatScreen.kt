package app.ripple.mesh.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ripple.mesh.R
import app.ripple.mesh.core.NodeId
import app.ripple.mesh.data.MessageEntity
import app.ripple.mesh.data.MessageStatus
import app.ripple.mesh.data.PeerEntity
import app.ripple.mesh.service.MeshService
import app.ripple.mesh.ui.MeshViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MeshViewModel, conversation: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isBroadcast = conversation == MeshService.BROADCAST_CONVERSATION
    val messages by remember(conversation) { vm.messages(conversation) }.collectAsStateWithLifecycle(emptyList())
    val peer by remember(conversation) { if (isBroadcast) flowOf(null) else vm.peer(conversation) }.collectAsStateWithLifecycle(null)
    val knownPeers by vm.peers.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForAction by remember { mutableStateOf<MessageEntity?>(null) }
    var showDeleteDialogForMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showForwardSheetForMessage by remember { mutableStateOf<MessageEntity?>(null) }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(conversation) {
        vm.setVisibleConversation(conversation)
        onDispose { vm.setVisibleConversation(null) }
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Editing banner
                AnimatedVisibility(
                    visible = editingMessage != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    editingMessage?.let { editing ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.editing_message),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = editing.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingMessage = null
                                        draft = ""
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Chat Input Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    stringResource(R.string.message_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        IconButton(
                            enabled = draft.isNotBlank(),
                            onClick = {
                                val currentEditing = editingMessage
                                if (currentEditing != null) {
                                    vm.editMessage(currentEditing.messageId, draft.trim())
                                    editingMessage = null
                                    draft = ""
                                } else {
                                    vm.send(conversation, draft.trim())
                                    draft = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (draft.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(if (editingMessage != null) R.string.save else R.string.send),
                                tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.messageId }) { msg ->
                MessageBubble(
                    m = msg,
                    showSender = isBroadcast,
                    onLongClick = {
                        selectedMessageForAction = msg
                    }
                )
            }
        }
    }

    // Message Actions Bottom Sheet
    selectedMessageForAction?.let { msg ->
        MessageActionBottomSheet(
            message = msg,
            onDismiss = { selectedMessageForAction = null },
            onCopy = {
                clipboardManager.setText(AnnotatedString(msg.text))
                Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                selectedMessageForAction = null
            },
            onEdit = {
                editingMessage = msg
                draft = msg.text
                selectedMessageForAction = null
            },
            onForward = {
                showForwardSheetForMessage = msg
                selectedMessageForAction = null
            },
            onDelete = {
                showDeleteDialogForMessage = msg
                selectedMessageForAction = null
            }
        )
    }

    // Delete Confirmation / Option Dialog
    showDeleteDialogForMessage?.let { msg ->
        DeleteMessageDialog(
            message = msg,
            onDismiss = { showDeleteDialogForMessage = null },
            onDeleteForMe = {
                showDeleteDialogForMessage = null
                vm.deleteMessageForMe(msg.messageId)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.message_deleted),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.restoreMessage(msg)
                    }
                }
            },
            onDeleteForEveryone = {
                showDeleteDialogForMessage = null
                vm.deleteMessageForEveryone(msg.messageId)
            }
        )
    }

    // Redesigned Modern Forward Bottom Sheet
    showForwardSheetForMessage?.let { msg ->
        ForwardBottomSheet(
            message = msg,
            peers = knownPeers,
            currentConversation = conversation,
            onDismiss = { showForwardSheetForMessage = null },
            onForwardTo = { targetConversation ->
                vm.forwardMessage(targetConversation, msg.text)
                showForwardSheetForMessage = null
                Toast.makeText(context, context.getString(R.string.message_forwarded), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    m: MessageEntity,
    showSender: Boolean,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val mine = m.outgoing
    val isDeleted = m.deletedForEveryone
    val bg = when {
        isDeleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        mine -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isDeleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        mine -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sender = if (showSender && !mine) m.fromName ?: NodeId.fromHex(m.fromNodeId).display else null
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(m.timestamp))
    val statusWord = if (mine && !isDeleted) when (m.status) {
        MessageStatus.PENDING -> stringResource(R.string.status_pending_short)
        MessageStatus.SENT -> stringResource(R.string.status_sent)
        MessageStatus.DELIVERED -> stringResource(R.string.status_delivered)
        MessageStatus.FAILED -> stringResource(R.string.status_failed)
        MessageStatus.RECEIVED -> null
        MessageStatus.READ -> stringResource(R.string.status_delivered)
    } else null
    val verification = if (!mine && !m.verified && !isDeleted) stringResource(R.string.unverified) else null
    val summary = listOfNotNull(sender, if (isDeleted) stringResource(R.string.deleted_message_placeholder) else m.text, time, statusWord, verification).joinToString(", ")

    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp, 16.dp, if (mine) 4.dp else 16.dp, if (mine) 16.dp else 4.dp))
                .background(bg)
                .combinedClickable(
                    onClick = { /* normal click */ },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
                .clearAndSetSemantics { contentDescription = summary }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (showSender && !mine && !isDeleted) {
                Text(
                    m.fromName ?: NodeId.fromHex(m.fromNodeId).display,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (m.isForwarded && !isDeleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Forward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.forwarded_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }

            if (isDeleted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = textColor
                    )
                    Text(
                        text = stringResource(R.string.deleted_message_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = textColor
                    )
                }
            } else {
                Text(m.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (m.isEdited && !isDeleted) {
                    Text(
                        text = stringResource(R.string.edited_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Text("·", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
                }

                Text(
                    time,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )

                if (mine && !isDeleted) {
                    DeliveryStatusIcon(status = m.status)
                } else if (!mine && !m.verified && !isDeleted) {
                    Text(
                        stringResource(R.string.unverified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionBottomSheet(
    message: MessageEntity,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Preview
            Text(
                text = if (message.deletedForEveryone) stringResource(R.string.deleted_message_placeholder) else message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            // Action: Copy (if not deleted)
            if (!message.deletedForEveryone) {
                ActionItem(
                    icon = Icons.Default.ContentCopy,
                    text = stringResource(R.string.action_copy),
                    onClick = onCopy
                )
            }

            // Action: Edit (if outgoing and not deleted)
            if (message.outgoing && !message.deletedForEveryone) {
                ActionItem(
                    icon = Icons.Default.Edit,
                    text = stringResource(R.string.action_edit),
                    onClick = onEdit
                )
            }

            // Action: Forward (if not deleted)
            if (!message.deletedForEveryone) {
                ActionItem(
                    icon = Icons.AutoMirrored.Filled.Forward,
                    text = stringResource(R.string.action_forward),
                    onClick = onForward
                )
            }

            // Action: Delete
            ActionItem(
                icon = Icons.Default.Delete,
                text = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
private fun DeleteMessageDialog(
    message: MessageEntity,
    onDismiss: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
) {
    val canDeleteForEveryone = message.outgoing && !message.deletedForEveryone

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_message_title)) },
        text = {
            if (canDeleteForEveryone) {
                Text(stringResource(R.string.delete_for_everyone_prompt))
            } else {
                Text(stringResource(R.string.delete_for_me))
            }
        },
        confirmButton = {
            if (canDeleteForEveryone) {
                TextButton(
                    onClick = onDeleteForEveryone,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete_for_everyone))
                }
            } else {
                TextButton(
                    onClick = onDeleteForMe,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete_for_me))
                }
            }
        },
        dismissButton = {
            if (canDeleteForEveryone) {
                TextButton(onClick = onDeleteForMe) {
                    Text(stringResource(R.string.delete_for_me))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

/**
 * Modern, production-grade Forward Picker Bottom Sheet.
 * Includes search filtering, quoted message preview card, peer avatars, and one-tap forwarding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForwardBottomSheet(
    message: MessageEntity,
    peers: List<PeerEntity>,
    currentConversation: String,
    onDismiss: () -> Unit,
    onForwardTo: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredPeers = remember(peers, currentConversation, searchQuery) {
        peers
            .filter { it.nodeId != currentConversation }
            .filter {
                if (searchQuery.isBlank()) true
                else it.name.contains(searchQuery, ignoreCase = true) ||
                     it.nodeId.contains(searchQuery, ignoreCase = true)
            }
    }

    val showBroadcast = currentConversation != MeshService.BROADCAST_CONVERSATION &&
            (searchQuery.isBlank() || "Everyone nearby broadcast public".contains(searchQuery, ignoreCase = true))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.forward_message),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Quoted Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        stringResource(R.string.forward_to) + "...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            )

            Spacer(Modifier.height(12.dp))

            // Destination List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Public Broadcast Channel
                if (showBroadcast) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onForwardTo(MeshService.BROADCAST_CONVERSATION) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.broadcast_channel),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.broadcast_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.send),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Peers List
                items(filteredPeers, key = { it.nodeId }) { peer ->
                    val displayName = peer.name.ifBlank { NodeId.fromHex(peer.nodeId).display }
                    val initial = displayName.firstOrNull()?.uppercase() ?: "?"

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onForwardTo(peer.nodeId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = NodeId.fromHex(peer.nodeId).display,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // Hop chip
                            SuggestionChip(
                                onClick = { onForwardTo(peer.nodeId) },
                                label = {
                                    Text(
                                        if (peer.hops <= 1) "Direct" else "${peer.hops} hops",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null,
                                modifier = Modifier.height(26.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.send),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (filteredPeers.isEmpty() && !showBroadcast) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No peers matching \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
