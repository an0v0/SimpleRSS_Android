package jp.hatenablog.an.simplerss

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.content_rss_detail.*

class RSSDetailActivity : AppCompatActivity() {

    companion object {
        val KEY_URL = "KeyUrl"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rss_detail)

        with(webview) {
            webViewClient = RSSDetailWebViewClient()
            settings.javaScriptEnabled = true
            val userAgent = settings.userAgentString
            settings.userAgentString = userAgent + ""
            intent.extras?.getString(KEY_URL)?.let {
                loadUrl(it)
            }
        }

        swipeRefreshLayout?.setOnRefreshListener {
            webview?.reload()
            swipeRefreshLayout?.isRefreshing = false
        }
    }

    @Override
    override fun onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private class RSSDetailWebViewClient: WebViewClient() {

        @Override
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            // 全てWebView上で開く
            return false
        }
    }
}