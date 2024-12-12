package com.example.ocr_poc.models

import android.os.Parcel
import android.os.Parcelable

data class TextEntity(
    val label: String,
    val text: String,
    val boundingBox: String? = null,
    val cornerPoints: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(label)
        parcel.writeString(text)
        parcel.writeString(boundingBox)
        parcel.writeString(cornerPoints)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TextEntity> {
        override fun createFromParcel(parcel: Parcel): TextEntity {
            return TextEntity(parcel)
        }

        override fun newArray(size: Int): Array<TextEntity?> {
            return arrayOfNulls(size)
        }
    }
}
