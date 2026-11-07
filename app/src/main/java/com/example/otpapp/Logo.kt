package com.example.otpapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** লোগো লোড করে গোল করে দেয়। আগে assets/logo.png, তারপর ক্যাশ, তারপর অনলাইন লিংক। */
object Logo {
    const val URL_LOGO = "https://i.postimg.cc/qgCPCKB3/IMG-20261029-221418-464.jpg"

    fun load(ctx: Context, done: (Bitmap?) -> Unit) {
        val app = ctx.applicationContext
        Thread {
            var bmp: Bitmap? = null
            try {
                app.assets.open("logo.png").use { bmp = BitmapFactory.decodeStream(it) }
            } catch (e: Exception) { }
            val cache = File(app.cacheDir, "logo.jpg")
            if (bmp == null && cache.exists()) bmp = BitmapFactory.decodeFile(cache.path)
            if (bmp == null) {
                try {
                    val c = URL(URL_LOGO).openConnection() as HttpURLConnection
                    c.connectTimeout = 8000
                    c.readTimeout = 8000
                    c.setRequestProperty("User-Agent", "Mozilla/5.0")
                    c.inputStream.use { input -> cache.outputStream().use { out -> input.copyTo(out) } }
                    bmp = BitmapFactory.decodeFile(cache.path)
                } catch (e: Exception) { }
            }
            val result = bmp?.let { circle(it) }
            Handler(Looper.getMainLooper()).post { done(result) }
        }.start()
    }

    fun circle(src: Bitmap, size: Int = 256): Bitmap {
        val m = minOf(src.width, src.height)
        val sq = Bitmap.createBitmap(src, (src.width - m) / 2, (src.height - m) / 2, m, m)
        val scaled = Bitmap.createScaledBitmap(sq, size, size, true)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        Canvas(out).drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return out
    }
}
