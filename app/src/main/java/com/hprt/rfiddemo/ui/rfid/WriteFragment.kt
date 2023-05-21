package com.hprt.rfiddemo.ui.rfid

import android.media.AudioManager
import android.media.SoundPool
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.utils.*

import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.ui.main.BaseFragment
import com.hprt.rfiddemo.service.RFService
import kotlinx.android.synthetic.main.fragment_write.*
import java.util.*

/**
 * @author yefl
 * 读标签
 */
class WriteFragment: BaseFragment() {
    var binder: RFService.RFBinder?=null
    companion object {
        fun newInstance() = WriteFragment()
    }

    lateinit var soundPool: SoundPool

    private lateinit var areaList: ArrayList<String>
    private lateinit var filterareaList: ArrayList<String>

    override fun getLayoutId(): Int = R.layout.fragment_write

    override fun initData() {
        binder= arguments?.getBinder("binder") as RFService.RFBinder
    }

    override fun initView() {
        // 初始化声音对象
        soundPool = SoundPool(10, AudioManager.STREAM_SYSTEM, 5)
        soundPool.load(activity, R.raw.beep333, 1)

        areaList= arrayListOf<String>(
            "Reserved区",
            "EPC区",
            "TID区",
            "用户区"
        )
        sp_area.attachDataSource(areaList)
        sp_area.selectedIndex = 0

        filterareaList= arrayListOf<String>(
            "EPC区",
            "TID区",
            "用户区"
        )
        sp_filterarea.attachDataSource(filterareaList)
        sp_filterarea.selectedIndex = 0


        btn_write.setOnClickListener {
            writeTag()
        }

        btn_write_epc.setOnClickListener {
            writeEPC()
        }
    }

    fun writeTag(){
        if(!checkRFIDReady()){
            return
        }
        if(et_content.text.isNullOrEmpty()) {
            ToastUtils.showShort("输入写入内容")
            return
        }
        var option = 0x00

        var password= byteArrayOf(0,0,0,0)
        if(ck_password.isChecked){//开启密码
            if(et_password.text.isNullOrEmpty()){
                ToastUtils.showShort("请输入密码")
                return
            }
            password = ChangeTool.HexToByteArr(et_password.text.toString())
            option = 0x05
        }


        var tagSingulationFields=byteArrayOf()
        var writeData =  ChangeTool.HexToByteArr(et_content.text.toString())
        if(ck_filter.isChecked){//开启过滤

            if(et_filter_addr.text.isNullOrEmpty()) {
                ToastUtils.showShort("输入起始地址")
                return
            }
            if(et_filter_content.text.isNullOrEmpty()) {
                ToastUtils.showShort("输入过滤数据")
                return
            }

            when(sp_filterarea.selectedIndex){
                0-> option = 4 //epc 过滤04
                1-> option = 2
                2-> option = 3
            }

            var filter_addr = ByteUtils.intToBytes4_h(et_filter_addr.text.toString().toInt())
            var content = ChangeTool.HexToByteArr(et_filter_content.text.toString())
            tagSingulationFields = ByteUtils.mergeData(filter_addr, byteArrayOf(content.size.toByte()), content)
        }


        var write_membank = byteArrayOf(sp_area.selectedIndex.toByte())
        var addr = et_addr.text.toString().toInt()

        binder?.writeTagData(1000, option, ByteUtils.intToBytes4_h(addr), write_membank, writeData, password, tagSingulationFields)
    }



    fun writeEPC(){
        if(!checkRFIDReady()){
            return
        }
        if(et_content.text.isNullOrEmpty()) {
            ToastUtils.showShort("输入写入内容")
            return
        }
        var option = 0x01

        var password= byteArrayOf(0,0,0,0)
        if(ck_password.isChecked){//开启密码
            if(et_password.text.isNullOrEmpty()){
                ToastUtils.showShort("请输入密码")
                return
            }
            password = ChangeTool.HexToByteArr(et_password.text.toString())
            option = 0x05
        }

        var tagSingulationFields=byteArrayOf()
        var epcid =  ChangeTool.HexToByteArr(et_content.text.toString())
        if(ck_filter.isChecked){//开启过滤

            if(et_filter_addr.text.isNullOrEmpty()) {
                ToastUtils.showShort("输入起始地址")
                return
            }
            if(et_filter_content.text.isNullOrEmpty()) {
                ToastUtils.showShort("输入过滤数据")
                return
            }

            when(sp_filterarea.selectedIndex){
                0-> option = 4 //epc 过滤04
                1-> option = 2
                2-> option = 3
            }

            var filter_addr = ByteUtils.intToBytes4_h(et_filter_addr.text.toString().toInt())
            var content = ChangeTool.HexToByteArr(et_filter_content.text.toString())
            tagSingulationFields = ByteUtils.mergeData(filter_addr, byteArrayOf(content.size.toByte()), content)
        }
        binder?.writeTagEPCData(1000, option, password, tagSingulationFields, epcid)
    }



}