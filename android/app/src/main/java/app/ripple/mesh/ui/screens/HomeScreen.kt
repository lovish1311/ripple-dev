package app.ripple.mesh.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ripple.mesh.core.AvatarHelper
import app.ripple.mesh.R
import app.ripple.mesh.core.NodeId
import app.ripple.mesh.data.PeerEntity
import app.ripple.mesh.service.MeshService
import app.ripple.mesh.ui.MeshViewModel
import java.text.DateFormat
import java.util.Date

import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.core.content.ContextCompat
import java.io.File
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MeshViewModel, onOpenChat: (String) -> Unit, onOpenSettings: () -> Unit) {
    val status by vm.status.collectAsStateWithLifecycle()
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val peers by vm.peers.collectAsStateWithLifecycle()
    val activeSosBeacons by vm.activeSosBeacons.collectAsStateWithLifecycle()
    val allSosBeacons by vm.allSosBeacons.collectAsStateWithLifecycle()
    val playbackState by vm.playbackState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showSosSheet by remember { mutableStateOf(false) }
    var showSosHubSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(2.dp))
                        MeshStatusPill(status = status)
                    }
                },
                actions = {
                    // Emergency Hub Action Button with active badge
                    if (allSosBeacons.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                if (activeSosBeacons.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text(activeSosBeacons.size.toString())
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = { showSosHubSheet = true }) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Emergency SOS Hub",
                                    tint = if (activeSosBeacons.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSosSheet = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 10.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.sos_fab_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Color.Transparent,
                    indicator = {},
                    divider = {}
                ) {
                    val totalChatsUnread = remember(conversations) { conversations.sumOf { it.unread } }
                    val totalPeersUnread = remember(conversations) { conversations.filter { it.conversation != MeshService.BROADCAST_CONVERSATION }.sumOf { it.unread } }
                    Tab(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.tab_chats), style = MaterialTheme.typography.titleSmall)
                                if (totalChatsUnread > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("$totalChatsUnread") }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.tab_peers, peers.size), style = MaterialTheme.typography.titleSmall)
                                if (totalPeersUnread > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("$totalPeersUnread") }
                                }
                            }
                        }
                    )
                }
            }

            if (tab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1 Master Alert Bar / Compact Top Banner (Occupies max 1 item height)
                    if (activeSosBeacons.size == 1) {
                        val beacon = activeSosBeacons.first()
                        val peer = peers.firstOrNull { it.nodeId == beacon.fromNodeId }
                        item(key = "single_sos_${beacon.messageId}") {
                            SosEmergencyBanner(
                                beacon = beacon,
                                peer = peer,
                                playbackState = playbackState,
                                onPlayVoice = { id, path -> vm.playVoice(id, path) },
                                onPauseVoice = { vm.pauseVoice() },
                                onOpenChat = {
                                    vm.acknowledgeSosBeacon(beacon.messageId)
                                    onOpenChat(MeshService.BROADCAST_CONVERSATION)
                                },
                                onAcknowledge = { vm.acknowledgeSosBeacon(beacon.messageId) },
                                onDismiss = { vm.acknowledgeSosBeacon(beacon.messageId) }
                            )
                        }
                    } else if (activeSosBeacons.size > 1) {
                        item(key = "master_sos_banner") {
                            SosMasterAlertBanner(
                                beacons = activeSosBeacons,
                                peers = peers,
                                onReviewAll = { showSosHubSheet = true },
                                onAcknowledgeAll = { vm.acknowledgeAllSosBeacons() },
                                onOpenBroadcastChat = { onOpenChat(MeshService.BROADCAST_CONVERSATION) }
                            )
                        }
                    }

                    val broadcast = conversations.firstOrNull { it.conversation == MeshService.BROADCAST_CONVERSATION }
                    item {
                        Card(
                            onClick = { onOpenChat(MeshService.BROADCAST_CONVERSATION) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.broadcast_channel), style = MaterialTheme.typography.titleMedium) },
                                supportingContent = { Text(broadcast?.lastText ?: stringResource(R.string.broadcast_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    Box(
                                        Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                trailingContent = { if ((broadcast?.unread ?: 0) > 0) UnreadBadge(broadcast!!.unread) },
                                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                    items(conversations.filter { it.conversation != MeshService.BROADCAST_CONVERSATION }, key = { it.conversation }) { c ->
                        val peer = peers.firstOrNull { it.nodeId == c.conversation }
                        Card(
                            onClick = { onOpenChat(c.conversation) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = { Text(peer?.name ?: NodeId.fromHex(c.conversation).display, style = MaterialTheme.typography.titleMedium) },
                                supportingContent = { Text(c.lastText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { Avatar(nodeIdHex = c.conversation, avatar = peer?.avatar, name = peer?.name) },
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(c.lastTimestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (c.unread > 0) UnreadBadge(c.unread)
                                    }
                                },
                                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            } else {
                if (peers.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.padding(16.dp).fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(24.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.peers_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            TextButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.peers_empty_cta)) }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(peers, key = { it.nodeId }) { peer ->
                            val unreadCount = conversations.firstOrNull { it.conversation == peer.nodeId }?.unread ?: 0
                            PeerRow(peer = peer, unreadCount = unreadCount) { onOpenChat(peer.nodeId) }
                        }
                    }
                }
            }
        }
    }

    if (showSosSheet) {
        SosBottomSheet(
            vm = vm,
            onDismiss = { showSosSheet = false }
        )
    }

    if (showSosHubSheet) {
        SosEmergencyHubSheet(
            vm = vm,
            activeBeacons = activeSosBeacons,
            allBeacons = allSosBeacons,
            peers = peers,
            playbackState = playbackState,
            onPlayVoice = { id, path -> vm.playVoice(id, path) },
            onPauseVoice = { vm.pauseVoice() },
            onOpenChat = { conv ->
                showSosHubSheet = false
                onOpenChat(conv)
            },
            onDismiss = { showSosHubSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosBottomSheet(
    vm: MeshViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var shareLocation by remember { mutableStateOf(false) }

    var voiceMemoFile by remember { mutableStateOf<File?>(null) }
    var voiceMemoDurationMs by remember { mutableIntStateOf(0) }

    val isRecording by vm.isRecording.collectAsStateWithLifecycle()
    val recordingAmplitude by vm.recordingAmplitude.collectAsStateWithLifecycle()
    val recordingDurationMs by vm.recordingDurationMs.collectAsStateWithLifecycle()
    val playbackState by vm.playbackState.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isHandsFreeLocked by remember { mutableStateOf(false) }

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            isHandsFreeLocked = false
        }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vm.startRecording { res ->
                voiceMemoFile = res.file
                voiceMemoDurationMs = res.durationMs
            }
        } else {
            android.widget.Toast.makeText(context, context.getString(R.string.audio_permission_required), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.stopVoice()
            if (vm.isRecording.value) vm.cancelRecording()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.sos_beacon_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            }

            // Warning Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.sos_beacon_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Message TextField
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.sos_message_label)) },
                placeholder = { Text("Describe emergency / need...") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Emergency Voice Memo Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.emergency_voice_memo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.emergency_voice_memo_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (voiceMemoFile == null) {
                    val infiniteTransition = rememberInfiniteTransition(label = "sos_sheet_rec")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "sheetPulse"
                    )

                    if (isRecording && isHandsFreeLocked) {
                        // Hands-free recording HUD
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.Red.copy(alpha = pulseAlpha), CircleShape)
                                )

                                val sec = (recordingDurationMs / 1000).coerceAtMost(5)
                                Text(
                                    text = "0:0$sec / 0:05",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val clampedAmp = recordingAmplitude.coerceIn(0.1f, 1.0f)
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
                                            label = "sheetWave"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(animHeight)
                                                .clip(RoundedCornerShape(1.5.dp))
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        vm.cancelRecording()
                                        isHandsFreeLocked = false
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cancel_recording),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        isHandsFreeLocked = false
                                        scope.launch {
                                            val res = vm.stopRecording()
                                            if (res != null) {
                                                voiceMemoFile = res.file
                                                voiceMemoDurationMs = res.durationMs
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.finish_memo), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Hold-to-record button (or tap for hands-free)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .pointerInput(context) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val startTime = System.currentTimeMillis()

                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                continue
                                            }

                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val started = vm.startRecording { res ->
                                                voiceMemoFile = res.file
                                                voiceMemoDurationMs = res.durationMs
                                            }
                                            if (!started) continue

                                            val pointerId = down.id
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == pointerId }
                                                if (change == null || !change.pressed) {
                                                    break
                                                }
                                            }

                                            val holdDuration = System.currentTimeMillis() - startTime
                                            if (holdDuration < 450L) {
                                                // Quick tap -> switch to hands-free locked mode!
                                                isHandsFreeLocked = true
                                            } else {
                                                // Held and released -> stop and attach memo!
                                                isHandsFreeLocked = false
                                                scope.launch {
                                                    val res = vm.stopRecording()
                                                    if (res != null) {
                                                        voiceMemoFile = res.file
                                                        voiceMemoDurationMs = res.durationMs
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRecording) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.White.copy(alpha = pulseAlpha), CircleShape)
                                    )
                                    val sec = (recordingDurationMs / 1000).coerceAtMost(5)
                                    Text(
                                        stringResource(R.string.recording_memo_hold) + " (0:0$sec)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Text(
                                        stringResource(R.string.hold_to_record_memo),
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isPlaying = playbackState.messageId == "sos_sheet_preview" && playbackState.isPlaying
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                    .clickable {
                                        if (isPlaying) vm.pauseVoice() else vm.playVoice("sos_sheet_preview", voiceMemoFile!!.absolutePath)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Attached Voice Memo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                Text("${(voiceMemoDurationMs + 500) / 1000}s · Opus 8kbps · Ready", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(
                                onClick = {
                                    vm.stopVoice()
                                    voiceMemoFile?.delete()
                                    voiceMemoFile = null
                                    voiceMemoDurationMs = 0
                                }
                            ) {
                                Text(stringResource(R.string.re_record), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Opt-in GPS Section
            Text(
                text = stringResource(R.string.sos_opt_in_gps),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.sos_include_location),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.sos_location_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = shareLocation,
                    onCheckedChange = { shareLocation = it }
                )
            }

            if (!shareLocation) {
                Text(
                    text = stringResource(R.string.sos_no_location_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }

            // Transmit SOS Beacon Button
            Button(
                onClick = {
                    vm.sendSos(message.trim(), shareLocation, voiceMemoFile, voiceMemoDurationMs)
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.sos_sent_toast),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.sos_send),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MeshStatusPill(status: app.ripple.mesh.service.MeshStatus) {
    val (dotColor, text) = when {
        !status.running -> Color(0xFFF39C12) to stringResource(R.string.status_starting)
        !status.bluetoothOn -> Color(0xFFE74C3C) to stringResource(R.string.status_bt_off)
        else -> Color(0xFF2ECC71) to pluralStringResource(R.plurals.status_links, status.directLinks, status.directLinks, status.knownPeers)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = dotColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PeerRow(peer: PeerEntity, unreadCount: Int = 0, onClick: () -> Unit) {
    val recent = System.currentTimeMillis() - peer.lastSeen < 5 * 60_000
    val dotLabel = stringResource(if (recent) R.string.peer_online_desc else R.string.peer_offline_desc)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(peer.name, style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(NodeId.fromHex(peer.nodeId).display, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                    Text("· " + pluralStringResource(R.plurals.hops, peer.hops, peer.hops), style = MaterialTheme.typography.labelSmall)
                }
            },
            leadingContent = { Avatar(nodeIdHex = peer.nodeId, avatar = peer.avatar, name = peer.name) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (unreadCount > 0) {
                        UnreadBadge(unreadCount)
                    }
                    Box(
                        Modifier.size(10.dp)
                            .background(if (recent) Color(0xFF2ECC71) else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .semantics { contentDescription = dotLabel },
                    )
                }
            },
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    // Resolve the plural in composable scope before entering the semantics lambda.
    val unreadLabel = pluralStringResource(R.plurals.unread_badge, count, count)
    Badge(Modifier.clearAndSetSemantics { contentDescription = unreadLabel }) { Text("$count") }
}

/** Deterministic colored avatar with rounded squircle, dynamic emoji display, and fallback initials. */
@Composable
fun Avatar(
    nodeIdHex: String,
    avatar: String? = null,
    name: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val (extractedAvatar, _) = if (avatar.isNullOrBlank() && !name.isNullOrBlank()) {
        AvatarHelper.extractAvatarAndName(name)
    } else {
        Pair(avatar, name.orEmpty())
    }

    val displayAvatar = extractedAvatar?.takeIf { it.isNotBlank() }
    val hue = (nodeIdHex.take(6).toLongOrNull(16) ?: (nodeIdHex.hashCode().toLong() and 0xFFFFFFL)) % 360
    val baseColor = Color.hsv(hue.toFloat(), 0.50f, 0.70f)
    val cornerRadius = (size.value * 0.28f).dp

    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        baseColor.copy(alpha = if (displayAvatar != null) 0.22f else 0.85f),
                        baseColor.copy(alpha = if (displayAvatar != null) 0.10f else 0.60f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.5f),
                        baseColor.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (displayAvatar != null) {
            Text(
                text = displayAvatar,
                fontSize = (size.value * 0.52f).sp,
                textAlign = TextAlign.Center
            )
        } else {
            val initials = if (!name.isNullOrBlank()) {
                name.trim().take(2).uppercase()
            } else {
                nodeIdHex.takeLast(2).uppercase()
            }
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Pinned Top-Priority Active Emergency SOS Banner (Home / Inbox Screen - Single Alert Mode).
 * Displays when exactly 1 active distress beacon exists.
 */
@Composable
fun SosEmergencyBanner(
    beacon: app.ripple.mesh.data.SosBeaconEntity,
    peer: PeerEntity?,
    playbackState: app.ripple.mesh.audio.OpusPlayer.PlaybackState? = null,
    onPlayVoice: ((String, String) -> Unit)? = null,
    onPauseVoice: (() -> Unit)? = null,
    onOpenChat: () -> Unit,
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lat = beacon.latE7?.let { it / 1e7 }
    val lng = beacon.lngE7?.let { it / 1e7 }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Emergency Alert Tag, Time & Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        "🚨 ACTIVE EMERGENCY SOS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(beacon.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sender Information Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Avatar(nodeIdHex = beacon.fromNodeId, avatar = peer?.avatar, name = beacon.fromName ?: peer?.name)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        peer?.name ?: beacon.fromName ?: NodeId.fromHex(beacon.fromNodeId).display,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if ((peer?.hops ?: 1) <= 1) "Direct BLE Link (1 hop)" else "${peer?.hops ?: 2} hops away in mesh",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Distress Message Text
            if (beacon.text.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = beacon.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Emergency Voice Memo player
            if (beacon.voicePath != null && playbackState != null && onPlayVoice != null && onPauseVoice != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isPlaying = playbackState.messageId == beacon.messageId && playbackState.isPlaying
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                .clickable {
                                    if (isPlaying) onPauseVoice() else onPlayVoice(beacon.messageId, beacon.voicePath!!)
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
                                "🚨 Emergency Voice Memo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "${(beacon.voiceDurationMs ?: 5000) / 1000}s · Opus 12kbps emergency audio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Location Coordinates info if present
            if (lat != null && lng != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text("📍", fontSize = 14.sp)
                    Text(
                        String.format("%.4f° N, %.4f° W (±%dm)", lat, -lng, beacon.accuracyMeters ?: 15),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action Buttons Row: Open Chat, Open Map & Acknowledge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenChat,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open Chat", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }

                if (lat != null && lng != null) {
                    OutlinedButton(
                        onClick = {
                            val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(SOS+Emergency)")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Map", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedButton(
                    onClick = onAcknowledge,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Text("Acknowledge", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Option 1 Master Alert Bar (Multi-Alert Aggregator).
 * Takes up exactly 1 item height on HomeScreen when multiple active emergencies are active.
 */
@Composable
fun SosMasterAlertBanner(
    beacons: List<app.ripple.mesh.data.SosBeaconEntity>,
    peers: List<PeerEntity>,
    onReviewAll: () -> Unit,
    onAcknowledgeAll: () -> Unit,
    onOpenBroadcastChat: () -> Unit
) {
    val latest = beacons.firstOrNull() ?: return
    val latestPeer = peers.firstOrNull { it.nodeId == latest.fromNodeId }
    val senderName = latestPeer?.name ?: latest.fromName ?: NodeId.fromHex(latest.fromNodeId).display

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Clean non-overlapping Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Emergencies",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        "Emergency SOS Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        "${beacons.size} ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Summary Callout Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Latest from $senderName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(latest.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = latest.text.ifBlank { "Emergency SOS Beacon Broadcasted" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Action Buttons Row: Review All Hub & Acknowledge All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReviewAll,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Review (${beacons.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onAcknowledgeAll,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Acknowledge All",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Emergency SOS Hub Sheet.
 * Allows triage, history browsing, filtering, and management of all active & archived SOS beacons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosEmergencyHubSheet(
    vm: MeshViewModel,
    activeBeacons: List<app.ripple.mesh.data.SosBeaconEntity>,
    allBeacons: List<app.ripple.mesh.data.SosBeaconEntity>,
    peers: List<PeerEntity>,
    playbackState: app.ripple.mesh.audio.OpusPlayer.PlaybackState? = null,
    onPlayVoice: ((String, String) -> Unit)? = null,
    onPauseVoice: (() -> Unit)? = null,
    onOpenChat: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active, 1: History (All)
    val context = LocalContext.current

    val displayedBeacons = if (selectedTab == 0) activeBeacons else allBeacons

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sheet Header Row: Full width title & subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Emergency SOS Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${activeBeacons.size} active · ${allBeacons.size} total recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Controls Row: Filter Chips & Acknowledge All Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Active (${activeBeacons.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error,
                            selectedLabelColor = MaterialTheme.colorScheme.onError
                        )
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("History (${allBeacons.size})") }
                    )
                }

                if (selectedTab == 0 && activeBeacons.isNotEmpty()) {
                    TextButton(
                        onClick = { vm.acknowledgeAllSosBeacons() },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Acknowledge All", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Beacons List
            if (displayedBeacons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (selectedTab == 0) "No active distress beacons. All clear." else "No SOS beacon history found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(displayedBeacons, key = { "hub_${it.messageId}" }) { beacon ->
                        val peer = peers.firstOrNull { it.nodeId == beacon.fromNodeId }
                        val lat = beacon.latE7?.let { it / 1e7 }
                        val lng = beacon.lngE7?.let { it / 1e7 }
                        val sender = peer?.name ?: beacon.fromName ?: NodeId.fromHex(beacon.fromNodeId).display

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!beacon.isAcknowledged)
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (!beacon.isAcknowledged) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Top row: Avatar, Sender, Status Badge & Time
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val peer = peers.firstOrNull { it.nodeId == beacon.fromNodeId }
                                        Avatar(nodeIdHex = beacon.fromNodeId, avatar = peer?.avatar, name = beacon.fromName ?: peer?.name, size = 38.dp)
                                        Column {
                                            Text(sender, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(beacon.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!beacon.isAcknowledged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ) {
                                        Text(
                                            if (!beacon.isAcknowledged) "ACTIVE" else "ACKNOWLEDGED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!beacon.isAcknowledged) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Message Text
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = beacon.text.ifBlank { "Distress beacon broadcasted without description." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                // Emergency Voice Memo player
                                if (beacon.voicePath != null && playbackState != null && onPlayVoice != null && onPauseVoice != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val isPlaying = playbackState.messageId == beacon.messageId && playbackState.isPlaying
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                                    .clickable {
                                                        if (isPlaying) onPauseVoice() else onPlayVoice(beacon.messageId, beacon.voicePath!!)
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
                                                    "🚨 Emergency Voice Memo",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    "${(beacon.voiceDurationMs ?: 5000) / 1000}s · Opus 12kbps audio memo",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // GPS Location if attached
                                if (lat != null && lng != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "📍 ${String.format("%.4f° N, %.4f° W", lat, -lng)} (±${beacon.accuracyMeters ?: 15}m)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(SOS+Emergency)")
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Open Map", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!beacon.isAcknowledged) {
                                        TextButton(onClick = { vm.acknowledgeSosBeacon(beacon.messageId) }) {
                                            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Acknowledge")
                                        }
                                    }
                                    TextButton(onClick = { onOpenChat(MeshService.BROADCAST_CONVERSATION) }) {
                                        Text("Open Thread")
                                    }
                                    IconButton(
                                        onClick = { vm.deleteSosBeacon(beacon.messageId) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

