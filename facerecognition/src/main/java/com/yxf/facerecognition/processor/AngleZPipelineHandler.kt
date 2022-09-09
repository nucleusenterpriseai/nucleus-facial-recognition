package com.yxf.facerecognition.processor

import android.media.Image
import com.google.mlkit.vision.face.Face
import kotlin.math.abs

class AngleZPipelineHandler(failedHint: String, private val angle: Float = 10.0f) : HintPipelineHandler(failedHint) {

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        return abs(face.headEulerAngleZ) < angle
    }

    override fun isHandleFinished(): Boolean {
        return false
    }

}