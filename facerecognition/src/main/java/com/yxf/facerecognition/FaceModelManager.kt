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

class FaceModelManager(
    private val modelPath: String,
    private val modelId: String,
    private val faceRecognition: FaceRecognition
) {

    companion object {

        private val TAG = "FR.FaceModelManager"
    }


    @Volatile
    private var faceModel: FaceModel? = null

    @Volatile
    private var modelAvailable: Boolean = false

    init {
        faceRecognition.executor.submit {
            if (isModelExist()) {
                val model = try {
                    FaceModel.fromFile(modelPath)
                } catch (e: Exception) {
                    Log.e(TAG, "load user model failed", e)
                    return@submit
                }
                if (model.id == modelId) {
                    updateFaceModelInternal(model)
                } else {
                    Log.d(TAG, "load model failed")
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

    fun getFaceModel(): FaceModel? {
        return faceModel
    }

    fun removeFaceModel() {
        faceModel = null
        File(modelPath).delete()
        modelAvailable = false
    }

    fun saveFaceModel() {
        faceRecognition.executor.submit {
            saveFaceModelSync()
        }
    }

    fun saveFaceModelSync() {
        val file = File(modelPath)
        if (file.exists()) {
            file.delete()
            file.createNewFile()
        }
        faceModel?.toFile(modelPath)
    }


    fun updateBaseFaceInfo(faceInfo: FaceInfo) {
        faceModel?.let { model ->
            val model = FaceModel(
                FaceModel.CURRENT_SOFTWARE_VERSION,
                model.version + 1,
                model.id,
                ConcurrentHashMap<Int, FaceInfo>(model.baseFaceInfoMap).also { it[faceInfo.type] = faceInfo },
                model.similarFaceInfoList,
                model.recentFaceInfoList
            )
            faceModel = model
            return
        }
        if (isModelExist()) {
            Log.w(TAG, "user face model is null, update base face info failed")
        } else {
            faceModel = FaceModel(
                FaceModel.CURRENT_SOFTWARE_VERSION,
                1,
                modelId,
                ConcurrentHashMap<Int, FaceInfo>().also { it[faceInfo.type] = faceInfo },
                emptyList(),
                emptyList()
            )
        }
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
    }

    fun updateSimilarFaceInfoList(list: List<FaceInfo>) {
        faceModel?.let {
            faceModel = FaceModel(CURRENT_SOFTWARE_VERSION, it.version + 1, it.id, it.baseFaceInfoMap, list, it.recentFaceInfoList)
            return
        }
        Log.e(TAG, "update similar face info list failed")
    }

    fun updateRecentFaceInfoList(list: List<FaceInfo>) {
        faceModel?.let {
            faceModel = FaceModel(CURRENT_SOFTWARE_VERSION, it.version + 1, it.id, it.baseFaceInfoMap, it.similarFaceInfoList, list)
            return
        }
        Log.e(TAG, "update recent face info list failed")
    }

    fun addRecentFaceInfo(faceInfo: FaceInfo) {
        faceModel?.let {
            val list = it.recentFaceInfoList.toMutableList()
            list.add(faceInfo)
            if (list.size > FaceModel.RECENT_MAX_SIZE) {
                list.removeAt(0)
            }
            updateRecentFaceInfoList(list)
            return
        }
        Log.w(TAG, "update recent face info failed")
    }


}