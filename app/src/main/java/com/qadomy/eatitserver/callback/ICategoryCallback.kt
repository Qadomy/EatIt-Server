package com.qadomy.eatitserver.callback

import com.qadomy.eatitserver.model.CategoryModel

interface ICategoryCallback {
    fun onCategoryLoadSuccess(categoryList: List<CategoryModel>)
    fun onCategoryLoadFailed(message: String)
}