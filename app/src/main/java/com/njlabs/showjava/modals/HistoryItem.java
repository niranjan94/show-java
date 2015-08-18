package com.njlabs.showjava.modals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ollie.Model;
import ollie.annotation.Column;
import ollie.annotation.Table;

@Table("notes")
public class HistoryItem extends Model{

    @Column("package_id")
    public String packageID;

    @Column("package_label")
    public String packageLabel;

    @Column("date")
    public String date;

    public HistoryItem() {

    }

    public HistoryItem(String packageID, String packageLabel) {
        this.packageID = packageID;
        this.packageLabel = packageLabel;
        this.date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(new Date());
    }

    public HistoryItem(String packageID, String packageLabel, String date) {
        this.packageID = packageID;
        this.packageLabel = packageLabel;
        this.date = date;
    }

    public HistoryItem(String packageID) {
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
