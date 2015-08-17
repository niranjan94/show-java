package com.njlabs.showjava.modals;

import android.support.annotation.NonNull;

public class Item implements Comparable<Item>{
    private String name;
    private String data;
    private String date;
    private String path;
    private String image;
   
    public Item(String n,String d, String dt, String p, String img)
    {
            name = n;
            data = d;
            date = dt;
            path = p;
            image = img;           
    }
    public String getName()
    {
            return name;
    }
    public String getData()
    {
            return data;
    }
    public String getDate()
    {
            return date;
    }
    public String getPath()
    {
            return path;
    }
    public String getImage() {
            return image;
    }
    public int compareTo(@NonNull Item o) {
            if(this.name != null)
                    return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
            else
                    throw new IllegalArgumentException();
    }
}
