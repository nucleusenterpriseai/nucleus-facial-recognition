package com.yxf.facerecognition.processor

import android.graphics.Matrix
import android.graphics.RectF
import android.media.Image
import android.util.Log
import androidx.core.graphics.toRect
import com.google.mlkit.vision.face.Face

class RangePipelineHandler(failedHint: String, private val margin: Int = 0) : HintPipelineHandler(failedHint) {

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        val rect = face.boundingBox
        val matrix = Matrix()
        matrix.postTranslate(-image.height / 2.0f, -image.width / 2.0f)
        matrix.postRotate(90.0f)
        matrix.postTranslate(image.width / 2.0f, image.height / 2.0f)
        val out = RectF()
        matrix.mapRect(out, RectF(rect))
        val result = out.toRect()
        if (result.left - margin < 0) {
            return false
        }
        if (result.top - margin < 0) {
            return false
        }
        if (result.right + margin > image.width) {
            return false
        }
        if (result.bottom + margin > image.height) {
            return false
        }
        Log.d("Debug.range", "image(${image.width}:${image.height}), rotate rect: $result, source rect: ${face.boundingBox}")
        return true
    }

    override fun isHandleFinished(): Boolean {
        return false
    }
}