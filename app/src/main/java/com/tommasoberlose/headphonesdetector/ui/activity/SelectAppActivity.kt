package com.tommasoberlose.headphonesdetector.ui.activity

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import com.tommasoberlose.headphonesdetector.*
import com.tommasoberlose.headphonesdetector.`object`.event.AppInfoSavedEvent
import com.tommasoberlose.headphonesdetector.`object`.event.ApplicationListEvent
import com.tommasoberlose.headphonesdetector.constant.Constants
import com.tommasoberlose.headphonesdetector.ui.adapter.ApplicationInfoAdapter
import kotlinx.android.synthetic.main.activity_select_app.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SelectAppActivity : AppCompatActivity() {

    lateinit var adapter: ApplicationInfoAdapter
    val appList = ArrayList<ApplicationInfo>()
    val appListFiltered = ArrayList<ApplicationInfo>()
    private lateinit var SP: SharedPreferences
    private lateinit var pm: PackageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_app)
        pm = packageManager
        SP = PreferenceManager.getDefaultSharedPreferences(this)

        action_default.setOnClickListener {
            onBackPressed()
        }

        list_view.setHasFixedSize(true);
        val mLayoutManager = LinearLayoutManager(this);
        list_view.layoutManager = mLayoutManager;

        adapter = ApplicationInfoAdapter(this, appListFiltered);
        list_view.adapter = adapter;

        location.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                Thread().run {
                    val appsFiltered = if (text == null || text.toString() == "") appList else appList.filter { pm.getApplicationLabel(it).toString().contains(text.toString(), true) }
                    EventBus.getDefault().post(ApplicationListEvent(appsFiltered, true))
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

    }

    @SuppressLint("ApplySharedPref")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun saveApp(e: AppInfoSavedEvent) {
        SP.edit()
                .putString(Constants.PREF_APP_NAME, pm.getApplicationLabel(e.app).toString())
                .putString(Constants.PREF_APP_PACKAGE, e.app.packageName)
                .commit()
        finish()
    }

    public override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    public override fun onResume() {
        super.onResume()
        Thread().run {
            val pm = packageManager
            val apps = pm.getInstalledApplications(0)
            EventBus.getDefault().post(ApplicationListEvent(apps, false))
        }
    }

    public override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMessageEvent(event: ApplicationListEvent) {
        if (!event.filtered) {
            appList.clear()
            event.apps.mapTo(appList, {it})
        }
        appListFiltered.clear()
        event.apps.mapTo(appListFiltered, {it})
        adapter.changeData(appListFiltered)
    }
}
