package com.aura.launcher

import android.app.admin.DevicePolicyManager
import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * AuraAdminReceiver — Device Admin jo Aura ko "screen lock" power deta hai.
 *
 * Double-tap home se screen lock karne ke liye ye chahiye.
 * User ek baar enable karega (settings prompt se), phir Aura
 * screen lock kar sakta hai — bilkul ek asli launcher jaisa.
 */
class AuraAdminReceiver : DeviceAdminReceiver()

object LockHelper {

    private fun adminComponent(context: Context) =
        ComponentName(context, AuraAdminReceiver::class.java)

    /** Check: device admin enabled hai? */
    fun isDeviceAdminEnabled(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(adminComponent(context))
    }

    /** Admin enable karne ka system prompt kholo. */
    fun requestDeviceAdmin(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Aura ko 'double-tap to lock' ke liye ye permission chahiye."
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Screen lock karo (admin active hona chahiye). */
    fun lockScreen(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (isDeviceAdminEnabled(context)) {
            try {
                dpm.lockNow()
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Screen lock fail ❌",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // Admin not enabled, request it
            requestDeviceAdmin(context)
        }
    }
}
