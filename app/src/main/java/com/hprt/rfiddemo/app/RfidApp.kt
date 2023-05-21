package com.hprt.rfiddemo.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.blankj.utilcode.util.LogUtils
import com.hprt.lib_rfid.RFHelper
import androidx.multidex.MultiDex
import com.hprt.rfiddemo.utils.AcitivityManager
import com.hprt.rfiddemo.utils.MyUtil

/**
 * @author yefl
 * @date 2019/9/27.
 * description：
 */
class RfidApp:Application(){

    enum class InventoryType{
        shortPress, longPress
    }


    enum class RFIDType{
        SLR1200, SLR5100
    }

    companion object{
        var pressType = InventoryType.longPress
        var rfidType = RFIDType.SLR5100

        @Volatile private var INSTANCE: RfidApp? = null
        fun getInstance(): RfidApp =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?:RfidApp()
            }
    }

    override fun onCreate() {
        super.onCreate()
        LogUtils.getConfig().setLogHeadSwitch(false)
        RFHelper.init(this)
        registerActivityLifecycleCallbacks(lifecycleCallbacks)
        MyUtil.stopScan(this)
    }



    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
    }



    override fun onTerminate() {
        super.onTerminate()
        RFHelper.disconnect()
        MyUtil.startScan(this)
    }



    /**
     * 以下为防止短时间内两次点击button
     */
    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
            MyUtil.fixViewMutiClickInShortTime(activity)
            AcitivityManager.instance.addActivity(activity)
        }

        override fun onActivityStarted(activity: Activity) {

        }

        override fun onActivityResumed(activity: Activity) {

        }

        override fun onActivityPaused(activity: Activity) {

        }

        override fun onActivityStopped(activity: Activity) {

        }

        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle?) {

        }

        override fun onActivityDestroyed(activity: Activity) {
            AcitivityManager.instance.removeActivity(activity)
        }
    }



}