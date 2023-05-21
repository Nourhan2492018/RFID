package com.hprt.rfiddemo.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.ToastUtils
import com.hprt.lib_rfid.RFHelper
import com.hprt.rfiddemo.R

/**
 */
abstract class BaseFragment: Fragment() {

    private var progressdialog: MaterialDialog?=null

    abstract fun getLayoutId(): Int

    abstract fun initView()

    abstract fun initData()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(getLayoutId(), container, false)
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
        initData()
    }


    fun checkRFIDReady():Boolean {
        if (!RFHelper.isConnect) {
            ToastUtils.showShort("未连接")
            return false
        }
        if (RFHelper.connectType == 0) {
            val result = FileIOUtils.readFile2String("/sys/devices/platform/10010000.kp/TSTBASE")
            if (result.isNullOrEmpty()) {
                ToastUtils.showShort("未检测到手柄")
                return false
            }
            if (result.equals("1") || result.equals("1\n")) {
                return true
            }
            return false
        }else{//蓝牙返回true
            return true
        }
    }

    fun showProgress(){
        showProgress(resources.getString(R.string.base_loading))
    }

    fun showProgress(stringId:Int){
        showProgress(resources.getString(stringId))
    }

    fun showProgress(msg:String){
        if (progressdialog == null) {
            progressdialog = MaterialDialog(activity!!).show {
                customView(R.layout.dialog_loading,null,false,true,false,false)
                cornerRadius(10.0f)
                cancelOnTouchOutside(true)
                cancelable(true)
                maxWidth(R.dimen.base_dp_140)
            }
        }
        progressdialog?.findViewById<TextView>(R.id.tv_msg)?.text = msg
        progressdialog?.show()
    }

    fun dismissProgress(){
        if(progressdialog?.isShowing?:false) {
            progressdialog?.dismiss()
            progressdialog = null
        }

    }

}