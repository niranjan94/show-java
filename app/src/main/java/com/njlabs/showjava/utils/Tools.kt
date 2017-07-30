package com.njlabs.showjava.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent

import com.njlabs.showjava.Constants
import com.njlabs.showjava.models.SourceInfo
import com.njlabs.showjava.services.processor.ProcessorService

import java.io.File
import android.net.ConnectivityManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo

object Tools {

    fun killAllProcessorServices(context: Context, forNew: Boolean) {
        val mServiceIntent = Intent(context, ProcessorService::class.java)
        if (forNew) {
            mServiceIntent.action = Constants.ACTION.STOP_PROCESS_FOR_NEW
        } else {
            mServiceIntent.action = Constants.ACTION.STOP_PROCESS
        }
        context.stopService(mServiceIntent)
    }

    fun forceKillAllProcessorServices(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = am.runningAppProcesses
        for (next in runningAppProcesses) {
            val processName = context.packageName + ":service"
            if (next.processName == processName) {
                android.os.Process.killProcess(next.pid)
                break
            }
        }
    }

    fun isSystemPackage(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    fun isProcessorServiceRunning(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = am.runningAppProcesses
        for (next in runningAppProcesses) {
            val processName = context.packageName + ":service"
            if (next.processName == processName) {
                return true
            }
        }
        return false
    }

    fun sourceExists(sourceDir: File): Boolean {
        if (sourceDir.exists() && sourceDir.isDirectory) {
            val infoFile = File(sourceDir.toString() + "/info.json")
            if (infoFile.exists() && infoFile.isFile) {
                val sourceInfo = SourceInfo.getSourceInfo(infoFile)
                if (sourceInfo != null) {
                    return true
                }
            }
        }
        return false
    }

    fun getSourceInfoFromSourcePath(sourceDir: File): SourceInfo? {
        if (sourceDir.isDirectory) {
            val infoFile = File(sourceDir.toString() + "/info.json")
            if (infoFile.exists() && infoFile.isFile) {
                return SourceInfo.getSourceInfo(infoFile)
            }
        }
        return null
    }

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
        val connectivityMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityMgr.activeNetworkInfo != null &&
                connectivityMgr.activeNetworkInfo.isAvailable &&
                connectivityMgr.activeNetworkInfo.isConnected) {
            status = true
        }
        return status
    }

}
