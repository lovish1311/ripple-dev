package app.ripple.mesh.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ripple.mesh.R
import app.ripple.mesh.ui.MeshViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(vm: MeshViewModel, onBack: () -> Unit) {
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
            Toast.makeText(context, context.getString(R.string.audio_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.stopVoice()
            if (vm.isRecording.value) vm.cancelRecording()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            title = { Text(stringResource(R.string.sos_screen)) },
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.sos_beacon_warning), style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.sos_message_label)) },
                maxLines = 4,
            )

            HorizontalDivider()

            // Emergency Voice Memo Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.emergency_voice_memo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.emergency_voice_memo_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (voiceMemoFile == null) {
                    val infiniteTransition = rememberInfiniteTransition(label = "sos_rec_pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "sos_pulse"
                    )

                    if (isRecording && isHandsFreeLocked) {
                            // Hands-free recording HUD
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
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
                                                targetValue = (fraction * 24).dp.coerceIn(4.dp, 26.dp),
                                                label = "sosWave"
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
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.cancel_recording),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
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
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(R.string.finish_memo), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Hold-to-record button (or tap for hands-free)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp)
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
                                    .padding(vertical = 14.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRecording) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color.White.copy(alpha = pulseAlpha), CircleShape)
                                        )
                                        val sec = (recordingDurationMs / 1000).coerceAtMost(4)
                                        Text(
                                            "0:0$sec / 0:04",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        // Dynamic audio waveform while holding
                                        Row(
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
                                                    targetValue = (fraction * 20).dp.coerceIn(4.dp, 22.dp),
                                                    label = "sosWaveHold"
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(animHeight)
                                                        .clip(RoundedCornerShape(1.5.dp))
                                                        .background(Color.White)
                                                )
                                            }
                                        }

                                        Text(
                                            stringResource(R.string.recording_memo_hold),
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Text(
                                            stringResource(R.string.hold_to_record_memo),
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                } else {
                    // Preview Recorded Memo Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isPlaying = playbackState.messageId == "sos_preview" && playbackState.isPlaying
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                    .clickable {
                                        if (isPlaying) {
                                            vm.pauseVoice()
                                        } else {
                                            vm.playVoice("sos_preview", voiceMemoFile!!.absolutePath)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) stringResource(R.string.pause_memo) else stringResource(R.string.play_memo),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Attached Voice Memo",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "${(voiceMemoDurationMs + 500) / 1000}s · Opus 8kbps · Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(
                                onClick = {
                                    vm.stopVoice()
                                    voiceMemoFile?.delete()
                                    voiceMemoFile = null
                                    voiceMemoDurationMs = 0
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    stringResource(R.string.re_record),
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.sos_opt_in_gps), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.sos_include_location), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.sos_location_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = shareLocation, onCheckedChange = { shareLocation = it })
            }
            if (!shareLocation) {
                Text(
                    stringResource(R.string.sos_no_location_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    vm.sendSos(message.trim(), shareLocation, voiceMemoFile, voiceMemoDurationMs)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
            ) {
                Text(stringResource(R.string.sos_send), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
