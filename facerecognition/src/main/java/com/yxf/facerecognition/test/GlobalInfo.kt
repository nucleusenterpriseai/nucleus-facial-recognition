package com.yxf.facerecognition.test

import android.annotation.SuppressLint
import android.content.Context
import com.yxf.androidutil.ktx.fromFile
import com.yxf.androidutil.ktx.toFile
import com.yxf.facerecognition.model.FaceInfo
import com.yxf.facerecognition.model.FaceModel
import com.yxf.facerecognition.tflite.TensorFlowLiteAnalyzer
import java.io.File

@SuppressLint("StaticFieldLeak")
object GlobalInfo {

    private lateinit var context: Context

    val tfa by lazy { TensorFlowLiteAnalyzer(context) }

    val modelPath by lazy { "${context.getExternalFilesDir(null)}/user_face.model" }
    var faceModel: FaceModel? = null

    val faceInfoMap = HashMap<Int, FaceInfo>()

    fun init(context: Context) {
        this.context = context.applicationContext
        loadFaceModel()
    }

    fun updateFaceModel() {
        if (faceModel == null) {
            faceModel = FaceModel(
                1, 1, "user id",
                faceInfoMap,
                emptyList(),
                emptyList()
            )
        } else {
            faceModel = FaceModel(
                1, faceModel!!.version + 1, "user id",
                faceInfoMap,
                emptyList(),
                emptyList()
            )
        }
        faceModel!!.toFile(modelPath)
    }

    fun loadFaceModel() {
        val file = File(modelPath)
        if (file.exists()) {
            faceModel = FaceModel.fromFile(modelPath)
        }
    }


}