package com.yxf.facerecognitionsample

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.yxf.facerecognition.FaceRecognition
import com.yxf.facerecognition.processor.*
import com.yxf.facerecognitionsample.databinding.ActivityAddFaceBinding

class AddFaceActivity : AppCompatActivity() {

    companion object {

        private val TAG = "FR.AddFace"
    }

    private val vb by lazy { ActivityAddFaceBinding.inflate(LayoutInflater.from(this)) }


    private lateinit var faceRecognition: FaceRecognition

    private val faceRect = Rect()

    private val faceRectUpdateTask = Runnable {
        val mapRect = faceRect
        val lp = vb.rect.layoutParams as ViewGroup.MarginLayoutParams
        lp.leftMargin = mapRect.left
        lp.topMargin = mapRect.top
        lp.width = mapRect.width()
        lp.height = mapRect.height()
        vb.rect.layoutParams = lp
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(vb.root)
        startCamera()
        vb.addFace.setOnClickListener {
            faceRecognition.updateFaceProcessor(createFaceProcessor())
        }
        vb.clearFace.setOnClickListener {
            faceRecognition.faceModelManager.removeFaceModel()
        }

    }

    private fun createFaceProcessor(): FaceProcessor {
        val faceProcessor = FaceProcessor()
        faceProcessor.addPrePipelineHandler(FaceRectPipelineHandler(vb.preview) {
            faceRect.set(it)
            vb.root.post(faceRectUpdateTask)
        })

        faceProcessor.addPipelineHandler(FaceTrackingPipeHandler {
            vb.root.post {
                faceRecognition.updateFaceProcessor(createFaceProcessor())
            }
        })
        faceProcessor.addPipelineHandler(RangePipelineHandler("请正对摄像头"))
        faceProcessor.addPipelineHandler(AngleZPipelineHandler("请勿倾斜"))
        faceProcessor.addPipelineHandler(LivePipelineHandler("请勿欺骗我的感情", this))
        val container = FaceInfoContainer()
        faceProcessor.addPipelineHandler(FrontFacePipelineHandler("请正对摄像头", container))
        faceProcessor.addPipelineHandler(LeftFacePipelineHandler("请缓慢向左转头", container))
        faceProcessor.addPipelineHandler(RightFacePipelineHandler("请缓慢向右转头", container))
        faceProcessor.addPipelineHandler(TopFacePipelineHandler("请缓慢抬头",container))
        faceProcessor.addPipelineHandler(BottomFacePipelineHandler("请缓慢低头",container))
        faceProcessor.addPipelineHandler(UpdateFacePipelineHandler(container).apply {
            setSuccessfullyCallback { face, image ->
                vb.hint.post {
                    vb.hint.text = "采集完毕"
                }
            }
        })
        return faceProcessor
    }

    private fun createEmptyFaceProcessor(): FaceProcessor {
        val faceProcessor = FaceProcessor(true)
        faceProcessor.addPrePipelineHandler(FaceRectPipelineHandler(vb.preview) {
            faceRect.set(it)
            vb.root.post(faceRectUpdateTask)
        })
        faceProcessor.addPrePipelineHandler(LandMarkPipelineHandler("请勿遮挡脸部"))
        return faceProcessor
    }

    private fun startCamera() {
        faceRecognition = FaceRecognition.Builder(vb.preview)
            .setProcessFailedListener {
                vb.hint.post {
                    if (it.isNotEmpty()) {
                        vb.hint.text = it
                    }
                }
            }
            .setProcessSuccessfullyListener {

            }
            /*.setExceptionListener {
                Log.e(TAG, "exception occupied when add face", it)
                vb.hint.post {
                    vb.hint.text = "采集失败"
                }
            }*/
            .setFaceProcessor(createEmptyFaceProcessor())
            .build()
        faceRecognition.start()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}