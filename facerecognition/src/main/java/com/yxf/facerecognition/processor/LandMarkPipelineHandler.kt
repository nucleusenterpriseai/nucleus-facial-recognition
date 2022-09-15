package com.yxf.facerecognition.processor

import android.media.Image
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark

@Deprecated("not useful")
class LandMarkPipelineHandler(failedHint: String) : HintPipelineHandler(failedHint) {

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        val leftEyeMark = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEyeMark = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val mouthBottomMark = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)
        Log.d("Debug.mark", "left eye: ${leftEyeMark}, right eye: ${rightEyeMark}, mouth bottom: ${mouthBottomMark}")
        return true
    }

    override fun isHandleFinished(): Boolean {
        return false
    }
}