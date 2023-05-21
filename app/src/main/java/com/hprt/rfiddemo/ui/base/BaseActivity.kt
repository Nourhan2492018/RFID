package com.hprt.rfiddemo.ui.base

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.blankj.utilcode.util.ToastUtils
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.data.Resource
import kotlinx.android.synthetic.main.dialog_loading.*
import org.simple.eventbus.EventBus

abstract class BaseActivity: AppCompatActivity(){
    private var progressdialog: MaterialDialog?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getContentView())
        EventBus.getDefault().register(this)
        initView()
        initData()
    }

    abstract fun getAct(): Context

    abstract fun getContentView():Int

    abstract fun initView()

    abstract fun initData()

    fun showProgress(){
        showProgress(resources.getString(R.string.base_loading))
    }

    fun showProgress(stringId:Int){
        showProgress(resources.getString(stringId))
    }

    fun showProgress(msg:String){
        if (progressdialog == null) {
            progressdialog = MaterialDialog(this).show {
                customView(R.layout.dialog_loading,null,false,true,false,false)
                cornerRadius(10.0f)
                cancelOnTouchOutside(false)
                cancelable(true)
                tv_msg.text = msg
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


    fun <T> handleData(liveData: LiveData<Resource<T>>, action: (T) -> Unit) = liveData.observe(this, Observer { result ->
        if (result?.code == Resource.LOADING) {
            showProgress()
        } else if (result?.data != null && result.code == Resource.SUCCESS) {
            dismissProgress()
            action(result.data)
        } else {
            ToastUtils.showShort(result?.msg)
            dismissProgress()
        }
    })

    protected fun <T> handleData(liveData: LiveData<Resource<T>>,
        loading: (String?) -> Unit = {
            if (it != null) {
                showProgress(it)
            }
        },
        success: (T?) -> Unit,
        error: (String?) -> Unit = { ToastUtils.showShort(it) }
    ) =
        liveData.observe(this, Observer { event ->
            when (event.code) {
                Resource.LOADING -> loading("loading")
                Resource.SUCCESS -> {
                    dismissProgress()
                    success(event.data)
                }
                Resource.ERROR -> {
                    dismissProgress()
                    error(event.msg)
                }
            }
        })


    override fun onDestroy() {
        progressdialog?.dismiss()
        progressdialog = null
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }




}