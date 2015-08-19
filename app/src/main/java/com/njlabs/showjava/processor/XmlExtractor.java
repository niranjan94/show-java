package com.njlabs.showjava.processor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.njlabs.showjava.Constants;
import com.njlabs.showjava.utils.SourceInfo;

import net.dongliu.apk.parser.ApkParser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Niranjan on 30-05-2015.
 */
public class XmlExtractor extends ProcessServiceHelper {

    ApkParser apkParser;

    public XmlExtractor(ProcessService processService) {
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

        broadcastStatus("xml");
        ThreadGroup group = new ThreadGroup("XML Extraction Group");
        Thread xmlExtractionThread = new Thread(group, new Runnable(){
            @Override
            public void run(){
                try {
                    ZipFile zipFile = new ZipFile(packageFilePath);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry zipEntry = entries.nextElement();
                        if(!zipEntry.isDirectory()&&!zipEntry.getName().equals("AndroidManifest.xml")&&FilenameUtils.getExtension(zipEntry.getName()).equals("xml")){
                            broadcastStatus("progress_stream",zipEntry.getName());
                            writeXML(zipEntry.getName());
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
        },"XML Extraction Thread", Constants.STACK_SIZE);
        xmlExtractionThread.setPriority(Thread.MAX_PRIORITY);
        xmlExtractionThread.setUncaughtExceptionHandler(exceptionHandler);
        xmlExtractionThread.start();

    }

    private void writeXML(String path) {
        try {
            String xml = apkParser.transBinaryXml(path);
            String fileFolderPath = sourceOutputDir + "/" + path.replace(FilenameUtils.getName(path), "");
            File fileFolder = new File(fileFolderPath);
            if (!fileFolder.isDirectory()) {
                fileFolder.mkdirs();
            }
            FileUtils.writeStringToFile(new File(fileFolderPath + FilenameUtils.getName(path)), xml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void allDone(){
        SourceInfo.setXmlSourceStatus(processService, true);
        processService.publishProgress("start_activity");
    }

    private void writeManifest(){
        try {
            String manifestXml = apkParser.getManifestXml();
            FileUtils.writeStringToFile(new File(sourceOutputDir + "/AndroidManifest.xml"), manifestXml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveIcon(){
        try {
            byte[] icon = apkParser.getIconFile().getData();
            Bitmap bitmap = BitmapFactory.decodeByteArray(icon, 0, icon.length);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(sourceOutputDir + "/icon.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
