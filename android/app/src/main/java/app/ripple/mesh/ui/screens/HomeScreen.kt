package app.ripple.mesh.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ripple.mesh.R
import app.ripple.mesh.core.NodeId
import app.ripple.mesh.data.PeerEntity
import app.ripple.mesh.service.MeshService
import app.ripple.mesh.ui.MeshViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MeshViewModel, onOpenChat: (String) -> Unit, onOpenSettings: () -> Unit) {
    val status by vm.status.collectAsStateWithLifecycle()
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val peers by vm.peers.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(2.dp))
                    MeshStatusPill(status = status)
                }
            },
            actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) } },
        )
    }) { padding ->
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
                    Tab(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.tab_chats), style = MaterialTheme.typography.titleSmall)
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
                                leadingContent = { Avatar(c.conversation) },
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
                    ) { items(peers, key = { it.nodeId }) { PeerRow(it) { onOpenChat(it.nodeId) } } }
                }
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
private fun PeerRow(peer: PeerEntity, onClick: () -> Unit) {
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
            leadingContent = { Avatar(peer.nodeId) },
            trailingContent = {
                Box(
                    Modifier.size(10.dp)
                        .background(if (recent) Color(0xFF2ECC71) else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .semantics { contentDescription = dotLabel },
                )
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

/** Deterministic colored avatar with rounded corners. */
@Composable
fun Avatar(nodeIdHex: String) {
    val hue = (nodeIdHex.take(6).toLong(16) % 360).toFloat()
    val color = Color.hsv(hue, 0.45f, 0.75f)
    Box(Modifier.size(44.dp).background(color, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text(nodeIdHex.takeLast(2).uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}
