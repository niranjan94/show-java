package com.njlabs.showjava.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.njlabs.showjava.Constants
import com.njlabs.showjava.workers.ProcessorService
import java.io.File

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
