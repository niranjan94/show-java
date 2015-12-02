package com.njlabs.showjava.utils;

import com.njlabs.showjava.processor.ProcessService;
import com.njlabs.showjava.utils.logging.Ln;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by Niranjan on 30-05-2015.
 */
public class SourceInfo {

    private String packageLabel;
    private String packageName;
    private final boolean hasSource;

    private SourceInfo(String packageLabel, String packageName) {
        this.packageLabel = packageLabel;
        this.packageName = packageName;
        this.hasSource = true;
    }

    public SourceInfo(boolean hasSource) {
        this.hasSource = hasSource;
    }

    public static void initialise(ProcessService processService) {
        try {
            JSONObject json = new JSONObject();
            json.put("package_label", processService.packageLabel);
            json.put("package_name", processService.packageName);
            json.put("has_java_sources", false);
            json.put("has_xml_sources", false);
            String filePath = processService.sourceOutputDir + "/info.json";
            FileUtils.writeStringToFile(new File(filePath), json.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static void setjavaSourceStatus(ProcessService processService, Boolean status) {
        try {
            File infoFile = new File(processService.sourceOutputDir + "/info.json");
            JSONObject json = new JSONObject(FileUtils.readFileToString(infoFile));
            json.put("has_java_sources", status);
            FileUtils.writeStringToFile(infoFile, json.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static void setXmlSourceStatus(ProcessService processService, Boolean status) {
        try {
            File infoFile = new File(processService.sourceOutputDir + "/info.json");
            JSONObject json = new JSONObject(FileUtils.readFileToString(infoFile));
            json.put("has_xml_sources", status);
            FileUtils.writeStringToFile(infoFile, json.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static void delete(ProcessService processService) {
        try {
            File infoFile = new File(processService.sourceOutputDir + "/info.json");
            infoFile.delete();
        } catch (Exception e) {
            Ln.e(e);
        }
    }

    public static String getLabel(String directory) {
        try {
            File infoFile = new File(directory + "/info.json");
            JSONObject json = new JSONObject(FileUtils.readFileToString(infoFile));
            return json.getString("package_label");
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    public static SourceInfo getSourceInfo(File infoFile) {
        try {
            JSONObject json = new JSONObject(FileUtils.readFileToString(infoFile));
            if (json.getBoolean("has_java_sources") || json.getBoolean("has_xml_sources")) {
                return new SourceInfo(json.getString("package_label"), json.getString("package_name"));
            } else {
                return null;
            }
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    public String getPackageLabel() {
        return packageLabel;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean hasSource() {
        return hasSource;
    }
}
