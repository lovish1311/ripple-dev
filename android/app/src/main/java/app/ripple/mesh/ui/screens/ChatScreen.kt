package app.ripple.mesh.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    val sosBeacons by vm.sosBeacons.collectAsStateWithLifecycle()

    val isRecording by vm.isRecording.collectAsStateWithLifecycle()
    val recordingAmplitude by vm.recordingAmplitude.collectAsStateWithLifecycle()
    val recordingDurationMs by vm.recordingDurationMs.collectAsStateWithLifecycle()
    val playbackState by vm.playbackState.collectAsStateWithLifecycle()

    var isHandsFreeLocked by remember { mutableStateOf(false) }
    var isSlideCancelling by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            isHandsFreeLocked = false
            isSlideCancelling = false
        }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vm.startRecording { res ->
                vm.sendVoiceMessage(conversation, res.file, res.durationMs)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.audio_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    var draft by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isBroadcast) {
                            Avatar(nodeIdHex = conversation, avatar = peer?.avatar, name = peer?.name, size = 38.dp)
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                if (isBroadcast) stringResource(R.string.broadcast_channel) else peer?.name ?: NodeId.fromHex(conversation).display,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!isBroadcast) Icon(Icons.Default.Lock, contentDescription = null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (isBroadcast) stringResource(R.string.broadcast_subtitle) else stringResource(R.string.direct_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                // Replying Preview Banner
                AnimatedVisibility(
                    visible = replyingToMessage != null && editingMessage == null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    replyingToMessage?.let { replying ->
                        ReplyPreviewBanner(
                            replyingTo = replying,
                            onCancel = { replyingToMessage = null }
                        )
                    }
                }

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

                // Chat Input Bar / Voice Recording Capsule
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRecording) {
                        VoiceRecordingHud(
                            durationMs = recordingDurationMs,
                            amplitude = recordingAmplitude,
                            isSlideCancelling = isSlideCancelling,
                            onCancel = {
                                vm.cancelRecording()
                                isHandsFreeLocked = false
                                isSlideCancelling = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
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
                    }

                    if (draft.isNotBlank() || editingMessage != null) {
                        IconButton(
                            onClick = {
                                val currentEditing = editingMessage
                                val currentReplying = replyingToMessage
                                if (currentEditing != null) {
                                    vm.editMessage(currentEditing.messageId, draft.trim())
                                    editingMessage = null
                                    draft = ""
                                } else {
                                    vm.send(conversation, draft.trim(), replyTo = currentReplying)
                                    replyingToMessage = null
                                    draft = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(if (editingMessage != null) R.string.save else R.string.send),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (isRecording && isHandsFreeLocked) {
                        // Hands-free locked mode: tap Send to stop recording & send!
                        IconButton(
                            onClick = {
                                isHandsFreeLocked = false
                                scope.launch {
                                    val res = vm.stopRecording()
                                    if (res != null) {
                                        vm.sendVoiceMessage(conversation, res.file, res.durationMs)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.send),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Hold-to-record (or tap for hands-free) mic button
                        val buttonScale by animateFloatAsState(
                            targetValue = if (isRecording) 1.2f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "micScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = buttonScale
                                    scaleY = buttonScale
                                }
                                .background(
                                    if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .pointerInput(context) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val startX = down.position.x
                                            val startTime = System.currentTimeMillis()

                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                continue
                                            }

                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val started = vm.startRecording { res ->
                                                vm.sendVoiceMessage(conversation, res.file, res.durationMs)
                                            }
                                            if (!started) continue

                                            var isCancelled = false
                                            val pointerId = down.id

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == pointerId }
                                                if (change == null || !change.pressed) {
                                                    break
                                                }
                                                val diffX = change.position.x - startX
                                                if (diffX < -150f) {
                                                    if (!isSlideCancelling) {
                                                        isSlideCancelling = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    isCancelled = true
                                                } else if (diffX > -60f && isSlideCancelling) {
                                                    isSlideCancelling = false
                                                    isCancelled = false
                                                }
                                            }

                                            val holdDuration = System.currentTimeMillis() - startTime
                                            if (isCancelled) {
                                                vm.cancelRecording()
                                                isSlideCancelling = false
                                            } else if (holdDuration < 450L) {
                                                // Quick tap -> lock into hands-free mode!
                                                isHandsFreeLocked = true
                                            } else {
                                                // Held and released -> stop and send!
                                                isHandsFreeLocked = false
                                                scope.launch {
                                                    val res = vm.stopRecording()
                                                    if (res != null) {
                                                        vm.sendVoiceMessage(conversation, res.file, res.durationMs)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = stringResource(R.string.voice_note_label),
                                tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
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
                val associatedBeacon = sosBeacons.firstOrNull { it.messageId == msg.messageId }
                SwipeToReplyContainer(
                    isOutgoing = msg.outgoing,
                    enabled = !msg.deletedForEveryone,
                    onReply = {
                        replyingToMessage = msg
                        editingMessage = null
                    }
                ) {
                    if (associatedBeacon != null || msg.text.startsWith("🚨 SOS") || msg.text.contains("EMERGENCY SOS")) {
                        SosEmergencyMessageCard(
                            message = msg,
                            beacon = associatedBeacon,
                            playbackState = playbackState,
                            onPlayVoice = { id, path -> vm.playVoice(id, path) },
                            onPauseVoice = { vm.pauseVoice() },
                            onLongClick = {
                                selectedMessageForAction = msg
                            }
                        )
                    } else {
                        MessageBubble(
                            m = msg,
                            showSender = isBroadcast,
                            playbackState = playbackState,
                            onPlayVoice = { id, path -> vm.playVoice(id, path) },
                            onPauseVoice = { vm.pauseVoice() },
                            onQuoteClick = { targetMsgId ->
                                val targetIdx = messages.indexOfFirst { it.messageId == targetMsgId }
                                if (targetIdx >= 0) {
                                    scope.launch { listState.animateScrollToItem(targetIdx) }
                                }
                            },
                            onLongClick = {
                                selectedMessageForAction = msg
                            }
                        )
                    }
                }
            }
        }
    }

    // Message Actions Bottom Sheet
    selectedMessageForAction?.let { msg ->
        MessageActionBottomSheet(
            message = msg,
            onDismiss = { selectedMessageForAction = null },
            onReply = {
                replyingToMessage = msg
                editingMessage = null
                selectedMessageForAction = null
            },
            onCopy = {
                clipboardManager.setText(AnnotatedString(msg.text))
                Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                selectedMessageForAction = null
            },
            onEdit = {
                editingMessage = msg
                replyingToMessage = null
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

@Composable
private fun ReplyPreviewBanner(
    replyingTo: MessageEntity,
    onCancel: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                val senderName = if (replyingTo.outgoing) {
                    stringResource(R.string.you)
                } else {
                    replyingTo.fromName ?: NodeId.fromHex(replyingTo.fromNodeId).display
                }
                Text(
                    text = stringResource(R.string.replying_to, senderName),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = replyingTo.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel_reply),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun QuotedMessagePreview(
    replySender: String?,
    replyText: String?,
    isMine: Boolean,
    onClick: () -> Unit
) {
    if (replyText == null) return
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isMine) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = replySender ?: stringResource(R.string.message_hint),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = replyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SwipeToReplyContainer(
    isOutgoing: Boolean,
    enabled: Boolean = true,
    onReply: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val maxDragPx = with(density) { 80.dp.toPx() }
    val triggerThresholdPx = with(density) { 54.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(isOutgoing) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        hasTriggeredHaptic = false
                    },
                    onDragEnd = {
                        val currentOffset = offsetX.value
                        val isTriggered = if (isOutgoing) {
                            currentOffset <= -triggerThresholdPx
                        } else {
                            currentOffset >= triggerThresholdPx
                        }
                        if (isTriggered) {
                            onReply()
                        }
                        coroutineScope.launch {
                            offsetX.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = if (isOutgoing) {
                            (offsetX.value + dragAmount).coerceIn(-maxDragPx, 0f)
                        } else {
                            (offsetX.value + dragAmount).coerceIn(0f, maxDragPx)
                        }
                        coroutineScope.launch {
                            offsetX.snapTo(newOffset)
                        }

                        val progress = kotlin.math.abs(newOffset)
                        if (progress >= triggerThresholdPx && !hasTriggeredHaptic) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            hasTriggeredHaptic = true
                        } else if (progress < triggerThresholdPx && hasTriggeredHaptic) {
                            hasTriggeredHaptic = false
                        }
                    }
                )
            }
    ) {
        val progress = (kotlin.math.abs(offsetX.value) / triggerThresholdPx).coerceIn(0f, 1f)
        if (progress > 0.05f) {
            val scale = 0.6f + 0.4f * progress
            val alpha = (progress * 1.2f).coerceIn(0f, 1f)
            val isTriggered = kotlin.math.abs(offsetX.value) >= triggerThresholdPx

            Box(
                modifier = Modifier
                    .align(if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 16.dp)
                    .size(36.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(
                        if (isTriggered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = stringResource(R.string.action_reply),
                    tint = if (isTriggered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offsetX.value
                }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    m: MessageEntity,
    showSender: Boolean,
    playbackState: app.ripple.mesh.audio.OpusPlayer.PlaybackState? = null,
    onPlayVoice: ((String, String) -> Unit)? = null,
    onPauseVoice: (() -> Unit)? = null,
    onQuoteClick: (String) -> Unit = {},
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

            if (m.replyToText != null && !isDeleted) {
                QuotedMessagePreview(
                    replySender = m.replyToSender,
                    replyText = m.replyToText,
                    isMine = mine,
                    onClick = {
                        m.replyToMessageId?.let { onQuoteClick(it) }
                    }
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
            } else if (m.voicePath != null && playbackState != null && onPlayVoice != null && onPauseVoice != null) {
                VoiceMessagePlayer(
                    messageId = m.messageId,
                    voicePath = m.voicePath!!,
                    durationMs = m.voiceDurationMs ?: 5000,
                    playbackState = playbackState,
                    onPlay = onPlayVoice,
                    onPause = onPauseVoice,
                    isMine = mine
                )
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
    onReply: () -> Unit,
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

            // Action: Reply (if not deleted)
            if (!message.deletedForEveryone) {
                ActionItem(
                    icon = Icons.AutoMirrored.Filled.Reply,
                    text = stringResource(R.string.action_reply),
                    onClick = onReply
                )
            }

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
                            Avatar(nodeIdHex = peer.nodeId, avatar = peer.avatar, name = displayName, size = 44.dp)

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

/**
 * Dedicated High-Priority Emergency Distress Message Card (ChatScreen).
 * Replaces plain chat bubbles for SOS emergency broadcasts with a prominent alert design,
 * displaying sender information, verified cryptographic signature tag, distress text,
 * attached GPS coordinates, accuracy, and direct 1-tap offline map launch action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SosEmergencyMessageCard(
    message: MessageEntity,
    beacon: app.ripple.mesh.data.SosBeaconEntity?,
    playbackState: app.ripple.mesh.audio.OpusPlayer.PlaybackState? = null,
    onPlayVoice: ((String, String) -> Unit)? = null,
    onPauseVoice: (() -> Unit)? = null,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val lat = beacon?.latE7?.let { it / 1e7 }
    val lng = beacon?.lngE7?.let { it / 1e7 }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Emergency Alert Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Distress",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        "🚨 EMERGENCY SOS BEACON",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (message.verified) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "VERIFIED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Sender Information
            Text(
                "From: ${message.fromName ?: NodeId.fromHex(message.fromNodeId).display}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Distress message text
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Attached Emergency Voice Memo (5s Opus 12kbps)
            val voicePath = beacon?.voicePath ?: message.voicePath
            val voiceDuration = beacon?.voiceDurationMs ?: message.voiceDurationMs ?: 5000
            if (voicePath != null && playbackState != null && onPlayVoice != null && onPauseVoice != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isPlaying = playbackState.messageId == message.messageId && playbackState.isPlaying
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                .clickable {
                                    if (isPlaying) onPauseVoice() else onPlayVoice(message.messageId, voicePath)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play Emergency Voice Memo",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.emergency_voice_memo),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "${(voiceDuration + 500) / 1000}s · Opus 12kbps emergency audio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Attached GPS coordinates & Map Action
            if (lat != null && lng != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("📍 GPS Location", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                String.format("%.4f° N, %.4f° W (±%dm)", lat, -lng, beacon.accuracyMeters ?: 15),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(SOS+Emergency)")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Open Map", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Footer: Timestamp & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (message.outgoing) {
                    Spacer(Modifier.width(4.dp))
                    DeliveryStatusIcon(message.status)
                }
            }
        }
    }
}

/**
 * Live HUD displayed in the message input bar while an emergency/chat voice note is being recorded.
 * Shows pulsing red indicator, live timer, dynamic amplitude waveform, slide-to-cancel cue, and trash cancel action.
 */
@Composable
private fun VoiceRecordingHud(
    durationMs: Long,
    amplitude: Float,
    isSlideCancelling: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (isSlideCancelling) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cancel_recording),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isSlideCancelling) {
                Text(
                    text = stringResource(R.string.release_to_cancel),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.Red.copy(alpha = pulseAlpha), CircleShape)
                )

                val sec = (durationMs / 1000).coerceAtMost(5)
                Text(
                    text = "0:0$sec / 0:05",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val clampedAmp = amplitude.coerceIn(0.1f, 1.0f)
                    val barHeights = listOf(
                        0.3f * clampedAmp + 0.15f,
                        0.7f * clampedAmp + 0.2f,
                        1.0f * clampedAmp + 0.25f,
                        0.6f * clampedAmp + 0.2f,
                        0.4f * clampedAmp + 0.15f
                    )
                    barHeights.forEach { fraction ->
                        val animHeight by animateDpAsState(
                            targetValue = (fraction * 22).dp.coerceIn(4.dp, 24.dp),
                            label = "waveBar"
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(animHeight)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Inline Voice Note Player within chat bubbles.
 * Features Play/Pause, pseudo-waveform progress, duration indicator, and OPUS 12kbps badge.
 */
@Composable
private fun VoiceMessagePlayer(
    messageId: String,
    voicePath: String,
    durationMs: Int,
    playbackState: app.ripple.mesh.audio.OpusPlayer.PlaybackState,
    onPlay: (String, String) -> Unit,
    onPause: () -> Unit,
    isMine: Boolean
) {
    val isPlayingThis = playbackState.messageId == messageId && playbackState.isPlaying
    val progress = if (playbackState.messageId == messageId) playbackState.progress else 0f
    val durationSec = (durationMs + 500) / 1000

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                    .clickable {
                        if (isPlayingThis) onPause() else onPlay(messageId, voicePath)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlayingThis) "Pause" else "Play",
                    tint = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val staticHeights = listOf(
                        0.3f, 0.6f, 0.9f, 0.5f, 0.7f, 1.0f, 0.8f, 0.4f,
                        0.6f, 0.9f, 0.7f, 0.5f, 0.8f, 0.3f, 0.6f, 0.4f
                    )
                    staticHeights.forEachIndexed { index, h ->
                        val barFraction = (index + 1).toFloat() / staticHeights.size
                        val isActive = barFraction <= progress || (isPlayingThis && barFraction <= progress + 0.05f)
                        val activeColor = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        val inactiveColor = activeColor.copy(alpha = 0.3f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((h * 16).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isActive) activeColor else inactiveColor)
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlayingThis) {
                            val curSec = (durationSec * progress).toInt()
                            String.format("0:%02d / 0:%02d", curSec, durationSec)
                        } else {
                            String.format("0:%02d", durationSec)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = (if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "OPUS 12kbps",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}


