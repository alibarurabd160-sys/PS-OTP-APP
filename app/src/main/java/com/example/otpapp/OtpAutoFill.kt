package com.example.otpapp

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/**
 * নাম্বার বা OTP ক্লিপবোর্ডে কপি করে।
 * ব্যাকগ্রাউন্ড (ভাসমান উইন্ডো) থেকে হলে কিছু ফোনে সরাসরি কপি আটকে যায়,
 * তাই একটা স্বচ্ছ CopyActivity এক ঝলকের জন্য খুলে ফোকাস নিয়ে কপি নিশ্চিত করা হয়।
 */
object OtpAutoFill {
    @Volatile private var lastText = ""
    @Volatile private var lastAt = 0L

    private fun copyText(ctx: Context, label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        val now = System.currentTimeMillis()
        val dup = text == lastText && now - lastAt < 3000
        lastText = text
        lastAt = now
        if (!dup && ctx !is Activity) {
            try {
                ctx.startActivity(
                    Intent(ctx, CopyActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        .putExtra("text", text)
                )
            } catch (e: Exception) { }
        }
    }

    fun copyNumber(ctx: Context, number: String) {
        copyText(ctx, "number", number)
        Toast.makeText(ctx, "নাম্বার কপি হয়েছে", Toast.LENGTH_SHORT).show()
    }

    fun copyOtp(ctx: Context, otp: String) {
        copyText(ctx, "otp", otp)
        Toast.makeText(ctx, "OTP কপি হয়েছে", Toast.LENGTH_SHORT).show()
    }

    fun copyQuiet(ctx: Context, text: String) {
        copyText(ctx, "text", text)
    }
}

/** API Key ফোনে স্থায়ীভাবে জমা থাকে; একবার সেভ হলে আর বদলানো যায় না। */
object Keys {
    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("ps", Context.MODE_PRIVATE)
    fun get(ctx: Context): String = sp(ctx).getString("key", "") ?: ""
    fun save(ctx: Context, k: String) {
        if (get(ctx).isEmpty() && k.isNotBlank()) sp(ctx).edit().putString("key", k.trim()).apply()
    }
}

/** অ্যাপের মূল স্ক্রিনের WebView-র জন্য ব্রিজ (HTML এর window.Android)। */
class OtpBridge(private val ctx: Context) {
    private val main = Handler(Looper.getMainLooper())
    var web: WebView? = null
    @JavascriptInterface fun readClipboard() {
        main.post {
            var text = ""
            try {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) text = clip.getItemAt(0).coerceToText(ctx).toString()
            } catch (e: Exception) { }
            web?.evaluateJavascript("window.__pasted&&window.__pasted(" + org.json.JSONObject.quote(text) + ")", null)
        }
    }
    @JavascriptInterface fun onNumber(plain: String, full: String) { main.post { OtpAutoFill.copyNumber(ctx, full) } }
    @JavascriptInterface fun onOtp(otp: String) { main.post { OtpAutoFill.copyOtp(ctx, otp) } }
    @JavascriptInterface fun copyText(t: String) { main.post { OtpAutoFill.copyQuiet(ctx, t) } }
    @JavascriptInterface fun getKey(): String = Keys.get(ctx)
    @JavascriptInterface fun saveKey(k: String) { Keys.save(ctx, k) }
}

/** বাইরের লিংকে (Telegram ইত্যাদি) চাপলে Chrome-এ খোলে; Chrome না থাকলে ডিফল্ট অ্যাপে। */
class LinkClient(private val ctx: Context, private val onGone: (() -> Unit)? = null) : WebViewClient() {
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onGone?.invoke()
        return true
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val u = request.url
        if (u.scheme == "file") return false
        val i = Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(Intent(i).setPackage("com.android.chrome"))
        } catch (e: Exception) {
            try { ctx.startActivity(i) } catch (e2: Exception) { }
        }
        return true
    }
}
