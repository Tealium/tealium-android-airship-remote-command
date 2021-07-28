package com.tealium.remotecommands.airship

import android.app.Application
import org.json.JSONArray
import org.json.JSONObject

interface AirshipCommand {

    fun initialize(application: Application, config: JSONObject)
    var channelTags: JSONArray
    fun setNamedUserTags(group: String, tags: JSONArray)
    fun addTag(tagName: String)
    fun removeTag(tagName: String)
    fun addTagGroup(group: String, tags: JSONArray, type: AirshipConstants.TagType)
    fun removeTagGroup(group: String, tags: JSONArray, type: AirshipConstants.TagType)
    fun setAttributes(attributes: JSONObject)
    fun identifyUser(id: String)
    fun enablePushNotifications(options: JSONArray?, channelId: String?)
    var userPushNotificationsEnabled: Boolean?

    fun setPushNotificationOptions(options: JSONArray, channelId: String?)
    var quietTimeEnabled: Boolean?
    fun setQuietTimeStartHour(hour: Int, minute: Int, endHour: Int, endMinute: Int)
    fun setCustomIdentifiers(identifiers: JSONObject?)
    fun enableAdvertisingIDs(id: String, isLimitAdTracking: Boolean)
    fun trackScreenView(screenName: String)
    fun trackEvent(eventName: String, value: Double?, eventProperties: JSONObject?)
    var analyticsEnabled: Boolean?
    var locationEnabled: Boolean?
    var backgroundLocationEnabled: Boolean?
    var dataCollectionEnabled: Boolean?
    var inAppMessagingEnabled: Boolean?
    var inAppMessagingPaused: Boolean?
    var inAppMessagingDisplayInterval: Long?
    fun displayMessageCenter(messageId: String?)

}