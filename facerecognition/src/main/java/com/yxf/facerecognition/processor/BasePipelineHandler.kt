package com.yxf.facerecognition.processor

import android.media.Image
import com.google.mlkit.vision.face.Face

abstract class BasePipelineHandler : PipelineHandler {

    private var successfullyCallback: ((face: Face, image: Image) -> Unit)? = null

    override fun setSuccessfullyCallback(block: (face: Face, image: Image) -> Unit) {
        successfullyCallback = block
    }

    override fun getSuccessfullyCallback(): ((face: Face, image: Image) -> Unit)? {
        return successfullyCallback
    }

}