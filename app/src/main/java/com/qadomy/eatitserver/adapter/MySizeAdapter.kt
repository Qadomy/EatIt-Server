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
import com.qadomy.eatitserver.eventbus.SelectSizeModel
import com.qadomy.eatitserver.eventbus.UpdateSizeModel
import com.qadomy.eatitserver.model.SizeModel
import org.greenrobot.eventbus.EventBus

class MySizeAdapter(var context: Context, var sizeModelList: MutableList<SizeModel>) :
    RecyclerView.Adapter<MySizeAdapter.MyViewHolder>() {

    var edtPos: Int
    private var updateSizeModel: UpdateSizeModel

    init {
        edtPos = -1
        updateSizeModel = UpdateSizeModel()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.layout_addon_size_item, parent, false)
        )
    }

    override fun getItemCount() = sizeModelList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textName!!.text = sizeModelList[position].name
        holder.textPrice!!.text = sizeModelList[position].price.toString()

        //Event, when click on delete image
        holder.imageDelete!!.setOnClickListener {
            sizeModelList.removeAt(position)
            notifyItemRemoved(position)
            updateSizeModel.sizeModelList = sizeModelList

            EventBus.getDefault().postSticky(updateSizeModel)

        }

        holder.setListener(object : IRecycleItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                edtPos = position
                EventBus.getDefault().postSticky(SelectSizeModel(sizeModelList[pos]))
            }
        })
    }

    // method for add new size
    fun addNewSize(sizeModel: SizeModel) {
        sizeModelList.add(sizeModel)
        notifyItemInserted(sizeModelList.size - 1)
        updateSizeModel.sizeModelList = sizeModelList

        EventBus.getDefault().postSticky(updateSizeModel)
    }

    // method for edit the old one (change it)
    fun editSize(sizeModel: SizeModel) {
        sizeModelList.set(edtPos, sizeModel)
        notifyItemChanged(edtPos)
        updateSizeModel.sizeModelList = sizeModelList

        EventBus.getDefault().postSticky(updateSizeModel)
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var textName: TextView? = null
        var textPrice: TextView? = null
        var imageDelete: ImageView? = null
        internal var listener: IRecycleItemClickListener? = null

        init {
            textName = itemView.findViewById(R.id.txt_name) as TextView
            textPrice = itemView.findViewById(R.id.txt_price) as TextView
            imageDelete = itemView.findViewById(R.id.img_delete) as ImageView

            // when click on item to set data in edit text
            itemView.setOnClickListener { view -> listener!!.onItemClick(view, adapterPosition) }
        }

        fun setListener(listener: IRecycleItemClickListener?) {
            this.listener = listener
        }
    }


}
