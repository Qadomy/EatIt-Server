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
import com.qadomy.eatitserver.eventbus.SelectAddonModel
import com.qadomy.eatitserver.eventbus.SelectSizeModel
import com.qadomy.eatitserver.eventbus.UpdateAddonModel
import com.qadomy.eatitserver.eventbus.UpdateSizeModel
import com.qadomy.eatitserver.model.AddonModel
import com.qadomy.eatitserver.model.SizeModel
import org.greenrobot.eventbus.EventBus

class MyAddonAdapter(var context: Context, var addonModelList: MutableList<AddonModel>) :
    RecyclerView.Adapter<MyAddonAdapter.MyViewHolder>() {

    var edtPos: Int
    private var updateAddonModel: UpdateAddonModel

    init {
        edtPos = -1
        updateAddonModel = UpdateAddonModel()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.layout_addon_size_item, parent, false)
        )
    }

    override fun getItemCount() = addonModelList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textName!!.text = addonModelList[position].name
        holder.textPrice!!.text = addonModelList[position].price.toString()

        //Event, when click on delete image
        holder.imageDelete!!.setOnClickListener {
            addonModelList.removeAt(position)
            notifyItemRemoved(position)
            updateAddonModel.addonModelList = addonModelList

            EventBus.getDefault().postSticky(updateAddonModel)

        }

        holder.setListener(object : IRecycleItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                edtPos = position
                EventBus.getDefault().postSticky(SelectAddonModel(addonModelList[pos]))
            }
        })
    }

    // method for add new size
    fun addNewAddon(addonModel: AddonModel) {
        addonModelList.add(addonModel)
        notifyItemInserted(addonModelList.size - 1)
        updateAddonModel.addonModelList = addonModelList

        EventBus.getDefault().postSticky(updateAddonModel)
    }

    // method for edit the old one (change it)
    fun editAddon(addonModel: AddonModel) {
        addonModelList[edtPos] = addonModel
        notifyItemChanged(edtPos)
        updateAddonModel.addonModelList = addonModelList

        EventBus.getDefault().postSticky(updateAddonModel)
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
