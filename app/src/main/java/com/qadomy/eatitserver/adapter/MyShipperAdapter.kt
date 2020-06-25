package com.qadomy.eatitserver.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.eventbus.UpdateActiveEvent
import com.qadomy.eatitserver.model.ShipperModel
import org.greenrobot.eventbus.EventBus

class MyShipperAdapter(
    internal var context: Context,
    internal var shipperList: List<ShipperModel>
) : RecyclerView.Adapter<MyShipperAdapter.MyViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_shipper, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount() = shipperList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textName!!.text = shipperList[position].name
        holder.textPhone!!.text = shipperList[position].phone
        holder.btnEnable!!.isChecked = shipperList[position].isActive!!

        //Event
        holder.btnEnable!!.setOnCheckedChangeListener { _, b ->
            EventBus.getDefault().postSticky(UpdateActiveEvent(shipperList[position], b))
        }

    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var textName: TextView? = null
        var textPhone: TextView? = null
        var btnEnable: SwitchCompat? = null


        init {
            textName = itemView.findViewById(R.id.shipper_txt_name) as TextView
            textPhone = itemView.findViewById(R.id.shipper_txt_phone) as TextView
            btnEnable = itemView.findViewById(R.id.shipper_btn_enabel) as SwitchCompat
        }

    }
}