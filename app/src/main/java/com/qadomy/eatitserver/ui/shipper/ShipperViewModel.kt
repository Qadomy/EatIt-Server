package com.qadomy.eatitserver.ui.shipper

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.qadomy.eatitserver.callback.IShipperLoadCallbackListener
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.model.CategoryModel
import com.qadomy.eatitserver.model.ShipperModel

class ShipperViewModel : ViewModel(), IShipperLoadCallbackListener {

    private var shipperListMutable: MutableLiveData<List<ShipperModel>>? = null
    private var messageError: MutableLiveData<String> = MutableLiveData()
    private val shipperCallbackListener: IShipperLoadCallbackListener

    init {
        shipperCallbackListener = this
    }


    fun getShipperList(): MutableLiveData<List<ShipperModel>> {
        if (shipperListMutable == null) {
            shipperListMutable = MutableLiveData()
            loadShipper()
        }

        return shipperListMutable!!
    }

    fun loadShipper() {
        val tempList = ArrayList<ShipperModel>()
        val shipperRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF)
        shipperRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                shipperCallbackListener.onShipperLoadFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (itemSnapshot in p0.children) {
                    val model = itemSnapshot.getValue<ShipperModel>(ShipperModel::class.java)
                    model!!.key = itemSnapshot.key
                    tempList.add(model)
                }
                shipperCallbackListener.onShipperLoadSuccess(tempList)
            }


        })
    }

    fun getMessageError(): MutableLiveData<String> {
        return messageError
    }

    override fun onShipperLoadSuccess(shipperList: List<ShipperModel>) {
        shipperListMutable!!.value = shipperList
    }

    override fun onShipperLoadFailed(message: String) {
        messageError.value = message
    }

}