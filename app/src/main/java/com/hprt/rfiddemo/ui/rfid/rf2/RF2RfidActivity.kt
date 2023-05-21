package com.hprt.rfiddemo.ui.rfid.rf2

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
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.listener.ScannerCodeListener
import com.hprt.lib_rfid.listener.ScannerKeyStatusListener
import com.hprt.lib_rfid.utils.ByteUtils
import com.hprt.lib_rfid.utils.SoundUtil
import com.hprt.lib_rfid.utils.ThreadExecutors
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.config.AndroidWakeLock
import com.hprt.rfiddemo.config.EventBusConfig
import com.hprt.rfiddemo.service.RFService
import com.hprt.rfiddemo.ui.base.BaseActivity
import com.hprt.rfiddemo.ui.rfid.*
import com.hprt.rfiddemo.utils.ProgressDialogUtil
import com.hprt.rfiddemo.widget.TipDialog
import kotlinx.android.synthetic.main.activity_rfid_rf2.*
import org.simple.eventbus.Subscriber
import org.simple.eventbus.ThreadMode
import java.util.*

class RF2RfidActivity : BaseActivity() {
    var Awl: AndroidWakeLock? = null
    var mProgressDialogUtil:ProgressDialogUtil?=null

    var rf2InventoryFragment = RF2InventoryFragment.newInstance()
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
                rf2InventoryFragment.arguments = bundle
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

                mFragments.add(rf2InventoryFragment)
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

    override fun getContentView(): Int = R.layout.activity_rfid_rf2

    override fun initView() {
        topbar.setTitle("RF2-RFID")
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
        RFHelper.getScannerKeyStatus(listener = ScannerKeyStatusListener {
            if(it == 0x00.toByte()){
                getScannerCode()
            }
        })
    }

    override fun initData() {

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_F10) {
            if (mFragments != null && mFragments.size >= 1 && RFHelper.isConnect) {
                val fragment = mFragments[0]
                (fragment as RF2InventoryFragment).onKeyDown()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_F10) {
            if (mFragments != null && mFragments.size >= 1 && RFHelper.isConnect) {
                val fragment = mFragments[0]
                (fragment as RF2InventoryFragment).onKeyUp()
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
        val fragment = mFragments[0]  as RF2InventoryFragment
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

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
    }

    fun getScannerCode(){
        binder.getScannerCode(listener = ScannerCodeListener {
            ThreadExecutors.mainThread.execute {
                if(it.isNotEmpty()) {
                    ToastUtils.showShort(ByteUtils.bytetohex(it))
                }else{
                    ToastUtils.showShort("nothing")
                }
            }
        })
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

    @Subscriber(tag = EventBusConfig.DEVICE_LOST, mode = ThreadMode.MAIN)
    public fun deviceLost(str:String) {
        if(!isDestroyed) {
            tipDialog.setTipMsg(str)
                .setOperation("确定")
                .show()
        }
    }



}
