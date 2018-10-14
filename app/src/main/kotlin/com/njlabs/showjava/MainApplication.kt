package com.njlabs.showjava

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.google.android.gms.ads.MobileAds
import com.njlabs.showjava.utils.logging.ProductionTree
import timber.log.Timber
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        /**

        if (LeakCanary.isInAnalyzerProcess(this)) {
        return
        }
        LeakCanary.install(this)

         **/

        CalligraphyConfig.initDefault(
            CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/lato-light.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        )

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ProductionTree())
        }

        MobileAds.initialize(this, getString(R.string.admobAppId))
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
