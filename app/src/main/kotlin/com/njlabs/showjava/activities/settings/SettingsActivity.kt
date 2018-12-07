/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2018 Niranjan Rajendran
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

package com.njlabs.showjava.activities.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.ListPreference
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class SettingsActivity : BaseActivity() {

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, PrefsFragment())
            .commit()
    }

    class PrefsFragment : PreferenceFragmentCompat() {

        private lateinit var settingsHandler: SettingsHandler
        private lateinit var deleteSubscription: Disposable

        private var progressBarView: View? = null
        private var containerView: View? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = Constants.USER_PREFERENCES_NAME
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE

            progressBarView = activity?.findViewById(R.id.progressBar)
            containerView = activity?.findViewById(R.id.container)

            settingsHandler = SettingsHandler(context!!)

            setPreferencesFromResource(R.xml.preferences, rootKey)

            findPreference("clearSourceHistory").setOnPreferenceClickListener {
                AlertDialog.Builder(context!!)
                    .setTitle(getString(R.string.deleteSourceHistory))
                    .setMessage(getString(R.string.deleteSourceHistoryConfirm))
                    .setIcon(R.drawable.ic_error_outline_black)
                    .setPositiveButton(
                        android.R.string.yes
                    ) { _, _ ->
                        deleteSources()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
                true
            }

            // Remove decompiler selection.
            // bindPreferenceSummaryToValue(findPreference("decompiler"))
            bindPreferenceSummaryToValue(findPreference("chunkSize"))
            bindPreferenceSummaryToValue(findPreference("maxAttempts"))
            bindPreferenceSummaryToValue(findPreference("memoryThreshold"))

            findPreference("darkMode").setOnPreferenceChangeListener { _, newValue ->
                AppCompatDelegate.setDefaultNightMode(
                    if (newValue as Boolean)
                        AppCompatDelegate.MODE_NIGHT_YES
                    else
                        AppCompatDelegate.MODE_NIGHT_NO
                )

                Toast.makeText(context, R.string.themeChangeCloseInfo, Toast.LENGTH_SHORT).show()

                activity?.let {
                    it.startActivity(Intent(it, SettingsActivity::class.java))
                    it.finish()
                }

                true
            }
        }

        private fun deleteSources() {
            progressBarView?.visibility = View.VISIBLE
            containerView?.visibility = View.INVISIBLE
            deleteSubscription = settingsHandler.deleteHistory()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Toast.makeText(
                        context,
                        getString(R.string.sourceHistoryDeleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    progressBarView?.visibility = View.GONE
                    containerView?.visibility = View.VISIBLE
                }
        }

        override fun onDestroy() {
            super.onDestroy()
            if (::deleteSubscription.isInitialized && !deleteSubscription.isDisposed) {
                deleteSubscription.dispose()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    companion object {
        private val sBindPreferenceSummaryToValueListener =
            Preference.OnPreferenceChangeListener { preference, value ->
                var stringValue = value.toString()
                if (preference is ListPreference) {
                    val index = preference.findIndexOfValue(stringValue)
                    stringValue = if (index >= 0) preference.entries[index].toString() else ""
                }

                if (stringValue != "") {
                    val decompilersValues = preference.context.resources.getStringArray(R.array.decompilersValues)
                    val index = decompilersValues.indexOf(stringValue)
                    if (index >= 0) {
                        val decompilers = preference.context.resources.getStringArray(R.array.decompilers)
                        preference.summary = decompilers[index]
                    } else {
                        preference.summary = stringValue
                    }
                    return@OnPreferenceChangeListener true
                }
                preference.summary = null
                true
            }

        private fun bindPreferenceSummaryToValue(preference: Preference) {
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                preference.context.getSharedPreferences(Constants.USER_PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .getString(preference.key, "")!!
            )
        }
    }
}