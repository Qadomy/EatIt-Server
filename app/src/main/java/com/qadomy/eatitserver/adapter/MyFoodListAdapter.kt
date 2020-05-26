package com.qadomy.eatitserver.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.callback.IRecycleItemClickListener
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.model.FoodModel

class MyFoodListAdapter(
    internal var context: Context,
    internal var foodList: List<FoodModel>
) : RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>() {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyFoodListAdapter.MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.layout_food_item, parent, false)
        )
    }

    override fun getItemCount() = foodList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(foodList[position].image)
            .into(holder.imgFoodImage!!)
        holder.txtFoodName!!.text = foodList[position].name
        holder.txtFoodPrice!!.text = StringBuilder("$ ").append(foodList[position].price.toString())


        // Event Bus
        holder.setListener(object : IRecycleItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                Common.FOOD_SELECTED = foodList[pos]
                Common.FOOD_SELECTED!!.key = pos.toString()
            }
        })
    }


    // MyViewHolder class
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        var txtFoodName: TextView? = null
        var txtFoodPrice: TextView? = null
        var imgFoodImage: ImageView? = null
        var imgFav: ImageView? = null
        var imgCart: ImageView? = null


        // for recycle item click
        internal var listener: IRecycleItemClickListener? = null

        fun setListener(listener: IRecycleItemClickListener) {
            this.listener = listener
        }


        init {
            txtFoodName = itemView.findViewById(R.id.txt_food_name)
            txtFoodPrice = itemView.findViewById(R.id.txt_food_price)
            imgFoodImage = itemView.findViewById(R.id.img_food_image)

            // here when click in item
            itemView.setOnClickListener(this)

        }

        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!, adapterPosition)
        }

    }

}