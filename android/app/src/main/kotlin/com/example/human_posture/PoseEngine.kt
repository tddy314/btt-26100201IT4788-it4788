package com.example.human_posture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.SystemClock
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

data class FrameResult(
    val input: ImageInput,
    val landmarks: List<Landmark> = emptyList(),
    val width: Int = 0,
    val height: Int = 0,
    val thumbnail: String? = null,
    val loadToPoseMs: Double? = null,
    val inferenceMs: Double? = null,
    val errors: Errors? = null,
    val message: String,
    val gtPath: String? = null,
    val gtIndex: Int? = null,
    val firstInference: Boolean = false,
    val comparison: Comparison? = null,
)

class PoseEngine(private val context: Context) : AutoCloseable {
    private val detector = PoseDetection.getClient(AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE).build())

    private fun decode(uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Không đọc được ảnh JPG" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2560) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val original = requireNotNull(resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }) { "Ảnh lỗi" }
        try {
            val exif = resolver.openInputStream(uri)?.use { ExifInterface(it) }
            val matrix = Matrix()
            when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            }
            if (matrix.isIdentity) return original
            val oriented = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            if (oriented !== original) original.recycle()
            return oriented
        } catch (e: Exception) {
            original.recycle()
            throw e
        }
    }

    suspend fun process(image: ImageInput, first: Boolean): FrameResult {
        val start = SystemClock.elapsedRealtimeNanos()
        val bitmap = decode(image.file.uri)
        try {
            val input = InputImage.fromBitmap(bitmap, 0)
            val inferenceStart = SystemClock.elapsedRealtimeNanos()
            // ML Kit does not cancel a running Task. Await it before recycling its bitmap.
            val pose = withContext(NonCancellable) { detector.process(input).await() }
            val end = SystemClock.elapsedRealtimeNanos()
            val points = pose.allPoseLandmarks.map { l ->
                val p = l.position3D
                Landmark(l.landmarkType, Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble()), l.inFrameLikelihood)
            }
            val directory = File(context.cacheDir, "pose-previews").apply { mkdirs() }
            val preview = File.createTempFile("frame-", ".jpg", directory)
            val ratio = minOf(1.0, 960.0 / maxOf(bitmap.width, bitmap.height))
            val small = Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
            try { preview.outputStream().use { check(small.compress(Bitmap.CompressFormat.JPEG, 85, it)) } }
            finally { if (small !== bitmap) small.recycle() }
            return FrameResult(image, points, bitmap.width, bitmap.height, preview.absolutePath,
                (end - start) / 1e6, (end - inferenceStart) / 1e6,
                message = if (points.isEmpty()) "Không phát hiện người" else "Đã nhận diện",
                firstInference = first)
        } finally { bitmap.recycle() }
    }

    override fun close() = detector.close()
}
