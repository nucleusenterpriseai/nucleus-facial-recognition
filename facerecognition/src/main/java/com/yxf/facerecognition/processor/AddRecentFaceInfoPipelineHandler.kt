package com.yxf.facerecognition.processor

import android.media.Image
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.yxf.facerecognition.model.FaceInfo
import com.yxf.facerecognition.processor.FaceProcessor.Companion.CACHE_KEY_ANALYZE_RESULT
import com.yxf.facerecognition.util.FaceRecognitionUtil

class AddRecentFaceInfoPipelineHandler(private val interval: Long = 1000) : BasePipelineHandler() {

    companion object {

        private val TAG = "FR." + "AddRecentFaceInfoPipelineHandler"

    }

    var addFinishCallback: Runnable? = null

    var faceInfoAdded = false

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        val faceInfo = faceProcessor.cache[CACHE_KEY_ANALYZE_RESULT].run {
            if (this == null) {
                val yuvData = faceProcessor.cache[FaceProcessor.CACHE_KEY_YUV].run {
                    if (this == null) {
                        FaceRecognitionUtil.imageToYuvImageData(image)
                    } else {
                        this as ByteArray
                    }
                }
                FaceRecognitionUtil.analyzeFace(face, image, faceProcessor.faceRecognition.analysis, yuvData).first
            } else {
                this as FaceInfo
            }
        }
        var lastInfo: FaceInfo? = null
        val recentList = faceProcessor.faceRecognition.faceModelManager.getFaceModel()!!.recentFaceInfoList
        if (recentList.isNotEmpty()) {
            lastInfo = recentList.last()
        }

        if (lastInfo == null || lastInfo.timestamp + interval < faceInfo.timestamp) {
            faceProcessor.faceRecognition.faceModelManager.addRecentFaceInfo(faceInfo)
            addFinishCallback?.run()
            faceInfoAdded = true
        } else {
            Log.d(TAG, "skip the face info")
        }
        return true
    }

    override fun isHandleFinished(): Boolean {
        return faceInfoAdded
    }

    override fun getFailedHint(): String {
        return ""
    }
}