package com.hprt.rfiddemo.ui.rfid

import android.media.AudioManager
import android.media.SoundPool
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.model.RFIDEntity
import com.hprt.lib_rfid.utils.ByteUtils
import com.hprt.lib_rfid.utils.ChangeTool
import com.hprt.lib_rfid.utils.ThreadExecutors
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.listener.GetTagListener
import com.hprt.rfiddemo.ui.main.BaseFragment
import com.hprt.rfiddemo.service.RFService
import kotlinx.android.synthetic.main.fragment_read.*
import java.util.*

/**
 * @author yefl
 * @date 2019/5/31.
 * description：
 * 读标签
 */
class ReadFragment: BaseFragment() {
    var binder: RFService.RFBinder?=null
    companion object {
        fun newInstance() = ReadFragment()
    }

    lateinit var soundPool: SoundPool

    private lateinit var areaList: ArrayList<String>
    private lateinit var filterareaList: ArrayList<String>

    override fun getLayoutId(): Int = R.layout.fragment_read

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



        btn_read.setOnClickListener {
            read()
        }


    }
    fun read(){
        if(!checkRFIDReady()){
            return
        }

        if(et_length.text.isNullOrEmpty()) {
            ToastUtils.showShort("输入块")
            return
        }
        if(et_addr.text.isNullOrEmpty()) {
            ToastUtils.showShort("输入起始地址")
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
            tagSingulationFields = ByteUtils.mergeData(filter_addr, byteArrayOf((content.size*8).toByte()), content)
        }

        var password= byteArrayOf(0,0,0,0)
        if(ck_password.isChecked){//开启密码
            if(et_password.text.isNullOrEmpty()){
                ToastUtils.showShort("请输入密码")
                return
            }
            password = ChangeTool.HexToByteArr(et_password.text.toString())
            option = 0x05
        }

        var read_membank = sp_area.selectedIndex
        var addr = et_addr.text.toString().toInt()
        var length = et_length.text.toString().toInt()
        var metadata = byteArrayOf()
        binder?.readStorageArea(100, option, metadata, read_membank, ByteUtils.intToBytes4_h(addr),length, password,tagSingulationFields, mGetTagListener)
    }

        var mGetTagListener = object :GetTagListener{
            override fun onData(rfidEntity: RFIDEntity) {
                ThreadExecutors.mainThread.execute {
                    //去掉option数据
                    et_content.setText(ByteUtils.bytetohex(rfidEntity.data.drop(1).toByteArray()).replace(" ",""))
                }
            }
        }

}