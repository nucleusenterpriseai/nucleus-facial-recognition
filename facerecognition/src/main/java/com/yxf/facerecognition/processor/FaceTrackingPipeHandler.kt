package com.yxf.facerecognition.processor

import android.media.Image
import com.google.mlkit.vision.face.Face

class FaceTrackingPipeHandler(private val changeCallback: () -> Unit) : BasePipelineHandler() {

    private var lastFaceId: Int = -1

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        if (lastFaceId == -1) {
            lastFaceId = face.trackingId ?: -1
            return true
        }
        if (lastFaceId == face.trackingId) {
            return true
        }
        changeCallback()
        return false
    }

    override fun isHandleFinished(): Boolean {
        return false
    }

    override fun getFailedHint(): String {
        return ""
    }
}