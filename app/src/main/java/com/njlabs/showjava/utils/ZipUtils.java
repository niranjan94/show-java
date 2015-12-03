package com.njlabs.showjava.utils;

import android.os.Environment;

import com.njlabs.showjava.utils.logging.Ln;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

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

    public static void unzip(File zipFile, File targetDirectory) throws IOException {
        unzip(zipFile, targetDirectory, System.out);
    }

    public static void unzip(File zipFile, File targetDirectory, PrintStream printStream) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                printStream.println(ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()){
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                }

                if (ze.isDirectory()) {
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
                long time = ze.getTime();
                if (time > 0) {
                    file.setLastModified(time);
                }
            }
        } finally {
            zis.close();
        }
    }
}
