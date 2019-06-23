/*
 * Original code borrowed from tiagohm/CodeView project
 * https://github.com/tiagohm/CodeView/blob/053a827e629089ae8fdd4787d42f2257a856a4c6/library/src/main/java/br/tiagohm/codeview/CodeView.java
 *
 * Copyright (c) 2016-2017 Tiago Melo
 *
 * Originally licensed under the MIT Licence, now modified and re-licenced under GPL 3.0
 */

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

package com.njlabs.showjava.utils.views

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.Html
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import com.njlabs.showjava.BuildConfig
import java.util.*
import java.util.regex.Pattern

class CodeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var code = ""
    private var escapeCode: String? = null
    private var language: String? = null
    private var fontSize = 16f
    private var wrapLine = false
    private var darkMode = true
    private var onHighlightListener: OnHighlightListener? = null
    private var pinchDetector: ScaleGestureDetector? = null
    private var zoomEnabled = false
    private var showLineNumber = false
    private var startLineNumber = 1
    private var lineCount = 0
    private var highlightLineNumber = -1

    interface OnHighlightListener {
        fun onStartCodeHighlight()
        fun onFinishCodeHighlight()
        fun onFontSizeChanged(sizeInPx: Int)
        fun onLineClicked(lineNumber: Int, content: String)
    }

    init { init(context) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (zoomEnabled) {
            pinchDetector!!.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun init(context: Context) {
        pinchDetector = ScaleGestureDetector(context, PinchListener())
        webChromeClient = WebChromeClient()
        settings.javaScriptEnabled = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        settings.loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true)
        }
    }

    @SuppressLint("AddJavascriptInterface")
    fun setOnHighlightListener(listener: OnHighlightListener?): CodeView {
        if (listener != null) {
            if (onHighlightListener !== listener) {
                onHighlightListener = listener
                /*
                    For applications built for API levels below 17, WebView#addJavascriptInterface presents a security hazard as
                    JavaScript on the target web page has the ability to use reflection to access the injected object's public fields
                    and thus manipulate the host application in unintended ways.

                    Issue id: AddJavascriptInterface

                    Ref: https://labs.mwrinfosecurity.com/blog/2013/09/24/webview-addjavascriptinterface-remote-code-execution/
                 */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    addJavascriptInterface(object : Any() {
                        @JavascriptInterface
                        fun onStartCodeHighlight() {
                            onHighlightListener?.onStartCodeHighlight()
                        }
                        @JavascriptInterface
                        fun onFinishCodeHighlight() {
                            onHighlightListener?.onFinishCodeHighlight()
                        }
                        @JavascriptInterface
                        fun onLineClicked(lineNumber: Int, content: String) {
                            onHighlightListener?.onLineClicked(lineNumber, content)
                        }
                    }, "android")
                }
            }
        } else {
            removeJavascriptInterface("android")
        }
        return this
    }

    fun setFontSize(fontSize: Float): CodeView {
        if (fontSize < 8) this.fontSize = 8f
        else this.fontSize = fontSize
        if (onHighlightListener != null) {
            onHighlightListener!!.onFontSizeChanged(this.fontSize.toInt())
        }
        return this
    }

    fun setCode(code: String?): CodeView {
        if (code == null) this.code = ""
        else this.code = code + "\n"
        this.escapeCode = Html.escapeHtml(this.code)
        return this
    }

    fun setLanguage(language: String): CodeView {
        this.language = language
        return this
    }

    fun setWrapLine(wrapLine: Boolean): CodeView {
        this.wrapLine = wrapLine
        return this
    }

    fun setZoomEnabled(zoomEnabled: Boolean): CodeView {
        this.zoomEnabled = zoomEnabled
        return this
    }

    fun setDarkMode(darkMode: Boolean): CodeView {
        this.darkMode = darkMode
        return this
    }

    fun setShowLineNumber(showLineNumber: Boolean): CodeView {
        this.showLineNumber = showLineNumber
        return this
    }

    fun setStartLineNumber(startLineNumber: Int): CodeView {
        if (startLineNumber < 0) this.startLineNumber = 1
        else this.startLineNumber = startLineNumber
        return this
    }

    fun toggleLineNumber() {
        showLineNumber = !showLineNumber
        showHideLineNumber(showLineNumber)
    }

    fun load() { reload() }

    override fun reload() {
        loadDataWithBaseURL(
            "",
            toHtml(),
            "text/html",
            "UTF-8",
            ""
        )
    }

    private fun getStyleAndClass(): Pair<String, String> {
        var bodyClass = "dark"
        var style = "androidstudio"
        if (!darkMode) {
            bodyClass = "light"
            style = "github-gist"
        }
        if (wrapLine) {
            bodyClass += " wrapped"
        }
        return Pair("file:///android_asset/codeview/highlightjs/styles/$style.css", bodyClass)
    }

    fun apply() {
        val (styleUri, bodyClass) = getStyleAndClass()
        executeJavaScript("updateStyleAndClasses('$styleUri', '$bodyClass')")
        showHideLineNumber(showLineNumber)
    }

    private fun toHtml(): String {
        val (styleUri, bodyClass) = getStyleAndClass()
        return """
<!DOCTYPE html>
<html>
<head>
  <link id='stylesheet' rel='stylesheet' href='$styleUri'/>
  <link rel='stylesheet' href='file:///android_asset/codeview/styles.css'/>
  <style type="text/css">
    body {
      font-size: ${fontSize.toInt()}px;
    }
  </style>
  <script src='file:///android_asset/codeview/highlightjs/highlight.js'></script>
  <script src='file:///android_asset/codeview/script.js'></script>
</head>
<body id='body' class='$bodyClass'>
<pre><code class='$language' id='code-holder'>${insertLineNumber(escapeCode)}</code></pre>
<script type="text/javascript">
highlightCode();
fillLineNumbers();
showHideLineNumber($showLineNumber)
highlightLineNumber($highlightLineNumber)
</script>
</body>
</html>
"""
    }

    private fun insertLineNumber(code: String?): String {
        val m = Pattern.compile("(.*?)&#10;").matcher(code ?: "")
        val sb = StringBuffer()
        var pos = startLineNumber
        lineCount = 0
        while (m.find()) {
            m.appendReplacement(
                sb,
                String.format(
                    Locale.ENGLISH,
                    "<tr><td line='%d' class='hljs-number ln'></td><td line='%d' onclick='android.onLineClicked(%d, this.textContent);' class='line'>$1 </td></tr>&#10;",
                    pos, pos, pos
                )
            )
            pos++
            lineCount++
        }
        return "<table>\n" + sb.toString().trim { it <= ' ' } + "</table>\n"
    }

    private inner class PinchListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private var fontSize: Float = 0.toFloat()
        private var oldFontSize: Int = 0

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            fontSize = this@CodeView.fontSize
            oldFontSize = fontSize.toInt()
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            this@CodeView.fontSize = fontSize
            super.onScaleEnd(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            fontSize = this@CodeView.fontSize * detector.scaleFactor
            if (fontSize >= 8) {
                changeFontSize(fontSize.toInt())
                if (oldFontSize != fontSize.toInt()) {
                    onHighlightListener?.onFontSizeChanged(fontSize.toInt())
                }
                oldFontSize = fontSize.toInt()
            } else {
                fontSize = 8f
            }
            return false
        }
    }

    private fun executeJavaScript(js: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript("javascript:$js", null)
        } else {
            loadUrl("javascript:$js")
        }
    }

    private fun changeFontSize(sizeInPx: Int) {
        executeJavaScript("changeFontSize($sizeInPx)")
    }

    private fun showHideLineNumber(show: Boolean) {
        executeJavaScript("showHideLineNumber($show)")
    }

    fun highlightLineNumber(lineNumber: Int) {
        this.highlightLineNumber = lineNumber
        executeJavaScript("highlightLineNumber($lineNumber)")
    }
}