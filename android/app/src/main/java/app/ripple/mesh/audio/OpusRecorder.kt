package app.ripple.mesh.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Production-grade audio recorder utilizing Android's native Opus encoder via MediaCodec
 * and MediaMuxer (OGG container) for ultra-low bitrate offline mesh voice notes.
 *
 * Audio specs: 16 kHz, 16-bit PCM mono, encoded to Opus at 12 kbps.
 * Resulting 4-5s voice memo is ~3.5 KB, fitting cleanly inside a single BLE mesh packet.
 */
class OpusRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val BIT_RATE = 8000 // 8 kbps for speech
        const val CHANNEL_COUNT = 1
        const val MAX_DURATION_MS = 4000L // 4 seconds max for emergency mesh voice
    }

    data class RecordingResult(val file: File, val durationMs: Int)

    private var recordingJob: Job? = null
    private var startTimeMs: Long = 0

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var currentOutputFile: File? = null
    private var onAutoStopCallback: ((RecordingResult) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startRecording(
        scope: CoroutineScope,
        onAutoStop: ((RecordingResult) -> Unit)? = null
    ): Boolean {
        if (_isRecording.value) return false
        onAutoStopCallback = onAutoStop

        val voiceDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
        val outputFile = File(voiceDir, "vn_${System.currentTimeMillis()}.ogg")
        currentOutputFile = outputFile

        _isRecording.value = true
        _amplitude.value = 0f
        _recordingDurationMs.value = 0L
        startTimeMs = System.currentTimeMillis()

        recordingJob = scope.launch(Dispatchers.IO) {
            recordLoop(outputFile)
        }
        return true
    }

    private fun recordLoop(outputFile: File) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, 4096)

        var audioRecord: AudioRecord? = null
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_COMPLEXITY, 3)
            }

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
            var audioTrackIndex = -1

            audioRecord.startRecording()

            val frameSizeBytes = 640 // 20ms @ 16kHz mono 16-bit PCM (16000 * 2 * 0.02)
            val pcmBuffer = ByteArray(frameSizeBytes)
            val bufferInfo = MediaCodec.BufferInfo()
            var presentationTimeUs = 0L

            while (recordingJob?.isActive == true && _isRecording.value) {
                val elapsed = System.currentTimeMillis() - startTimeMs
                _recordingDurationMs.value = elapsed

                // Check 5-second emergency safety cutoff
                if (elapsed >= MAX_DURATION_MS) {
                    break
                }

                // 1. Read 20ms PCM audio frame from microphone
                val readBytes = audioRecord.read(pcmBuffer, 0, pcmBuffer.size)
                if (readBytes > 0) {
                    // Compute RMS amplitude for UI waveform
                    computeAmplitude(pcmBuffer, readBytes)

                    // 2. Feed PCM into MediaCodec input buffer
                    val inputBufferIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val bytesToPut = minOf(readBytes, inputBuffer.remaining())
                            inputBuffer.put(pcmBuffer, 0, bytesToPut)
                            val durationUs = (bytesToPut * 1_000_000L) / (SAMPLE_RATE * 2)
                            codec.queueInputBuffer(inputBufferIndex, 0, bytesToPut, presentationTimeUs, 0)
                            presentationTimeUs += durationUs
                        }
                    }
                }

                // 3. Drain encoded Opus buffers from MediaCodec into MediaMuxer
                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        val newFormat = codec.outputFormat
                        audioTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
            }

            // End-of-stream signal to codec
            try {
                val inputIndex = codec.dequeueInputBuffer(10_000L)
                if (inputIndex >= 0) {
                    codec.queueInputBuffer(inputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }

                var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                while (outputIndex >= 0) {
                    val outBuf = codec.getOutputBuffer(outputIndex)
                    if (outBuf != null && bufferInfo.size > 0 && muxerStarted) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(audioTrackIndex, outBuf, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                }
            } catch (e: Exception) {
                android.util.Log.e("OpusRecorder", "recordLoop inner exception: ${e.message}", e)
            }
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                android.util.Log.e("OpusRecorder", "recordLoop error: ${e.message}", e)
                outputFile.delete()
            }
        } finally {
            android.util.Log.d("OpusRecorder", "recordLoop finally block entered. muxerStarted=$muxerStarted, file exists=${outputFile.exists()}, length=${outputFile.length()}")
            try { audioRecord?.stop() } catch (e: Exception) { android.util.Log.w("OpusRecorder", "audioRecord stop failed: ${e.message}") }
            try { audioRecord?.release() } catch (_: Exception) {}
            try { codec?.stop() } catch (e: Exception) { android.util.Log.w("OpusRecorder", "codec stop failed: ${e.message}") }
            try { codec?.release() } catch (_: Exception) {}
            if (muxerStarted) {
                try { muxer?.stop() } catch (e: Exception) { android.util.Log.w("OpusRecorder", "muxer stop failed: ${e.message}") }
            }
            try { muxer?.release() } catch (_: Exception) {}

            _isRecording.value = false
            _amplitude.value = 0f

            val durationMs = (System.currentTimeMillis() - startTimeMs).toInt()
            if (outputFile.exists() && outputFile.length() > 0) {
                android.util.Log.i("OpusRecorder", "Recording finished successfully: ${outputFile.name}, size=${outputFile.length()}, dur=${durationMs}ms")
                onAutoStopCallback?.invoke(RecordingResult(outputFile, durationMs))
            } else {
                android.util.Log.w("OpusRecorder", "Recording output file is empty or missing")
            }
        }
    }

    private fun computeAmplitude(buffer: ByteArray, length: Int) {
        var sum = 0.0
        val sampleCount = length / 2
        for (i in 0 until sampleCount) {
            val sample = ((buffer[i * 2 + 1].toInt() shl 8) or (buffer[i * 2].toInt() and 0xff)).toShort()
            sum += (sample * sample).toDouble()
        }
        val rms = if (sampleCount > 0) sqrt(sum / sampleCount) else 0.0
        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        _amplitude.value = normalized
    }

    suspend fun stopRecording(): RecordingResult? {
        if (!_isRecording.value) return null
        onAutoStopCallback = null // Cleared so finally block doesn't double-invoke
        _isRecording.value = false
        recordingJob?.join()
        val durationMs = (System.currentTimeMillis() - startTimeMs).toInt()
        val file = currentOutputFile
        return if (file != null && file.exists() && file.length() > 0) {
            RecordingResult(file, durationMs)
        } else null
    }

    fun cancelRecording() {
        onAutoStopCallback = null
        _isRecording.value = false
        recordingJob?.cancel()
        currentOutputFile?.delete()
        currentOutputFile = null
        _amplitude.value = 0f
        _recordingDurationMs.value = 0L
    }
}
