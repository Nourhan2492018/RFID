package com.hprt.rfiddemo.listener

import com.hprt.lib_rfid.model.RFIDEntity

interface GetTagListener {
    fun onData(data: RFIDEntity)
}
