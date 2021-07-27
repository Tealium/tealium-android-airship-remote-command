package com.tealium.airship

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.urbanairship.push.fcm.AirshipFirebaseIntegration


class MyFirebaseService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        AirshipFirebaseIntegration.processNewToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        AirshipFirebaseIntegration.processMessageSync(applicationContext, message)
    }
}