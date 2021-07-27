package com.tealium.airship

import android.app.Application
import com.tealium.adidentifier.AdIdentifier
import com.tealium.airship.BuildConfig.TEALIUM_INSTANCE
import com.tealium.core.*
import com.tealium.core.consent.*
import com.tealium.core.events.EventTrigger
import com.tealium.core.messaging.UserConsentPreferencesUpdatedListener
import com.tealium.dispatcher.TealiumEvent
import com.tealium.dispatcher.TealiumView
import com.tealium.lifecycle.Lifecycle
import com.tealium.remotecommanddispatcher.RemoteCommands
import com.tealium.remotecommanddispatcher.remoteCommands
import com.tealium.remotecommands.airship.AirshipRemoteCommand
import com.tealium.remotecommands.airship.BuildConfig
import java.util.concurrent.TimeUnit

object TealiumHelper {

    const val INSTANCE = "main"

    fun init(application: Application) {
        val config = TealiumConfig(
            application,
            "tealiummobile",
            "airship-demo",
            Environment.DEV,
            modules = mutableSetOf(
                Modules.Lifecycle, Modules.AdIdentifier
            ),
            dispatchers = mutableSetOf(
                Dispatchers.RemoteCommands
            )
        ).apply {
            deepLinkTrackingEnabled = false
            qrTraceEnabled = false
        }

        Tealium.create(INSTANCE, config) {

            val airshipCommand = AirshipRemoteCommand(application)
            remoteCommands?.add(airshipCommand, filename = "airship.json")
        }
    }

    fun trackView(name: String, data: Map<String, Any>?) {
        val viewDispatch = TealiumView(name, data)
        Tealium[INSTANCE]?.track(viewDispatch)
    }

    fun trackEvent(name: String, data: Map<String, Any>?) {
        val eventDispatch = TealiumEvent(name, data)
        Tealium[INSTANCE]?.track(eventDispatch)
    }
}