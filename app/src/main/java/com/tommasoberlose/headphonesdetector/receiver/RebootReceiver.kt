package com.tommasoberlose.headphonesdetector.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.fence.FenceUpdateRequest
import com.google.android.gms.awareness.fence.HeadphoneFence
import com.google.android.gms.awareness.state.HeadphoneState
import com.google.android.gms.common.api.GoogleApiClient
import com.tommasoberlose.headphonesdetector.R
import com.tommasoberlose.headphonesdetector.constant.Constants
import com.tommasoberlose.headphonesdetector.ui.activity.MainActivity

class RebootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val SP = PreferenceManager.getDefaultSharedPreferences(context)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && SP.getBoolean(MainActivity.Constants.PREF_FENCE_ENABLED, false)) {
            registerFence(context)
        }
    }

    private fun registerFence(context: Context) {
        val i = Intent(context, MyBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 1, i, 0)

        val mGoogleApiClient = GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build()
        mGoogleApiClient.connect()

        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                FenceUpdateRequest.Builder()
                        .addFence(Constants.headphoneFenceKey, HeadphoneFence.during(HeadphoneState.PLUGGED_IN), pendingIntent)
                        .build())
                .setResultCallback {
                    mGoogleApiClient.disconnect()
                }
    }
}
