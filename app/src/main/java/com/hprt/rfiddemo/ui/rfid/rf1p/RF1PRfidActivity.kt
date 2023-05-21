package com.hprt.rfiddemo.ui.rfid.rf1p

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.utils.SoundUtil
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.config.AndroidWakeLock
import com.hprt.rfiddemo.config.EventBusConfig
import com.hprt.rfiddemo.service.RFService
import com.hprt.rfiddemo.ui.base.BaseActivity
import com.hprt.rfiddemo.ui.rfid.*
import com.hprt.rfiddemo.utils.ProgressDialogUtil
import com.hprt.rfiddemo.widget.TipDialog
import kotlinx.android.synthetic.main.activity_rfid_rf1p.*
import org.simple.eventbus.Subscriber
import org.simple.eventbus.ThreadMode
import java.util.*

class RF1PRfidActivity : BaseActivity() {
    var Awl: AndroidWakeLock? = null
    var mProgressDialogUtil:ProgressDialogUtil?=null
    var rf1pInventoryFragment = RF1PInventoryFragment.newInstance()
    private val tipDialog by lazy {
        val dialog = TipDialog(this).setTipTitle("提示")
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog
    }

    private lateinit var binder: RFService.RFBinder
    private var bundle = Bundle()
    private var binded = false
    private val serviceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder = service as RFService.RFBinder

                bundle.putBinder("binder", binder)
                rf1pInventoryFragment.arguments = bundle
                var readFragment = ReadFragment.newInstance()
                readFragment.arguments = bundle
                var writeFragment = WriteFragment.newInstance()
                writeFragment.arguments = bundle
                var settingFragment = SettingFragment.newInstance()
                settingFragment.arguments = bundle
                var freqSettingFragment = FreqSettingFragment.newInstance()
                freqSettingFragment.arguments = bundle
                var lockFragment = LockFragment.newInstance()
                lockFragment.arguments = bundle
                var killFragment = KillFragment.newInstance()
                killFragment.arguments = bundle
                var updateFragment = UpdateFragment.newInstance()
                updateFragment.arguments = bundle

                mFragments.add(rf1pInventoryFragment)
                mFragments.add(readFragment)
                mFragments.add(writeFragment)
                mFragments.add(settingFragment)
                mFragments.add(freqSettingFragment)
                mFragments.add(lockFragment)
                mFragments.add(killFragment)
                mFragments.add(updateFragment)

                vp.adapter = MyPagerAdapter(supportFragmentManager)
                vp.offscreenPageLimit = 7
                vp.currentItem = 0
                stl.setViewPager(vp)
            }
        }
    }

    override fun getAct(): Context = this

    override fun getContentView(): Int = R.layout.activity_rfid_rf1p

    override fun initView() {
        topbar.setTitle("RF1P-RFID")
        topbar.addLeftBackImageButton().setOnClickListener { onBackPressed() }
        binded = bindService(
            Intent(this.applicationContext, RFService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        Awl = AndroidWakeLock(
            getSystemService(Context.POWER_SERVICE) as PowerManager
        )
        Awl?.WakeLock()
        mProgressDialogUtil = ProgressDialogUtil()

        mTitles = arrayOf(
            getString(R.string.inventory),
            getString(R.string.read_label),
            getString(R.string.write_label),
            getString(R.string.setting),
            getString(R.string.setting_freq),
            getString(R.string.password_lock),
            getString(R.string.destory),
            getString(R.string.update)
        )
        SoundUtil.get().init(this)
    }

    override fun initData() {

    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_F10) {
            if (mFragments != null && mFragments.size >= 1 && RFHelper.isConnect) {
                val fragment = mFragments[0]
                (fragment as RF1PInventoryFragment).onKeyDown()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_F10) {
            if (mFragments != null && mFragments.size >= 1 && RFHelper.isConnect) {
                val fragment = mFragments[0]
                (fragment as RF1PInventoryFragment).onKeyUp()
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        Awl?.ReleaseWakeLock()
        if (binded) {
            unbindService(serviceConnection)
        }
        super.onDestroy()

    }

    override fun onBackPressed() {
        val fragment = mFragments[0]  as RF1PInventoryFragment
        if(fragment.inventorying){
            showProgress("正在停止")
            fragment.stopInventory()
            Handler().postDelayed({
                finish()
            },3000)
        }else{
            super.onBackPressed()
        }
    }

    private lateinit var mTitles: Array<String>
    private val mFragments = ArrayList<Fragment>()

    private inner class MyPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return mFragments[position]
        }

        override fun getCount(): Int {
            return mFragments.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mTitles[position]
        }
    }

    @Subscriber(tag = EventBusConfig.HANDLE_LOST, mode = ThreadMode.ASYNC)
    public fun handleLost(string: String) {
        rf1pInventoryFragment?.stopInventory()
        rf1pInventoryFragment?.stopTimer()
        binder?.disconnect()
    }

    @Subscriber(tag = EventBusConfig.ERROR_STATUS, mode = ThreadMode.MAIN)
    public fun showErrorStatus(str:String) {
        if(!isDestroyed) {
            tipDialog.setTipMsg(str)
                .setOperation("确定")
                .show()
        }
    }

    @Subscriber(tag = EventBusConfig.TRANS_ERROR, mode = ThreadMode.MAIN)
    public fun showTransError(str:String) {
        if(!isDestroyed) {
            tipDialog.setTipMsg(str)
                .setOperation("确定")
                .show()
        }
    }

}
