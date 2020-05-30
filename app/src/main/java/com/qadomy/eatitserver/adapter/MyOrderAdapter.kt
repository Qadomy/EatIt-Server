package com.qadomy.eatitserver.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.model.OrderModel
import java.text.SimpleDateFormat

class MyOrderAdapter(
    internal var context: Context,
    internal var orderList: MutableList<OrderModel>
) : RecyclerView.Adapter<MyOrderAdapter.MyViewHolder>() {

    lateinit var simpleDateFormat: SimpleDateFormat

    init {
        simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyOrderAdapter.MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.laayout_order_item, parent, false)
        )
    }

    override fun getItemCount() = orderList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context!!).load(orderList[position].cartItemList!![0].foodImage)
            .into(holder.imageOrder!!)

        holder.txtOrderNumber!!.text = orderList[position].key

        Common.setSpanStringColor(
            "Order date ", simpleDateFormat.format(orderList[position].createDate),
            holder.txtTime, Color.parseColor("#333639")
        )

        Common.setSpanStringColor(
            "Order status ", Common.convertStatusToString(orderList[position].orderStatus),
            holder.txtOrderStatus, Color.parseColor("#005758")
        )

        Common.setSpanStringColor(
            "Num of items ",

            if (orderList[position].cartItemList == null)
                "0"
            else
                orderList[position].cartItemList!!.size.toString(),

            holder.txtNumItem, Color.parseColor("#00574B")
        )

        Common.setSpanStringColor(
            "Name ", orderList[position].userName,
            holder.txtName, Color.parseColor("#006061")
        )
    }

    // function for return the position of item in recycler view
    fun getItemAtPosition(pos: Int): OrderModel {
        return orderList[pos]
    }

    // function for remove at position from recycler view
    fun removeItem(pos: Int) {
        orderList.removeAt(pos)
    }


    // MyViewHolder class
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        var txtName: TextView? = null
        var txtOrderTime: TextView? = null
        var txtOrderNumber: TextView? = null
        var txtOrderStatus: TextView? = null
        var txtTime: TextView? = null
        var txtNumItem: TextView? = null

        var imageOrder: ImageView? = null


        init {
            imageOrder = itemView.findViewById(R.id.img_order_food_image) as ImageView

            txtName = itemView.findViewById(R.id.txt_order_name) as TextView
            txtOrderTime = itemView.findViewById(R.id.txt_order_time) as TextView
            txtOrderNumber = itemView.findViewById(R.id.rxr_order_number) as TextView
            txtOrderStatus = itemView.findViewById(R.id.txt_order_status) as TextView
            txtTime = itemView.findViewById(R.id.txt_order_time) as TextView
            txtNumItem = itemView.findViewById(R.id.txt_order_num_item) as TextView
        }

        override fun onClick(v: View?) {

        }


    }


}