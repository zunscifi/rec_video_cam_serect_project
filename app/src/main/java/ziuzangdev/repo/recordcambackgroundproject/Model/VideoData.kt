package ziuzangdev.repo.recordcambackgroundproject.Model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import java.io.File
import java.util.*
import kotlin.math.roundToInt

data class VideoData(val file: File) {
    val thumbnail: Bitmap?
    val title: String?
    val dateRecorded: Date?
    val duration: Long?
    val size: Double?
    val path : String?
    val fileOf : File?
    init {
        // Retrieve video metadata using MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        fileOf = file

        //Extract path
        path = file.absolutePath

        // Extract thumbnail
        thumbnail = retriever.frameAtTime

        // Extract title
        title = file.name
        // Extract date recorded
        val dateString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
        dateRecorded = if (dateString != null) {
            val format = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss")
            format.parse(dateString)
        } else {
            null
        }

        // Extract duration
        duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()

        // Extract size
        val sizeTemp = file.length().toDouble() / (1024 * 1024)
        size = (sizeTemp * 100).roundToInt() / 100.0
    }
    private fun getVideoSizeInMB(file: File): Double? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)

        // Retrieve duration and bitrate
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

        if (durationStr != null && bitrateStr != null) {
            val duration = durationStr.toLong() // in milliseconds
            val bitrate = bitrateStr.toLong() // in bits per second

            // Calculate size in bytes
            val size = duration * bitrate / 8

            // Convert to MB and return
            return size.toDouble() / (1024 * 1024)
        }

        // Duration or bitrate is missing
        return null
    }
}

