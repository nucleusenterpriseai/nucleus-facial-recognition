package com.yxf.facerecognition

import android.media.Image
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.yxf.androidutil.io.fromFile
import com.yxf.androidutil.io.toFile
import com.yxf.facerecognition.model.FaceInfo
import com.yxf.facerecognition.model.FaceModel
import com.yxf.facerecognition.model.FaceModel.CREATOR.CURRENT_SOFTWARE_VERSION
import com.yxf.facerecognition.processor.FaceProcessor.Companion.CACHE_KEY_YUV
import com.yxf.facerecognition.util.FaceRecognitionUtil
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class FaceModelManager(
    private val modelPath: String,
    private val modelId: String,
    private val faceRecognition: FaceRecognition,
    private val additionalModelPath: String? = null
) {

    companion object {

        private val TAG = "FR.FaceModelManager"
    }


    @Volatile
    private var faceModel: FaceModel? = null

    @Volatile
    private var additionalFaceModel: FaceModel? = null

    @Volatile
    private var fullFaceModel: FaceModel? = null

    @Volatile
    private var modelAvailable: Boolean = false

    @Volatile
    private var additionalModelAvailable: Boolean = false

    init {
        faceRecognition.submitToThreadPool {
            if (isModelExist()) {
                val model = try {
                    FaceModel.fromFile(modelPath)
                } catch (e: Exception) {
                    Log.e(TAG, "load user model failed", e)
                    return@submitToThreadPool
                }
                if (model.id == modelId) {
                    updateFaceModelInternal(model)
                } else {
                    Log.d(TAG, "load model failed")
                }
                if (isAdditionalModelExist()) {
                    val additionalModel = try {
                        FaceModel.fromFile(additionalModelPath!!)
                    } catch (e: Exception) {
                        Log.e(TAG, "load update model failed", e)
                        return@submitToThreadPool
                    }
                    if (additionalModel.id == modelId) {
                        updateAdditionalFaceModelInternal(additionalModel)
                    }
                }

            } else {
                Log.e(TAG, "model not exist")
            }
        }
    }

    fun isModelAvailable(): Boolean {
        return modelAvailable
    }

    fun isModelExist(): Boolean {
        return faceModel != null || File(modelPath).exists()
    }

    fun isAdditionalModelExist(): Boolean {
        return additionalFaceModel != null || (additionalModelPath != null && File(
            additionalModelPath
        ).exists())
    }

    fun getFaceModel(): FaceModel? {
        if (fullFaceModel == null) {
            combineFullModel()
        }
        return fullFaceModel
    }

    fun combineFullModel() {
        if (faceModel == null) {
            return
        }
        if (additionalFaceModel == null) {
            fullFaceModel = faceModel
            return
        }
        fullFaceModel = FaceModel(
            faceModel!!.softwareVersion,
            faceModel!!.version,
            faceModel!!.id,
            faceModel!!.baseFaceInfoMap,
            additionalFaceModel!!.similarFaceInfoList,
            additionalFaceModel!!.recentFaceInfoList
        )
    }

    fun removeFaceModel() {
        faceModel = null
        File(modelPath).delete()
        modelAvailable = false

        additionalModelPath?.let { File(it).delete() }
        additionalModelAvailable = false
    }

    fun saveFaceModel() {
        if (faceRecognition.executor.isShutdown) {
            saveFaceModelSync()
        } else {
            faceRecognition.submitToThreadPool {
                saveFaceModelSync()
            }
        }
    }

    fun saveFaceModelSync() {
        val file = File(modelPath)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        faceModel?.toFile(modelPath)
        if (additionalModelPath == null) {
            return
        }
        val additionalFile = File(additionalModelPath)
        if (additionalFile.exists()) {
            additionalFile.delete()
        }
        additionalFile.createNewFile()
        additionalFaceModel?.toFile(additionalModelPath)
    }


    fun updateBaseFaceInfo(faceInfo: FaceInfo) {
        faceModel?.let { model ->
            val model = FaceModel(
                CURRENT_SOFTWARE_VERSION,
                model.version + 1,
                model.id,
                ConcurrentHashMap<Int, FaceInfo>(model.baseFaceInfoMap).also {
                    it[faceInfo.type] = faceInfo
                },
                model.similarFaceInfoList,
                model.recentFaceInfoList
            )
            faceModel = model
            return
        }
        if (isModelExist()) {
            Log.w(TAG, "user face model is null, update base face info failed")
        } else {
            throw RuntimeException("update base face info failed, model is not exist")
        }
    }

    private fun getNextVersion(): Int {
        return (faceModel?.version ?: 0) + 1
    }

    private fun createBaseFaceModel(
        baseList: List<FaceInfo>,
        version: Int = getNextVersion()
    ): FaceModel {
        val model = FaceModel(
            CURRENT_SOFTWARE_VERSION,
            version,
            modelId,
            ConcurrentHashMap<Int, FaceInfo>().also { map ->
                baseList.forEach {
                    map[it.type] = it
                }
            },
            CopyOnWriteArrayList(),
            CopyOnWriteArrayList()
        )
        return model
    }

    fun updateBaseModelByImage(face: Face, image: Image, yuvData: ByteArray? = null) {
        val yuv = yuvData ?: faceRecognition.faceProcessor.cache[CACHE_KEY_YUV].run {
            if (this == null) {
                FaceRecognitionUtil.imageToYuvImageData(image)
            } else {
                this as ByteArray
            }
        }
        val result = FaceRecognitionUtil.analyzeFace(face, image, faceRecognition.analysis, yuv)
        val faceInfo = result.first
        updateBaseFaceInfo(faceInfo)
    }

    private fun updateFaceModelInternal(faceModel: FaceModel) {
        this.faceModel = faceModel
        modelAvailable = true
    }

    private fun updateAdditionalFaceModelInternal(faceModel: FaceModel) {
        this.additionalFaceModel = faceModel
        additionalModelAvailable = true
    }

    fun coverFaceModelWithBaseFaceInfo(list: List<FaceInfo>) {
        val version = (faceModel?.version ?: 0) + 1
        val model = createBaseFaceModel(list, version)
        faceModel = model
    }

    fun updateFaceModel(faceModel: FaceModel, force: Boolean = false) {
        if (this.faceModel != null) {
            if (!force) {
                if (this.faceModel!!.version <= faceModel.version && this.faceModel!!.id == faceModel.id) {
                    updateFaceModelInternal(faceModel)
                }
                return
            }
        }
        updateFaceModelInternal(faceModel)
        combineFullModel()
    }

    fun updateSimilarFaceInfoList(list: List<FaceInfo>) {
        additionalFaceModel = FaceModel(CURRENT_SOFTWARE_VERSION,
            (additionalFaceModel?.version ?: faceModel?.version ?: 0) + 1,
            modelId,
            emptyMap(),
            list,
            additionalFaceModel?.recentFaceInfoList ?: emptyList()
            )
        combineFullModel()
    }

    fun updateRecentFaceInfoList(list: List<FaceInfo>) {
        additionalFaceModel = FaceModel(CURRENT_SOFTWARE_VERSION,
            (additionalFaceModel?.version ?: faceModel?.version ?: 0) + 1,
            modelId,
            emptyMap(),
            additionalFaceModel?.similarFaceInfoList ?: emptyList(),
            list
        )
        combineFullModel()
    }

    fun addRecentFaceInfo(faceInfo: FaceInfo) {
        val list = additionalFaceModel?.recentFaceInfoList?.toMutableList() ?: mutableListOf()
        list.add(faceInfo)
        if (list.size > FaceModel.RECENT_MAX_SIZE) {
            list.removeAt(0)
        }
        updateRecentFaceInfoList(list)
    }


}