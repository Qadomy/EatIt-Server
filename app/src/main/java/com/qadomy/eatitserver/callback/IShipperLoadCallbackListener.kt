package com.qadomy.eatitserver.callback

import com.qadomy.eatitserver.model.ShipperModel

interface IShipperLoadCallbackListener {
    fun onShipperLoadSuccess(shipperList: List<ShipperModel>)
    fun onShipperLoadFailed(message: String)
}
