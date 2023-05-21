package com.hprt.rfiddemo.ui.rfid

import android.media.AudioManager
import android.media.SoundPool
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.utils.*

import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.ui.main.BaseFragment
import com.hprt.rfiddemo.service.RFService
import kotlinx.android.synthetic.main.fragment_lock.*
import java.util.*

/**
 * @author yefl
 * @date 2019/5/31.
 * description：
 * 锁
 */
class LockFragment: BaseFragment() {
    var binder: RFService.RFBinder?=null
    companion object {
        fun newInstance() = LockFragment()
    }

    lateinit var soundPool: SoundPool

    private lateinit var lockarea: ArrayList<String>
    private lateinit var lockstyle: ArrayList<String>
    private lateinit var filterareaList: ArrayList<String>

    override fun getLayoutId(): Int = R.layout.fragment_lock

    override fun initData() {
        binder= arguments?.getBinder("binder") as RFService.RFBinder
    }

    override fun initView() {
        // 初始化声音对象
        soundPool = SoundPool(10, AudioManager.STREAM_SYSTEM, 5)
        soundPool.load(activity, R.raw.beep333, 1)

        lockarea= arrayListOf<String>(
            "销毁密码",
            "访问密码",
            "EPC区",
            "TID区",
            "用户区"
        )
        sp_lock_area.attachDataSource(lockarea)
        sp_lock_area.selectedIndex = 0


        lockstyle= arrayListOf<String>(
            "解锁定",
            "暂时锁定",
            "永久锁定"
        )

        sp_lock_style.attachDataSource(lockstyle)
        sp_lock_style.selectedIndex = 0

        filterareaList= arrayListOf<String>(
            "EPC区",
            "TID区",
            "用户区"
        )
        sp_filterarea.attachDataSource(filterareaList)
        sp_filterarea.selectedIndex = 0


        btn_lock.setOnClickListener {
            lock()
        }

    }



    fun lock(){
        if(!checkRFIDReady()){
            return
        }
        var option = 0x00
        var tagSingulationFields=byteArrayOf()
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
            tagSingulationFields = ByteUtils.mergeData(filter_addr, byteArrayOf(filter_length.text.toString().toInt().toByte()), content)
        }

        var password= byteArrayOf(0,0,0,0)
        if(et_password.text.isNullOrEmpty()){
            ToastUtils.showShort("请输入密码")
            return
        }
        password = ChangeTool.HexToByteArr(et_password.text.toString())

        var maskbits = byteArrayOf(0x00,0x00)
        var actionBits = byteArrayOf(0x00,0x00)
        when(sp_lock_area.selectedIndex){
            0-> {
                maskbits = byteArrayOf(0x03,0x00)
                when(sp_lock_style.selectedIndex){
                    0-> actionBits = byteArrayOf(0x00,0x00)
                    1-> actionBits = byteArrayOf(0x02,0x00.toByte())
                    2-> actionBits = byteArrayOf(0x01,0x20.toByte())
                }
            }
            1-> {
                maskbits = byteArrayOf(0x00, 0xC0.toByte())
                when(sp_lock_style.selectedIndex){
                    0-> actionBits = byteArrayOf(0x00,0x00)
                    1-> actionBits = byteArrayOf(0x00,0x80.toByte())
                    2-> actionBits = byteArrayOf(0x00,0x40.toByte())
                }
            }
            2-> {
                maskbits = byteArrayOf(0x00, 0x30.toByte())
                when(sp_lock_style.selectedIndex){
                    0-> actionBits = byteArrayOf(0x00,0x00)
                    1-> actionBits = byteArrayOf(0x00,0x20.toByte())
                    2-> actionBits = byteArrayOf(0x00,0x10.toByte())
                }
            }
            3-> {
                maskbits = byteArrayOf(0x00, 0x0C.toByte())
                when(sp_lock_style.selectedIndex){
                    0-> actionBits = byteArrayOf(0x00,0x00)
                    1-> actionBits = byteArrayOf(0x00,0x08.toByte())
                    2-> actionBits = byteArrayOf(0x00,0x04.toByte())
                }
            }
            4-> {
                maskbits = byteArrayOf(0x00, 0x03.toByte())
                when(sp_lock_style.selectedIndex){
                    0-> actionBits = byteArrayOf(0x00,0x00)
                    1-> actionBits = byteArrayOf(0x00,0x02.toByte())
                    2-> actionBits = byteArrayOf(0x00,0x03.toByte())
                }
            }
        }
        binder?.lockTag(1000, option, password, maskbits, actionBits, tagSingulationFields)
    }

}