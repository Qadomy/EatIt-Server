package com.qadomy.eatitserver.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.qadomy.eatitserver.common.Common
import kotlin.random.Random

class MyFCMServices : FirebaseMessagingService() {

    /**
     *
     * Service for firebase notification message
     */

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Common.updateToken(this, p0)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val dataReCV = remoteMessage.data

        if (dataReCV != null) {
            Common.showNotification(
                this, Random.nextInt(),
                dataReCV[Common.NOTI_TITLE],
                dataReCV[Common.NOTI_CONTENT],
                null
            )
        }
    }

}