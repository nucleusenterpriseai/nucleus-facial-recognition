package com.yxf.facerecognitionsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.yxf.facerecognition.FaceRecognition
import com.yxf.facerecognition.processor.*
import com.yxf.facerecognitionsample.databinding.ActivityFaceRecognitionBinding
import java.util.concurrent.Executors


class FaceRecognitionActivity : AppCompatActivity() {


    private val vb by lazy { ActivityFaceRecognitionBinding.inflate(LayoutInflater.from(this)) }

    private val executor by lazy { Executors.newSingleThreadExecutor() }

    private lateinit var faceRecognition: FaceRecognition


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(vb.root)
        startCamera()
    }

    private fun createFaceProcessor(): FaceProcessor {
        return FaceProcessor(false).apply {
            addPipelineHandler(FaceRectPipelineHandler(vb.preview) {
                vb.root.post {
                    val lp = vb.rect.layoutParams as ViewGroup.MarginLayoutParams
                    lp.leftMargin = it.left
                    lp.topMargin = it.top
                    lp.width = it.width()
                    lp.height = it.height()
                    vb.rect.layoutParams = lp
                }
            })
            addPipelineHandler(RangePipelineHandler("请正对摄像头"))
            addPipelineHandler(AngleZPipelineHandler("请勿倾斜", 20.0f))
            //addPipelineHandler(WinkPipelineHandler("请眨眼"))
            addPipelineHandler(LivePipelineHandler("请勿欺骗我的感情", this@FaceRecognitionActivity))
            addPipelineHandler(FaceComparePipelineHandler("认证失败") { result, faceInfo, difference ->
                if (result) {
                    vb.root.post {
                        vb.status.text = "orientation: ${faceInfo.getTypeName()}\n difference: ${"%.4f".format(difference)}"
                    }
                }
            })
            addPipelineHandler(AddRecentFaceInfoPipelineHandler())
        }
    }

    private fun startCamera() {
        faceRecognition = FaceRecognition.Builder(vb.preview)
            .setProcessSuccessfullyListener {

            }
            .setProcessFailedListener {
                vb.root.post {
                    vb.status.text = it
                }
            }
            .setFaceProcessor(createFaceProcessor())
            .build()

        faceRecognition.start()


        /*val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(vb.preview.surfaceProvider)
                }

            val ia = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(executor, ImageAnalysis.Analyzer { proxy ->
                        val image = proxy.image ?: return@Analyzer
                        Log.d("Debug.image", "image width: ${image.width}, height: ${image.height}")

                        val ii = InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees)
                        val faceListTask = detector.process(ii)
                        faceListTask
                            .addOnSuccessListener { list ->
                                try {
                                    if (list.isEmpty()) {
                                        return@addOnSuccessListener
                                    }
                                    if (list.size > 1) {
                                        Log.d("Debug", "have two face")
                                        return@addOnSuccessListener
                                    }
                                    list.forEach {
                                        Log.d("Debug.rect", "rect: ${it.boundingBox}")
                                        Log.d("Debug.left", "left eye: ${it.leftEyeOpenProbability}")
                                        Log.d("Debug.right", "right eye: ${it.rightEyeOpenProbability}")
                                        Log.d(
                                            "Debug.angle",
                                            "angle x: ${it.headEulerAngleX}, y: ${it.headEulerAngleY}, z: ${it.headEulerAngleZ}"
                                        )
                                    }
                                    val face = list[0]
                                    faceProcessor.execute(face, image, {
                                        vb.status.post {
                                            if (it.isNotEmpty()) {
                                                vb.status.text = it
                                            }
                                        }
                                    }, {
                                        val result = FaceRecognitionUtil.analyzeFace(face, image, GlobalInfo.tfa)
                                        val faceInfo = result.first
                                        if (GlobalInfo.userFaceModel == null) {
                                            vb.status.post {
                                                vb.status.text = "face model is not exist"
                                            }
                                            return@execute
                                        }
                                        val compareResult = FaceRecognitionUtil.compareUserFaceModel(faceInfo, GlobalInfo.userFaceModel!!)
                                        if (compareResult.first > 1.0f) {
                                            vb.status.post {
                                                vb.status.text = "not same"
                                            }
                                        } else {
                                            vb.status.post {
                                                val message = "similar: ${compareResult.first} \n" +
                                                        "face orientation: ${compareResult.second.getTypeName()}"
                                                vb.status.text = message
                                            }
                                        }

                                    })

                                } finally {
                                    proxy.close()
                                }
                            }
                            .addOnFailureListener {
                                Log.e("Debug", "get face list failed", it)
                                proxy.close()
                            }
                    })
                }


            //val capture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, cameraSelector, preview, ia)
            } catch (e: Exception) {
                throw e
            }

        }, ContextCompat.getMainExecutor(this))*/
    }


    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }


}