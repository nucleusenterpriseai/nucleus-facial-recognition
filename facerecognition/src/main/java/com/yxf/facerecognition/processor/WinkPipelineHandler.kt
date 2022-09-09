package com.yxf.facerecognition.processor

import android.media.Image
import com.google.mlkit.vision.face.Face

class WinkPipelineHandler(private val failedHint: String) : BasePipelineHandler() {


    private var faceId: Int? = null
    private var leftEyeOpen: Int = -1
    private var rightEyeOpen: Int = -1

    private var live = false

    private var trackingTime = 0L

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        val leftValue = ((face.leftEyeOpenProbability ?: -2.0f) + 0.5f).toInt()
        val rightValue = ((face.rightEyeOpenProbability ?: -2.0f) + 0.5f).toInt()
        if (this.faceId == face.trackingId) {
            if (live && (leftValue == 1 || rightValue == 1)) {
                return true
            }
            var leftWink = false
            if (leftEyeOpen >= 0) {
                if (leftValue + leftEyeOpen == 1) {
                    leftWink = true
                } else {
                    if (leftValue != -1) {
                        leftEyeOpen = leftValue
                    }
                }
            } else {
                leftEyeOpen = leftValue
            }
            var rightWink = false
            if (rightEyeOpen >= 0) {
                if (rightValue + rightEyeOpen == 1) {
                    rightWink = true
                } else {
                    if (rightValue != -1) {
                        rightEyeOpen = rightValue
                    }
                }
            } else {
                rightEyeOpen = rightValue
            }
            if (leftWink && rightWink) {
                live = true
            }
            return live && (leftValue == 1 || rightValue == 1)
        } else {
            this.faceId = face.trackingId
            live = false
            trackingTime = System.currentTimeMillis()
            leftEyeOpen = leftValue
            rightEyeOpen = rightValue
        }
        return false
    }

    private fun getWinkValue(probability: Float?): Int {
        val critical = 0.05f
        probability?.let {
            if (it - critical <= 0) {
                return 0
            } else if (it + critical >= 1) {
                return 1
            }
        }
        return -1
    }

    override fun isHandleFinished(): Boolean {
        return false
    }

    override fun getFailedHint(): String {
        if (System.currentTimeMillis() - trackingTime > 3 * 1000 && !live) {
            return failedHint
        } else {
            return ""
        }
    }
}