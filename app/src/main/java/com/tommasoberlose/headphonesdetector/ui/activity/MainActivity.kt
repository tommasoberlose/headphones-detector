package com.tommasoberlose.headphonesdetector.ui.activity

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.awareness.fence.HeadphoneFence
import com.google.android.gms.awareness.state.HeadphoneState
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
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import android.transition.Fade
import android.transition.Transition
import android.view.View
import android.view.Window
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Toast
import com.tommasoberlose.headphonesdetector.receiver.MyBroadcastReceiver
import com.tommasoberlose.headphonesdetector.R
import com.tommasoberlose.headphonesdetector.`object`.event.CircleAnimationEvent
import com.tommasoberlose.headphonesdetector.constant.Constants.headphoneFenceKey
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


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

    }

    fun updateUI() {
        val pm = packageManager
        if (SP.getString(Constants.PREF_APP_PACKAGE, "") != "") {
            toggleRipple(true)
            rounded_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
            text.text = SP.getString(Constants.PREF_APP_NAME, "")
            try {
                icon.setImageDrawable(packageManager.getApplicationIcon(SP.getString(Constants.PREF_APP_PACKAGE, "")))
            } catch (ignore: Exception) {
            }

            edit_card.setOnClickListener {
                selectApp()
            }
            action_stop.setOnClickListener {
                unregisterFences()
            }

            edit_card.animate().alpha(1f).translationY(0f).start()
            action_select_app.visibility = View.GONE
        } else {
            toggleRipple(false)
            rounded_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
            action_select_app.setOnClickListener {
                selectApp()
            }

            edit_card.animate().alpha(0f).translationY(500f).start()
            action_select_app.visibility = View.VISIBLE
        }
    }

    @SuppressLint("RestrictedApi")
    private fun selectApp() {
        startActivityForResult(Intent(this, SelectAppActivity::class.java), 0)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        registerFence()
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
                        updateUI()
                    } else {
                        Toast.makeText(this@MainActivity, R.string.error, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
    }

    private fun unregisterFences() {
        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                FenceUpdateRequest.Builder()
                        .removeFence(headphoneFenceKey)
                        .build()).setResultCallback(object : ResultCallbacks<Status>() {
            @SuppressLint("ApplySharedPref")
            override fun onSuccess(status: Status) {
                SP.edit()
                        .remove(Constants.PREF_APP_NAME)
                        .remove(Constants.PREF_APP_PACKAGE)
                        .commit()
                updateUI()
            }
            override fun onFailure(status: Status) {
                Toast.makeText(this@MainActivity, R.string.error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleRipple(show: Boolean) {
        if (show) {
            animCircle()
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

    public override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    public override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMessageEvent(event: CircleAnimationEvent) {
        if (SP.getBoolean(Constants.PREF_FENCE_ENABLED, false)) {
            if (event.enlarge) {
                enlargeCircle()
            } else {
                reduceCircle()
            }
        }
    }

    private fun animCircle() {
        enlargeCircle()
    }

    private fun enlargeCircle() {
        rounded_card.animate()
                .scaleY(1.1f)
                .scaleX(1.1f)
                .setDuration(800)
                .withEndAction {
                    EventBus.getDefault().post(CircleAnimationEvent(false))
                }
                .start()
    }

    private fun reduceCircle() {
        rounded_card.animate()
                .scaleY(1f)
                .scaleX(1f)
                .setDuration(800)
                .withEndAction {
                    EventBus.getDefault().post(CircleAnimationEvent(true))
                }
                .start()
    }

}
