package com.njlabs.showjava.utils.logging

import android.util.Log
import com.google.firebase.crash.FirebaseCrash
import timber.log.Timber

class ProductionTree : Timber.Tree() {

    override fun log(priority: Int, tag: String, message: String?, t: Throwable?) {
        FirebaseCrash.log(message)
        if (t != null) {
            FirebaseCrash.report(t)
        }
        if (priority > Log.WARN) {
            FirebaseCrash.report(Throwable(message))
        }
    }
}
