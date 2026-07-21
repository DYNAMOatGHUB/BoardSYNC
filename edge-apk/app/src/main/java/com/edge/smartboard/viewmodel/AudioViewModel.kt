package com.edge.smartboard.viewmodel

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edge.smartboard.models.AudioState
import com.edge.smartboard.repository.EdgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: EdgeRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private val SAMPLE_RATE   = 16000
    private val CHANNEL_IN    = AudioFormat.CHANNEL_IN_MONO
    private val ENCODING      = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE   = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING) * 4

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job?  = null
    private var timerJob: Job?      = null
    private var pcmFile: File?      = null
    private val rawPcmBuffer = mutableListOf<ByteArray>()

    // ── Public controls ───────────────────────────────────────────
    fun startRecording() {
        rawPcmBuffer.clear()
        val dir = File(appContext.getExternalFilesDir(null), "audio")
        dir.mkdirs()
        pcmFile = File(dir, "recording_${System.currentTimeMillis()}.pcm")

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_IN, ENCODING, BUFFER_SIZE
            )
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                _audioState.value = _audioState.value.copy(
                    isRecording = true, isPaused = false, durationMs = 0L, savedFilePath = null
                )
                startRecordingLoop()
                startTimer()
            } else {
                _audioState.value = _audioState.value.copy(savedFilePath = "ERROR: Microphone not available")
                audioRecord?.release()
                audioRecord = null
            }
        } catch (e: SecurityException) {
            _audioState.value = _audioState.value.copy(savedFilePath = "ERROR: RECORD_AUDIO permission denied")
        }
    }

    fun pauseRecording() {
        _audioState.value = _audioState.value.copy(isPaused = true)
        audioRecord?.stop()
        timerJob?.cancel()
        recordingJob?.cancel()
    }

    fun resumeRecording() {
        _audioState.value = _audioState.value.copy(isPaused = false)
        audioRecord?.startRecording()
        startRecordingLoop()
        startTimer()
    }

    fun stopRecording() {
        _audioState.value = _audioState.value.copy(isRecording = false, isPaused = false)
        timerJob?.cancel()
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        // Auto-save as WAV
        viewModelScope.launch(Dispatchers.IO) { saveWav() }
    }

    // ── Internal ──────────────────────────────────────────────────
    private fun startRecordingLoop() {
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            val buf = ByteArray(BUFFER_SIZE)
            val fos = FileOutputStream(pcmFile, true)
            try {
                while (_audioState.value.isRecording && !_audioState.value.isPaused) {
                    val read = audioRecord?.read(buf, 0, buf.size) ?: break
                    if (read > 0) {
                        fos.write(buf, 0, read)
                        // Compute RMS levels for waveform
                        val levels = computeWaveformLevels(buf, read, 40)
                        withContext(Dispatchers.Main) {
                            _audioState.value = _audioState.value.copy(audioLevels = levels)
                        }
                    }
                }
            } finally {
                fos.close()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_audioState.value.isRecording && !_audioState.value.isPaused) {
                delay(100L)
                _audioState.value = _audioState.value.copy(
                    durationMs = _audioState.value.durationMs + 100L
                )
            }
        }
    }

    private fun computeWaveformLevels(buf: ByteArray, read: Int, bars: Int): List<Float> {
        val samplesPerBar = (read / 2) / bars
        if (samplesPerBar == 0) return List(bars) { 0f }
        return (0 until bars).map { b ->
            val start = b * samplesPerBar * 2
            val end   = minOf(start + samplesPerBar * 2, read)
            var sum   = 0.0
            for (i in start until end step 2) {
                if (i + 1 < end) {
                    val sample = (buf[i + 1].toInt() shl 8) or (buf[i].toInt() and 0xFF)
                    sum += sample.toDouble() * sample.toDouble()
                }
            }
            val rms = sqrt(sum / samplesPerBar).toFloat() / 32768f
            rms.coerceIn(0f, 1f)
        }
    }

    fun saveWav() {
        val pcm = pcmFile ?: return
        if (!pcm.exists() || pcm.length() == 0L) return
        val wavFile = File(pcm.parent, pcm.name.replace(".pcm", ".wav"))
        val pcmSize = pcm.length()

        try {
            FileOutputStream(wavFile).use { out ->
                writeWavHeader(out, pcmSize)
                pcm.inputStream().use { it.copyTo(out) }
            }
            // Fix the header sizes now that we know total length
            val raf = RandomAccessFile(wavFile, "rw")
            raf.seek(4)
            raf.write(intToByteArray((wavFile.length() - 8).toInt()))
            raf.seek(40)
            raf.write(intToByteArray(pcmSize.toInt()))
            raf.close()

            pcm.delete() // Remove raw PCM

            viewModelScope.launch {
                _audioState.value = _audioState.value.copy(savedFilePath = wavFile.absolutePath)
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _audioState.value = _audioState.value.copy(savedFilePath = "ERROR: ${e.message}")
            }
        }
    }

    private fun writeWavHeader(out: FileOutputStream, pcmLength: Long) {
        val byteRate     = (SAMPLE_RATE * 1 * 16 / 8).toLong()
        val blockAlign   = (1 * 16 / 8)
        val totalDataLen = pcmLength + 36

        out.write("RIFF".toByteArray())
        out.write(intToByteArray(totalDataLen.toInt()))
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.write(intToByteArray(16))          // PCM chunk size
        out.write(shortToByteArray(1))         // PCM format
        out.write(shortToByteArray(1))         // Mono
        out.write(intToByteArray(SAMPLE_RATE))
        out.write(intToByteArray(byteRate.toInt()))
        out.write(shortToByteArray(blockAlign.toShort()))
        out.write(shortToByteArray(16))        // Bits per sample
        out.write("data".toByteArray())
        out.write(intToByteArray(pcmLength.toInt()))
    }

    private fun intToByteArray(value: Int) = byteArrayOf(
        (value and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 24 and 0xFF).toByte()
    )

    private fun shortToByteArray(value: Short) = byteArrayOf(
        (value.toInt() and 0xFF).toByte(),
        (value.toInt() shr 8 and 0xFF).toByte()
    )

    private fun shortToByteArray(value: Int) = shortToByteArray(value.toShort())

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        recordingJob?.cancel()
        audioRecord?.release()
        audioRecord = null
    }
}
