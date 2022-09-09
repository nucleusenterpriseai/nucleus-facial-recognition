package com.yxf.facerecognition.processor

abstract class HintPipelineHandler(private val failedHint: String) : BasePipelineHandler() {
    override fun getFailedHint(): String {
        return failedHint
    }
}