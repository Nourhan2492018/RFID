package com.hprt.rfiddemo.ui.rfid

import android.media.AudioManager
import android.media.SoundPool
import com.blankj.utilcode.util.ToastUtils

import com.hprt.lib_rfid.utils.ByteUtils
import com.hprt.lib_rfid.utils.ChangeTool
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.ui.main.BaseFragment
import com.hprt.rfiddemo.service.RFService
import kotlinx.android.synthetic.main.fragment_kill.*
import java.util.*

/**
 * @author yefl
 * @date 2019/5/31.
 * description：
 * 销毁
 */
class KillFragment: BaseFragment() {
    var binder: RFService.RFBinder?=null
    companion object {
        fun newInstance() = KillFragment()
    }

    lateinit var soundPool: SoundPool

    private lateinit var filterareaList: ArrayList<String>

    override fun getLayoutId(): Int = R.layout.fragment_kill

    override fun initData() {
        binder= arguments?.getBinder("binder") as RFService.RFBinder
    }

    override fun initView() {
        // 初始化声音对象
        soundPool = SoundPool(10, AudioManager.STREAM_SYSTEM, 5)
        soundPool.load(activity, R.raw.beep333, 1)

        filterareaList= arrayListOf<String>(
            "EPC区",
            "TID区",
            "用户区"
        )
        sp_filterarea.attachDataSource(filterareaList)
        sp_filterarea.selectedIndex = 0


        btn_destory.setOnClickListener {
            kill();
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun kill(){
        if(!checkRFIDReady()){
            return
        }
        if(et_password.text.isNullOrEmpty()) {
            ToastUtils.showShort("输入密码")
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
            tagSingulationFields = ByteUtils.mergeData(filter_addr, content)
        }

        var password= byteArrayOf(0,0,0,0)
            if(et_password.text.isNullOrEmpty()){
                ToastUtils.showShort("请输入密码")
                return
            }
        password = ChangeTool.HexToByteArr(et_password.text.toString())


        var rfu = byteArrayOf(0x00);
        binder?.KillTag(1000, option,  password,rfu,tagSingulationFields)
    }











}