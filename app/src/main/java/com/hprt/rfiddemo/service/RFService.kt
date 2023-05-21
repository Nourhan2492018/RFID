package com.hprt.rfiddemo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.ext.logFile
import com.hprt.lib_rfid.listener.*
import com.hprt.lib_rfid.utils.ByteUtils
import com.hprt.lib_rfid.utils.ProtocolUtil
import com.hprt.lib_rfid.utils.SLR5100ProtocolUtil
import com.hprt.lib_rfid.utils.ThreadExecutors
import com.hprt.rfiddemo.config.EventBusConfig
import com.hprt.rfiddemo.listener.*
import kotlinx.android.synthetic.main.activity_main_rf1p.*
import org.simple.eventbus.EventBus
import java.lang.Exception
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class RFService:Service() {
    private val binder by lazy { RFBinder() }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //后台service启动时需要
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel("40","App Service", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val builder = NotificationCompat.Builder(this, "40")
            startForeground(2, builder.build())
        }

        try {
            var intent2 = IntentFilter()
            intent2.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            intent2.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            intent2.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            intent2.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            intent2.addAction(BluetoothDevice.ACTION_FOUND)
            intent2.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            applicationContext.registerReceiver(mReceiver, intent2)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }


    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        }
        super.onDestroy()

    }

    inner class RFBinder : Binder(){
        var mFastDataListener: FastDataListener?=null
        var mGetTagListener: GetTagListener?=null
        var mSettingListener: SettingListener?=null
        var mFreqSettingListener: FreqSettingListener?=null
        var mReadTagSingleListener: ReadTagSingleListener?=null
        var mReadTagMultipleListener: ReadTagMultipleListener?=null
        @Volatile
        var inventoryStatus = false

        fun connect(path:String, baudrate:Int, listener:ConnectListener){
            ThreadExecutors.transThread.execute {
                RFHelper.connect(path, baudrate, listener)
                RFHelper.getRFIDData(mRfidDataListener)
                listenTransStatus()
            }
        }

        fun connect(mac:String, listener:ConnectListener){
            ThreadExecutors.transThread.execute {
                RFHelper.connect(mac, listener)
                RFHelper.getRFIDData(mRfidDataListener)
                listenTransStatus()
            }
        }

        fun disconnect(){
            RFHelper.disconnect()
        }

        fun poweroff(){
            RFHelper.poweroff()
        }

        fun fastInventory(listener:FastDataListener){
            inventoryStatus = true
            this.mFastDataListener = listener
            ThreadExecutors.transThread.execute {
                RFHelper.fastInventory()
            }
        }

        fun smartFastInventory(speed:Int){
            ThreadExecutors.transThread.execute {
                if(inventoryStatus) {
                    RFHelper.fastInventory(speed)
                }
            }
        }

        fun stopFastInventory(){
            ThreadExecutors.transThread.execute {
                RFHelper.stopFastInventory()
                inventoryStatus = false
            }
        }
        fun readStorageArea( timeout: Int,
                             option: Int,
                             metadataFlags: ByteArray,
                             readMemBank: Int,
                             readAddr: ByteArray,
                             wordCount: Int,
                             password: ByteArray,
                             tagSingulationFields: ByteArray, listener: GetTagListener){
            this.mGetTagListener = listener
            RFHelper.readStorageArea(timeout, option, metadataFlags, readMemBank, readAddr, wordCount, password, tagSingulationFields)
        }

        fun writeTagData(
            timeout: Int,
            option: Int,
            writeAddr: ByteArray,
            writeMemBank: ByteArray,
            writeData: ByteArray,
            password: ByteArray,
            tagSingulationFields: ByteArray
        ) {
            RFHelper.writeTagData(timeout, option, writeAddr, writeMemBank, writeData, password, tagSingulationFields)
        }

        fun writeTagEPCData(
            timeout: Int,
            option: Int,
            password: ByteArray,
            tagSingulationFields: ByteArray,
            epcid: ByteArray
        ) {
            RFHelper.writeTagEPCData(timeout, option, password, tagSingulationFields, epcid)
        }


        fun getAntPower(listener: SettingListener) {
            this.mSettingListener = listener
            RFHelper.getAntPower()
        }

        fun setPower(readPower: Int, writePower: Int, listener: SettingListener) {
            this.mSettingListener = listener
            RFHelper.setPower(readPower, writePower)
        }

        fun getHandlePower(listener:BatteryListener) {
            RFHelper.getControlBattery(listener)
        }

        fun buzzer(sound:Int){
            RFHelper.controlBuzzer(sound)
        }

        fun getFrequency(){
            RFHelper.getFrequency()
        }

        fun getHoppingFrequency(){
            RFHelper.getHoppingFrequency()
        }

        fun setHoppingFrequency(code: ByteArray, listener: FreqSettingListener) {
            this.mFreqSettingListener = listener
            RFHelper.setHoppingFrequency(code)
        }

        fun setFrequency(code: ByteArray, listener: FreqSettingListener) {
            this.mFreqSettingListener = listener
            RFHelper.setFrequency(code)
        }

        fun KillTag(
            timeout: Int,
            option: Int,
            password: ByteArray,
            rfu: ByteArray,
            tagSingulationFields: ByteArray
        ) {
            RFHelper.KillTag(timeout, option, password, rfu, tagSingulationFields)
        }

        fun lockTag(
            timeout: Int,
            option: Int,
            password: ByteArray,
            maskbits: ByteArray,
            actionBits: ByteArray,
            tagSingulationFields: ByteArray
        ) {
            RFHelper.lockTag(
                timeout,
                option,
                password,
                maskbits,
                actionBits,
                tagSingulationFields
            )
        }

        fun getHandleVersion(listener: VersionListener){
            RFHelper.getControlVersion(listener)
        }

        fun goToUpdate(){
            RFHelper.controlToUpdate()
        }

        fun updateFirmware(path: String, listener: UpdateListener){
            RFHelper.updateControlFirmware(path, listener)
        }

        fun singleInventory(
            timeout: Int,
            option: ByteArray,
            metadataflags: ByteArray,
            tagSingulationFields: ByteArray,
            listener:ReadTagSingleListener
        ){
            this.mReadTagSingleListener = listener
            RFHelper.singleInventory(timeout, option, metadataflags, tagSingulationFields)
        }

        fun multiInventory(listener:ReadTagMultipleListener){
            this.mReadTagMultipleListener = listener
            RFHelper.multiInventory()
        }

        fun getTagInfo(){
            RFHelper.getTagInfo()
        }

        fun controlTwinkle(){
            RFHelper.controlTwinkle()
        }

        fun getScannerCode(listener:ScannerCodeListener){
            RFHelper.getScannerCode(listener)
        }

        fun checkHandle(){
            ThreadExecutors.scheduledThread.execute {
                while (true){
                    val result = FileIOUtils.readFile2String("/sys/devices/platform/10010000.kp/TSTBASE")
                    if(result!= null && (result.equals("1") || result.equals("1\n"))){
                        //手柄已连接
                    }else{
                        ThreadExecutors.mainThread.execute {
                            EventBus.getDefault().post("lost",EventBusConfig.HANDLE_LOST)
                            ToastUtils.showShort("手柄断开")
                        }
                        binder?.stopFastInventory()
                        binder.disconnect()
                        break
                    }
                    sleep(5*1000)
                }
            }
        }

        fun listenTransStatus(){
            RFHelper.setTransStatusListener(object:TransStatusListener{
                override fun onSendSuccess() {
                }

                override fun onSendFail(e: Exception) {
                    EventBus.getDefault().post("发送指令失败：${e.message},请重新连接后再试",EventBusConfig.TRANS_ERROR)
                }

                override fun onReadSuccess() {
                }

                override fun onReadFail(e: Exception) {
                    EventBus.getDefault().post("接收数据失败：${e.message},请重新连接后再试",EventBusConfig.TRANS_ERROR)
                }

            })
        }
    }



    private var mRfidDataListener = RfidDataListener {
        when(it.Command){
            0X21.toByte() ->{
                logFile("单标签读取0x21--->$it")
                binder.mReadTagSingleListener?.onData(it)
            }
            0X22.toByte() ->{
                logFile("多标签读取0x22--->$it")
                binder.mReadTagMultipleListener?.onMultiple(it)

            }
            0X23.toByte(),0X24.toByte()  ->{
                logFile("写标签--->$it")
                if (ByteUtils.bytes2ToInt_h(it.Status, 0) == 0) {
                    showToast("写入成功")
                }else{
                    showToast(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status))
                    EventBus.getDefault().post(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status),EventBusConfig.ERROR_STATUS)
                }
            }
            0X25.toByte() ->{
                logFile("LOCK_TAG 0x25--->$it")
                if (ByteUtils.bytes2ToInt_h(it.Status, 0) == 0) {
                    showToast("锁定成功")
                }else{
                    showToast(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status))
                    EventBus.getDefault().post(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status),EventBusConfig.ERROR_STATUS)
                }
            }
            0X26.toByte() ->{
                logFile("KILL_TAG 0x26--->$it")
                if (ByteUtils.bytes2ToInt_h(it.Status, 0) == 0) {
                    showToast("销毁成功")
                }else{
                    showToast(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status))
                    EventBus.getDefault().post(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status),EventBusConfig.ERROR_STATUS)
                }
            }
            0X28.toByte() ->{
                logFile("获取数据 0x28--->$it")
                binder.mGetTagListener?.onData(it)
            }
            0X29.toByte() ->{
                logFile("获取EPC 0x29--->$it")
                binder.mReadTagMultipleListener?.onTagEPC(it)
            }
            0X61.toByte() ->{
                RFHelper.controlTwinkle()
                logFile("ANT_POWER 0x61--->$it")
                binder.mSettingListener?.onAntPower(it)
            }
            0X65.toByte() ->{
                logFile("GET_HOPPING_FREQUENCY 0x65--->$it")
                binder.mFreqSettingListener?.getHoppingFrequency(it)
            }
            0X67.toByte() ->{
                logFile("GET_FREQUENCY 0x67--->$it")
                binder.mFreqSettingListener?.getFrequency(it)
            }
            0X91.toByte() ->{
                logFile("SET_POWER 0x91--->$it")
                if (ByteUtils.bytes2ToInt_h(it.Status, 0) == 0) {
                    showToast("设置成功")
                }else{
                    showToast(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status))
                    EventBus.getDefault().post(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status),EventBusConfig.ERROR_STATUS)
                }
            }
            0X97.toByte() ->{
                logFile("SET_FREQUENCY 0x97--->$it")
                if (ByteUtils.bytes2ToInt_h(it.Status, 0) == 0) {
                    showToast("设置成功")
                }else{
                    showToast(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status))
                    EventBus.getDefault().post(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status),EventBusConfig.ERROR_STATUS)
                }
            }
            0X95.toByte() ->{
                logFile("HOPPING_FREQUENCY 0x95--->$it")
                if (ByteUtils.bytes2ToInt_h(it.Status, 0) == 0) {
                    showToast("设置成功")
                }else{
                    showToast(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status))
                    EventBus.getDefault().post(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status),EventBusConfig.ERROR_STATUS)
                }
            }
            0XAA.toByte() ->{
                if (ByteUtils.bytes2ToInt_h(it.Status, 0) == 0) {
                    if (it.data[it.data.size - 2] == 0xAA.toByte() && it.data[it.data.size - 1] == 0x48.toByte()) {//启动快速模式
                        logFile("启动快速模式 0xAA48--->$it")
                        binder.mFastDataListener?.onStart(true)
                    } else if (it.data[it.data.size - 2] == 0xAA.toByte()  && it.data[it.data.size - 1] == 0x49.toByte()) {//结束快速模式
                        logFile("停止快速模式 0xAA49--->$it")
                        binder.mFastDataListener?.onStop(true)
                    } else {//取得数据
                        if(it.Header != 0xFF.toByte()) {
                            logFile("快速模式数据 0xAA--->$it")
                        }
                        binder.mFastDataListener?.onData(it)
                    }
                }else{
                    showToast(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status))
                    EventBus.getDefault().post(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(it.Status,0))+ ByteUtils.bytetohex(it.Status),EventBusConfig.ERROR_STATUS)
                }
            }
        }
    }

    private fun showToast(msg:String){
        ThreadExecutors.mainThread.execute {
            ToastUtils.showShort(msg)
        }
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    RF2Lost("BluetoothDevice ACTION_ACL_DISCONNECTED")
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    var state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    if(state == BluetoothAdapter.STATE_TURNING_OFF){
                        RF2Lost("STATE_TURNING_OFF")
                    }

                }
            }
        }
    }


    /**
     * 设备丢失
     */
    fun RF2Lost(reason:String){
        LogUtils.file("device lost---------------------$reason")
        EventBus.getDefault().post("device Lost-->$reason",EventBusConfig.DEVICE_LOST)
    }






}