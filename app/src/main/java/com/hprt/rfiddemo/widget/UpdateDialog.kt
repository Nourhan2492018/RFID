package com.hprt.rfiddemo.widget

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.view.Gravity
import androidx.annotation.FloatRange
import com.hprt.rfiddemo.R
import kotlinx.android.synthetic.main.dialog_update.*

class UpdateDialog(context: Context) : Dialog(context, R.style.update_dialog) {

    private val handler by lazy { MyHandler() }

    init {
        initView()
    }

    private fun initView() {
        this.setContentView(R.layout.dialog_update)
        this.setCancelable(false)
        this.setCanceledOnTouchOutside(false)

        if (window != null) {
            val attributes = window!!.attributes
            attributes.gravity = Gravity.CENTER
            attributes.dimAmount = 0.2f
            window!!.attributes = attributes
        } else {
            throw NullPointerException("dialog.getWindow() is null")
        }
        progressbar.progress = 0
        printTvMsg.text = "准备更新"
    }

    fun setProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float): UpdateDialog {
        progressbar.progress = (progress*100).toInt()
        if (progress < 100) {
            printTvMsg.text = "正在更新"
        } else {
            printTvMsg.text = "更新完成"
            handler.postDelayed({ dismiss() }, 1500)
        }
        return this
    }

    private class MyHandler : Handler()
}