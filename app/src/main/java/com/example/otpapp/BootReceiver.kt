package com.example.otpapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** ফোন রিস্টার্ট বা অ্যাপ আপডেটের পর, ইউজার চালু রেখে থাকলে বাবল নিজে আবার চালু করে। */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val a = intent.action ?: return
        if (a != Intent.ACTION_BOOT_COMPLETED &&
            a != Intent.ACTION_MY_PACKAGE_REPLACED &&
            a != "android.intent.action.QUICKBOOT_POWERON"
        ) return
        if (!Persist.enabled(ctx)) return
        if (!Settings.canDrawOverlays(ctx)) return
        if (OverlayService.running) return
        try { OverlayService.start(ctx) } catch (e: Exception) { }
    }
}
