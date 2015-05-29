/*
 * Copyright (c) 2014. Niranjan Rajendran <niranjan94@yahoo.com>
 */

package com.njlabs.showjava.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class FontsOverride {

    public static void with(Context context) {
        final Typeface regular = Typeface.createFromAsset(context.getAssets(),"roboto_light.ttf");
        replaceFont("DEFAULT", regular);
        replaceFont("SANS_SERIF", regular);
    }

    protected static void replaceFont(String staticTypefaceFieldName,  final Typeface newTypeface) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, Typeface> newMap = new HashMap<String, Typeface>();
            newMap.put("sans-serif", newTypeface);
            try {
                final Field staticField = Typeface.class
                        .getDeclaredField("sSystemFontMap");
                staticField.setAccessible(true);
                staticField.set(null, newMap);
            } catch (Exception e) {
                Log.e("com.njlabs.showjava",e.toString());
            }
        } else {
            try {
                final Field staticField = Typeface.class
                        .getDeclaredField(staticTypefaceFieldName);
                staticField.setAccessible(true);
                staticField.set(null, newTypeface);
            } catch (Exception e) {
                Log.e("com.njlabs.showjava", e.toString());
            }
        }
    }
}