package com.hprt.rfiddemo.utils

import kotlin.experimental.and

/**
 * @author yefl
 * @date 2019/4/17.
 * description：Byte数组处理
 */
class UtilFastClick {
    companion object {

        val MIN_CLICK_DELAY_TIME = 500
        var lastClickTime:Long = 0L

        fun isFastClick():Boolean {
            var flag = false
            var curClickTime = System.currentTimeMillis()
            if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
                flag = true
            }
            lastClickTime = curClickTime
            return flag
        }



    }
}