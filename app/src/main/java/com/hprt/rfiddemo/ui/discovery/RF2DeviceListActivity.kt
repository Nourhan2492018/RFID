package com.hprt.rfiddemo.ui.discovery

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.adapter.BluetoothRVAdapter
import kotlinx.android.synthetic.main.activity_device_list.*

class RF2DeviceListActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "BluetoothActivity"
    }

    private val intentFilter by lazy {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter
    }

    private lateinit var receiver: BluetoothReceiver
    private val bluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val rvAdapter by lazy {
        val adapter = BluetoothRVAdapter()
        adapter.setOnItemClickListener { _, _, position ->
            val intent = Intent()
            intent.putExtra("bluetooth", adapter.data[position])
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        recyclerview.layoutManager = LinearLayoutManager(this)
        recyclerview.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
        recyclerview.adapter = rvAdapter
        //
        receiver = BluetoothReceiver(object : BluetoothReceiver.BluetoothListener {
            override fun onDiscoveryStart() {
                Log.i(TAG, "onDiscoveryStart")
            }

            override fun onDeviceFound(device: BluetoothDevice) {
                if(device != null  && device.name != null &&
                    device.name.contains("", true)) {
                    for (element in rvAdapter.data) {
                        if (element.name == device.name) {
                            return
                        }
                    }
                    rvAdapter.addData(device)
                    rvAdapter.notifyDataSetChanged()
                    Log.i(TAG, "onDeviceFound -> ${device.name}")
                }
            }

            override fun onDeviceBondStateChange(state: Int) {
                when (state) {
                    BluetoothDevice.BOND_NONE -> {
                        Log.i(TAG, "配对失败")
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Log.i(TAG, "正在配对")
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        Log.i(TAG, "配对成功")
                    }
                    else -> {
                        Log.i(TAG, "onDeviceBondStateChange -> UnKnow")
                    }
                }
            }

            override fun onDeviceLost(device: BluetoothDevice?) {
                Log.i(TAG, "onDeviceLost 设备丢失 -> $device")
            }

            override fun onBluetoothStateChange(state: Int) {
                Log.i(TAG, "onBluetoothStateChange -> $state")
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {

                    }
                    BluetoothAdapter.STATE_ON -> {

                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {

                    }
                    else -> {
                    }
                }
            }

            override fun onDiscoveryFinish() {
                Log.i(TAG, "onDiscoveryFinish")
            }
        })
        registerReceiver(receiver, intentFilter)
        bluetoothAdapter.enable()
        bluetoothAdapter.startDiscovery()
    }


    override fun onDestroy() {
        super.onDestroy()
        bluetoothAdapter.cancelDiscovery()
        unregisterReceiver(receiver)
    }

}