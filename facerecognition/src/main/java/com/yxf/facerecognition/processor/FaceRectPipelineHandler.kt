package com.yxf.facerecognition.processor

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import android.view.View
import androidx.camera.view.PreviewView
import androidx.core.graphics.toRect
import com.google.mlkit.vision.face.Face

class FaceRectPipelineHandler(
    private val previewView: PreviewView,
    private val rectCallback: (rect: Rect) -> Unit
) : BasePipelineHandler() {
    private val sourceRect = Rect()


    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        sourceRect.set(face.boundingBox)
        val mapRect = mapRect(sourceRect, image.width, image.height, true)
        rectCallback.invoke(mapRect)
        return true
    }

    private fun mapRect(rect: Rect, imageWidth: Int, imageHeight: Int, mirror: Boolean = true): Rect {
        val previewWidth = previewView.width
        val previewHeight = previewView.height
        val width = imageHeight
        val height = imageWidth
        val rate = previewWidth / width.toFloat()
        val matrix = Matrix()
        matrix.postScale(rate, rate)
        matrix.postTranslate(0.0f, (previewHeight - height * rate) / 2)
        val out = RectF()
        matrix.mapRect(out, RectF(rect))
        if (mirror) {
            out.set(previewWidth - out.right, out.top, previewWidth - out.left, out.bottom)
        }
        return out.toRect()
    }

    override fun isHandleFinished(): Boolean {
        return false
    }

    override fun getFailedHint(): String {
        return ""
    }
}