package com.hprt.rfiddemo.adapter

import android.bluetooth.BluetoothDevice
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.hprt.rfiddemo.R
import kotlinx.android.synthetic.main.item_ble_device.view.*

class BluetoothRVAdapter :
    BaseQuickAdapter<BluetoothDevice, BaseViewHolder>(R.layout.item_ble_device) {
    override fun convert(holder: BaseViewHolder, item: BluetoothDevice) {
        holder.itemView.tv_name.text = item.name
        holder.itemView.tv_mac.text = item.address
    }
}