package com.hprt.rfiddemo.ext

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.blankj.utilcode.util.ConvertUtils
import com.hprt.lib_rfid.utils.ThreadExecutors

object DialogExt {

    fun showDialog(activity: FragmentActivity, msg:String, operation: () -> Unit = {}){
        ThreadExecutors.mainThread.execute {
            if(activity.isDestroyed){
                return@execute
            }
            MaterialDialog(activity).show {
                message(null,msg)
                cornerRadius(5.0f)
                maxWidth(literal = ConvertUtils.dp2px(280.0f))
                positiveButton(null,"确定"){
                    dismiss()
                    operation()
                }
            }
        }
    }

}