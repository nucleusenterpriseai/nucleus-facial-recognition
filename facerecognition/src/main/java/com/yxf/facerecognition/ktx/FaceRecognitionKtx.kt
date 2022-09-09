package com.yxf.facerecognition.ktx

import android.os.Parcel
import com.yxf.androidutil.ktx.fromByteArray
import com.yxf.androidutil.ktx.toByteArray
import com.yxf.facerecognition.model.FaceInfo

internal fun Parcel.writeFaceInfo(faceInfo: FaceInfo) {
    val bytes = faceInfo.toByteArray()
    val size = bytes.size
    writeInt(size)
    writeByteArray(bytes)
}


internal fun Parcel.readFaceInfo(): FaceInfo {
    val size = readInt()
    val bytes = ByteArray(size)
    readByteArray(bytes)
    return FaceInfo.fromByteArray(bytes)
}

internal fun Parcel.writeFaceInfoList(list: List<FaceInfo>) {
    val size = list.size
    writeInt(size)
    for (i in 0 until size) {
        writeFaceInfo(list[i])
    }
}

internal fun Parcel.readFaceInfoList(): List<FaceInfo> {
    val size = readInt()
    val list = ArrayList<FaceInfo>()
    for (i in 0 until size) {
        list.add(readFaceInfo())
    }
    return list
}