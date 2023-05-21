package com.hprt.rfiddemo.utils

import android.content.Context
import android.service.autofill.TextValueSanitizer
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.hprt.rfiddemo.R


/**
 * @author yefl
 * @date 2019/9/20.
 * description：
 */
class ProgressDialogUtil {
    private var mAlertDialog: AlertDialog? = null

    /**
     * 弹出耗时对话框
     * @param context
     */
    fun showProgressDialog(context: Context) {
        if (mAlertDialog == null) {
            mAlertDialog = AlertDialog.Builder(context, R.style.CustomProgressDialog).create()
        }

        val loadView = LayoutInflater.from(context).inflate(R.layout.custom_progress_dialog_view, null)
        mAlertDialog!!.setView(loadView, 0, 0, 0, 0)
        mAlertDialog!!.setCanceledOnTouchOutside(false)

        val tvTip = loadView.findViewById(R.id.tvTip) as TextView
        tvTip.setText("加载中...")

        mAlertDialog!!.show()
    }

    fun showProgressDialog(context: Context, tip: String) {
        try {
            var tip = tip
            if (TextUtils.isEmpty(tip)) {
                tip = "加载中..."
            }

            if (mAlertDialog == null) {
                mAlertDialog = AlertDialog.Builder(context, R.style.CustomProgressDialog).create()
            }

            val loadView = LayoutInflater.from(context).inflate(R.layout.custom_progress_dialog_view, null)
            mAlertDialog!!.setView(loadView, 0, 0, 0, 0)
            mAlertDialog!!.setCanceledOnTouchOutside(false)

            val tvTip = loadView.findViewById(R.id.tvTip) as TextView
            tvTip.setText(tip)
            mAlertDialog!!.show()

        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    /**
     * 隐藏耗时对话框
     */
    fun dismiss() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing()) {
            mAlertDialog!!.dismiss()
        }
    }
}