/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2019 Niranjan Rajendran
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.njlabs.showjava

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.google.firebase.iid.FirebaseInstanceId
import com.njlabs.showjava.utils.Ads
import com.njlabs.showjava.utils.StethoUtils
import com.njlabs.showjava.utils.UserPreferences
import com.njlabs.showjava.utils.ktx.Storage
import com.njlabs.showjava.utils.logging.ProductionTree
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


class MainApplication : MultiDexApplication() {

    lateinit var instanceId: String

    override fun onCreate() {
        super.onCreate()

        Storage.init(this)

        instanceId = FirebaseInstanceId.getInstance().id

        PreferenceManager.setDefaultValues(
            applicationContext,
            UserPreferences.NAME,
            Context.MODE_PRIVATE,
            R.xml.preferences,
            false
        )

        val preferences =
            UserPreferences(
                applicationContext.getSharedPreferences(
                    UserPreferences.NAME,
                    Context.MODE_PRIVATE
                )
            )

        AppCompatDelegate.setDefaultNightMode(
            if (preferences.darkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        Ads(this).init()

        val crashlyticsCore = CrashlyticsCore.Builder()
            .disabled(BuildConfig.DEBUG)
            .build()

        Fabric.with(this, Crashlytics.Builder().core(crashlyticsCore).build())

        Crashlytics.setUserIdentifier(instanceId)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            StethoUtils.install(this)
        } else {
            Timber.plant(ProductionTree())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            GlobalScope.launch {
                cleanStaleNotifications()
            }
        }
    }

    /**
     * Clean any stale notifications not linked to any decompiler process
     */
    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun cleanStaleNotifications() {
        withContext(Dispatchers.IO) {
            val manager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val workManager = WorkManager.getInstance(this@MainApplication)
            manager.activeNotifications.forEach { notification ->
                val status = workManager.getWorkInfosForUniqueWorkLiveData(notification.tag)
                    .value?.any { it.state.isFinished }
                if (status == null || status == true) {
                    manager.cancel(notification.tag, notification.id)
                }
            }
        }
    }
}
