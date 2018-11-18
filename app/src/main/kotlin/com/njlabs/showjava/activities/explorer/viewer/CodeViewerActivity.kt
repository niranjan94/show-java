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

package com.njlabs.showjava.activities.explorer.viewer

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.common.html.HtmlEscapers
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_code_viewer.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class CodeViewerActivity : BaseActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_code_viewer)
        val extras = intent.extras
        extras?.let {
            val file = File(it.getString("filePath"))
            val packageName = it.getString("name")

            Timber.d(file.canonicalPath)
            Timber.d(packageName)

            supportActionBar?.title = file.name
            val subtitle = file.canonicalPath.replace(
                "${Environment.getExternalStorageDirectory()}/show-java/sources/$packageName/",
                ""
            )
            supportActionBar?.subtitle = subtitle
            if (file.name.trim().equals("AndroidManifest.xml", true)) {
                supportActionBar?.subtitle = packageName
            }

            val assetBaseUrl = "file:///android_asset/code_viewer/"
            val sourceCodeText = HtmlEscapers.htmlEscaper().escape(file.readText())

            codeView.settings.javaScriptEnabled = true
            codeView.settings.defaultTextEncodingName = "utf-8"
            codeView.webViewClient = object : WebViewClient() {
                @Suppress("OverridingDeprecatedMember")
                override fun shouldInterceptRequest(
                    view: WebView,
                    url: String
                ): WebResourceResponse {
                    val stream = inputStreamForAndroidResource(url)
                    @Suppress("DEPRECATION")
                    return if (stream != null) {
                        WebResourceResponse("text/javascript", "utf-8", stream)
                    } else super.shouldInterceptRequest(view, url)
                }

                private fun inputStreamForAndroidResource(_url: String): InputStream? {
                    var url = _url
                    if (url.contains(assetBaseUrl)) {
                        url = url.replaceFirst(assetBaseUrl.toRegex(), "")
                        try {
                            val assets = assets
                            val uri = Uri.parse(url)
                            return assets.open(uri.path, AssetManager.ACCESS_STREAMING)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    return null
                }
            }

            val data =
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                            <script src="run_prettify.js?skin=sons-of-obsidian"></script>
                        </head>
                        <body style="background-color: #000000;">
                            <pre class="prettyprint linenums"><code class="language-${file.extension}">${sourceCodeText.trim()}</code></pre>
                        </body>
                        </html>
                    """
            codeView.loadDataWithBaseURL(assetBaseUrl, data.trim(), "text/html", "UTF-8", null)

        }
    }
}