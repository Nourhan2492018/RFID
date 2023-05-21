package com.hprt.rfiddemo.ui.discovery

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass.Device.Major.IMAGING
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

internal class BluetoothReceiver(private val listener: BluetoothListener) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        when (intent.action ?: return) {
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                listener.onDiscoveryStart()
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                listener.onDiscoveryFinish()
            }
            BluetoothDevice.ACTION_FOUND -> {
                val blueDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (blueDevice != null) {
                    Log.i(
                        "hprt",
                        "onReceive: ${blueDevice.name} " +
                                "  ${blueDevice.bluetoothClass.majorDeviceClass} " +
                                "  ${blueDevice.address}"
                    )
                    if (blueDevice.bluetoothClass.majorDeviceClass == IMAGING) {
                        listener.onDeviceFound(blueDevice)
                    }
                }
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                listener.onDeviceBondStateChange(
                    intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE,
                        -1
                    )
                )
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                listener.onDeviceLost(device)
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                listener.onBluetoothStateChange(
                    intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                )
            }
        }
    }

    interface BluetoothListener {
        /**
         * 开始搜索蓝牙设备时调用
         */
        fun onDiscoveryStart()

        /**
         * 当发现蓝牙设备时调用
         *
         * @param device 当前发现的蓝牙设备
         */
        fun onDeviceFound(device: BluetoothDevice)

        /**
         * 当前蓝牙设备配对状态改变时调用
         *
         * @param state 当前的配对状态
         */
        fun onDeviceBondStateChange(state: Int)

        /**
         * 当设备断开时调用
         */
        fun onDeviceLost(device: BluetoothDevice?)

        /**
         * 当蓝牙状态改变时调用
         *
         * @param state 当前蓝牙状态
         */
        fun onBluetoothStateChange(state: Int)

        /**
         * 当搜索结束时调用
         */
        fun onDiscoveryFinish()
    }
}