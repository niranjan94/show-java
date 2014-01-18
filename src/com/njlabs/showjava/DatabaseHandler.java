package com.njlabs.showjava;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {
	
	// Database version
	private static final int DATABASE_VERSION = 1;
	
	// Database Name
	private static final String DATABASE_NAME = "PushManager";
	// History table name
	private static final String TABLE_HISTORY = "decompile_history";
	
	// History Table Columns names
	private static final String KEY_ID = "id";
	private static final String KEY_PACKAGE_ID = "package_id";
	private static final String KEY_PACKAGE_NAME = "package_name";
	private static final String KEY_DATETIME = "datetime";
	
	public DatabaseHandler(Context context) 
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	////
	//// Creating Tables
	////
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_PACKAGE_ID + " TEXT," + KEY_PACKAGE_NAME + " TEXT," + KEY_DATETIME + " TEXT)";
		db.execSQL(CREATE_HISTORY_TABLE);
	}
	////
	//// Upgrading database
	////
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
		// Create tables again
		onCreate(db);
	}
	////
    //// Adding new History Item
	////
	public void addHistoryItem(DecompileHistoryItem announcement) 
	{
	    SQLiteDatabase db = this.getWritableDatabase();
	 
	    ContentValues values = new ContentValues();
	    values.put(KEY_PACKAGE_ID, announcement.getPackageID()); // Title
	    values.put(KEY_PACKAGE_NAME, announcement.getPackageName()); // Alert
	    values.put(KEY_DATETIME, announcement.getDatetime()); // Datetime
	    
	 
	    // Inserting Row
	    db.insert(TABLE_HISTORY, null, values);
	    db.close(); // Closing database connection
	}
	////
	//// Getting a single history item
	///
	public DecompileHistoryItem getHistoryItem(int id) {
	    SQLiteDatabase db = this.getReadableDatabase();
	 
	    Cursor cursor = db.query(TABLE_HISTORY, new String[] { KEY_ID,
	            KEY_PACKAGE_ID, KEY_PACKAGE_NAME , KEY_DATETIME}, KEY_ID + "=?",
	            new String[] { String.valueOf(id) }, null, null, null, null);
	    if (cursor != null)
	        cursor.moveToFirst();
	 
	    DecompileHistoryItem singleHistoryItem = new DecompileHistoryItem(Integer.parseInt(cursor.getString(0)),
	            cursor.getString(1), cursor.getString(2),cursor.getString(3));
	    // return Announcement
	    return singleHistoryItem;
	}
    ////
	//// Getting All normal Announcements
	////
	public List<DecompileHistoryItem> getAllHistoryItems() {
	    List<DecompileHistoryItem> allHistoryItemList = new ArrayList<DecompileHistoryItem>();
	    // Select All Query
	    String selectQuery = "SELECT  * FROM " + TABLE_HISTORY+" WHERE status<>'ace_club' ORDER BY id DESC";
	 
	    SQLiteDatabase db = this.getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    // looping through all rows and adding to list
	    if (cursor.moveToFirst()) {
	        do {
	        	DecompileHistoryItem singleHistoryItem = new DecompileHistoryItem();
	        	singleHistoryItem.setID(Integer.parseInt(cursor.getString(0)));
	        	singleHistoryItem.setPackageID(cursor.getString(1));
	        	singleHistoryItem.setPackageName(cursor.getString(2));
	        	singleHistoryItem.setDatetime(cursor.getString(3));
	            // Adding Announcement to list
	            allHistoryItemList.add(singleHistoryItem);
	        } while (cursor.moveToNext());
	    }

	    return allHistoryItemList;
	}
    
	public int getHistoryItemCount() {
        String countQuery = "SELECT  * FROM " + TABLE_HISTORY;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();
 
        // return count
        return cursor.getCount();
    }

	
	public int updateHistoryItem(DecompileHistoryItem announcement) {
	    SQLiteDatabase db = this.getWritableDatabase();
	 
	    ContentValues values = new ContentValues();
	    values.put(KEY_PACKAGE_ID, announcement.getPackageID());
	    values.put(KEY_PACKAGE_NAME, announcement.getPackageName());
	    values.put(KEY_DATETIME, announcement.getDatetime());
	 
	    // updating row
	    return db.update(TABLE_HISTORY, values, KEY_ID + " = ?",
	            new String[] { String.valueOf(announcement.getID()) });
	}

	public void deleteHistoryItem(DecompileHistoryItem announcement) {
	    SQLiteDatabase db = this.getWritableDatabase();
	    db.delete(TABLE_HISTORY, KEY_ID + " = ?",
	            new String[] { String.valueOf(announcement.getID()) });
	    db.close();
	}
	
}
