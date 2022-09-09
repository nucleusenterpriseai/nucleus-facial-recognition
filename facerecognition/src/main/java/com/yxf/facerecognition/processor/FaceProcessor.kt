package com.yxf.facerecognition.processor

import android.media.Image
import com.google.mlkit.vision.face.Face
import com.yxf.facerecognition.FaceRecognition
import com.yxf.facerecognition.util.FaceRecognitionUtil

class FaceProcessor(private val executeOnce: Boolean = true) {


    companion object {

        const val CACHE_KEY_YUV = "key_yuv"
        const val CACHE_KEY_ANALYZE_RESULT = "analyze_result"

    }

    internal lateinit var faceRecognition: FaceRecognition

    private val handlerList = ArrayList<PipelineHandler>()

    private var finished = false

    val cache = HashMap<String, Any>()

    fun addPipelineHandler(handler: PipelineHandler) {
        handlerList.add(handler)
    }

    fun execute(face: Face, image: Image, failedCallback: (hint: String) -> Unit, finishCallback: () -> Unit) {
        try {
            for (i in 0 until handlerList.size) {
                val handler = handlerList[i]
                if (handler.isHandleFinished()) {
                    continue
                }
                val result = handler.handle(face, image, this)
                if (!result) {
                    failedCallback(handler.getFailedHint())
                    return
                } else {
                    handler.getSuccessfullyCallback()?.invoke(face, image)
                }
            }
            finishCallback()
            finished = true
        } finally {
            cache.clear()
        }
    }

    fun isFinished(): Boolean {
        return executeOnce && finished
    }


}