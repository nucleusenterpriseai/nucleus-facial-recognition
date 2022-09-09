package com.yxf.facerecognition.model

import android.os.Bundle
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.Serializable
import java.nio.ByteBuffer

data class FaceInfo(
    val tfData: FloatArray,
    val type: Int,
    var weightingDifference: Float = Float.MAX_VALUE,
    internal var difference: Float = Float.MAX_VALUE,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    fun getTypeName(): String {
        return when (type) {
            TYPE_FRONT -> "front"
            TYPE_LEFT -> "left"
            TYPE_TOP -> "top"
            TYPE_RIGHT -> "right"
            TYPE_BOTTOM -> "bottom"
            else -> type.toString()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceInfo

        if (!tfData.contentEquals(other.tfData)) return false
        if (type != other.type) return false
        if (difference != other.difference) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        return (timestamp % Int.MAX_VALUE).toInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(tfData.size)
        parcel.writeFloatArray(tfData)
        parcel.writeInt(type)
        parcel.writeFloat(weightingDifference)
        parcel.writeFloat(difference)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FaceInfo> {

        const val HISTORY_WEIGHT = 0.7f


        const val TYPE_FRONT = 0
        const val TYPE_LEFT = 1
        const val TYPE_TOP = 2
        const val TYPE_RIGHT = 3
        const val TYPE_BOTTOM = 4

        override fun createFromParcel(parcel: Parcel): FaceInfo {
            val size = parcel.readInt()
            val tfData = FloatArray(size)
            parcel.readFloatArray(tfData)
            val type = parcel.readInt()
            val weightingDifference = parcel.readFloat()
            val difference = parcel.readFloat()
            val timestamp = parcel.readLong()
            return FaceInfo(tfData, type, weightingDifference, difference, timestamp)
        }

        override fun newArray(size: Int): Array<FaceInfo?> {
            return arrayOfNulls(size)
        }
    }


}
