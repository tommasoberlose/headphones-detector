package com.tommasoberlose.headphonesdetector

import android.annotation.SuppressLint
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.awareness.fence.HeadphoneFence
import com.google.android.gms.awareness.state.HeadphoneState
import android.support.annotation.NonNull
import com.google.android.gms.common.api.ResultCallbacks
import com.google.android.gms.awareness.fence.FenceUpdateRequest
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.GoogleApiClient
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.text.Html
import android.view.View
import com.tommasoberlose.headphonesdetector.MainActivity.Constants.headphoneFenceKey
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Toast
import android.support.v4.app.ActivityOptionsCompat




class MainActivity : AppCompatActivity() {

    object Constants {
        val headphoneFenceKey = "headphoneFenceKey_discover"
        val PREF_FENCE_ENABLED = "PREF_FENCE_ENABLED"
        val PREF_APP_PACKAGE = "PREF_APP_PACKAGE"
        val PREF_APP_NAME = "PREF_APP_NAME"
    }

    private var mGoogleApiClient: GoogleApiClient? = null
    private lateinit var SP: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SP = PreferenceManager.getDefaultSharedPreferences(this)

        mGoogleApiClient = GoogleApiClient.Builder(this@MainActivity)
                .addApi(Awareness.API)
                .build()
        updateUI()

    }

    fun updateUI() {
        if (SP.getBoolean(Constants.PREF_FENCE_ENABLED, false)) {
            toggleRipple(true)
            rounded_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
            if (SP.getString(Constants.PREF_APP_PACKAGE, "") != "") {
                app_name_label.text = SP.getString(Constants.PREF_APP_NAME, "")
            } else {
                app_name_label.text = getString(R.string.not_selected)
            }

            bottom_container.visibility = View.VISIBLE
            toggle_app.visibility = View.GONE
            bottom_container.setOnClickListener {
                startActivityForResult(Intent(this, SelectAppActivity::class.java), 0)
            }
            action_stop.setOnClickListener {
                unregisterFences()
            }
        } else {
            toggleRipple(false)
            bottom_container.visibility = View.GONE
            toggle_app.visibility = View.VISIBLE
            rounded_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
            toggle_app.setOnClickListener {
                registerFence()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        mGoogleApiClient!!.connect()
    }

    override fun onPause() {
        mGoogleApiClient!!.disconnect()
        super.onPause()
    }

    private fun registerFence() {
        val i = Intent(this, MyBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 1, i, 0)

        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                FenceUpdateRequest.Builder()
                        .addFence(headphoneFenceKey, HeadphoneFence.during(HeadphoneState.PLUGGED_IN), pendingIntent)
                        .build())
                .setResultCallback { status ->
                    if (status.isSuccess) {
                        saveFenceStatus(true)
                        updateUI()
                    } else {
                        Toast.makeText(this@MainActivity, R.string.error, Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun unregisterFences() {
        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                FenceUpdateRequest.Builder()
                        .removeFence(headphoneFenceKey)
                        .build()).setResultCallback(object : ResultCallbacks<Status>() {
            override fun onSuccess(status: Status) {
                saveFenceStatus(false)
                updateUI()
            }
            override fun onFailure(status: Status) {
                Toast.makeText(this@MainActivity, R.string.error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("ApplySharedPref")
    private fun saveFenceStatus(enabled: Boolean) {
        SP.edit()
                .putBoolean(Constants.PREF_FENCE_ENABLED, enabled)
                .commit()
    }

    private fun toggleRipple(show: Boolean) {
        if (show) {
            content.animate().alpha(1f)
                    .withStartAction {
                        content.startRippleAnimation()
                    }
                    .start()
        } else {
            content.animate().alpha(0f)
                    .withEndAction {
                        content.stopRippleAnimation()
                    }
                    .start()
        }
    }

}
