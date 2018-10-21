package com.njlabs.showjava.utils

import android.content.Context
import android.net.ConnectivityManager
import java.io.File

object Tools {

    fun getFolderSize(f: File): Long {
        var size: Long = 0
        if (f.isDirectory) {
            for (file in f.listFiles()) {
                size += getFolderSize(file)
            }
        } else {
            size = f.length()
        }
        return size
    }

    fun checkDataConnection(context: Context): Boolean {
        var status = false
        val connectivityMgr =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityMgr.activeNetworkInfo != null &&
            connectivityMgr.activeNetworkInfo.isAvailable &&
            connectivityMgr.activeNetworkInfo.isConnected) {
            status = true
        }
        return status
    }
}
