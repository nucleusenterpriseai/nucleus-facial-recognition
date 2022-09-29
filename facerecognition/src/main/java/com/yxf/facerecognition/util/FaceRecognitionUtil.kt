package com.yxf.facerecognition.util

import android.graphics.*
import android.media.Image
import android.util.Log
import androidx.core.graphics.toRect
import com.google.mlkit.vision.face.Face
import com.yxf.facerecognition.FaceModelManager
import com.yxf.facerecognition.model.FaceInfo
import com.yxf.facerecognition.model.FaceModel
import com.yxf.facerecognition.tflite.TensorFlowLiteAnalyzer
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

object FaceRecognitionUtil {

    const val TAG = "FR.Util"

    fun imageToYuvImageData(image: Image, save: ByteArray? = null): ByteArray {
        require(!(image.format !== ImageFormat.YUV_420_888)) { "Invalid image format" }

        val width = image.width
        val height = image.height


        // Full size Y channel and quarter size U+V channels.
        val numPixels = (width * height * 1.5f).toInt()
        val result = if (numPixels == save?.size) {
            save
        } else {
            ByteArray(numPixels)
        }

        // Order of U/V channel guaranteed, read more:
        // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        var index = 0
        // Copy Y channel.
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        for (y in 0 until height) {
            for (x in 0 until width) {
                result[index++] = yBuffer[y * yRowStride + x * yPixelStride]
            }
        }

        // Copy VU data
        // NV21 format is expected to have YYYYVU packaging.
        // The U/V planes are guaranteed to have the same row stride and pixel stride.
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        val uvWidth = width / 2
        val uvHeight = height / 2

        for (y in 0 until uvHeight) {
            for (x in 0 until uvWidth) {
                // V channel
                result[index++] = vBuffer[y * uvRowStride + x * uvPixelStride]
                // U channel
                result[index++] = uBuffer[y * uvRowStride + x * uvPixelStride]
            }
        }
        return result
    }

    fun yuvImageDataToBitmap(data: ByteArray, width: Int, height: Int, quality: Int = 100): Bitmap {
        val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), quality, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun yuvImageDataToPng(data: ByteArray, width: Int, height: Int, path: String, quality: Int = 100): File {
        val bitmap = yuvImageDataToBitmap(data, width, height)
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        val out = file.outputStream()
        out.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, it)
        }
        return file
    }

    fun imageToBitmap(image: Image): Bitmap {
        return yuvImageDataToBitmap(imageToYuvImageData(image), image.width, image.height)
    }

    fun analyzeFace(
        face: Face,
        image: Image,
        analyzer: TensorFlowLiteAnalyzer,
        yuvData: ByteArray = imageToYuvImageData(image),
        angle: Float = 10.0f
    ): Pair<FaceInfo, Bitmap> {
        val fixBound = face.boundingBox.run {
            val size = width().coerceAtMost(height())
            return@run Rect(left, top, left + size, top + size)
        }
        val rotateRect = fixBound.run {
            val matrix = Matrix()
            matrix.postTranslate(-image.height / 2.0f, -image.width / 2.0f)
            matrix.postRotate(90.0f)
            matrix.postTranslate(image.width / 2.0f, image.height / 2.0f)
            val out = RectF()
            matrix.mapRect(out, RectF(this))
            return@run out.toRect()
        }
        var bitmap = yuvImageDataToBitmap(yuvData, image.width, image.height)
        if (rotateRect.top < 0 || rotateRect.left < 0 || rotateRect.right > image.width || rotateRect.bottom > image.height) {
            Log.d(
                TAG,
                "image(${image.width}:${image.height}), rotate rect: $rotateRect, source rect: ${face.boundingBox}, fix rect: $fixBound"
            )
        }
        var faceBitmap = Bitmap.createBitmap(
            bitmap,
            rotateRect.left,
            rotateRect.top,
            rotateRect.width(),
            rotateRect.height(),
            Matrix().apply {
                postRotate(270.0f)
                val scale = 112.0f / rotateRect.width()
                postScale(scale, scale)
            },
            false
        )
        val data = analyzer.analyzeFaceImage(faceBitmap)
        val type = if (abs(face.headEulerAngleY) < angle && abs(face.headEulerAngleX) < angle) {
            FaceInfo.TYPE_FRONT
        } else if (abs(face.headEulerAngleX) > abs(face.headEulerAngleY)) {
            if (face.headEulerAngleX > 0) {
                FaceInfo.TYPE_TOP
            } else {
                FaceInfo.TYPE_BOTTOM
            }
        } else {
            if (face.headEulerAngleY > 0) {
                FaceInfo.TYPE_LEFT
            } else {
                FaceInfo.TYPE_RIGHT
            }
        }
        val faceInfo = FaceInfo(data, type)
        return Pair(faceInfo, faceBitmap)
    }

    fun compareFaceData(first: FloatArray, second: FloatArray): Float {
        var sum = 0.0
        for (i in first.indices) {
            val dif = first[i] - second[i]
            sum += dif * dif
        }
        return sqrt(sum).toFloat()
    }

    fun baseCompareUserFaceModel(target: FaceInfo, faceModel: FaceModel): Pair<Float, FaceInfo> {
        val fl = faceModel.getBaseFaceInfoList()
        var min = Float.MAX_VALUE
        var similar: FaceInfo = fl[0]
        fl.forEach {
            val result = compareFaceData(target.tfData, it.tfData)
            if (result < min) {
                min = result
                similar = it
            }
        }
        return Pair(min, similar)
    }

    fun similarCompareAndUpdateUserFaceModel(target: FaceInfo, faceModel: FaceModel, manager: FaceModelManager): Pair<Float, FaceInfo> {
        val map = HashMap<Long, FaceInfo>()
        faceModel.recentFaceInfoList.forEach {
            map[it.timestamp] = it
        }
        faceModel.similarFaceInfoList.forEach {
            map[it.timestamp] = it
        }
        var min = Float.MAX_VALUE
        var similar = target
        map.values.forEach {
            val difference = compareFaceData(target.tfData, it.tfData)
            it.difference = difference
            if (difference <= min) {
                min = difference
                similar = it
            }
            if (it.weightingDifference == Float.MAX_VALUE) {
                it.weightingDifference = it.difference
            } else {
                it.weightingDifference = it.weightingDifference * FaceInfo.HISTORY_WEIGHT + it.difference * (1 - FaceInfo.HISTORY_WEIGHT)
            }
        }

        val list = map.values.toMutableList()
        list.sortWith(Comparator { o1, o2 ->
            return@Comparator if (o1.weightingDifference - o2.weightingDifference > 0) 1 else -1
        })
        val count = list.size - FaceModel.SIMILAR_MAX_SIZE
        if (count > 0) {
            repeat(count) {
                list.removeAt(list.size - 1)
            }
        }
        manager.updateSimilarFaceInfoList(list)
        return Pair(min, similar)
    }


}