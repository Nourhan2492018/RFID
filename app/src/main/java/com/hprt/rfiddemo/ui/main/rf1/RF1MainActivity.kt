package com.hprt.rfiddemo.ui.main.rf1

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.listener.ConnectListener
import com.hprt.lib_rfid.utils.RxTimerUtil
import com.hprt.lib_rfid.utils.SoundUtil
import com.hprt.lib_rfid.utils.ThreadExecutors
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.service.RFService
import com.hprt.rfiddemo.ui.base.BaseActivity
import com.hprt.rfiddemo.ui.rfid.rf1.RF1RfidActivity
import com.hprt.rfiddemo.utils.MyUtil
import com.hprt.rfiddemo.widget.TipDialog
import kotlinx.android.synthetic.main.activity_main_rf1.*
import org.simple.eventbus.Subscriber
import org.simple.eventbus.ThreadMode

/**
 * RF1-5100
 */
class RF1MainActivity : BaseActivity() {
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
            }
        }
    }


    override fun getAct(): Context = this

    override fun getContentView(): Int = R.layout.activity_main_rf1

    override fun initView() {
        topbar.setTitle("RF1功能展示")
        topbar.addLeftBackImageButton().setOnClickListener { onBackPressed() }
        binded = bindService(
            Intent(this.applicationContext, RFService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        SoundUtil.get().init(this)

        btn_connect.setOnClickListener {
            connect()
        }
        btn_disconnect.setOnClickListener {
            disconnect()
        }
        btn_rfid.setOnClickListener {
            startActivity(Intent(baseContext, RF1RfidActivity::class.java))
        }

        btn_power_off.setOnClickListener {
            binder.poweroff()
            btn_connect.isEnabled = true
            tv_content.text = "未连接"
            RFHelper.isConnect = false
            dismissProgress()
        }

    }

    override fun initData() {

    }

    override fun onDestroy() {
        if (binded) {
            unbindService(serviceConnection)
        }
        disconnect()
        dismissProgress()
        MyUtil.startScan(this)
        super.onDestroy()

    }

    private var pressedTime = 0L
    override fun onBackPressed() {
        if (isTaskRoot) {
            val tempTime = System.currentTimeMillis()
            if (tempTime - pressedTime > 2000) {
                ToastUtils.showShort("再按一次退出程序")
                pressedTime = tempTime
            } else {
                finish()
            }
        } else {
            super.onBackPressed()
        }
    }



    private fun connect(){
        showProgress("正在连接")
        binder.connect("/dev/ttyS1", 115200, object: ConnectListener {
            override fun onSuccess() {
                dismissProgress()
                ThreadExecutors.mainThread.execute {
                    tv_content.text = "已连接"
                    btn_connect.isEnabled = false
                    tipDialog?.dismiss()
                }

            }

            override fun onFail(e: Exception) {
                dismissProgress()
                RxTimerUtil.cancel()
                ThreadExecutors.mainThread.execute {
                    tipDialog?.dismiss()
                    tipDialog?.setTipMsg("连接失败").show()
                }
            }

        })
    }

    /**
     * 断开连接
     */
    fun disconnect(){
        binder.disconnect()
        btn_connect.isEnabled = true
        tv_content.text = "连接断开"
        dismissProgress()
        tipDialog?.dismiss()
    }

    @Subscriber(tag = "handleLost", mode = ThreadMode.ASYNC)
    public fun handleLost(str:String) {
        binder?.stopFastInventory()
        binder?.disconnect()
        if(!isDestroyed){
            btn_connect.isEnabled = true
            tv_content.text = "连接断开"
            dismissProgress()
            tipDialog?.dismiss()
        }
    }





}
