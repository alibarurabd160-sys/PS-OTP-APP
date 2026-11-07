package com.example.otpapp

import android.content.Context

/** ছোট স্থায়ী অবস্থা: ইউজার ভাসমান সিস্টেম চালু রেখেছে কিনা (রিবুট/আপডেটের পর নিজে ফিরতে), একবারের প্রম্পট ইত্যাদি। */
object Persist {
    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("ps_state", Context.MODE_PRIVATE)

    fun enabled(ctx: Context): Boolean = sp(ctx).getBoolean("enabled", false)
    fun setEnabled(ctx: Context, on: Boolean) { sp(ctx).edit().putBoolean("enabled", on).commit() }

    fun oemAsked(ctx: Context): Boolean = sp(ctx).getBoolean("oem_asked", false)
    fun setOemAsked(ctx: Context) { sp(ctx).edit().putBoolean("oem_asked", true).apply() }
}
