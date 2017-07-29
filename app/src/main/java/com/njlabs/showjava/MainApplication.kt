package com.njlabs.showjava

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import com.njlabs.showjava.utils.logging.ProductionTree
import uk.co.chrisjenx.calligraphy.CalligraphyConfig
import timber.log.Timber

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CalligraphyConfig.initDefault(CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/lato-light.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build())
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ProductionTree())
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}
