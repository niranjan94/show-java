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

package com.njlabs.showjava.fragments.explorer.viewer

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.njlabs.showjava.R
import com.njlabs.showjava.fragments.BaseFragment
import com.njlabs.showjava.utils.views.CodeView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_code_viewer.*
import java.io.File

class CodeViewerFragment: BaseFragment<ViewModel>(), CodeView.OnHighlightListener {
    override val layoutResource = R.layout.fragment_code_viewer

    override var isBlack: Boolean = true

    private var extensionTypeMap = hashMapOf(
        "txt" to "plaintext",
        "class" to "java",
        "yml" to "yaml",
        "md" to "markdown"
    )

    private lateinit var codeViewPreferences: SharedPreferences
    private lateinit var file: File

    private var wrapLine = false
    private var zoomable = true
    private var showLineNumbers = true
    private var invertColors = true

    override fun init(savedInstanceState: Bundle?) {

        file = File(arguments?.getString("filePath"))
        holder.setBackgroundColor(ContextCompat.getColor(context!!, R.color.grey_900))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            codeView.visibility = View.INVISIBLE
            codeLoadProgress.visibility = View.VISIBLE
        } else {
            codeView.visibility = View.VISIBLE
            codeLoadProgress.visibility = View.GONE
        }

        var language = file.extension
        extensionTypeMap[language]?.let {
            language = it
        }

        codeViewPreferences = containerActivity.getSharedPreferences(
            "code_view_prefs",
            Context.MODE_PRIVATE
        )

        wrapLine = codeViewPreferences.getBoolean("wrapLine", false)
        zoomable = codeViewPreferences.getBoolean("zoomable", true)
        showLineNumbers = codeViewPreferences.getBoolean("showLineNumbers", true)
        invertColors = codeViewPreferences.getBoolean("invertColors", true)

        disposables.add(
            loadFile(file)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn {
                    it.localizedMessage
                }
                .subscribe { fileContent ->
                    codeView.setCode(fileContent)
                        .setLanguage(language)
                        .setWrapLine(wrapLine)
                        .setDarkMode(invertColors)
                        .setFontSize(14F)
                        .setZoomEnabled(zoomable)
                        .setShowLineNumber(showLineNumbers)
                        .setOnHighlightListener(this)
                        .load()
                }
        )

    }

    private fun loadFile(fileToLoad: File): Observable<String> {
        return Observable.fromCallable {
            fileToLoad.readText()
        }
    }

    override fun onFontSizeChanged(sizeInPx: Int) {}
    override fun onLineClicked(lineNumber: Int, content: String) {}

    override fun onStartCodeHighlight() {
        containerActivity.runOnUiThread {
            codeView.visibility = View.INVISIBLE
            codeLoadProgress.visibility = View.VISIBLE
        }
    }

    override fun onFinishCodeHighlight() {
        containerActivity.runOnUiThread {
            codeView.visibility = View.VISIBLE
            codeLoadProgress.visibility = View.GONE
        }
    }

    override fun onSetToolbar(menu: Menu) {
        super.onSetToolbar(menu)
        setDecor()

        menu.findItem(R.id.wrap_text).isVisible = true
        menu.findItem(R.id.invert_colors).isVisible = true
        menu.findItem(R.id.zoomable).isVisible = true

        menu.findItem(R.id.wrap_text).isChecked = wrapLine
        menu.findItem(R.id.zoomable).isChecked = zoomable

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            menu.findItem(R.id.line_number).isVisible = true
            menu.findItem(R.id.line_number).isChecked = showLineNumbers
        }

        val packageName = arguments?.getString("name")

        containerActivity.supportActionBar?.title = file.name
        val subtitle = file.canonicalPath.replace(
            "${Environment.getExternalStorageDirectory()}/show-java/sources/$packageName/",
            ""
        )

        file.name.trim().let {
            if (it.equals("AndroidManifest.xml", true) || it.equals("info.json", true)) {
                containerActivity.setSubtitle(packageName)
            } else {
                containerActivity.setSubtitle(subtitle)
            }
        }
    }

    override fun onResetToolbar(menu: Menu) {
        super.onResetToolbar(menu)
        setDecor(false)
        menu.findItem(R.id.wrap_text).isVisible = false
        menu.findItem(R.id.invert_colors).isVisible = false
        menu.findItem(R.id.zoomable).isVisible = false
        menu.findItem(R.id.line_number).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.wrap_text -> {
                val newState = !item.isChecked
                codeViewPreferences.edit().putBoolean("wrapLine", newState).apply()
                codeView.setWrapLine(newState).apply()
                item.isChecked = newState
                return true
            }
            R.id.zoomable -> {
                val newState = !item.isChecked
                codeViewPreferences.edit().putBoolean("zoomable", newState).apply()
                codeView.setZoomEnabled(newState)
                item.isChecked = newState
                return true
            }
            R.id.line_number -> {
                val newState = !item.isChecked
                codeViewPreferences.edit().putBoolean("showLineNumbers", newState).apply()
                codeView.setShowLineNumber(newState).apply()
                item.isChecked = newState
                return true
            }
            R.id.invert_colors -> {
                invertColors = !invertColors
                codeViewPreferences.edit().putBoolean("invertColors", invertColors).apply()
                codeView.setDarkMode(invertColors).apply()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}