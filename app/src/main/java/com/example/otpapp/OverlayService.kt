package com.example.otpapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * ভাসমান সিস্টেম: লোগোওয়ালা গোল বাবল (ড্র্যাগ করা যায়) -> ট্যাপ করলে ভাসমান উইন্ডো খোলে।
 * উইন্ডোর ভেতরে otp_popup.html চলে। "—" চাপলে আবার বাবল হয়, "✕" চাপলে পুরোপুরি বন্ধ।
 *
 * রিসেন্ট ট্যাব থেকে কেটে দিলেও চলার ব্যবস্থা:
 *  - Foreground service + stopWithTask=false + START_STICKY
 *  - onTaskRemoved-এ চলমান নাম্বার সেভ + ব্যাকআপ রিস্টার্ট অ্যালার্ম
 *  - প্রতি ৫ মিনিটে keep-alive (wake lock নবায়ন, WebView টাইমার চালু, বাবল/উইন্ডো ঠিক আছে কিনা দেখা)
 *  - WebView-র রেন্ডারার মরে গেলে অ্যাপ ক্র্যাশ না করে নতুন WebView বানিয়ে পপআপ আবার চালু
 *  - রিবুট/আপডেটের পর BootReceiver দিয়ে নিজে ফেরা
 */
class OverlayService : Service() {

    companion object {
        @Volatile var running = false
        var listener: (() -> Unit)? = null
        const val ACTION_STOP = "com.example.otpapp.STOP"
        private const val KEEP_ALIVE_MS = 5 * 60 * 1000L

        fun start(ctx: Context) {
            Persist.setEnabled(ctx, true)
            val i = Intent(ctx, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            Persist.setEnabled(ctx, false)
            if (running) ctx.startService(Intent(ctx, OverlayService::class.java).setAction(ACTION_STOP))
        }
    }

    private lateinit var wm: WindowManager
    private val main = Handler(Looper.getMainLooper())

    private var bubble: FrameLayout? = null
    private var bubbleP: WindowManager.LayoutParams? = null
    private var bubbleImg: ImageView? = null
    private var bubbleTxt: TextView? = null

    private var panel: LinearLayout? = null
    private var panelP: WindowManager.LayoutParams? = null
    private var web: WebView? = null
    private var wl: android.os.PowerManager.WakeLock? = null
    private var lastRebuild = 0L

    private val FLAG_BASE = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    private val FLAG_SHOWN = FLAG_BASE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    private val FLAG_HIDDEN = FLAG_SHOWN or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(1, buildNotification())
        try {
            val w = (getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
                .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "psnumber:poll")
            w.setReferenceCounted(false)
            w.acquire(30 * 60 * 1000L)
            wl = w
        } catch (e: Exception) { }
        buildPanel()
        buildBubble()
        showBubble()
        Logo.load(this) { bmp ->
            if (bmp != null) {
                bubbleImg?.setImageBitmap(bmp)
                bubbleImg?.visibility = View.VISIBLE
                bubbleTxt?.visibility = View.GONE
            }
        }
        main.postDelayed(keepAlive, KEEP_ALIVE_MS)
        main.post { listener?.invoke() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopClean(); return START_NOT_STICKY }
        try { startForeground(1, buildNotification()) } catch (e: Exception) { }
        return START_STICKY
    }

    /** রিসেন্ট ট্যাব থেকে কেটে দিলে: চলমান নাম্বার সেভ করে নেয় + ১.৫ সেকেন্ড পরে সার্ভিস আবার চালুর ব্যাকআপ অ্যালার্ম। */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            web?.evaluateJavascript("try{window.saveJobsNow&&window.saveJobsNow()}catch(e){}", null)
        } catch (e: Exception) { }
        try {
            val i = Intent(applicationContext, OverlayService::class.java)
            val fl = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            val pi = if (Build.VERSION.SDK_INT >= 26) PendingIntent.getForegroundService(applicationContext, 7, i, fl)
                     else PendingIntent.getService(applicationContext, 7, i, fl)
            (getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager)
                .setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1500, pi)
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        running = false
        try { wl?.release() } catch (e: Exception) { }
        main.removeCallbacksAndMessages(null)
        try { bubble?.let { wm.removeView(it) } } catch (e: Exception) { }
        try { panel?.let { wm.removeView(it) } } catch (e: Exception) { }
        web?.destroy(); web = null
        bubble = null; panel = null
        listener?.invoke()
        super.onDestroy()
    }

    /** ইউজার নিজে বন্ধ করলে: "চালু আছে" চিহ্ন মুছে, জমানো অপেক্ষারত নাম্বার মুছে, তারপর বন্ধ। */
    private fun stopClean() {
        Persist.setEnabled(this, false)
        try { web?.evaluateJavascript("try{localStorage.setItem('jobs','[]')}catch(e){}", null) } catch (e: Exception) { }
        main.postDelayed({ stopSelf() }, 250)
    }

    /** প্রতি ৫ মিনিটে: wake lock নবায়ন, WebView টাইমার চালু, নোটিফিকেশন ও বাবল/উইন্ডো ঠিক আছে কিনা দেখা। */
    private val keepAlive = object : Runnable {
        override fun run() {
            try { wl?.acquire(30 * 60 * 1000L) } catch (e: Exception) { }
            try { web?.onResume(); web?.resumeTimers() } catch (e: Exception) { }
            try { startForeground(1, buildNotification()) } catch (e: Exception) { }
            val b = bubble; val bp = bubbleP
            if (b != null && bp != null && !b.isAttachedToWindow) { try { wm.addView(b, bp) } catch (e: Exception) { } }
            val r = panel; val rp = panelP
            if (r != null && rp != null && !r.isAttachedToWindow) { try { wm.addView(r, rp) } catch (e: Exception) { } }
            main.postDelayed(this, KEEP_ALIVE_MS)
        }
    }

    // ---------- notification ----------
    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("otp", "OTP ভাসমান উইন্ডো", NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "otp") else Notification.Builder(this)
        b.setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("PS NUMBER চালু আছে")
            .setContentText("বাবলে ট্যাপ করে উইন্ডো খুলুন")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_launcher), "বন্ধ করুন", stop).build())
        return b.build()
    }

    // ---------- floating panel ----------
    @SuppressLint("SetJavaScriptEnabled")
    private fun makeWeb(): WebView {
        val c: Context = this
        val wv = WebView(applicationContext)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.setBackgroundColor(Color.parseColor("#050a1a"))
        // রেন্ডারার মরে গেলে অ্যাপ ক্র্যাশ করবে না; নতুন WebView বানিয়ে পপআপ আবার লোড হবে (চলমান নাম্বার HTML নিজে ফিরিয়ে আনে)
        wv.webViewClient = LinkClient(c) { main.post { rebuildWeb() } }
        wv.addJavascriptInterface(object {
            @JavascriptInterface fun onNumber(plain: String, full: String) {
                main.post {
                    OtpAutoFill.copyNumber(c, full)
                    MainActivity.fillNumber(plain)
                }
            }
            @JavascriptInterface fun onOtp(otp: String) { main.post { OtpAutoFill.copyOtp(c, otp) } }
            @JavascriptInterface fun copyText(t: String) { main.post { OtpAutoFill.copyQuiet(c, t) } }
            @JavascriptInterface fun getKey(): String = Keys.get(c)
            @JavascriptInterface fun saveKey(k: String) { Keys.save(c, k) }
            @JavascriptInterface fun close() { main.post { stopClean() } }
            @JavascriptInterface fun minimize() { main.post { showBubble() } }
            @JavascriptInterface fun setFocusable(on: Boolean) { main.post { setPanelFocusable(on) } }
            @JavascriptInterface fun readClipboard() { main.post { readClipboardToJs() } }
        }, "Android")
        wv.loadUrl("file:///android_asset/otp_popup.html")
        wv.onResume()
        wv.resumeTimers()
        return wv
    }

    private fun rebuildWeb() {
        val p = panel ?: return
        val now = System.currentTimeMillis()
        if (now - lastRebuild < 2000) return
        lastRebuild = now
        web?.let {
            try { p.removeView(it) } catch (e: Exception) { }
            try { it.destroy() } catch (e: Exception) { }
        }
        val nw = makeWeb()
        web = nw
        p.addView(nw, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildPanel() {
        val c: Context = this
        val dm = resources.displayMetrics
        // পপআপের সাইজ: প্রস্থ ৭৮% × উচ্চতা ৩২% ≈ স্ক্রিনের চার ভাগের এক ভাগ। বদলাতে শুধু এই দুটো সংখ্যা বদলান।
        val w = (dm.widthPixels * 0.78f).toInt()
        val h = (dm.heightPixels * 0.32f).toInt()
        val radius = 20f * dm.density

        val wv = makeWeb()
        web = wv

        // উপরের ড্র্যাগ-হ্যান্ডেল
        val pill = View(c)
        pill.background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(Color.parseColor("#8aa0d0")) }
        val handle = FrameLayout(c)
        handle.setBackgroundColor(Color.parseColor("#0a1330"))
        handle.addView(pill, FrameLayout.LayoutParams(dp(56), dp(6), Gravity.CENTER))

        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#050a1a"))
        root.clipToOutline = true
        root.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, radius) }
        }
        root.addView(handle, LinearLayout.LayoutParams(-1, dp(34)))
        root.addView(wv, LinearLayout.LayoutParams(-1, 0, 1f))

        val p = WindowManager.LayoutParams(w, h, overlayType(), FLAG_HIDDEN, PixelFormat.TRANSLUCENT)
        p.gravity = Gravity.TOP or Gravity.START
        p.x = (dm.widthPixels - w) / 2
        p.y = dm.heightPixels - h - dp(56)
        p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        handle.setOnTouchListener(dragger(root, p, null))

        root.alpha = 0f
        wm.addView(root, p)
        panel = root; panelP = p
    }

    /** পেস্ট বাটন: Android 10+ এ ক্লিপবোর্ড পড়তে উইন্ডোর ফোকাস লাগে, তাই সাময়িক ফোকাসযোগ্য করে পড়ে HTML-কে (window.__pasted) দেয়। */
    private fun readClipboardToJs() {
        val r = panel ?: return
        val p = panelP ?: return
        if (r.alpha < 1f) return
        val prev = p.flags
        p.flags = FLAG_BASE
        try { wm.updateViewLayout(r, p) } catch (e: Exception) { }
        web?.requestFocus()
        var tries = 0
        val reader = object : Runnable {
            override fun run() {
                var text = ""
                try {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = cm.primaryClip
                    if (clip != null && clip.itemCount > 0) text = clip.getItemAt(0).coerceToText(this@OverlayService).toString()
                } catch (e: Exception) { }
                if (text.isEmpty() && ++tries < 3) { main.postDelayed(this, 200); return }
                p.flags = prev
                try { wm.updateViewLayout(r, p) } catch (e: Exception) { }
                web?.evaluateJavascript("window.__pasted&&window.__pasted(" + org.json.JSONObject.quote(text) + ")", null)
            }
        }
        main.postDelayed(reader, 200)
    }

    private fun setPanelFocusable(on: Boolean) {
        val r = panel ?: return
        val p = panelP ?: return
        if (r.alpha < 1f) return
        p.flags = if (on) FLAG_BASE else FLAG_SHOWN
        wm.updateViewLayout(r, p)
        if (on) {
            web?.requestFocus()
            main.postDelayed({
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                web?.let { imm.showSoftInput(it, 0) }
            }, 120)
        }
    }

    // ---------- bubble ----------
    @SuppressLint("ClickableViewAccessibility")
    private fun buildBubble() {
        val c: Context = this
        val size = dp(64)
        val dm = resources.displayMetrics

        val ring = FrameLayout(c)
        ring.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#4D83FF"), Color.parseColor("#8A5CFF"), Color.parseColor("#22D3EE"))
        ).apply { shape = GradientDrawable.OVAL }
        ring.setPadding(dp(3), dp(3), dp(3), dp(3))

        val inner = FrameLayout(c)
        inner.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#0a1330")) }
        val img = ImageView(c)
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        img.visibility = View.GONE
        val txt = TextView(c)
        txt.text = "OTP"; txt.setTextColor(Color.WHITE); txt.textSize = 16f; txt.gravity = Gravity.CENTER
        txt.setTypeface(txt.typeface, android.graphics.Typeface.BOLD)
        inner.addView(img, FrameLayout.LayoutParams(-1, -1))
        inner.addView(txt, FrameLayout.LayoutParams(-1, -1))
        ring.addView(inner, FrameLayout.LayoutParams(-1, -1))

        val p = WindowManager.LayoutParams(size, size, overlayType(), FLAG_SHOWN, PixelFormat.TRANSLUCENT)
        p.gravity = Gravity.TOP or Gravity.START
        p.x = dm.widthPixels - size
        p.y = (dm.heightPixels * 0.35f).toInt()
        ring.setOnTouchListener(dragger(ring, p) { showPanel() })

        wm.addView(ring, p)
        bubble = ring; bubbleP = p; bubbleImg = img; bubbleTxt = txt
    }

    private fun showPanel() {
        val r = panel ?: return
        val p = panelP ?: return
        bubble?.visibility = View.GONE
        r.visibility = View.VISIBLE
        r.alpha = 1f
        p.flags = FLAG_SHOWN
        wm.updateViewLayout(r, p)
        wm.updateViewLayout(bubble, bubbleP)
    }

    private fun showBubble() {
        val r = panel ?: return
        val p = panelP ?: return
        r.visibility = View.VISIBLE
        r.alpha = 0f
        p.flags = FLAG_HIDDEN
        wm.updateViewLayout(r, p)
        bubble?.visibility = View.VISIBLE
        wm.updateViewLayout(bubble, bubbleP)
    }

    // ---------- drag helper ----------
    @SuppressLint("ClickableViewAccessibility")
    private fun dragger(target: View, p: WindowManager.LayoutParams, onTap: (() -> Unit)?): View.OnTouchListener {
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
        val slop = dp(6)
        return View.OnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = p.x; sy = p.y; tx = e.rawX; ty = e.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) { p.x = sx + dx; p.y = sy + dy; wm.updateViewLayout(target, p) }
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onTap?.invoke()
                    else if (target === bubble) {
                        val sw = resources.displayMetrics.widthPixels
                        p.x = if (p.x + target.width / 2 < sw / 2) 0 else sw - target.width   // কিনারায় লেগে যায়
                        wm.updateViewLayout(target, p)
                    }
                }
            }
            true
        }
    }
}
