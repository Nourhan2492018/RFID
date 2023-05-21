package com.hprt.rfiddemo.ui.test

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import com.blankj.utilcode.util.LogUtils
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.listener.*
import com.hprt.lib_rfid.utils.ThreadExecutors
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.service.RFService
import kotlinx.android.synthetic.main.activity_test.*
import java.lang.Exception

class TestActivity : AppCompatActivity() {
    private lateinit var binder: RFService.RFBinder
    private var binded = false
    private val serviceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder = service as RFService.RFBinder
                LogUtils.d("binder = $binder")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        binded = bindService(
            Intent(this.applicationContext, RFService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
//        RFHelper.getResult(resultListener)
        btn_connect.setOnClickListener {
            binder.connect("/dev/ttyS1", 115200, object:ConnectListener{
                override fun onSuccess() {
                    showMsg("连接成功")
                }

                override fun onFail(e: Exception) {
                    showMsg("fail:"+ e.message)
                }

            })
        }
        btn_disconnect.setOnClickListener {
            binder.disconnect()
            showMsg("")
        }
        btn_version.setOnClickListener {
            RFHelper.getControlVersion(VersionListener {
                showMsg(it)
            })
        }
        btn_sn.setOnClickListener {
            RFHelper.getControlSN(SNListener {
                showMsg(it)
            })
        }
        btn_device_id.setOnClickListener {
            RFHelper.getControlDeviceId(DeviceIdListener {
                showMsg(it)
            })
        }
        btn_battery.setOnClickListener {
            RFHelper.getControlBattery(BatteryListener {
                showMsg(it.toInt().toString())
            })
        }
        btn_power_off.setOnClickListener {
            RFHelper.poweroff()
        }
        btn_twinkle.setOnClickListener {
            RFHelper.controlTwinkle()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (binded) {
            unbindService(serviceConnection)
        }
    }




    fun showMsg(msg:String){
        ThreadExecutors.mainThread.execute {
            tv_msg.text = msg
        }
    }

    var resultListener = ResultListener {
        ThreadExecutors.mainThread.execute {
            tv_log.text = "recv --> $it"
        }
    }
}