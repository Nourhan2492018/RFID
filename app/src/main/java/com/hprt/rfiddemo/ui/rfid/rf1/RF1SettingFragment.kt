package com.hprt.rfiddemo.ui.rfid.rf1

import android.media.AudioManager
import android.media.SoundPool
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.listener.BatteryListener
import com.hprt.lib_rfid.model.RFIDEntity
import com.hprt.lib_rfid.utils.ByteUtils
import com.hprt.lib_rfid.utils.SLR5100ProtocolUtil
import com.hprt.lib_rfid.utils.ThreadExecutors
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.app.RfidApp
import com.hprt.rfiddemo.listener.SettingListener
import com.hprt.rfiddemo.ui.main.BaseFragment
import com.hprt.rfiddemo.service.RFService
import kotlinx.android.synthetic.main.fragment_setting.*
import java.util.*

/**
 * @author yefl
 * @date 2019/5/31.
 * description：
 * 设置频率/功率
 */
class RF1SettingFragment: BaseFragment() {
    var binder: RFService.RFBinder?=null
    companion object {
        fun newInstance() = RF1SettingFragment()
    }

    lateinit var soundPool: SoundPool

    private  var readPower: ArrayList<Int> = ArrayList()
    private  var writePower: ArrayList<Int> = ArrayList()

    override fun getLayoutId(): Int = R.layout.fragment_setting

    override fun initView() {
        // 初始化声音对象
        soundPool = SoundPool(10, AudioManager.STREAM_SYSTEM, 5)
        soundPool.load(activity, R.raw.beep333, 1)

        for(i in 5..30){
            readPower.add(i*100)
            writePower.add(i*100)
        }
        sp_read_power.attachDataSource(readPower)
        sp_read_power.selectedIndex = 14

        sp_write_power.attachDataSource(writePower)
        sp_write_power.selectedIndex = 14

        btn_get_power.setOnClickListener {
            if(!checkRFIDReady()){
                return@setOnClickListener
            }
            binder?.getAntPower(mSettingListener)
        }

        btn_setpower.setOnClickListener {
            if(!checkRFIDReady()){
                return@setOnClickListener
            }
            binder?.setPower(readPower[sp_read_power.selectedIndex], writePower[sp_write_power.selectedIndex], mSettingListener)
        }

        btn_handle_power.setOnClickListener {
            if(!checkRFIDReady()){
                return@setOnClickListener
            }
            binder?.getHandlePower(BatteryListener {
                RFHelper.controlTwinkle()
                ThreadExecutors.mainThread.execute {
                    tv_handle_power.text=it.toString()+"%"
                }
            })
        }

        switch_buzzer.setOnCheckedChangeListener { compoundButton, b ->
            if(!checkRFIDReady()){
                return@setOnCheckedChangeListener
            }
            ThreadExecutors.cachedThread.execute {
                if(b){
                    binder?.buzzer(50)
                }else{
                    binder?.buzzer(0)
                }
            }
        }
        switch_buzzer.isChecked=true
        btn_inventory_time.setOnClickListener {
            val time = et_inventory_time.text.toString().toInt()
            if(time>14) {
                SLR5100ProtocolUtil.MULTI_LABEL_INVENTORY_TIMEOUT = time
                ToastUtils.showShort("设置盘存时间" + time + "ms")
            }else{
                ToastUtils.showShort("盘存时间应该大于15ms")
            }
        }

        rg_keyboard.setOnCheckedChangeListener { _, i ->
            if(rb_long.isChecked){
                RfidApp.pressType = RfidApp.InventoryType.longPress
            }else if(rb_short.isChecked){
                RfidApp.pressType = RfidApp.InventoryType.shortPress
            }
            LogUtils.d("press--"+RfidApp.pressType)
        }


    }

    override fun initData() {
        binder= arguments?.getBinder("binder") as RFService.RFBinder
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun soundMsg(msg:String){
        ToastUtils.showShort(msg)
        soundPool.play(1, 1f, 1f, 0, 0, 1f)
    }

    var mSettingListener = object :SettingListener{
        override fun onAntPower(rfidEntity: RFIDEntity){
            val curReadPower = ByteUtils.bytes2ToInt_h(rfidEntity.data, 2)
            val curWritePower = ByteUtils.bytes2ToInt_h(rfidEntity.data, 4)
            ThreadExecutors.mainThread.execute {
                for ((index, e) in readPower.withIndex()) {
                    if (curReadPower == e) {
                        sp_read_power.selectedIndex = index
                    }
                }
                for ((index, e) in writePower.withIndex()) {
                    if (curWritePower == e) {
                        sp_write_power.selectedIndex = index
                    }
                }
                soundMsg("获取成功")
            }
        }
    }
}