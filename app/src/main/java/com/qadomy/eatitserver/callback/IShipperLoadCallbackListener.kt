package com.qadomy.eatitserver.callback

import android.app.AlertDialog
import android.widget.Button
import android.widget.RadioButton
import com.qadomy.eatitserver.model.OrderModel
import com.qadomy.eatitserver.model.ShipperModel

interface IShipperLoadCallbackListener {
    fun onShipperLoadSuccess(shipperList: List<ShipperModel>)
    fun onShipperLoadSuccess(
        pos: Int,
        orderModel: OrderModel?,
        shipperList: List<ShipperModel>?,
        dialog: AlertDialog?,
        ok: Button?,
        cancel: Button?,
        rdiShipping: RadioButton?,
        rdiShipped: RadioButton?,
        rdiCancelled: RadioButton?,
        rdiDelete: RadioButton?,
        rdiRestore: RadioButton?
    )

    fun onShipperLoadFailed(message: String)
}
