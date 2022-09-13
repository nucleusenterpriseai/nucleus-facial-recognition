package com.yxf.facerecognition.model

import com.yxf.androidutil.io.DataSerializable
import com.yxf.androidutil.io.DataSerializableCreator
import com.yxf.androidutil.io.readFloatArray
import com.yxf.androidutil.io.writeFloatArray
import java.io.*

data class FaceInfo(
    val tfData: FloatArray,
    val type: Int,
    var weightingDifference: Float = Float.MAX_VALUE,
    internal var difference: Float = Float.MAX_VALUE,
    val timestamp: Long = System.currentTimeMillis()
) : DataSerializable {

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

    override fun writeToData(out: DataOutputStream) {
        out.writeFloatArray(tfData)
        out.writeInt(type)
        out.writeFloat(weightingDifference)
        out.writeFloat(difference)
        out.writeLong(timestamp)
    }

    companion object CREATOR : DataSerializableCreator<FaceInfo> {

        const val HISTORY_WEIGHT = 0.7f


        const val TYPE_FRONT = 0
        const val TYPE_LEFT = 1
        const val TYPE_TOP = 2
        const val TYPE_RIGHT = 3
        const val TYPE_BOTTOM = 4

        override fun readFromData(ins: DataInputStream): FaceInfo {
            val tfData = ins.readFloatArray()
            val type = ins.readInt()
            val weightingDifference = ins.readFloat()
            val difference = ins.readFloat()
            val timestamp = ins.readLong()
            return FaceInfo(tfData, type, weightingDifference, difference, timestamp)
        }
    }


}
