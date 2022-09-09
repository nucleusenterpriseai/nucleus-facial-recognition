package com.yxf.facerecognition.model

import android.os.Parcel
import android.os.Parcelable
import com.yxf.facerecognition.ktx.readFaceInfoList
import com.yxf.facerecognition.ktx.writeFaceInfoList
import java.util.concurrent.ConcurrentHashMap

data class FaceModel(
    val softwareVersion: Int = CURRENT_SOFTWARE_VERSION,
    val version: Int,
    val id: String,
    val baseFaceInfoMap: Map<Int, FaceInfo>,
    val similarFaceInfoList: List<FaceInfo>,
    val recentFaceInfoList: List<FaceInfo>
) : Parcelable {

    fun getBaseFaceInfoList(): List<FaceInfo> {
        return baseFaceInfoMap.values.toList()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(softwareVersion)
        parcel.writeInt(version)
        parcel.writeString(id)
        parcel.writeFaceInfoList(getBaseFaceInfoList())
        parcel.writeFaceInfoList(similarFaceInfoList)
        parcel.writeFaceInfoList(recentFaceInfoList)
    }

    companion object CREATOR : Parcelable.Creator<FaceModel> {

        const val CURRENT_SOFTWARE_VERSION = 1

        const val SIMILAR_MAX_SIZE = 30
        const val RECENT_MAX_SIZE = 10


        override fun createFromParcel(parcel: Parcel): FaceModel {
            val softwareVersion = parcel.readInt()
            val version = parcel.readInt()
            val userId = parcel.readString()!!

            val baseFaceInfoList = parcel.readFaceInfoList()
            val baseFaceInfoMap = ConcurrentHashMap<Int, FaceInfo>()
            baseFaceInfoList.forEach {
                baseFaceInfoMap[it.type] = it
            }
            val similarFaceInfoList = parcel.readFaceInfoList()
            val recentFaceInfoList = parcel.readFaceInfoList()

            return FaceModel(softwareVersion, version, userId, baseFaceInfoMap, similarFaceInfoList, recentFaceInfoList)
        }

        override fun newArray(size: Int): Array<FaceModel?> {
            return arrayOfNulls(size)
        }
    }


}