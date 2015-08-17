package com.njlabs.showjava.modals;

import com.orm.SugarRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("unused")
public class DecompileHistoryItem extends SugarRecord<DecompileHistoryItem>{

    String packageID;
    String packageLabel;
    String date;

    public DecompileHistoryItem() {
    }

    public DecompileHistoryItem(String packageID, String packageLabel) {
        this.packageID = packageID;
        this.packageLabel = packageLabel;
        this.date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(new Date());
    }

    public DecompileHistoryItem(String packageID, String packageLabel, String date) {
        this.packageID = packageID;
        this.packageLabel = packageLabel;
        this.date = date;
    }

    public DecompileHistoryItem(String packageID) {
        this.packageID = packageID;
        this.packageLabel = packageID;
        this.date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(new Date());
    }

    public String getPackageID() {
        return packageID;
    }

    public void setPackageID(String packageID) {
        this.packageID = packageID;
    }

    public String getPackageLabel() {
        return packageLabel;
    }

    public void setPackageLabel(String packageLabel) {
        this.packageLabel = packageLabel;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}