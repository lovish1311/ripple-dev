package app.ripple.mesh.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Lifecycle-aware Opus audio player using Android's native MediaPlayer.
 * Tracks active playback message ID, progress, and playback state for UI waveforms.
 */
class OpusPlayer(private val context: Context) {

    data class PlaybackState(
        val activeMessageId: String? = null,
        val isPlaying: Boolean = false,
        val progress: Float = 0f,
        val currentPositionMs: Int = 0,
        val durationMs: Int = 0
    ) {
        val messageId: String? get() = activeMessageId
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    fun play(scope: CoroutineScope, messageId: String, filePath: String) {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return

        // If clicking the currently playing message, toggle pause
        if (_playbackState.value.activeMessageId == messageId && _playbackState.value.isPlaying) {
            pause()
            return
        }

        // If resuming the same paused message
        if (_playbackState.value.activeMessageId == messageId && mediaPlayer != null && !_playbackState.value.isPlaying) {
            mediaPlayer?.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTracker(scope)
            return
        }

        // Stop any currently playing audio
        stop()

        try {
            val player = MediaPlayer.create(context, Uri.fromFile(file)) ?: return
            mediaPlayer = player

            val duration = player.duration
            _playbackState.value = PlaybackState(
                activeMessageId = messageId,
                isPlaying = true,
                progress = 0f,
                currentPositionMs = 0,
                durationMs = duration
            )

            player.setOnCompletionListener {
                stopProgressTracker()
                _playbackState.value = PlaybackState(
                    activeMessageId = null,
                    isPlaying = false,
                    progress = 0f,
                    currentPositionMs = 0,
                    durationMs = 0
                )
                try { it.release() } catch (_: Exception) {}
                mediaPlayer = null
            }

            player.setOnErrorListener { _, _, _ ->
                stop()
                true
            }

            player.start()
            startProgressTracker(scope)
        } catch (e: Exception) {
            stop()
        }
    }

    private fun startProgressTracker(scope: CoroutineScope) {
        stopProgressTracker()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val mp = mediaPlayer
                if (mp != null) {
                    val pos = mp.currentPosition
                    val dur = mp.duration
                    val prog = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                    _playbackState.value = _playbackState.value.copy(
                        progress = prog,
                        currentPositionMs = pos,
                        durationMs = dur
                    )
                }
                delay(50L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            stopProgressTracker()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        } catch (_: Exception) {}
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _playbackState.value = PlaybackState()
    }

    fun release() {
        stop()
    }
}
