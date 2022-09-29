package com.yxf.facerecognition.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import com.google.mlkit.vision.face.Face
import com.yxf.facerecognition.processor.FaceProcessor.Companion.CACHE_KEY_YUV
import com.yxf.facerecognition.util.FaceRecognitionUtil
import java.io.File

class SaveToPngPipelineHandler(private val path: String, private val quality: Int, private val once: Boolean = true) : BasePipelineHandler() {


    private var finished = false

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        val yuvData = (faceProcessor.cache[CACHE_KEY_YUV] ?: FaceRecognitionUtil.imageToYuvImageData(image)) as ByteArray
        val bitmap = FaceRecognitionUtil.yuvImageDataToBitmap(yuvData, image.width, image.height)
        val matrix = Matrix()
        matrix.postRotate(270.0f)
        val rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        file.outputStream().use {
            rotateBitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
        }
        if (once) {
            finished = true
        }
        return true
    }

    override fun isHandleFinished(): Boolean {
        return finished
    }

    override fun getFailedHint(): String {
        return ""
    }


}