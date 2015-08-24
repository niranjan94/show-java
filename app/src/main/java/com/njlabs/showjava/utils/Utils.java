package com.njlabs.showjava.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.io.File;
import java.util.List;

public class Utils {

    public static void killAllProcessorServices(Context context){
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo next : runningAppProcesses) {
            String processName = context.getPackageName() + ":service";
            if (next.processName.equals(processName)) {
                android.os.Process.killProcess(next.pid);
                break;
            }
        }
    }

    public static boolean isProcessorServiceRunning(Context context){
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo next : runningAppProcesses) {
            String processName = context.getPackageName() + ":service";
            if (next.processName.equals(processName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean sourceExists(File sourceDir){
        if (sourceDir.exists()&&sourceDir.isDirectory()) {
            File infoFile = new File(sourceDir + "/info.json");
            if(infoFile.exists() && infoFile.isFile()){
                SourceInfo sourceInfo = SourceInfo.getSourceInfo(infoFile);
                if(sourceInfo != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static SourceInfo getSourceInfoFromSourcePath(File sourceDir){
        if (sourceDir.isDirectory()) {
            File infoFile = new File(sourceDir + "/info.json");
            if(infoFile.exists() && infoFile.isFile()){
                return SourceInfo.getSourceInfo(infoFile);
            }
        }
        return null;
    }
}
