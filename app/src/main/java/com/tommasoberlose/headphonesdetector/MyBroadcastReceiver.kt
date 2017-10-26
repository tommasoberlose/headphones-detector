package com.tommasoberlose.headphonesdetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.provider.AlarmClock
import com.google.android.gms.awareness.fence.FenceState
import android.text.TextUtils
import android.util.Log
import android.widget.Toast


class MyBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val fenceState = FenceState.extract(intent)
        if (TextUtils.equals(fenceState.fenceKey, MainActivity.Constants.headphoneFenceKey)) {
            when (fenceState.currentState) {
                FenceState.TRUE -> openSelectedApplication(context)
            }
        }
    }

    private fun openSelectedApplication(context: Context) {
        val SP = PreferenceManager.getDefaultSharedPreferences(context)
        if (!SP.getString(MainActivity.Constants.PREF_APP_PACKAGE, "").equals("")) {
            try {
                val pm: PackageManager = context.packageManager
                val intent: Intent = pm.getLaunchIntentForPackage(SP.getString(MainActivity.Constants.PREF_APP_PACKAGE, ""))
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
