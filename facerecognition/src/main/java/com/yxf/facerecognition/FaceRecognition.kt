package com.yxf.facerecognition

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.Keep
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.yxf.facerecognition.processor.FaceProcessor
import com.yxf.facerecognition.tflite.TensorFlowLiteAnalyzer
import java.io.File
import java.util.concurrent.*

@Keep
class FaceRecognition private constructor(private val builder: Builder) {


    companion object {

        private val TAG = "FR.FaceRecognition"


    }

    internal val context: FragmentActivity = builder.preview.context as FragmentActivity

    @Volatile
    internal var faceProcessor = builder.faceProcessor.apply { faceRecognition = this@FaceRecognition }

    internal val analysis by lazy { TensorFlowLiteAnalyzer(context) }

    internal val executor = object : ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>()) {

        override fun afterExecute(r: Runnable?, t: Throwable?) {
            super.afterExecute(r, t)
            var e: Throwable? = null
            if (t == null && r is Future<*>) {
                try {
                    val future = r as Future<*>
                    if (future.isDone) {
                        future.get()
                    }
                } catch (ce: CancellationException) {
                    e = ce
                } catch (ee: ExecutionException) {
                    e = ee.cause
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            } else {
                e = t
            }
            if (e != null) {
                reportException(e)
            }
        }
    }

    private val detector = FaceDetection.getClient(builder.options)

    val faceModelManager = FaceModelManager(builder.modelPath, builder.modelId, this, builder.additionalModelPath ?: "${builder.modelPath}.additional")


    internal fun submitToThreadPool(task: Runnable) {
        if (executor.isShutdown) {
            return
        }
        executor.submit(task)
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(builder.preview.surfaceProvider)
                }
            val ia = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            ia.setAnalyzer(executor, ImageAnalysis.Analyzer { proxy ->
                if (proxy.image == null) {
                    proxy.close()
                    return@Analyzer
                }
                val image = proxy.image!!
                val ii = InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees)
                val task = detector.process(ii)
                task.addOnSuccessListener { list ->
                    submitToThreadPool {
                        try {
                            if (list.isEmpty()) {
                                builder.processEmptyCallback?.invoke()
                                return@submitToThreadPool
                            }
                            val face = list[0]
                            faceProcessor.preExecute(face, image) {
                                builder.processFailedCallback?.invoke(it)
                            }
                            if (!faceProcessor.isFinished()) {
                                faceProcessor.execute(face, image,
                                    {
                                        builder.processFailedCallback?.invoke(it)
                                    }, {
                                        builder.processSuccessfullyCallback?.invoke()
                                    })
                            }
                        } finally {
                            faceProcessor.clearCache()
                            proxy.close()
                        }
                    }
                }
                task.addOnFailureListener {
                    submitToThreadPool {
                        try {
                            Log.w(TAG, "detect face failed", it)
                            reportException(it)
                        } finally {
                            proxy.close()
                        }
                    }
                }
                task.addOnCanceledListener {
                    proxy.close()
                }
            })
            try {
                provider.unbindAll()
                provider.bindToLifecycle(context, builder.cameraSelector, preview, ia)
            } catch (e: Exception) {
                reportException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun updateFaceProcessor(faceProcessor: FaceProcessor) {
        faceProcessor.faceRecognition = this
        this.faceProcessor = faceProcessor
    }

    internal fun reportException(e: Throwable) {
        if (builder.exceptionListener != null) {
            builder.exceptionListener!!.invoke(e)
        } else {
            throw e
        }
    }


    init {
        context.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    if (builder.autoSaveModel) {
                        faceModelManager.saveFaceModel()
                    }
                    executor.shutdown()
                }
            }
        })
    }


    public class Builder(internal val preview: PreviewView) {

        private val context by lazy { preview.context.applicationContext }

        private val defaultFileFolderPath by lazy {
            //TODO : set to file dir
            val path = "${context.getExternalFilesDir(null)}/face_recognition"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            return@lazy path
        }

        internal lateinit var faceProcessor: FaceProcessor
        internal lateinit var options: FaceDetectorOptions
        internal lateinit var modelPath: String
        var additionalModelPath: String? = null
        internal var modelId: String = "default"
        internal var exceptionListener: ((e: Throwable) -> Unit)? = null
        internal var processFailedCallback: ((failedHint: String) -> Unit)? = null
        internal var processSuccessfullyCallback: (() -> Unit)? = null
        internal var processEmptyCallback: (() -> Unit)? = null
        internal var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        internal var autoSaveModel: Boolean = false

        fun saveModelOnDestroy(): Builder {
            autoSaveModel = true
            return this
        }

        fun setCameraSelector(selector: CameraSelector): Builder {
            this.cameraSelector = selector
            return this
        }

        fun setFaceProcessor(processor: FaceProcessor): Builder {
            this.faceProcessor = processor
            return this
        }

        fun setFaceDetectorOptions(options: FaceDetectorOptions): Builder {
            this.options = options
            return this
        }

        fun setExceptionListener(listener: (e: Throwable) -> Unit): Builder {
            exceptionListener = listener
            return this
        }

        fun setProcessFailedListener(callback: (failedHint: String) -> Unit): Builder {
            processFailedCallback = callback
            return this
        }

        fun setProcessSuccessfullyListener(callback: () -> Unit): Builder {
            processSuccessfullyCallback = callback
            return this
        }

        fun setProcessEmptyListener(callback: () -> Unit): Builder {
            processEmptyCallback = callback
            return this
        }

        fun setModelPath(path: String): Builder {
            modelPath = path
            return this
        }

        fun setAdditionModelPath(path: String): Builder {
            additionalModelPath = path
            return this
        }

        fun setModelId(id: String): Builder {
            modelId = id
            return this
        }

        fun build(): FaceRecognition {
            if (!this::options.isInitialized) {
                options = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .enableTracking()
                    .build()
            }
            if (!this::faceProcessor.isInitialized) {
                faceProcessor = FaceProcessor()
            }

            if (!this::modelPath.isInitialized) {
                modelPath = "${defaultFileFolderPath}/user_face.model"
            }

            return FaceRecognition(this)
        }

    }


}