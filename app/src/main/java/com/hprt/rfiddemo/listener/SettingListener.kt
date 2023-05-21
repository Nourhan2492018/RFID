package com.hprt.rfiddemo.listener

import com.hprt.lib_rfid.model.RFIDEntity

interface SettingListener {
    fun onAntPower(data: RFIDEntity)
}
