package com.njlabs.showjava.utils.logging

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

class ProductionTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
            return
        }

        if (message.isNotEmpty()) {
            Crashlytics.log("[$tag] $message")
        }

        if (t !== null) {
            Crashlytics.logException(t)
        }

        if (priority > Log.WARN) {
            Crashlytics.logException(Throwable("[$tag] $message"))
        }
    }
}
