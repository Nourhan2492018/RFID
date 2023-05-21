package com.hprt.rfiddemo.model

import com.hprt.lib_rfid.utils.ByteUtils
import java.util.*

/**
 * @author yefl
 * @date 2019/5/31.
 * description：
 */
data class IDModel(
    var ReadCount:Int,//标签被盘存到的次数
    var RSSI:Int,
    var antennaID:Int,//盘存到标签的天线 ID
    var Frequency:ByteArray,//盘存到标签时的频率
    var Timestamp:Int, //执行该指令到标签首次被盘存到时的时间，单位毫秒
    var RFU:ByteArray,//预留数据
    var ProtocolID:Byte,//标签协议
    var TagDataLength:ByteArray,//标签数据长度
    var EPCID:String,//标签 EPC 号
    var CRC:ByteArray//标签 CRC

){
    override fun equals(other: Any?): Boolean {
        if(EPCID.equals ((other as IDModel).EPCID)){
            return true
        }
        return super.equals(other)
    }

    override fun toString(): String {
        return "IDModel(ReadCount=$ReadCount, " +
                "RSSI=$RSSI, " +
                "antennaID=$antennaID, " +
                "Frequency=${ByteUtils.bytetohex(Frequency)}, " +
                "Timestamp=$Timestamp, " +
                "RFU=${ByteUtils.bytetohex(RFU)}, " +
                "ProtocolID=$ProtocolID, " +
                "TagDataLength=${ByteUtils.bytetohex(TagDataLength)}, " +
                "EPCID='$EPCID', " +
                "CRC=${ByteUtils.bytetohex(CRC)})"
    }


}
