package com.example.otpapp

import android.app.Activity
import android.content.ComponentName
import android.content.Intent

/**
 * কিছু ফোন (Xiaomi/Redmi, Oppo/Realme, Vivo, Huawei) রিসেন্ট থেকে কেটে দিলে অ্যাপ বন্ধ করে দেয়,
 * যদি না "Autostart / Background" অনুমতি দেওয়া থাকে। সেই পেজ খোলার চেষ্টা করে; না পেলে false দেয়।
 */
object OemSettings {
    private val targets = listOf(
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
    )

    fun open(act: Activity): Boolean {
        for (t in targets) {
            try {
                act.startActivity(Intent().setComponent(ComponentName(t.first, t.second)))
                return true
            } catch (e: Exception) { }
        }
        return false
    }
}
