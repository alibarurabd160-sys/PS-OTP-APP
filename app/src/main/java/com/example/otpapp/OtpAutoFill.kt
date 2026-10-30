package com.example.otpapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/**
 * Copies the generated number or OTP to the clipboard.
 * It intentionally does not inspect or fill fields on other websites/apps.
 */
object OtpAutoFill {
    private fun copyText(ctx: Context, label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun copyNumber(ctx: Context, number: String) {
        copyText(ctx, "number", number)
        Toast.makeText(ctx, "নাম্বার কপি হয়েছে", Toast.LENGTH_SHORT).show()
    }

    fun copyOtp(ctx: Context, otp: String) {
        copyText(ctx, "otp", otp)
        Toast.makeText(ctx, "OTP কপি হয়েছে", Toast.LENGTH_SHORT).show()
    }
}
