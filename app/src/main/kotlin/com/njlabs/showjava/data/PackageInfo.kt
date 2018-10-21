package com.njlabs.showjava.data

import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable

class PackageInfo() : Parcelable {
    var label = ""
    var name = ""
    var version = ""
    var filePath = ""
    var icon: Drawable? = null

    constructor(parcel: Parcel) : this() {
        label = parcel.readString()
        name = parcel.readString()
        version = parcel.readString()
        filePath = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(label)
        parcel.writeString(name)
        parcel.writeString(version)
        parcel.writeString(filePath)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return String.format("filePath: %s", filePath)
    }

    companion object CREATOR : Parcelable.Creator<PackageInfo> {
        override fun createFromParcel(parcel: Parcel): PackageInfo {
            return PackageInfo(parcel)
        }

        override fun newArray(size: Int): Array<PackageInfo?> {
            return arrayOfNulls(size)
        }
    }
}
