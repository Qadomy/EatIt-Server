package com.qadomy.eatitserver.common

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.TextView
import com.qadomy.eatitserver.model.CategoryModel
import com.qadomy.eatitserver.model.FoodModel
import com.qadomy.eatitserver.model.ServerUserModel

object Common {


    // function for set user name in header menu
    fun setSpanString(welcome: String, name: String?, textUser: TextView?) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)

        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan, 0, name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)

        textUser!!.setText(builder, TextView.BufferType.SPANNABLE)
    }

    const val SERVER_REF = "Server"
    var currentServerUser: ServerUserModel? = null

    var AUTHORISE_TOKEN: String? = null
    var CURRENT_TOKEN: String = ""

    //    var CURRENT_USER: UserModel? = null
    var FOOD_SELECTED: FoodModel? = null
    var CATEGORY_SELECTED: CategoryModel? = null
    const val ORDER_REF: String = "Order"
    const val COMMENT_REF: String = "Comments"
    const val DEFAULT_COLUMN_COUNT: Int = 0
    const val FULL_WIDTH_COLUMN: Int = 1
    const val BEST_DEALS_REF: String = "BestDeals"
    const val POPULAR_REF: String = "MostPopular"
    const val USER_REFERENCE = "Users"
    const val CATEGORY_REF: String = "Category"
    const val TOKEN_REF: String = "Tokens"
    const val NOTI_CONTENT: String = "content"
    const val NOTI_TITLE: String = "title"
}