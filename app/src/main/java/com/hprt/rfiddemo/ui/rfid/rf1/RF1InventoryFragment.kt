package com.hprt.rfiddemo.ui.rfid.rf1

import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.model.RFIDEntity
import com.hprt.lib_rfid.utils.ByteUtils
import com.hprt.lib_rfid.utils.SLR5100ProtocolUtil
import com.hprt.lib_rfid.utils.ThreadExecutors
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.adapter.IDAdapter
import com.hprt.rfiddemo.app.RfidApp
import com.hprt.rfiddemo.config.EventBusConfig
import com.hprt.rfiddemo.ext.DialogExt
import com.hprt.rfiddemo.listener.ReadTagMultipleListener
import com.hprt.rfiddemo.listener.ReadTagSingleListener
import com.hprt.rfiddemo.model.IDModel
import com.hprt.rfiddemo.service.RFService
import com.hprt.rfiddemo.ui.main.BaseFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_inventory.*
import org.simple.eventbus.Subscriber
import org.simple.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList

/**
 * 盘点 5100
 */
class RF1InventoryFragment : BaseFragment() {
    var binder: RFService.RFBinder?=null
    var all_nums = 0
    companion object {
        fun newInstance() = RF1InventoryFragment()
    }
    @Volatile
    var idModels = CopyOnWriteArrayList<IDModel>()
    private lateinit var adapter: IDAdapter

    private var startTime: Long = 0
    private val periodTime: Long = 15

    @Volatile
    var inventorying = false// false未盘点  true 盘点中
    var timer = Timer()
    var keyDown = false

    override fun getLayoutId(): Int = R.layout.fragment_inventory

    override fun initView() {
        adapter = IDAdapter(R.layout.item_inventory, idModels)
        rv_ids.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        rv_ids.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        rv_ids.adapter = adapter

        btn_single.setOnClickListener {
            if (!checkRFIDReady()) {
                return@setOnClickListener
            }
            singleInventory()
        }

        btn_loop.setOnClickListener {
            inventory()
        }
        btn_stop.setOnClickListener {
            stopInventory()
        }
        btn_clear.setOnClickListener {
            if(inventorying) {
                stopInventory()
            }
            startTime = 0
            idModels.clear()
            adapter.notifyDataSetChanged()
            tv_all_labels.text = ""
            tv_labels.text = ""
            tv_all_time.text = "总时间："
            tv_all_nums.text = ""
            all_nums = 0
        }
    }

    override fun initData() {
        binder= arguments?.getBinder("binder") as RFService.RFBinder
    }

    override fun onDestroy() {
        timer?.cancel()
        binder?.stopFastInventory()
        inventorying = false
        super.onDestroy()
    }

    fun onKeyDown() {
        when (RfidApp.pressType) {
            RfidApp.InventoryType.longPress -> {//长按盘点
                if(!keyDown) {
                    inventory()
                    keyDown = true
                }
            }
            RfidApp.InventoryType.shortPress -> {//短按盘点, 第一次松开按键是开始盘点， 第二次松开按键是停止盘点
                if (inventorying) {
                    //停止
                    stopInventory()
                    keyDown = false
                } else {
                    if(!keyDown) {
                        inventory()
                        keyDown = true
                    }
                }
            }
        }
    }

    /**
     * 1.长按盘点时 松开按键是停止盘点
     */
    fun onKeyUp() {
        when (RfidApp.pressType) {
            RfidApp.InventoryType.longPress -> {
                LogUtils.d("onKeyUp-------")
                if (inventorying) {
                    stopInventory()
                    inventorying = false
                    timer?.cancel()
                }
                keyDown = false
            }
        }
    }

    private fun inventory() {
        if (!checkRFIDReady()) {
            return
        }
        idModels.clear()
        adapter.notifyDataSetChanged()
        if (!inventorying) {
            inventorying = true
            btn_loop.isEnabled = false
            btn_single.isEnabled = false
            inventory5100()
        }
    }
    //5100多标签盘存
    private fun inventory5100() {
        btn_loop.isEnabled = false
        btn_single.isEnabled = false
        inventorying = true
        timer = Timer()
        timer?.schedule(MyTimeTask(), 0,periodTime)
        binder?.multiInventory(mReadTagMultipleListener)
    }
    fun stopInventory() {
        ThreadExecutors.mainThread.execute {
            btn_loop.isEnabled = true
            btn_single.isEnabled = true
            dismissProgress()
        }
        inventorying = false
        timer?.cancel()
    }

    //单标签盘存
    fun singleInventory() {
        btn_single.isEnabled = false
        btn_loop.isEnabled = false
        timer = Timer()
        timer?.schedule(MyTimeTask(), 0,periodTime)
        binder?.singleInventory(
            200,
            byteArrayOf(0x10),
            byteArrayOf(0x00, 0xFF.toByte()),
            byteArrayOf(),
            mReadTagSingleListener
        )

    }

    /**
     * 解析单标签数据返回
     */
    fun resolveSingleData(rfidEntity:RFIDEntity): Observable<IDModel> {
        return Observable.create<IDModel> {
            if (ByteUtils.bytes2ToInt_h(rfidEntity.Status,0) == 0) {
                val idmodel = singleToIDModel(rfidEntity)
                it.onNext(idmodel)
                it.onComplete()
            } else {
                it.onError(Throwable(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(rfidEntity.Status,0))))

            }
        }
    }

    /**
     * 解析多标签盘存指令回复数据
     */
    fun resolveMultipleData(rfidEntity: RFIDEntity): Observable<Int> {
        return Observable.create<Int> {
            if (ByteUtils.bytes2ToInt_h(rfidEntity.Status, 0) == 0) {
                var found = rfidEntity.data[3].toInt() and 0xff
                if (found > 0) {
                    it.onNext(found)
                } else {
                    if (inventorying) {
                        RFHelper.multiInventory()
                    }
                }
                it.onComplete()
            } else {//未盘存到标签继续发
                if (inventorying) {
                    binder?.multiInventory(mReadTagMultipleListener)
                }
            }
        }
    }

    /**
     * 解析EPC数据0x29
     */
    fun resolveEpcData(rfidEntity:RFIDEntity): Observable<List<IDModel>> {
        return Observable.create<List<IDModel>> {
            if (ByteUtils.bytes2ToInt_h(rfidEntity.Status,0) == 0) {
                val labelCount = rfidEntity.data[3].toInt() and 0xff//标签数量
                if (labelCount > 0) {
                    val labelArray: ArrayList<ByteArray> = ArrayList(labelCount)
                    var labelData = rfidEntity.data.drop(4).toByteArray()
                    var len_flag = 0
                    for (i in 0 until labelCount) {
                        val epclength = ByteUtils.bytes2ToInt_h(labelData, len_flag + 15) / 8
                        var temp_label = ByteArray(17 + epclength)//读取到单个标签数据长度
                        if (labelData.size < 17 + epclength) {
                            it.onError(Throwable("err epc length=== $epclength labelDataSize = ${labelData.size}"))
                        }
                        System.arraycopy(labelData, len_flag, temp_label, 0, temp_label.size)
                        labelArray.add(temp_label)
                        len_flag += temp_label.size
                    }
                    var list = ArrayList<IDModel>()
                    for (arr in labelArray) {
                        val idmodel = convertMultiLabelBytesToIDModel(arr)
                        list.add(idmodel)
                    }
                    if (labelCount < 7) {//一次最多可以读7个标签
                        if (inventorying) {//读取完数据继续发盘点
                            binder?.multiInventory(mReadTagMultipleListener)
                        }
                    } else {
                        binder?.getTagInfo()
                    }
                    it.onNext(list)
                    it.onComplete()
                }else{
                    if (inventorying) {
                        binder?.multiInventory(mReadTagMultipleListener)
                    }
                }
            } else {
                it.onError(Throwable(SLR5100ProtocolUtil.getStatusInfo(ByteUtils.bytes2ToInt_h(rfidEntity.Status,0))))
            }
        }
    }

    private fun convertMultiLabelBytesToIDModel(it: ByteArray): IDModel {
        val readcount = it[0].toInt() and 0xff
        val rssi = it[1].toInt()
        val antennaId = it[2].toInt() and 0xff
        val frequency = ByteArray(3)
        System.arraycopy(it, 3, frequency, 0, frequency.size)
        val timestamp = ByteUtils.bytes4ToInt_h(it, 6)
        val rfu = ByteArray(2)
        System.arraycopy(it, 10, rfu, 0, rfu.size)
        val protocolId = it[12]
        val datalength = ByteArray(2)
        System.arraycopy(it, 15, datalength, 0, datalength.size)
        val epcLength = ByteUtils.bytes2ToInt_h(datalength, 0) / 8
        val pcword = ByteArray(2)
        System.arraycopy(it, 17, pcword, 0, pcword.size)
        val epcid_arr = ByteArray(epcLength - 4)
        System.arraycopy(it, 19, epcid_arr, 0, epcid_arr.size)
        var epcid = ByteUtils.bytetohex(epcid_arr).replace(" ", "")
        val tagcrc = ByteArray(2)
        System.arraycopy(it, it.size - 2, tagcrc, 0, 2)
        val idModel =
            IDModel(
                readcount,
                rssi,
                antennaId,
                frequency,
                timestamp,
                rfu,
                protocolId,
                datalength,
                epcid,
                byteArrayOf()
            )
        return idModel
    }

    private fun singleToIDModel(rfidEntity: RFIDEntity): IDModel {
        val readcount = rfidEntity.data[3].toInt() and 0xff
        val rssi = rfidEntity.data[4].toInt()
        val antennaId = rfidEntity.data[5].toInt() and 0xff
        val frequency = byteArrayOf(rfidEntity.data[6], rfidEntity.data[7], rfidEntity.data[8])
        val timestamp = ByteUtils.bytes4ToInt_h(rfidEntity.data, 9)
        val rfu = byteArrayOf(rfidEntity.data[13], rfidEntity.data[14])
        val protocolId = rfidEntity.data[15]
        val datalength = byteArrayOf(rfidEntity.data[16], rfidEntity.data[17])
        val tagcrc = byteArrayOf(rfidEntity.data[rfidEntity.Data.size-2], rfidEntity.data[rfidEntity.Data.size-1])
        val epcid_arr = rfidEntity.data.drop(18).dropLast(2).toByteArray()
        var epcid = ByteUtils.bytetohex(epcid_arr).replace(" ", "")
        val idModel =
            IDModel(
                readcount,
                rssi,
                antennaId,
                frequency,
                timestamp,
                rfu,
                protocolId,
                datalength,
                epcid,
                byteArrayOf()
            )
        return idModel
    }

    var mReadTagSingleListener = object:ReadTagSingleListener{
        override fun onData(data: RFIDEntity) {
            resolveSingleData(data)
                .map{idmodel->
                    RFHelper.controlTwinkle()
                    idmodel
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {idmodel->
                        if (idModels.contains(idmodel)) {
                            for (index in 0 until idModels.size) {
                                var id = idModels[index]
                                if (id.EPCID == idmodel.EPCID) {
                                    id.RSSI = idmodel.RSSI
                                    id.ReadCount = id.ReadCount + idmodel.ReadCount
                                    adapter.notifyItemChanged(index)
                                }
                            }
                        } else {
                            idModels.add(idmodel)
                            adapter.notifyItemChanged(idModels.size)
                        }
                        //本次标签数
                        tv_labels.text = "1"
                        tv_all_labels.text = idModels.size.toString()
                        //总次数增加
                        all_nums += idmodel.ReadCount
                        tv_all_nums?.text = all_nums.toString()
                    },
                    {
                        timer?.cancel()
                        DialogExt.showDialog(activity!!,it.message!!)
                        btn_loop.isEnabled = true
                        btn_single.isEnabled = true
                    },
                    {
                        timer?.cancel()
                        btn_single.isEnabled = true
                        btn_loop.isEnabled = true
                    }
                )
        }
    }

    var mReadTagMultipleListener = object:ReadTagMultipleListener{
        override fun onMultiple(data: RFIDEntity) {
            resolveMultipleData(data)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({nums->
                    //本次标签数
                    tv_labels.text = nums.toString()
                    RFHelper.getTagInfo()
                },
                    {
                        ToastUtils.showShort(it.message)
                        LogUtils.file(it.message)
                    },
                    {
                    }
                )
        }

        override fun onTagEPC(data: RFIDEntity) {
            resolveEpcData(data)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({list->
                    for(idmodel in list) {
                        if (idModels.contains(idmodel)) {
                            for (index in 0 until idModels.size) {
                                var id = idModels[index]
                                if (id.EPCID.equals(idmodel.EPCID)) {
                                    id.RSSI = idmodel.RSSI
                                    id.ReadCount = id.ReadCount + idmodel.ReadCount
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        } else {
                            idModels.add(idmodel)
                            adapter.notifyDataSetChanged()
                        }
                        if(inventorying) {
                            RFHelper.controlTwinkle()
                        }
                    }
                    //总计标签数
                    tv_all_labels.text = idModels.size.toString()
                    //总次数
                    var nums = 0
                    for (model in idModels) {
                        nums += model.ReadCount
                    }
                    tv_all_nums.text = nums.toString()
                },
                    {},
                    {
                    }
                )
        }
    }

    fun stopTimer(){
        timer?.cancel()
    }

    /**定时任务*/
    inner class MyTimeTask: TimerTask() {
        override fun run() {
            ThreadExecutors.mainThread.execute {
                startTime += periodTime
                tv_all_time?.text = "总时间：" + (startTime) + " ms"
            }
        }
    }

    @Subscriber(tag = EventBusConfig.ERROR_STATUS, mode = ThreadMode.ASYNC)
    public fun showErrorStatus(str:String) {
        timer?.cancel()
    }

    @Subscriber(tag = EventBusConfig.TRANS_ERROR, mode = ThreadMode.ASYNC)
    public fun showTransError(str:String) {
        timer?.cancel()
    }
}