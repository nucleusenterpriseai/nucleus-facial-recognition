package com.yxf.facerecognition.processor

import android.media.Image
import com.google.mlkit.vision.face.Face

interface PipelineHandler {

    fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean

    fun setSuccessfullyCallback(block: (face: Face, image: Image) -> Unit)

    fun getSuccessfullyCallback(): ((face: Face, image: Image) -> Unit)?

    fun isHandleFinished(): Boolean

    fun getFailedHint(): String


}