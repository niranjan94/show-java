package com.njlabs.showjava.data

import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable

class PackageInfo() : Parcelable {
    var packageLabel = ""
    var packageName = ""
    var packageVersion = ""
    var packageFilePath = ""
    var packageIcon: Drawable? = null

    constructor(parcel: Parcel) : this() {
        packageLabel = parcel.readString()
        packageName = parcel.readString()
        packageVersion = parcel.readString()
        packageFilePath = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(packageLabel)
        parcel.writeString(packageName)
        parcel.writeString(packageVersion)
        parcel.writeString(packageFilePath)
    }

    override fun describeContents(): Int {
        return 0
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
