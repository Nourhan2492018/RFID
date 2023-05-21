package com.hprt.rfiddemo.ui.main.rf2

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.listener.ConnectListener
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.ui.discovery.RF2DeviceListActivity
import com.hprt.rfiddemo.ui.rfid.rf2.RF2RfidActivity
import com.hprt.rfiddemo.service.RFService
import com.hprt.rfiddemo.ui.base.BaseActivity
import com.tbruyelle.rxpermissions2.RxPermissions

import kotlinx.android.synthetic.main.activity_main_rf2.*
import java.lang.Exception

/**
 * RF2-1200
 */
class RF2MainActivity : BaseActivity() {

    private var bluetoothDevice: BluetoothDevice? = null

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

    override fun getAct(): Context {
        return this
    }

    override fun getContentView(): Int = R.layout.activity_main_rf2

    override fun initView() {
        topbar.setTitle("RF2功能展示")
        topbar.addLeftBackImageButton().setOnClickListener { onBackPressed() }
        binded = bindService(
            Intent(this.applicationContext, RFService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        RxPermissions(this).request(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)
            .subscribe {
                if (it) {

                }
            }
        btn_discovery.setOnClickListener {    discovery()   }
        btn_connect.setOnClickListener {    connect()   }
        btn_disconnect.setOnClickListener { disconnect()}
        btn_rfid.setOnClickListener { toRfid() }
        btn_scan.setOnClickListener { toScan() }


    }

    override fun initData() {

    }

    override fun onDestroy() {
        if (binded) {
            unbindService(serviceConnection)
        }
        disconnect()
        dismissProgress()
        super.onDestroy()
    }

    private fun discovery(){
        startActivityForResult(Intent(baseContext, RF2DeviceListActivity::class.java),1001)
    }

    private fun connect(){
        showProgress("正在连接")
        binder?.connect(bluetoothDevice!!.address, object:ConnectListener{
            override fun onSuccess() {
                dismissProgress()
                tv_content.text = "连接成功";
            }

            override fun onFail(e: Exception) {
                dismissProgress()
                tv_content.text = "连接失败";
            }

        })
    }

    private fun disconnect(){
        binder?.disconnect()
        tv_content.text = "连接断开";
    }

    private fun toRfid(){
        if(RFHelper.isConnect){
            startActivity(Intent(baseContext, RF2RfidActivity::class.java))
        }else{
            ToastUtils.showShort("请先连接")
        }
    }

    private fun toScan(){

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                1001->{
                    bluetoothDevice = data?.getParcelableExtra("bluetooth")
                    tv_content.text = "设备：${bluetoothDevice?.name}    mac=${bluetoothDevice?.address}"
                }
            }
        }
    }


}
