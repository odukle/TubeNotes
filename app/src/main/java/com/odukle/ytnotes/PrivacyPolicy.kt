package com.odukle.ytnotes

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_privacy_policy.*


class PrivacyPolicy : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Privacy Policy"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.home_as_up)

        web_view.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                Toast.makeText(this@PrivacyPolicy, description, Toast.LENGTH_SHORT).show()
            }
        }

        val progressBar = ProgressBar(this, )
        web_view.loadUrl("https://sites.google.com/view/ytnotes-privacypolicy/home")
    }

}
