package com.tealium.remotecommands.airship

import android.app.Application
import android.util.Log
import com.tealium.remotecommands.RemoteCommand
import com.tealium.remotecommands.airship.AirshipConstants.COMMAND_NAME
import com.tealium.remotecommands.airship.AirshipConstants.DEFAULT_COMMAND_DESCRIPTION
import com.tealium.remotecommands.airship.AirshipConstants.DEFAULT_COMMAND_NAME
import com.tealium.remotecommands.airship.AirshipConstants.Keys.GOOGLE_ADID
import com.tealium.remotecommands.airship.AirshipConstants.Keys.GOOGLE_AD_TRACKING
import com.tealium.remotecommands.airship.AirshipConstants.SEPARATOR
import org.json.JSONObject
import java.util.*

class AirshipRemoteCommand(
    private val application: Application,
    commandName: String = DEFAULT_COMMAND_NAME,
    commandDescription: String = DEFAULT_COMMAND_DESCRIPTION,
    private val airshipCommand: AirshipCommand = AirshipInstance()
): RemoteCommand(commandName, commandDescription) {

    public override fun onInvoke(response: Response?) {
        response?.requestPayload?.let { payload ->
            val commandList = splitCommands(payload)
            parseCommands(commandList, payload)
        }
        response?.send()
    }

    internal fun splitCommands(payload: JSONObject): Array<String> {
        val commandString = payload.optString(COMMAND_NAME, "")
        return commandString.split(SEPARATOR).map {
            it.trim().toLowerCase(Locale.ROOT)
        }.toTypedArray()
    }

    internal fun parseCommands(commands: Array<String>, payload: JSONObject) {
        commands.forEach { command ->
            try {
                Log.d(BuildConfig.TAG, "Handling command: $command")
                when (command) {
                    AirshipConstants.Commands.INITIALIZE -> {
                        airshipCommand.initialize(application, payload)
                    }
                    AirshipConstants.Commands.TRACK_EVENT -> {
                        val eventName = payload.getString(AirshipConstants.Keys.EVENT_NAME)
                        val eventValue: Double? = if (payload.has(AirshipConstants.Keys.EVENT_VALUE)) payload.optDouble(AirshipConstants.Keys.EVENT_VALUE) else null
                        val eventProperties = payload.optJSONObject(AirshipConstants.Keys.EVENT_PROPERTIES)

                        airshipCommand.trackEvent(eventName, eventValue, eventProperties)
                    }
                    AirshipConstants.Commands.TRACK_SCREEN_VIEW -> {
                        val screenName = payload.getString(AirshipConstants.Keys.SCREEN_NAME)

                        airshipCommand.trackScreenView(screenName)
                    }
                    AirshipConstants.Commands.SET_NAMED_USER -> {
                        val userName = payload.getString(AirshipConstants.Keys.NAMED_USER_IDENTIFIER)

                        airshipCommand.identifyUser(userName)
                    }
                    AirshipConstants.Commands.SET_CUSTOM_IDENTIFIERS -> {
                        val customIdentifiers = payload.getJSONObject(AirshipConstants.Keys.CUSTOM_IDENTIFIERS)

                        airshipCommand.setCustomIdentifiers(customIdentifiers)
                    }
                    AirshipConstants.Commands.SET_IN_APP_MESSAGING_DISPLAY_INTERVAL -> {
                        val interval = payload.getLong(AirshipConstants.Keys.IN_APP_MESSAGING_DISPLAY_INTERVAL)

                        airshipCommand.inAppMessagingDisplayInterval = interval
                    }
                    AirshipConstants.Commands.SET_PUSH_NOTIFICATION_OPTIONS -> {
                        val options = payload.getJSONArray(AirshipConstants.Keys.PUSH_NOTIFICATION_OPTIONS)
                        val channelId = payload.optString(AirshipConstants.Keys.CHANNEL_ID)

                        airshipCommand.setPushNotificationOptions(options, channelId)
                    }
//                    AirshipConstants.Commands.SET_FOREGROUND_PRESENTATION_OPTIONS -> {
//                        val options = payload.getJSONArray(AirshipConstants.Keys.FOREGROUND_PRESENTATION_OPTIONS)
//
//                        airshipCommand.foregroundPresentationOptions = options
//                    }
//                    AirshipConstants.Commands.SET_BADGE_NUMBER -> {
//                        val badgeNumber = payload.getInt(AirshipConstants.Keys.BADGE_NUMBER)
//
//                        airshipCommand.badgeNumber = badgeNumber
//                    }
//                    AirshipConstants.Commands.RESET_BADGE_NUMBER -> {
//                        airshipCommand.resetBadgeNumber()
//                    }
                    AirshipConstants.Commands.SET_QUIET_TIME_START -> {
                        val quietTime = payload.getJSONObject(AirshipConstants.Keys.QUIET)

                        val startHour = quietTime.getInt(AirshipConstants.Keys.START_HOUR)
                        val startMinute = quietTime.getInt(AirshipConstants.Keys.START_MINUTE)
                        val endHour = quietTime.getInt(AirshipConstants.Keys.END_HOUR)
                        val endMinute = quietTime.getInt(AirshipConstants.Keys.END_MINUTE)

                        airshipCommand.setQuietTimeStartHour(startHour, startMinute, endHour, endMinute)
                    }
                    AirshipConstants.Commands.SET_CHANNEL_TAGS -> {
                        val tags = payload.getJSONArray(AirshipConstants.Keys.CHANNEL_TAGS)

                        airshipCommand.channelTags = tags
                    }
                    AirshipConstants.Commands.SET_NAMED_USER_TAGS -> {
                        val group = payload.getString(AirshipConstants.Keys.TAG_GROUP)
                        val tags = payload.getJSONArray(AirshipConstants.Keys.NAMED_USER_TAGS)

                        airshipCommand.setNamedUserTags(group, tags)
                    }
                    AirshipConstants.Commands.ADD_TAG -> {
                        val tag = payload.getString(AirshipConstants.Keys.CHANNEL_TAG)

                        airshipCommand.addTag(tag)
                    }
                    AirshipConstants.Commands.REMOVE_TAG -> {
                        val tag = payload.getString(AirshipConstants.Keys.CHANNEL_TAG)

                        airshipCommand.removeTag(tag)
                    }
                    AirshipConstants.Commands.ADD_TAG_GROUP -> {
                        val channelTags = payload.optJSONArray(AirshipConstants.Keys.CHANNEL_TAGS)
                        val userTags = payload.optJSONArray(AirshipConstants.Keys.NAMED_USER_TAGS)
                        val group = payload.getString(AirshipConstants.Keys.TAG_GROUP)

                        channelTags?.let {
                            airshipCommand.addTagGroup(group, it, AirshipConstants.TagType.CHANNEL_TAG)
                        }

                        userTags?.let {
                            airshipCommand.addTagGroup(group, it, AirshipConstants.TagType.NAMED_USER_TAG)
                        }
                    }
                    AirshipConstants.Commands.REMOVE_TAG_GROUP -> {
                        val group = payload.getString(AirshipConstants.Keys.TAG_GROUP)

                        val channelTags = payload.optJSONArray(AirshipConstants.Keys.CHANNEL_TAGS)
                        val userTags = payload.optJSONArray(AirshipConstants.Keys.NAMED_USER_TAGS)

                        channelTags?.let {
                            airshipCommand.removeTagGroup(group, it, AirshipConstants.TagType.CHANNEL_TAG)
                        }

                        userTags?.let {
                            airshipCommand.removeTagGroup(group, it, AirshipConstants.TagType.NAMED_USER_TAG)
                        }
                    }
                    AirshipConstants.Commands.SET_ATTRIBUTES -> {
                        val attributes = payload.getJSONObject(AirshipConstants.Keys.ATTRIBUTES)

                        airshipCommand.setAttributes(attributes)
                    }
//                    AirshipConstants.Commands.SET_MESSAGE_CENTER_TITLE -> {
//                        val title = payload.getString(AirshipConstants.Keys.MESSAGE_CENTER_TITLE)
//
//                        airshipCommand.messageCenterTitle = title
//                    }
//                    AirshipConstants.Commands.SET_MESSAGE_CENTER_STYLE -> {
//                        val style = payload.getJSONObject(AirshipConstants.Keys.MESSAGE_CENTER_STYLE)
//
//                        airshipCommand.setMessageCenterStyle(style)
//                    }
                    // No props required
                    AirshipConstants.Commands.ENABLE_ANALYTICS -> {
                        airshipCommand.analyticsEnabled = true
                    }
                    AirshipConstants.Commands.DISABLE_ANALYTICS -> {
                        airshipCommand.analyticsEnabled = false
                    }
                    AirshipConstants.Commands.ENABLE_ADVERTISING_IDENTIFIERS -> {
                        airshipCommand.enableAdvertisingIDs(
                            payload.getString(GOOGLE_ADID),
                            payload.getBoolean(GOOGLE_AD_TRACKING)
                        )
                    }
                    AirshipConstants.Commands.ENABLE_IN_APP_MESSAGING -> {
                        airshipCommand.inAppMessagingEnabled = true
                    }
                    AirshipConstants.Commands.DISABLE_IN_APP_MESSAGING -> {
                        airshipCommand.inAppMessagingEnabled = false
                    }
                    AirshipConstants.Commands.PAUSE_IN_APP_MESSAGING -> {
                        airshipCommand.inAppMessagingPaused = true
                    }
                    AirshipConstants.Commands.UNPAUSE_IN_APP_MESSAGING -> {
                        airshipCommand.inAppMessagingPaused = false
                    }
                    AirshipConstants.Commands.ENABLE_USER_PUSH_NOTIFICATIONS -> {
                        val options = payload.optJSONArray(AirshipConstants.Keys.PUSH_NOTIFICATION_OPTIONS)
                        val channelId = payload.optString(AirshipConstants.Keys.CHANNEL_ID)

                        airshipCommand.enablePushNotifications(options, channelId)
                    }
                    AirshipConstants.Commands.DISABLE_USER_PUSH_NOTIFICATIONS -> {
                        airshipCommand.userPushNotificationsEnabled = false
                    }
                    AirshipConstants.Commands.ENABLE_QUIET_TIME -> {
                        airshipCommand.quietTimeEnabled = true
                    }
                    AirshipConstants.Commands.DISABLE_QUIET_TIME -> {
                        airshipCommand.quietTimeEnabled = false
                    }
                    AirshipConstants.Commands.ENABLE_LOCATION -> {
                        airshipCommand.locationEnabled = true
                    }
                    AirshipConstants.Commands.DISABLE_LOCATION -> {
                        airshipCommand.locationEnabled = false
                    }
                    AirshipConstants.Commands.ENABLE_BACKGROUND_LOCATION -> {
                        airshipCommand.backgroundLocationEnabled = true
                    }
                    AirshipConstants.Commands.DISABLE_BACKGROUND_LOCATION -> {
                        airshipCommand.backgroundLocationEnabled = false
                    }
                    AirshipConstants.Commands.DISPLAY_MESSAGE_CENTER -> {
                        val messageId: String? = payload.optString(AirshipConstants.Keys.MESSAGE_ID).let {
                            if (it.isEmpty()) null else it
                        }
                        airshipCommand.displayMessageCenter(messageId)
                    }
                }
            } catch (ex: Exception) {
                Log.w(BuildConfig.TAG, "Error processing command: $command", ex)
            }
        }
    }
}
