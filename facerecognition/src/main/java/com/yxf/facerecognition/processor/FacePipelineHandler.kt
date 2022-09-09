package com.yxf.facerecognition.processor

import android.media.Image
import com.google.mlkit.vision.face.Face
import kotlin.math.abs

abstract class FacePipelineHandler(
    failedHint: String,
    private val condition: (face: Face) -> Boolean,
    protected val angle: Float = 10.0f
) : HintPipelineHandler(failedHint) {

    private var finished = false

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        return condition(face).also {
            finished = it
            if (it) {
                faceProcessor.faceRecognition.faceModelManager.updateBaseModelByImage(face, image)
            }
        }
    }

    override fun isHandleFinished(): Boolean {
        return finished
    }

}

class FrontFacePipelineHandler(failedHint: String, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, { abs(it.headEulerAngleX) < angle && abs(it.headEulerAngleY) < angle }, angle) {
}

class LeftFacePipelineHandler(failedHint: String, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, { abs(it.headEulerAngleX) < abs(it.headEulerAngleY) && it.headEulerAngleY > angle }, angle) {
}

class RightFacePipelineHandler(failedHint: String, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, { abs(it.headEulerAngleX) < abs(it.headEulerAngleY) && it.headEulerAngleY < -angle }, angle) {
}

class TopFacePipelineHandler(failedHint: String, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, { abs(it.headEulerAngleX) > abs(it.headEulerAngleY) && it.headEulerAngleX > angle }, angle) {
}

class BottomFacePipelineHandler(failedHint: String, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, { abs(it.headEulerAngleX) > abs(it.headEulerAngleY) && it.headEulerAngleX < -angle }, angle) {
}