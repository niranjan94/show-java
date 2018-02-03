package com.njlabs.showjava.activities.explorer.viewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import com.google.common.html.HtmlEscapers
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_code_viewer.*
import java.io.File
import android.content.res.AssetManager
import android.net.Uri
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

class CodeViewerActivity : BaseActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_code_viewer)
        val extras = intent.extras
        extras?.let {
            val file = File(it.getString("filePath"))
            val packageName = it.getString("packageName")

            Timber.d(file.absolutePath)
            Timber.d(packageName)

            supportActionBar?.title = file.name
            val subtitle = file.absolutePath.replace("${Environment.getExternalStorageDirectory()}/show-java/sources/$packageName/", "")
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
                override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse {
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