package com.hprt.rfiddemo.listener

import com.hprt.lib_rfid.model.RFIDEntity

interface FastDataListener {
    fun onStart(result: Boolean)
    fun onData(data: RFIDEntity)
    fun onStop(result: Boolean)
}