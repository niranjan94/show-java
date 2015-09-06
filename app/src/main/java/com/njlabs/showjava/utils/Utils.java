package com.njlabs.showjava.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.njlabs.showjava.Constants;
import com.njlabs.showjava.processor.ProcessService;
import com.njlabs.showjava.utils.logging.Ln;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utils {

    public static void killAllProcessorServices(Context context) {

        Intent mServiceIntent = new Intent(context, ProcessService.class);
        mServiceIntent.setAction(Constants.ACTION.STOP_PROCESS);
        context.startService(mServiceIntent);

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

    public static boolean isProcessorServiceRunning(Context context) {
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

    public static boolean sourceExists(File sourceDir) {
        if (sourceDir.exists() && sourceDir.isDirectory()) {
            File infoFile = new File(sourceDir + "/info.json");
            if (infoFile.exists() && infoFile.isFile()) {
                SourceInfo sourceInfo = SourceInfo.getSourceInfo(infoFile);
                if (sourceInfo != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static SourceInfo getSourceInfoFromSourcePath(File sourceDir) {
        if (sourceDir.isDirectory()) {
            File infoFile = new File(sourceDir + "/info.json");
            if (infoFile.exists() && infoFile.isFile()) {
                return SourceInfo.getSourceInfo(infoFile);
            }
        }
        return null;
    }

    public static long getFolderSize(File f) {
        long size = 0;
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                size += getFolderSize(file);
            }
        } else {
            size = f.length();
        }
        return size;
    }

    public static File zipDir(File dir, String packageId){

        File zipIntoDir = new File(Environment.getExternalStorageDirectory() + "/ShowJava/archives/");

        if(!zipIntoDir.exists() || !zipIntoDir.isDirectory()){
            zipIntoDir.mkdirs();
        }

        File zipFile = new File(zipIntoDir, packageId+".zip");

        if(zipFile.exists()) {
            zipFile.delete();
        }

        try {

            ZipOutputStream zip;
            FileOutputStream fileWriter;
            fileWriter = new FileOutputStream(zipFile);
            zip = new ZipOutputStream(fileWriter);
            addFolderToZip("", dir.toString(), zip);
            zip.flush();
            zip.close();

        } catch (Exception e) {
            Ln.e(e);
        }

        return zipFile;
    }

    static private void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
        }
    }

    static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFolder);
        for (String fileName : folder.list()) {
            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            } else {
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
            }
        }
    }
}
