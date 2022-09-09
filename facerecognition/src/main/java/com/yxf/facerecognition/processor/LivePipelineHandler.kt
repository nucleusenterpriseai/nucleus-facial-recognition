package com.yxf.facerecognition.processor

import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.face.Face
import com.mv.engine.FaceBox
import com.mv.engine.FaceDetector
import com.mv.engine.Live
import com.yxf.facerecognition.processor.FaceProcessor.Companion.CACHE_KEY_YUV
import com.yxf.facerecognition.util.FaceRecognitionUtil


class LivePipelineHandler(private val failedHint: String, private val context: AppCompatActivity, private val critical: Float = 0.915f) :
    BasePipelineHandler() {


    private var lastId = -1
    private var finished = false
    private var startTime = 0L

    private val faceDetector: FaceDetector by lazy {
        FaceDetector().apply { loadModel(context.assets) }
    }

    private val live: Live by lazy { Live().apply { loadModel(context.assets) } }

    init {
        context.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    faceDetector.destroy()
                    live.destroy()
                }
            }
        })
    }

    override fun handle(face: Face, image: Image, faceProcessor: FaceProcessor): Boolean {
        Log.d("Debug.face", "face id: ${face.trackingId}")
        if (lastId != face.trackingId) {
            startTime = System.currentTimeMillis()
            lastId = face.trackingId ?: -1
            finished = false
        } else {
            if (finished) {
                return true
            }
        }
        val width = image.width
        val height = image.height
        val orientation = 7
        val yuv = FaceRecognitionUtil.imageToYuvImageData(image)
        faceProcessor.cache[CACHE_KEY_YUV] = yuv

        val boxes = detectFace(yuv, width, height, orientation)
        if (boxes.isNotEmpty()) {
            val c = detectLive(yuv, width, height, orientation, boxes[0])
            return (c > critical).also {
                if (it) {
                    finished = true
                }
            }
        }
        return false
    }

    private fun detectFace(
        yuv: ByteArray,
        width: Int,
        height: Int,
        orientation: Int
    ): List<FaceBox> = faceDetector.detect(yuv, width, height, orientation)

    private fun detectLive(
        yuv: ByteArray,
        width: Int,
        height: Int,
        orientation: Int,
        faceBox: FaceBox
    ): Float = live.detect(yuv, width, height, orientation, faceBox)

    override fun isHandleFinished(): Boolean {
        return false
    }

    override fun getFailedHint(): String {
        if (System.currentTimeMillis() - startTime < 1000) {
            return ""
        }
        return failedHint
    }
}