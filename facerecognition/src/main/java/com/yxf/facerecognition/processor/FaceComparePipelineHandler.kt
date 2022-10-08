package com.yxf.facerecognition.processor

import android.media.Image
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.yxf.facerecognition.model.FaceInfo
import com.yxf.facerecognition.processor.FaceProcessor.Companion.CACHE_KEY_ANALYZE_RESULT
import com.yxf.facerecognition.processor.FaceProcessor.Companion.CACHE_KEY_YUV
import com.yxf.facerecognition.util.FaceRecognitionUtil

class FaceComparePipelineHandler(
    failedHint: String,
    private val compareRecent: Boolean = true,
    private val critical: Float = DEFAULT_DIFFERENCE_CRITICAL,
    private val minCompareInterval: Int = 100,
    private val showErrorDelay: Long = 15 * 1000,
    private val callback: (result: Boolean, faceInfo: FaceInfo, difference: Float) -> Unit
) : HintPipelineHandler(failedHint) {

    companion object {


        val DEFAULT_DIFFERENCE_CRITICAL = 0.8f

        private val TAG = "FR." + "FaceComparePipelineHandler"

    }

    private var trackingTime = -1L
    private var firstSuccessfulTime = 0L

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        if (trackingTime == -1L) {
            trackingTime = System.currentTimeMillis()
        }
        val yuvData = faceProcessor.cache[CACHE_KEY_YUV].run {
            if (this == null) {
                FaceRecognitionUtil.imageToYuvImageData(image)
            } else {
                this as ByteArray
            }
        }
        val result = FaceRecognitionUtil.analyzeFace(face, image, faceProcessor.faceRecognition.analysis, yuvData)
        val faceInfo = result.first
        faceProcessor.cache[CACHE_KEY_ANALYZE_RESULT] = faceInfo
        val faceModel = faceProcessor.faceRecognition.faceModelManager.getFaceModel() ?: return false
        val baseResult = FaceRecognitionUtil.baseCompareUserFaceModel(faceInfo, faceModel)
        val minResult = if (compareRecent) {
            val similarResult = FaceRecognitionUtil.similarCompareAndUpdateUserFaceModel(
                faceInfo,
                faceModel,
                faceProcessor.faceRecognition.faceModelManager
            )
            if (baseResult.first < similarResult.first) baseResult.also {
                Log.d(TAG, "use base result")
            } else similarResult.also {
                Log.d(TAG, "use similar result")
            }
        } else {
            baseResult
        }
        val currentResult = minResult.first < critical
        var finalResult = false
        if (currentResult) {
            if (firstSuccessfulTime == 0L) {
                firstSuccessfulTime = System.currentTimeMillis()
            } else {
                if (System.currentTimeMillis() - firstSuccessfulTime >= minCompareInterval) {
                    finalResult = true
                }
            }
        } else {
            firstSuccessfulTime = 0L
        }
        return finalResult.also {
            callback(it, minResult.second, minResult.first)
            if (it) {
                trackingTime = -1
            }
        }
    }

    override fun isHandleFinished(): Boolean {
        return false
    }

    override fun getFailedHint(): String {
        if (trackingTime == -1L || System.currentTimeMillis() - trackingTime < showErrorDelay) {
            return ""
        }
        return super.getFailedHint()
    }


}