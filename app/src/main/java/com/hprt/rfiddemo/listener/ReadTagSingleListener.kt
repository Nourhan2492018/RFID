package com.hprt.rfiddemo.listener

import com.hprt.lib_rfid.model.RFIDEntity

interface ReadTagSingleListener {
    fun onData(data: RFIDEntity)
}
