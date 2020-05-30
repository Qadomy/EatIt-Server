package com.qadomy.eatitserver.callback

import com.qadomy.eatitserver.model.OrderModel

interface IOrderCallbackListener {

    fun onOrderLoadSuccess(orderModel: List<OrderModel>)
    fun onOrderLoadFailed(message: String)

}
