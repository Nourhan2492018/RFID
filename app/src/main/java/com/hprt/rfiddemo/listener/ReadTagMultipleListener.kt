package com.hprt.rfiddemo.listener

import com.hprt.lib_rfid.model.RFIDEntity

interface ReadTagMultipleListener {
    fun onMultiple(data: RFIDEntity)
    fun onTagEPC(data: RFIDEntity)
}
