package com.example.otpapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        @Volatile private var inst: MainActivity? = null

        // Number is copied by the overlay service. No cross-site form injection.
        fun fillNumber(n: String) {
            // Deliberately kept as a compatibility callback for the HTML bridge.
        }
    }

    private lateinit var fab: Button
    private lateinit var web: WebView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inst = this
        window.statusBarColor = Color.rgb(5, 10, 26)
        window.navigationBarColor = Color.rgb(5, 10, 26)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(5, 10, 26))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(Color.rgb(10, 23, 43))
        }
        val logo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(25, 43, 72))
            }
            clipToOutline = true
        }
        Logo.load(this) { bmp -> if (bmp != null) logo.setImageBitmap(bmp) }
        header.addView(logo, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(11) })

        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(this).apply {
            text = "OTP STUDIO"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
        })
        titles.addView(TextView(this).apply {
            text = "Secure number workspace"
            setTextColor(Color.rgb(145, 166, 198))
            textSize = 11f
        })
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "● READY"
            setTextColor(Color.rgb(53, 212, 154))
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
        })
        // নেটিভ হেডার সরানো হয়েছে: HTML এর নিজের হেডার (PS NUMBER) ব্যবহার হয়

        web = WebView(this).apply {
            setBackgroundColor(Color.rgb(5, 10, 26))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webChromeClient = WebChromeClient()
            webViewClient = LinkClient(this@MainActivity) { this@MainActivity.recreate() }
        }

        fab = Button(this).apply {
            text = "▶"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            elevation = dp(12).toFloat()
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(77, 131, 255), Color.rgb(138, 92, 255))
            ).apply { shape = GradientDrawable.OVAL }
            contentDescription = "Floating OTP window চালু করুন"
            setOnClickListener { toggleOverlay() }
        }

        val content = FrameLayout(this).apply {
            addView(web, FrameLayout.LayoutParams(-1, -1))
            addView(fab, FrameLayout.LayoutParams(dp(62), dp(62), Gravity.BOTTOM or Gravity.END).apply {
                bottomMargin = dp(22)
                rightMargin = dp(18)
            })
        }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        // Load only the bundled app page; it does not browse or inject into third-party sites.
        web.addJavascriptInterface(OtpBridge(this), "Android")
        web.loadUrl("file:///android_asset/otp_popup.html")
        OverlayService.listener = { runOnUiThread { refreshFab() } }
        refreshFab()
    }

    private fun refreshFab() {
        val on = OverlayService.running
        fab.text = if (on) "■" else "▶"
        fab.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            if (on) intArrayOf(Color.rgb(255, 95, 109), Color.rgb(255, 138, 92))
            else intArrayOf(Color.rgb(77, 131, 255), Color.rgb(138, 92, 255))
        ).apply { shape = GradientDrawable.OVAL }
    }

    private fun toggleOverlay() {
        if (OverlayService.running) {
            OverlayService.stop(this)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "অন্য অ্যাপের উপরে দেখানোর অনুমতি চালু করুন, তারপর Start চাপুন", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        var batteryOk = true
        try {
            val pm = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                batteryOk = false
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            }
        } catch (e: Exception) { }
        OverlayService.start(this)
        if (batteryOk) askBackgroundSetup()
    }

    /** Xiaomi/Oppo/Vivo/Huawei-তে একবার Autostart পেজ খোলে, যাতে রিসেন্ট থেকে কেটে দিলেও অ্যাপ বন্ধ না হয়। */
    private fun askBackgroundSetup() {
        if (Persist.oemAsked(this)) return
        Persist.setOemAsked(this)
        if (OemSettings.open(this)) {
            Toast.makeText(this, "PS NUMBER-এর Autostart / Background চালু করুন, আর রিসেন্টে অ্যাপ কার্ড লক করুন", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshFab()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onDestroy() {
        if (inst === this) inst = null
        OverlayService.listener = null
        super.onDestroy()
    }
}
