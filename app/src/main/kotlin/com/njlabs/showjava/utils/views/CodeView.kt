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

package com.njlabs.showjava.utils.views


import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import timber.log.Timber
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
    private var onHighlightListener: OnHighlightListener? = null
    private var pinchDetector: ScaleGestureDetector? = null
    private var zoomEnabled = false
    private var showLineNumber = false
    private var startLineNumber = 1
    var lineCount = 0
    var highlightLineNumber = -1

    interface OnHighlightListener {
        fun onStartCodeHighlight()
        fun onFinishCodeHighlight()
        fun onFontSizeChanged(sizeInPx: Int)
        fun onLineClicked(lineNumber: Int, content: String)
    }

    init {
        init(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isZoomEnabled()) {
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
        settings.loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    /**
     * Define um listener.
     */
    @SuppressLint("AddJavascriptInterface")
    fun setOnHighlightListener(listener: OnHighlightListener?): CodeView {
        if (listener != null) {
            if (onHighlightListener !== listener) {
                onHighlightListener = listener
                addJavascriptInterface(object : Any() {
                    @JavascriptInterface
                    fun onStartCodeHighlight() {
                        if (onHighlightListener != null) {
                            onHighlightListener!!.onStartCodeHighlight()
                        }
                    }

                    @JavascriptInterface
                    fun onFinishCodeHighlight() {
                        if (onHighlightListener != null) {
                            Handler(Looper.getMainLooper()).post {
                                fillLineNumbers()
                                showHideLineNumber(isShowLineNumber())
                                highlightLineNumber(highlightLineNumber)
                            }
                            onHighlightListener!!.onFinishCodeHighlight()
                        }
                    }

                    @JavascriptInterface
                    fun logText(text: String) {
                        Timber.d("logText: %s", text)
                    }

                    @JavascriptInterface
                    fun onLineClicked(lineNumber: Int, content: String) {
                        if (onHighlightListener != null) {
                            onHighlightListener!!.onLineClicked(lineNumber, content)
                        }
                    }
                }, "android")
            }
        } else {
            removeJavascriptInterface("android")
        }
        return this
    }

    fun getFontSize(): Float {
        return fontSize
    }

    fun setFontSize(fontSize: Float): CodeView {
        if (fontSize < 8) this.fontSize = 8f
        else this.fontSize = fontSize
        if (onHighlightListener != null) {
            onHighlightListener!!.onFontSizeChanged(this.fontSize.toInt())
        }
        return this
    }

    fun getCode(): String {
        return code
    }

    fun setCode(code: String?): CodeView {
        if (code == null) this.code = ""
        else this.code = code
        this.escapeCode = Html.escapeHtml(this.code)
        return this
    }

    fun getLanguage(): String? {
        return language
    }

    fun setLanguage(language: String): CodeView {
        this.language = language
        return this
    }

    fun isWrapLine(): Boolean {
        return wrapLine
    }

    fun setWrapLine(wrapLine: Boolean): CodeView {
        this.wrapLine = wrapLine
        return this
    }

    fun isZoomEnabled(): Boolean {
        return zoomEnabled
    }

    fun setZoomEnabled(zoomEnabled: Boolean): CodeView {
        this.zoomEnabled = zoomEnabled
        return this
    }

    fun isShowLineNumber(): Boolean {
        return showLineNumber
    }

    fun setShowLineNumber(showLineNumber: Boolean): CodeView {
        this.showLineNumber = showLineNumber
        return this
    }

    fun getStartLineNumber(): Int {
        return startLineNumber
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

    fun apply() {
        loadDataWithBaseURL(
            "",
            toHtml(),
            "text/html",
            "UTF-8",
            ""
        )
    }

    private fun toHtml(): String {
        var wrapLineCss = ""
        if (wrapLine) {
            wrapLineCss = "word-wrap: break-word; white-space: pre-wrap; word-break: break-all;"
        }
        return """
<!DOCTYPE html>
<html>
<head>
  <link rel='stylesheet' href='file:///android_asset/codeview/highlightjs/styles/androidstudio.css'/>
  <link rel='stylesheet' href='file:///android_asset/codeview/styles.css'/>
  <style type="text/css">
    body {
      font-size: ${getFontSize().toInt()}px;
    }
    td.line {
        $wrapLineCss
    }
  </style>
  <script src='file:///android_asset/codeview/highlightjs/highlight.js'></script>
  <script src='file:///android_asset/codeview/script.js'></script>
</head>
<body>
<pre><code class='$language' id='code-holder'>${insertLineNumber(escapeCode)}</code></pre>
<script>highlightCode()</script>
</body>
</html>
""".trimIndent()
    }

    private fun executeJavaScript(js: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            evaluateJavascript("javascript:$js", null)
        } else {
            loadUrl("javascript:$js")
        }
    }

    private fun changeFontSize(sizeInPx: Int) {
        executeJavaScript("document.body.style.fontSize = '" + sizeInPx + "px'")
    }

    private fun fillLineNumbers() {
        executeJavaScript("var i; var x = document.querySelectorAll('td.ln'); for(i = 0; i < x.length; i++) {x[i].innerHTML = x[i].getAttribute('line');}")
    }

    private fun showHideLineNumber(show: Boolean) {
        executeJavaScript(
            String.format(
                Locale.ENGLISH,
                "var i; var x = document.querySelectorAll('td.ln'); for(i = 0; i < x.length; i++) {x[i].style.display = %s;}",
                if (show) "''" else "'none'"
            )
        )
    }

    fun highlightLineNumber(lineNumber: Int) {
        this.highlightLineNumber = lineNumber
        executeJavaScript(
            String.format(
                Locale.ENGLISH,
                "var x = document.querySelectorAll('.highlighted-line'); if(x && x.length == 1) x[0].classList.remove('highlighted-line');"
            )
        )
        if (lineNumber >= 0) {
            executeJavaScript(
                String.format(
                    Locale.ENGLISH,
                    "var x = document.querySelectorAll(\"td.line[line='%d']\"); if(x && x.length == 1) x[0].classList.add('highlighted-line');",
                    lineNumber
                )
            )
        }
    }

    private fun insertLineNumber(code: String?): String {
        val m = Pattern.compile("(.*?)&#10;").matcher(code!!)
        val sb = StringBuffer()
        var pos = getStartLineNumber()
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
            fontSize = getFontSize()
            oldFontSize = fontSize.toInt()
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            this@CodeView.fontSize = fontSize
            super.onScaleEnd(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            fontSize = getFontSize() * detector.scaleFactor
            if (fontSize >= 8) {
                changeFontSize(fontSize.toInt())
                if (onHighlightListener != null && oldFontSize != fontSize.toInt()) {
                    onHighlightListener!!.onFontSizeChanged(fontSize.toInt())
                }
                oldFontSize = fontSize.toInt()
            } else {
                fontSize = 8f
            }
            return false
        }
    }
}