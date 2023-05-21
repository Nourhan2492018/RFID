package com.hprt.rfiddemo.ui.select

import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.ui.base.BaseActivity
import com.hprt.rfiddemo.ui.main.rf1.RF1MainActivity
import com.hprt.rfiddemo.ui.main.rf1p.RF1PMainActivity
import com.hprt.rfiddemo.ui.main.rf2.RF2MainActivity

import kotlinx.android.synthetic.main.activity_choose_device.*

class ChooseDeviceActivity: BaseActivity() {
    override fun getAct(): Context {
        return this
    }

    override fun getContentView(): Int = R.layout.activity_choose_device

    override fun initView() {
        topbar.setTitle("RF功能演示")

        btn_rf1.setOnClickListener {
            startActivity(Intent(baseContext, RF1MainActivity::class.java))
        }
        btn_rf1p.setOnClickListener {
            startActivity(Intent(baseContext, RF1PMainActivity::class.java))
        }
        btn_rf2.setOnClickListener {
            startActivity(Intent(baseContext, RF2MainActivity::class.java))
        }
        tv_version.text = "版本:V"+AppUtils.getAppVersionName()
    }

    override fun initData() {

    }

    private var pressedTime = 0L
    override fun onBackPressed() {
        if (isTaskRoot) {
            val tempTime = System.currentTimeMillis()
            if (tempTime - pressedTime > 2000) {
                ToastUtils.showShort("再按一次退出程序")
                pressedTime = tempTime
            } else {
                finish()
            }
        } else {
            super.onBackPressed()
        }
    }
}