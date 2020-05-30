package com.qadomy.eatitserver.ui.order

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.model.FoodModel

class OrderViewModel : ViewModel() {
    private var mutableOrder: MutableLiveData<List<FoodModel>>? = null


    fun getMutableFoodListData(): MutableLiveData<List<FoodModel>> {
        if (mutableOrder == null) {
            mutableOrder = MutableLiveData()
        }
        mutableOrder!!.value = Common.CATEGORY_SELECTED!!.foods
        return mutableOrder!!
    }
}