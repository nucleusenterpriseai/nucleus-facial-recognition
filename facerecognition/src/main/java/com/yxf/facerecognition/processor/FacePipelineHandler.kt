package com.yxf.facerecognition.processor

import android.media.Image
import com.google.mlkit.vision.face.Face
import com.yxf.facerecognition.model.FaceInfo
import com.yxf.facerecognition.util.FaceRecognitionUtil
import kotlin.math.abs

class FaceInfoContainer() {

    val list = ArrayList<FaceInfo>()

}

abstract class FacePipelineHandler(
    failedHint: String,
    private val container: FaceInfoContainer,
    private val condition: (face: Face) -> Boolean,
    protected val angle: Float = 10.0f
) : HintPipelineHandler(failedHint) {

    private var finished = false

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        return condition(face).also {
            finished = it
            if (it) {
                val yuv = faceProcessor.cache[FaceProcessor.CACHE_KEY_YUV].run {
                    if (this == null) {
                        FaceRecognitionUtil.imageToYuvImageData(image)
                    } else {
                        this as ByteArray
                    }
                }
                val result = FaceRecognitionUtil.analyzeFace(face, image, faceProcessor.faceRecognition.analysis, yuv)
                container.list.add(result.first)
            }
        }
    }

    override fun isHandleFinished(): Boolean {
        return finished
    }

}

class FrontFacePipelineHandler(failedHint: String, container: FaceInfoContainer, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, container, { abs(it.headEulerAngleX) < angle && abs(it.headEulerAngleY) < angle }, angle) {
}

class LeftFacePipelineHandler(failedHint: String, container: FaceInfoContainer, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, container, { abs(it.headEulerAngleX) < abs(it.headEulerAngleY) && it.headEulerAngleY > angle }, angle) {
}

class RightFacePipelineHandler(failedHint: String, container: FaceInfoContainer, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, container, { abs(it.headEulerAngleX) < abs(it.headEulerAngleY) && it.headEulerAngleY < -angle }, angle) {
}

class TopFacePipelineHandler(failedHint: String, container: FaceInfoContainer, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, container, { abs(it.headEulerAngleX) > abs(it.headEulerAngleY) && it.headEulerAngleX > angle }, angle) {
}

class BottomFacePipelineHandler(failedHint: String, container: FaceInfoContainer, angle: Float = 10.0f) :
    FacePipelineHandler(failedHint, container, { abs(it.headEulerAngleX) > abs(it.headEulerAngleY) && it.headEulerAngleX < -angle }, angle) {
}

class UpdateFacePipelineHandler(private val container: FaceInfoContainer) : BasePipelineHandler() {

    private var finished = false

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        val manager = faceProcessor.faceRecognition.faceModelManager
        manager.coverFaceModelWithBaseFaceInfo(container.list)
        finished = true
        return true
    }

    override fun isHandleFinished(): Boolean {
        return finished
    }

    override fun getFailedHint(): String {
        return "update face info failed"
    }
}