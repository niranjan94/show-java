package com.njlabs.showjava.utils;

import android.content.Context;

import xyz.codezero.apl.HC;
import xyz.codezero.apl.LPC;
import xyz.codezero.apl.TC;

public class Verify {
    public static boolean good(Context context) {
        return HC.good() && LPC.good(context) && TC.good(context);
    }
}
