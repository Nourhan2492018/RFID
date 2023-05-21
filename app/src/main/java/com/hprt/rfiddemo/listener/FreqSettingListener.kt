package com.hprt.rfiddemo.listener

import com.hprt.lib_rfid.model.RFIDEntity

interface FreqSettingListener {
    fun getFrequency(data: RFIDEntity)
    fun getHoppingFrequency(data: RFIDEntity)
}
