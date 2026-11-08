package com.example.otpapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** ফোন চালু বা অ্যাপ আপডেটের পর, আগে চালু থাকলে ভাসমান বাবল নিজে ফিরিয়ে আনে। */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!Persist.isEnabled(context)) return
        if (!Settings.canDrawOverlays(context)) return
        try { OverlayService.start(context) } catch (e: Exception) { }
    }
}
