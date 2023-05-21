package com.hprt.rfiddemo.ui.rfid.rf1p

import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.RFHelper
import com.hprt.lib_rfid.ext.logFile
import com.hprt.lib_rfid.model.RFIDEntity
import com.hprt.lib_rfid.utils.*
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.adapter.IDAdapter
import com.hprt.rfiddemo.app.RfidApp
import com.hprt.rfiddemo.config.EventBusConfig
import com.hprt.rfiddemo.ext.DialogExt
import com.hprt.rfiddemo.listener.FastDataListener
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

/**
 * @author yefl
 * @date 2019/5/31.
 * description：
 * 盘点c
 */
class RF1PInventoryFragment : BaseFragment() {
    var binder: RFService.RFBinder?=null
    var all_nums = 0
    companion object {
        fun newInstance() = RF1PInventoryFragment()
    }
    @Volatile
    var idModels = CopyOnWriteArrayList<IDModel>()
    var idsLength = 0;
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
        if (!inventorying) {
            inventorying = true
            btn_loop.isEnabled = false
            btn_single.isEnabled = false
            inventory1200()
        }
    }

    private fun inventory1200() {
        btn_single.isEnabled = false
        btn_loop.isEnabled = false
        timer = Timer()
        timer?.schedule(MyTimeTask(), 0,periodTime)
        binder?.fastInventory(fastDataListener)
//            smartSpeed()
    }

    fun stopInventory() {
        binder?.stopFastInventory()
        timer?.cancel()
        btn_loop.isEnabled = true
        btn_single.isEnabled = true
        dismissProgress()
        inventorying = false
    }

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
     * 解析fast模式数据
     * */
    fun resolveFastData(rfidEntity: RFIDEntity): Observable<IDModel>  {
        return Observable.create<IDModel> {
            if (ByteUtils.bytes2ToInt_h(rfidEntity.Status, 0) == 0) {
                if(rfidEntity.data.size>19) {
                    var readCount = rfidEntity.data[2].toInt()
                    var rssi = rfidEntity.data[3].toInt()
                    var antenaId = rfidEntity.data[4].toInt()
                    var frequency = rfidEntity.data.toList().subList(5,8).toByteArray()
                    var timestamp = rfidEntity.data.toList().subList(8,12).toByteArray()
                    var rfu = rfidEntity.data.toList().subList(12,14).toByteArray()
                    var protocolId = rfidEntity.data[14]
                    var epcIdLength = rfidEntity.data.toList().subList(15,17).toByteArray()
                    var epcData = rfidEntity.data.drop(17).dropLast(2).toByteArray()
                    val idModel = IDModel(
                        readCount,
                        rssi,
                        antenaId,
                        frequency,
                        ByteUtils.bytesToInt(timestamp,0),
                        rfu,
                        protocolId,
                        epcIdLength,
                        ByteUtils.bytetohex(epcData).replace(" ", ""),
                        rfidEntity.crc16
                    )
                    ThreadExecutors.mainThread.execute {
                        if(activity?.isDestroyed == false)
                            tv_labels.text = readCount.toString()
                    }
                    it.onNext(idModel)
                }
                it.onComplete()
            }else{
                it.onError(Throwable("盘存出错 status = ${ByteUtils.bytetohex(rfidEntity.Status)}"))
                LogUtils.file("盘存出错 status = ${ByteUtils.bytetohex(rfidEntity.Status)}")
            }
        }
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
    var lastSpeed = 100;
    /**
     * 间隔10s检查一次，如果新增标签数量
     * <100，50%速度，
     * 100~500，80%速度
     * >500, 100%全速
     */
    private fun smartSpeed(){
        RxTimerUtil.interval(10*1000, object :RxTimerUtil.IRxNext{
            override fun doNext(number: Long) {
                var speed = 100
                var diff = idModels.size-idsLength
                when{
                    diff<10*10->{
                        speed = 50
                    }
                    diff in 10*10 .. 50*10->{
                        speed = 80
                    }
                    diff>50*10->{
                        speed = 100
                    }
                }
                if(lastSpeed != speed){
                    if(lastSpeed<speed){
                        logFile("提速 === time = ${TimeUtils.getNowString()} idsLength = ${idsLength} idModels = ${idModels.size} speed = ${speed} lastSpeed = ${lastSpeed}")
                    }else{
                        logFile("降速 === time = ${TimeUtils.getNowString()} idsLength = ${idsLength} idModels = ${idModels.size} speed = ${speed} lastSpeed = ${lastSpeed}")
                    }
                    if(inventorying){
                        binder?.smartFastInventory(speed)
                    }
                    lastSpeed = speed
                }
                idsLength = idModels.size
            }
        })
    }

    var fastDataListener = object : FastDataListener {
        override fun onStart(result: Boolean) {
            if(result){
                inventorying = true
            }else{
                DialogExt.showDialog(activity!!,"快速模式开启失败了, 请重试")
            }
        }

        override fun onData(it: RFIDEntity) {
            resolveFastData(it)
                .map{idmodel->
                    if(inventorying) {
                        RFHelper.controlTwinkle()
                    }
                    idmodel
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { idmodel ->
                        if (idModels.contains(idmodel)) {
                            for (index in 0 until idModels.size) {
                                var id = idModels[index]
                                if (id.EPCID == idmodel.EPCID) {
                                    id.RSSI = idmodel.RSSI
                                    id.ReadCount = id.ReadCount + idmodel.ReadCount
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        } else {
                            idModels.add(idmodel)
                            adapter.notifyDataSetChanged()
                        }
                        //本次标签数
                        tv_labels.text = "1"
                        tv_all_labels.text = idModels.size.toString()
                        //总次数增加
                        all_nums += idmodel.ReadCount
                        tv_all_nums?.text = all_nums.toString()
                    },
                    {
                        ToastUtils.showShort("error-->"+it.message)
                        LogUtils.file("error-->"+it.message)
                    },
                    {

                    })
        }

        override fun onStop(result: Boolean) {
            if(result){
            }else{
                DialogExt.showDialog(activity!!, "停止命令执行失败了, 请重试")
            }
        }
    }


    var mReadTagSingleListener = object:ReadTagSingleListener{
        override fun onData(data: RFIDEntity) {
            resolveSingleData(data)
                .map{idmodel->
                    for(i in 0 until idmodel.ReadCount) {
                        RFHelper.controlTwinkle()
                    }
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
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        } else {
                            idModels.add(idmodel)
                            adapter.notifyDataSetChanged()
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