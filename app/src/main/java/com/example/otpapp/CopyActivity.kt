package com.example.otpapp

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle

/** স্বচ্ছ অ্যাক্টিভিটি: ফোকাস পেলেই কপি করে নিজেকে বন্ধ করে দেয়। */
class CopyActivity : Activity() {
    private var done = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !done) {
            done = true
            val text = intent.getStringExtra("text") ?: ""
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("otp", text))
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
