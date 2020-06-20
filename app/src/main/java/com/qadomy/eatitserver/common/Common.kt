package com.qadomy.eatitserver.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.model.CategoryModel
import com.qadomy.eatitserver.model.FoodModel
import com.qadomy.eatitserver.model.ServerUserModel
import com.qadomy.eatitserver.model.TokenModel

object Common {


    // function for make name style in header menu
    fun setSpanString(welcome: String, name: String?, textUser: TextView?) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)

        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan, 0, name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)

        textUser!!.setText(builder, TextView.BufferType.SPANNABLE)
    }

    // function for make order strings style in order fragment
    fun setSpanStringColor(welcome: String, name: String?, textUser: TextView?, color: Int) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)

        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan, 0, name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        txtSpannable.setSpan(
            ForegroundColorSpan(color),
            0,
            name!!.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.append(txtSpannable)
        textUser!!.setText(builder, TextView.BufferType.SPANNABLE)
    }

    // function convert status number to string, shipping, shipped, cancelled, placed
    fun convertStatusToString(orderStatus: Int): String? =
        when (orderStatus) {
            0 -> "Placed"
            1 -> "Shipping"
            2 -> "Shipped"
            -1 -> "Canceled"
            else -> "Error"
        }


    // update token from firebase cloud messaging
    fun updateToken(context: Context, token: String) {
        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REF)
            .child(Common.currentServerUser!!.uid!!)
            .setValue(TokenModel(Common.currentServerUser!!.phone!!, token))
            .addOnFailureListener { e ->
                Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }


    // show notification
    fun showNotification(
        context: Context,
        id: Int,
        title: String?,
        content: String?,
        intent: Intent?
    ) {
        var pendingIntent: PendingIntent? = null
        if (intent != null)
            pendingIntent =
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val NOTIFICATION_CHANNEL_ID = "qadomy.dev.eatit"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Eat It",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationChannel.description = "Eat It"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = (Color.RED)
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)

            // create notification
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)

        // build notification
        builder.setContentTitle(title!!).setContentText(content!!).setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_restaurant_menu_black
                )
            )

        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent)

        val notification = builder.build()

        notificationManager.notify(id, notification)
    }


    fun getNewOrderTopic(): String {
        return StringBuilder("/topics/new_order").toString()
    }

    const val SERVER_REF = "Server"
    var currentServerUser: ServerUserModel? = null
    var FOOD_SELECTED: FoodModel? = null
    var CATEGORY_SELECTED: CategoryModel? = null
    var AUTHORISE_TOKEN: String? = null
    var CURRENT_TOKEN: String = ""
    const val DEFAULT_COLUMN_COUNT: Int = 0
    const val FULL_WIDTH_COLUMN: Int = 1
    const val CATEGORY_REF: String = "Category"
    const val ORDER_REF: String = "Order"
    const val COMMENT_REF: String = "Comments"
    const val BEST_DEALS_REF: String = "BestDeals"
    const val POPULAR_REF: String = "MostPopular"
    const val USER_REFERENCE = "Users"
    const val TOKEN_REF: String = "Tokens"
    const val NOTI_CONTENT: String = "content"
    const val NOTI_TITLE: String = "title"
}