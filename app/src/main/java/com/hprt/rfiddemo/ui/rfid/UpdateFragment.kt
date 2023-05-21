package com.hprt.rfiddemo.ui.rfid

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import com.hprt.lib_rfid.utils.*

import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.ui.main.BaseFragment
import kotlinx.android.synthetic.main.fragment_update.*
import java.util.*
import com.leon.lfilepickerlibrary.LFilePicker
import com.blankj.utilcode.util.*
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.listener.ConnectListener
import com.hprt.lib_rfid.listener.UpdateListener
import com.hprt.lib_rfid.listener.VersionListener
import com.hprt.rfiddemo.service.RFService
import com.hprt.rfiddemo.widget.UpdateDialog
import com.leon.lfilepickerlibrary.utils.Constant
import com.tbruyelle.rxpermissions2.RxPermissions
import java.lang.Exception
import kotlin.concurrent.thread

/**
 * @author yefl
 * @date 2019/5/31.
 * description：
 * 固件升级
 */
class UpdateFragment: BaseFragment() {
    var binder: RFService.RFBinder?=null
    companion object {
        fun newInstance() = UpdateFragment()
    }
    val REQUESTCODE_HANDLE = 1002
    val REQUESTCODE_HANDLE2 = 1003

    private lateinit var firmdata: ArrayList<ByteArray>

    lateinit var firmPath: String

    private val updateDialog by lazy { UpdateDialog(context!!) }

    override fun getLayoutId(): Int = R.layout.fragment_update

    override fun initData() {
        binder= arguments?.getBinder("binder") as RFService.RFBinder
    }

    override fun initView() {
        btn_handle_version.setOnClickListener {
            if(!checkRFIDReady()){
                return@setOnClickListener
            }
            binder?.getHandleVersion(VersionListener{
                ThreadExecutors.mainThread.execute {
                    tv_handle_version.setText("手柄固件:$it")
                }
            })
        }

        btn_handle_update.setOnClickListener {
            if (!RFHelper.isConnect) {
                RFHelper.connect("/dev/ttyS1", 115200, object :ConnectListener{
                    override fun onSuccess() {
                        TODO("Not yet implemented")
                    }

                    override fun onFail(e: Exception) {
                        TODO("Not yet implemented")
                    }

                })
            }
            RFHelper.stopHandSark = true
            RxPermissions(this).request(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).subscribe {
                if(it){
                    LFilePicker().withSupportFragment(this@UpdateFragment)
                        .withRequestCode(REQUESTCODE_HANDLE)
                        .withMutilyMode(false)
                        .withTitle("选择固件")
                        .withFileFilter(arrayOf(".bin"))
                        .start()
                }else{
                    ToastUtils.showShort("需要打开读写存储权限")
                }
            }
        }

        btn_handle_update2.setOnClickListener{
            if (!RFHelper.isConnect) {
                RFHelper.connect("/dev/ttyS1", 115200, object :ConnectListener{
                    override fun onSuccess() {

                    }

                    override fun onFail(e: Exception) {

                    }

                })
            }
            RxPermissions(this).request(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).subscribe {
                if(it){
                    LFilePicker().withSupportFragment(this@UpdateFragment)
                        .withRequestCode(REQUESTCODE_HANDLE2)
                        .withMutilyMode(false)
                        .withTitle("选择固件")
                        .withFileFilter(arrayOf(".bin"))
                        .start()
                }else{
                    ToastUtils.showShort("需要打开读写存储权限")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when(requestCode){
                REQUESTCODE_HANDLE->{
                    ToastUtils.showShort(data!!.getStringArrayListExtra(Constant.RESULT_INFO).get(0).toString())
                    firmPath = data!!.getStringArrayListExtra(Constant.RESULT_INFO)[0].toString()
                    binder?.goToUpdate()
                    thread{
                        while (true){
                            if(RFHelper.updateMode) {
                                break
                            }
                        }
                        updateFirmware(firmPath)
                    }
                }
                REQUESTCODE_HANDLE2->{//已经进入第一道， 强刷固件
                    ToastUtils.showShort(data!!.getStringArrayListExtra(Constant.RESULT_INFO).get(0).toString())
                    firmPath = data!!.getStringArrayListExtra(Constant.RESULT_INFO).get(0).toString()
                    RFHelper.updateMode = true
                    updateFirmware(firmPath)
                }
            }
        }
    }

    fun updateFirmware(path:String){
        binder?.updateFirmware(path, object :UpdateListener{
            override fun onProgress(progress: Float) {
                ThreadExecutors.mainThread.execute {
                    if(progress<100) {
                        updateDialog.setProgress(progress).show()
                    }else{
                        updateDialog.dismiss()
                        ToastUtils.showLong("升级成功")
                        RFHelper.updateMode = false
                        RFHelper.stopHandSark = false
                    }
                }
            }

            override fun onError(e: Exception) {
                ThreadExecutors.mainThread.execute {
                    ToastUtils.showLong(e.message)
                }
            }

        })
    }

    var num = 0//标记当前发送第几包

    fun getRfidVersion(data:ByteArray){
        val boot_ver_arr = ByteArray(4)
        val hardware_ver_arr = ByteArray(4)
        val firmware_data_arr = ByteArray(4)
        val firmware_ver_arr = ByteArray(4)
        System.arraycopy(data, 5, boot_ver_arr,0, 4)
        System.arraycopy(data, 9, hardware_ver_arr,0, 4)
        System.arraycopy(data, 13, firmware_data_arr,0, 4)
        System.arraycopy(data, 17, firmware_ver_arr,0, 4)
        val boot_ver = String.format("%02X", boot_ver_arr[0])+"."+String.format("%02X", boot_ver_arr[1])+"."+String.format("%02X", boot_ver_arr[2])+"."+String.format("%02X", boot_ver_arr[3])
        val hardware_ver = String.format("%02X", hardware_ver_arr[0])+"."+String.format("%02X", hardware_ver_arr[1])+"."+String.format("%02X", hardware_ver_arr[2])+"."+String.format("%02X", hardware_ver_arr[3])
        val firmware_data = String.format("%02X", firmware_data_arr[0])+String.format("%02X", firmware_data_arr[1])+"."+String.format("%02X", firmware_data_arr[2])+"."+String.format("%02X", firmware_data_arr[3])
        val firmware_ver = String.format("%02X", firmware_ver_arr[0])+"."+String.format("%02X", firmware_ver_arr[1])+"."+String.format("%02X", firmware_ver_arr[2])+"."+String.format("%02X", firmware_ver_arr[3])
        tv_rfid_version.setText("Bootloader ver:"+boot_ver+"\n")
        tv_rfid_version.append("Hardware Ver:"+hardware_ver+"\n")
        tv_rfid_version.append("Firmware data:"+firmware_data+"\n")
        tv_rfid_version.append("Firmware Version:"+firmware_ver+"\n")
    }

}