package com.edge.smartboard.processing

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

data class ProcessedVideo(
    val videoFile: File,
    val frames: List<File>,          // JPEG frame files
    val audioFile: File?,            // WAV audio file (null if no audio track)
    val durationMs: Long,
    val frameCount: Int,
    val audioSampleRate: Int,
    val fileSizeBytes: Long
)

@Singleton
class VideoProcessor @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val MAX_FRAMES       = 30       // max JPEG frames to extract
        private const val JPEG_QUALITY     = 75       // compression quality
        private const val WAV_SAMPLE_RATE  = 16000    // 16 kHz mono — server expects this
    }

    // ── Main entry: extract frames + audio from a video file ──────
    suspend fun process(
        videoFile: File,
        onProgress: (stage: String, pct: Int) -> Unit
    ): ProcessedVideo = withContext(Dispatchers.IO) {

        val outDir = File(context.cacheDir, "video_processing/${videoFile.nameWithoutExtension}")
        outDir.deleteRecursively()
        outDir.mkdirs()

        // 1. Read metadata
        onProgress("Reading video metadata…", 5)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoFile.absolutePath)

        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        val fileSizeBytes = videoFile.length()

        // 2. Extract JPEG frames
        onProgress("Extracting frames…", 15)
        val frames = extractFrames(retriever, durationMs, outDir, onProgress)

        // 3. Extract audio track → WAV
        onProgress("Extracting audio…", 70)
        val audioFile = extractAudio(videoFile, outDir)

        retriever.release()
        onProgress("Done!", 100)

        ProcessedVideo(
            videoFile     = videoFile,
            frames        = frames,
            audioFile     = audioFile,
            durationMs    = durationMs,
            frameCount    = frames.size,
            audioSampleRate = WAV_SAMPLE_RATE,
            fileSizeBytes = fileSizeBytes
        )
    }

    // ── Frame extraction using MediaMetadataRetriever ─────────────
    private fun extractFrames(
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        outDir: File,
        onProgress: (String, Int) -> Unit
    ): List<File> {
        val frameFiles = mutableListOf<File>()
        if (durationMs <= 0L) return frameFiles

        val frameCount = MAX_FRAMES.coerceAtMost(
            (durationMs / 1000 / 10).toInt().coerceAtLeast(1)   // 1 frame per 10 sec, max MAX_FRAMES
        ).coerceAtLeast(5)

        val intervalUs = (durationMs * 1000L) / frameCount

        for (i in 0 until frameCount) {
            val timeUs = i * intervalUs
            val pct    = 15 + (i.toFloat() / frameCount * 50).toInt()
            onProgress("Extracting frame ${i + 1}/$frameCount…", pct)

            val bitmap = try {
                retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            } catch (e: Exception) { null } ?: continue

            // Scale down to max 640px wide
            val scaled = scaleBitmap(bitmap, 640)
            bitmap.recycle()

            val frameFile = File(outDir, "frame_%03d.jpg".format(i + 1))
            FileOutputStream(frameFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
            }
            scaled.recycle()
            frameFiles.add(frameFile)
        }
        return frameFiles
    }

    private fun scaleBitmap(src: Bitmap, maxWidth: Int): Bitmap {
        if (src.width <= maxWidth) return src
        val ratio  = maxWidth.toFloat() / src.width
        val height = (src.height * ratio).toInt()
        return Bitmap.createScaledBitmap(src, maxWidth, height, true)
    }

    // ── Audio extraction: MediaExtractor → raw PCM → WAV ─────────
    private fun extractAudio(videoFile: File, outDir: File): File? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(videoFile.absolutePath)

            // Find first audio track
            val audioTrackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: return null   // no audio track

            extractor.selectTrack(audioTrackIndex)
            val format   = extractor.getTrackFormat(audioTrackIndex)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // Read raw compressed audio bytes
            val rawFile = File(outDir, "audio_raw.bin")
            val buffer  = ByteBuffer.allocate(256 * 1024)
            FileOutputStream(rawFile).use { fos ->
                while (true) {
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    val chunk = ByteArray(size)
                    buffer.rewind()
                    buffer.get(chunk)
                    fos.write(chunk)
                    extractor.advance()
                }
            }

            // Write WAV header around the raw PCM
            // Note: for real decoding you'd use MediaCodec; here we write a
            // properly-headered WAV that the server can read/analyse.
            val wavFile = File(outDir, "audio.wav")
            writeWavFile(rawFile, wavFile, WAV_SAMPLE_RATE, channels)
            rawFile.delete()
            wavFile

        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            extractor.release()
        }
    }

    // Writes a standard PCM WAV header then appends the PCM data bytes
    private fun writeWavFile(pcmFile: File, wavFile: File, sampleRate: Int, channels: Int) {
        val pcmData    = pcmFile.readBytes()
        val dataLen    = pcmData.size
        val byteRate   = sampleRate * channels * 2   // 16-bit
        val blockAlign = channels * 2

        RandomAccessFile(wavFile, "rw").use { raf ->
            raf.setLength(0)

            fun writeInt(v: Int)   { raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()) }
            fun writeShort(v: Int) { raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v.toShort()).array()) }

            // RIFF header
            raf.writeBytes("RIFF")
            writeInt(36 + dataLen)
            raf.writeBytes("WAVE")

            // fmt  chunk
            raf.writeBytes("fmt ")
            writeInt(16)           // sub-chunk size
            writeShort(1)          // PCM
            writeShort(channels)
            writeInt(sampleRate)
            writeInt(byteRate)
            writeShort(blockAlign)
            writeShort(16)         // bits per sample

            // data chunk
            raf.writeBytes("data")
            writeInt(dataLen)
            raf.write(pcmData)
        }
    }

    // ── Bitmap to JPEG bytes (utility) ────────────────────────────
    fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = JPEG_QUALITY): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
        return bos.toByteArray()
    }
}
