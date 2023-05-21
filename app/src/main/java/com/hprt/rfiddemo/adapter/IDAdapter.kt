package com.hprt.rfiddemo.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.hprt.rfiddemo.R
import com.hprt.rfiddemo.model.IDModel
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author yefl
 * @date 2019/4/15.
 * description：
 */
class IDAdapter(layoutResId:Int, data: CopyOnWriteArrayList<IDModel>): BaseQuickAdapter<IDModel, BaseViewHolder>(layoutResId, data){
    override fun convert(helper: BaseViewHolder, item: IDModel) {
        helper.setText(R.id.tv_index, (helper.adapterPosition+1).toString())
        helper.setText(R.id.tv_id_data, item.EPCID)
        helper.setText(R.id.tv_num, item.ReadCount.toString())
        helper.setText(R.id.tv_rssi, item.RSSI.toString())
    }

}