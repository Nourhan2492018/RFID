package com.hprt.rfiddemo.widget

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import androidx.core.content.ContextCompat
import com.hprt.rfiddemo.R
import kotlinx.android.synthetic.main.k_dialog_tip.*

class TipDialog(context: Context) : Dialog(context, R.style.k_dialog) {
    init {
        initView()
    }

    private var operationUnit: () -> Unit = {}

    private fun initView() {
        this.setContentView(R.layout.k_dialog_tip)
        this.setCancelable(true)
        this.setCanceledOnTouchOutside(true)

        if (window != null) {
            val attributes = window!!.attributes
            attributes.gravity = Gravity.CENTER
            attributes.dimAmount = 0.2f
            window!!.attributes = attributes
        } else {
            throw NullPointerException("dialog.getWindow() is null")
        }

        printTvIKnow.setOnClickListener {
            cancel()
            operationUnit()
        }
    }

    fun setTipTitle(title: String): TipDialog {
        printTvTipTitle.text = title
        return this
    }

    fun setTipMsg(msg: String): TipDialog {
        printTvTipMsg.text = msg
        return this
    }

    fun setTipTitle(titleId: Int): TipDialog {
        printTvTipTitle.setText(titleId)
        return this
    }

    fun setTipMsg(msgId: Int): TipDialog {
        printTvTipMsg.setText(msgId)
        return this
    }

    fun setOperation(
        text: String,
        colorId: Int = R.color.black,
        operation: () -> Unit = {}
    ): TipDialog {
        printTvIKnow.text = text
        printTvIKnow.setTextColor(ContextCompat.getColor(context, colorId))
        this.operationUnit = operation
        return this
    }

    fun setOperation(
        textId: Int,
        colorId: Int = R.color.black,
        operation: () -> Unit = {}
    ): TipDialog {
        printTvIKnow.setText(textId)
        printTvIKnow.setTextColor(ContextCompat.getColor(context, colorId))
        this.operationUnit = operation
        return this
    }
}