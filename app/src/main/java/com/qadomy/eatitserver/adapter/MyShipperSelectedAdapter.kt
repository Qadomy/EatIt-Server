package com.qadomy.eatitserver.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.callback.IRecycleItemClickListener
import com.qadomy.eatitserver.model.ShipperModel

class MyShipperSelectedAdapter(
    internal var context: Context,
    internal var shipperList: List<ShipperModel>
) : RecyclerView.Adapter<MyShipperSelectedAdapter.MyViewHolder>() {


    var lastCheckedImageView: ImageView? = null
    var selectedShipper: ShipperModel? = null
        private set


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(context).inflate(
                R.layout.layout_shipper_selected,
                parent,
                false
            )
        return MyViewHolder(itemView)
    }

    override fun getItemCount() = shipperList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textName!!.text = shipperList[position].name
        holder.textPhone!!.text = shipperList[position].phone
        holder.setClick(object : IRecycleItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                if (lastCheckedImageView != null)
                    lastCheckedImageView!!.setImageResource(0)

                holder.imageChecked!!.setImageResource(R.drawable.ic_baseline_done_24)
                lastCheckedImageView = holder.imageChecked
                selectedShipper = shipperList[pos]
            }
        })

    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        var textName: TextView? = null
        var textPhone: TextView? = null
        var imageChecked: ImageView? = null

        var iRecycleItemClickListener: IRecycleItemClickListener? = null
        fun setClick(iRecycleItemClickListener: IRecycleItemClickListener) {
            this.iRecycleItemClickListener = iRecycleItemClickListener
        }


        init {
            textName = itemView.findViewById(R.id.txt_name) as TextView
            textPhone = itemView.findViewById(R.id.txt_phone) as TextView
            imageChecked = itemView.findViewById(R.id.img_checked  ) as ImageView

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            iRecycleItemClickListener!!.onItemClick(v!!, adapterPosition)
        }

    }
}