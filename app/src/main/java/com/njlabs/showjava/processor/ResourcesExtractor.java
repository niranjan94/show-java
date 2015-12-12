package com.njlabs.showjava.processor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.crashlytics.android.Crashlytics;
import com.njlabs.showjava.utils.SourceInfo;

import net.dongliu.apk.parser.ApkParser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jadx.api.JadxDecompiler;

/**
 * Created by Niranjan on 30-05-2015.
 */
public class ResourcesExtractor extends ProcessServiceHelper {

    private final ApkParser apkParser;

    public ResourcesExtractor(ProcessService processService) {
        this.processService = processService;
        this.UIHandler = processService.UIHandler;
        this.packageFilePath = processService.packageFilePath;
        this.packageName = processService.packageName;
        this.exceptionHandler = processService.exceptionHandler;
        this.apkParser = processService.apkParser;
        this.sourceOutputDir = processService.sourceOutputDir;
        this.javaSourceOutputDir = processService.javaSourceOutputDir;
    }

    public void extract() {
        broadcastStatus("res");
        if(processService.decompilerToUse.equals("jadx")){
            extractResourcesWithJadx();
        } else {
            extractResourcesWithParser();
        }
    }

    private void extractResourcesWithJadx(){
        ThreadGroup group = new ThreadGroup("XML Extraction Group");
        Thread xmlExtractionThread = new Thread(group, new Runnable() {
            @Override
            public void run() {
                try {
                    File resDir = new File(sourceOutputDir);

                    JadxDecompiler jadx = new JadxDecompiler();
                    jadx.setOutputDir(resDir);
                    jadx.loadFile(new File(packageFilePath));
                    jadx.saveResources();

                    ZipFile zipFile = new ZipFile(packageFilePath);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry zipEntry = entries.nextElement();
                        if (!zipEntry.isDirectory() && (FilenameUtils.getExtension(zipEntry.getName()).equals("png") || FilenameUtils.getExtension(zipEntry.getName()).equals("jpg"))) {
                            broadcastStatus("progress_stream", zipEntry.getName());
                            writeFile(zipFile.getInputStream(zipEntry), zipEntry.getName());
                        }
                    }
                    zipFile.close();
                    saveIcon();
                    allDone();

                } catch (Exception | StackOverflowError e) {
                    processService.publishProgress("start_activity_with_error");
                }
            }
        }, "XML Extraction Thread", processService.STACK_SIZE);
        xmlExtractionThread.setPriority(Thread.MAX_PRIORITY);
        xmlExtractionThread.setUncaughtExceptionHandler(exceptionHandler);
        xmlExtractionThread.start();
    }

    private void extractResourcesWithParser(){
        ThreadGroup group = new ThreadGroup("XML Extraction Group");
        Thread xmlExtractionThread = new Thread(group, new Runnable() {
            @Override
            public void run() {
                try {
                    ZipFile zipFile = new ZipFile(packageFilePath);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry zipEntry = entries.nextElement();
                        if (!zipEntry.isDirectory() && !zipEntry.getName().equals("AndroidManifest.xml") && FilenameUtils.getExtension(zipEntry.getName()).equals("xml")) {
                            broadcastStatus("progress_stream", zipEntry.getName());
                            writeXML(zipEntry.getName());
                        } else if (!zipEntry.isDirectory() && (FilenameUtils.getExtension(zipEntry.getName()).equals("png") || FilenameUtils.getExtension(zipEntry.getName()).equals("jpg"))) {
                            broadcastStatus("progress_stream", zipEntry.getName());
                            writeFile(zipFile.getInputStream(zipEntry), zipEntry.getName());
                        }
                    }
                    zipFile.close();
                    writeManifest();
                    saveIcon();
                    allDone();
                } catch (Exception | StackOverflowError e) {
                    processService.publishProgress("start_activity_with_error");
                }
            }
        }, "XML Extraction Thread", processService.STACK_SIZE);
        xmlExtractionThread.setPriority(Thread.MAX_PRIORITY);
        xmlExtractionThread.setUncaughtExceptionHandler(exceptionHandler);
        xmlExtractionThread.start();
    }

    private void writeFile(InputStream fileStream, String path) {
        FileOutputStream outputStream = null;
        try {
            String fileFolderPath = sourceOutputDir + "/" + path.replace(FilenameUtils.getName(path), "");
            File fileFolder = new File(fileFolderPath);
            if (!fileFolder.exists() || !fileFolder.isDirectory()) {
                fileFolder.mkdirs();
            }

            outputStream = new FileOutputStream(new File(fileFolderPath + FilenameUtils.getName(path)));

            int read;
            byte[] bytes = new byte[1024];

            while ((read = fileStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

        } catch (IOException e) {
            Crashlytics.logException(e);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    Crashlytics.logException(e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Crashlytics.logException(e);
                }
            }
        }
    }

    private void writeXML(String path) {
        try {
            String xml = apkParser.transBinaryXml(path);
            String fileFolderPath = sourceOutputDir + "/" + path.replace(FilenameUtils.getName(path), "");
            File fileFolder = new File(fileFolderPath);
            if (!fileFolder.exists() || !fileFolder.isDirectory()) {
                fileFolder.mkdirs();
            }
            FileUtils.writeStringToFile(new File(fileFolderPath + FilenameUtils.getName(path)), xml);
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
    }

    private void allDone() {
        SourceInfo.setXmlSourceStatus(processService, true);
        processService.publishProgress("start_activity");
    }

    private void writeManifest() {
        try {
            String manifestXml = apkParser.getManifestXml();
            FileUtils.writeStringToFile(new File(sourceOutputDir + "/AndroidManifest.xml"), manifestXml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveIcon() {
        try {
            byte[] icon = apkParser.getIconFile().getData();
            Bitmap bitmap = BitmapFactory.decodeByteArray(icon, 0, icon.length);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(sourceOutputDir + "/icon.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                Crashlytics.logException(e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    Crashlytics.logException(e);
                }
            }
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
    }
}
