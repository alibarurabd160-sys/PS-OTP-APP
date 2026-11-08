package com.example.otpapp

import android.content.Context

/** ভাসমান বাবল "চালু আছে" কিনা মনে রাখে; রিবুট/আপডেটের পর BootReceiver এটা দেখে নিজে ফেরে। */
object Persist {
    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("ps_state", Context.MODE_PRIVATE)
    fun setEnabled(ctx: Context, on: Boolean) { sp(ctx).edit().putBoolean("overlay_on", on).apply() }
    fun isEnabled(ctx: Context): Boolean = sp(ctx).getBoolean("overlay_on", false)
}
