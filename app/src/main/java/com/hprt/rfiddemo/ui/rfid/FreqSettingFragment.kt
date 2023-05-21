package com.hprt.rfiddemo.ui.rfid

import android.media.AudioManager
import android.media.SoundPool
import android.widget.CheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.hprt.lib_rfid.model.RFIDEntity
import com.hprt.lib_rfid.utils.ByteUtils
import com.hprt.lib_rfid.utils.ThreadExecutors
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.listener.FreqSettingListener
import com.hprt.rfiddemo.model.HoppingFreq
import com.hprt.rfiddemo.ui.main.BaseFragment
import com.hprt.rfiddemo.service.RFService
import kotlinx.android.synthetic.main.fragment_freq_setting.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author yefl
 * @date 2019/5/31.
 * description：
 * 设置频率/功率
 */
class FreqSettingFragment: BaseFragment() {
    var binder: RFService.RFBinder?=null
    companion object {
        fun newInstance() = FreqSettingFragment()
    }
    private lateinit var freqList: ArrayList<String>
    var hoppingFreqs: ArrayList<HoppingFreq> = ArrayList<HoppingFreq>()

    lateinit var adapter:BaseQuickAdapter<HoppingFreq, BaseViewHolder>

    override fun getLayoutId(): Int = R.layout.fragment_freq_setting

    override fun initView() {
        adapter = object :BaseQuickAdapter<HoppingFreq, BaseViewHolder>(R.layout.item_hopping_freq, hoppingFreqs){
            override fun convert(helper: BaseViewHolder, item: HoppingFreq) {
                helper.setText(R.id.tv_freq, item.freq.toString())
                var checkBox = helper.getView<CheckBox>(R.id.cb_choose)
                checkBox.isChecked = item.checked
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.setOnItemClickListener { adapter, view, position ->
            var cbox = view.findViewById<CheckBox>(R.id.cb_choose)
            cbox.isChecked = !cbox.isChecked
            hoppingFreqs[position].checked = cbox.isChecked
        }

        freqList= arrayListOf<String>(
            resources.getString(R.string.china_freq_1),
            resources.getString(R.string.china_freq_2),
            resources.getString(R.string.north_america_freq),
            resources.getString(R.string.europe_freq)
        )
        sp_freq.attachDataSource(freqList)
        sp_freq.selectedIndex = 0

        btn_get_freq.setOnClickListener {
            read_freq()
        }

        btn_set_freq.setOnClickListener {
            set_freq()
        }
        get_freq.setOnClickListener {
            checkRFIDReady().apply {
                binder?.getHoppingFrequency()
            }

        }

        set_freq.setOnClickListener {
            checkRFIDReady().apply {
                var data = byteArrayOf()
                for (hf in hoppingFreqs){
                    if(hf.checked){
                        var d = ByteUtils.intToBytes4_h(hf.freq)
                        data += d
                    }
                }
                binder?.setHoppingFrequency(data, mFreqSettingListener)
            }
        }


    }

    override fun initData() {
        binder= arguments?.getBinder("binder") as RFService.RFBinder
    }

    fun soundMsg(msg:String){
        ToastUtils.showShort(msg)
    }

    /**
     * 读取工作频率
     */
    fun read_freq(){
        if(!checkRFIDReady()){
            return
        }
        binder?.getFrequency()
    }

    /**
     * 设置工作频率
     */
    fun set_freq(){
        if(!checkRFIDReady()){
            return
        }
        var code:Int=0x01
        when(sp_freq.selectedIndex){
            0->code=0x06
            1->code=0x0a
            2->code=0x01
            3->code=0x08
        }
        binder?.setFrequency(byteArrayOf(code.toByte()), mFreqSettingListener)
    }

    var mFreqSettingListener = object : FreqSettingListener{
        override fun getFrequency(rfidEntity: RFIDEntity) {
            ThreadExecutors.mainThread.execute {
                var index: Int = 0
                when (rfidEntity.data[0]) {
                    0x06.toByte() -> index = 0
                    0x0a.toByte() -> index = 1
                    0x01.toByte() -> index = 2
                    0x08.toByte() -> index = 3
                }
                sp_freq.selectedIndex = index
                soundMsg("获取成功")
            }
        }

        override fun getHoppingFrequency(rfidEntity: RFIDEntity) {
            hoppingFreqs.clear()
            ThreadExecutors.mainThread.execute {
                var freqData = rfidEntity.data
                var array = IntArray(freqData.size/4);
                for (i in 0 until freqData.size / 4) {
                    var value = ByteUtils.bytes4ToInt_h(freqData, i * 4);
                    array[i] = value

                }
                Arrays.sort(array)
                for(element in array) {
                    hoppingFreqs.add(HoppingFreq(element, false))
                }
                adapter.notifyDataSetChanged()
                soundMsg("获取成功")
            }
        }
    }

}