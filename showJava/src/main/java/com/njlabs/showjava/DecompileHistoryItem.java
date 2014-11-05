package com.njlabs.showjava;

public class DecompileHistoryItem {
    
    //private variables
    int _id;
    String _packageid;
    String _packagename;
    String _datetime;
     
    // Empty constructor
    public DecompileHistoryItem(){
         
    }
    // constructor
    public DecompileHistoryItem(int id, String packageid, String packagename, String datetime){
        this._id = id;
        this._packageid = packageid;
        this._packagename = packagename;
        this._datetime = datetime;
    }
     
    // constructor
    public DecompileHistoryItem(String packageid, String packagename, String datetime){
        this._packageid = packageid;
        this._packagename = packagename;
        this._datetime = datetime;
    }
    // getting ID
    public int getID(){
        return this._id;
    }
     
    // setting id
    public void setID(int id){
        this._id = id;
    }
     
    // getting title
    public String getPackageID(){
        return this._packageid;
    }
     
    // setting name
    public void setPackageID(String title){
        this._packageid = title;
    }
     
    // getting alert
    public String getPackageName(){
        return this._packagename;
    }
     
    // setting alert
    public void setPackageName(String alert){
        this._packagename = alert;
    }
    
 // getting datetime
    public String getDatetime(){
        return this._datetime;
    }
     
    // setting datetime
    public void setDatetime(String datetime){
        this._datetime = datetime;
    }
    
}