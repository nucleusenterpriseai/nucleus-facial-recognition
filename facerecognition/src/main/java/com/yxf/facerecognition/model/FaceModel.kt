package com.yxf.facerecognition.model

import com.yxf.androidutil.io.DataSerializable
import com.yxf.androidutil.io.DataSerializableCreator
import com.yxf.androidutil.io.readList
import com.yxf.androidutil.io.writeList
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.ConcurrentHashMap

data class FaceModel(
    val softwareVersion: Int = CURRENT_SOFTWARE_VERSION,
    val version: Int,
    val id: String,
    val baseFaceInfoMap: Map<Int, FaceInfo>,
    val similarFaceInfoList: List<FaceInfo>,
    val recentFaceInfoList: List<FaceInfo>
) : DataSerializable {

    fun getBaseFaceInfoList(): List<FaceInfo> {
        return baseFaceInfoMap.values.toList()
    }

    override fun writeToData(out: DataOutputStream) {
        out.writeInt(softwareVersion)
        out.writeInt(version)
        out.writeUTF(id)
        out.writeList(getBaseFaceInfoList(), writeFaceInfo)
        out.writeList(similarFaceInfoList, writeFaceInfo)
        out.writeList(recentFaceInfoList, writeFaceInfo)
    }


    companion object CREATOR : DataSerializableCreator<FaceModel> {

        const val CURRENT_SOFTWARE_VERSION = 1

        const val SIMILAR_MAX_SIZE = 30
        const val RECENT_MAX_SIZE = 10

        private val writeFaceInfo: (out: DataOutputStream, info: FaceInfo) -> Unit = {out, info ->
            info.writeToData(out)
        }

        private val readFaceInfo: (ins: DataInputStream) -> FaceInfo = {
            FaceInfo.readFromData(it)
        }

        override fun readFromData(ins: DataInputStream): FaceModel {
            val softwareVersion = ins.readInt()
            val version = ins.readInt()
            val userId = ins.readUTF()
            val baseFaceInfoList = ins.readList(readFaceInfo)
            val baseFaceInfoMap = ConcurrentHashMap<Int, FaceInfo>()
            baseFaceInfoList.forEach {
                baseFaceInfoMap[it.type] = it
            }
            val similarFaceInfoList = ins.readList(readFaceInfo)
            val recentFaceInfoList = ins.readList(readFaceInfo)
            return FaceModel(softwareVersion, version, userId, baseFaceInfoMap, similarFaceInfoList, recentFaceInfoList)

        }
    }

}