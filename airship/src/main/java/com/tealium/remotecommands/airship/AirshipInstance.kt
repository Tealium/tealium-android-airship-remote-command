package com.tealium.remotecommands.airship

import android.app.Application
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.urbanairship.AirshipConfigOptions
import com.urbanairship.PrivacyManager
import com.urbanairship.UAirship
import com.urbanairship.analytics.CustomEvent
import com.urbanairship.automation.InAppAutomation
import com.urbanairship.channel.TagGroupsEditor
import com.urbanairship.json.JsonMap
import com.urbanairship.location.AirshipLocationManager
import com.urbanairship.messagecenter.MessageCenter
import com.urbanairship.push.notifications.NotificationChannelCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class AirshipInstance : AirshipCommand {

    private val quietTimeDateFormatter = SimpleDateFormat("hh:mm", Locale.US)
    private val attributeDateFormatter = SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", Locale.ROOT)
    private var defaultChannelName: String? = null

    override fun initialize(application: Application, config: JSONObject) {

        UAirship.takeOff(application, parseOptions(config))
    }

    internal fun parseOptions(config: JSONObject): AirshipConfigOptions {
        val options: AirshipConfigOptions.Builder = AirshipConfigOptions.newBuilder()

        val productionAppKey = config.optString(AirshipConstants.Config.PRODUCTION_APP_KEY)
        if (productionAppKey.isNotEmpty()) {
            options.setProductionAppKey(productionAppKey)
        }

        val productionAppSecret = config.optString(AirshipConstants.Config.PRODUCTION_APP_SECRET)
        if (productionAppSecret.isNotEmpty()) {
            options.setProductionAppSecret(productionAppSecret)
        }

        val developmentAppKey = config.optString(AirshipConstants.Config.DEVELOPMENT_APP_KEY)
        if (developmentAppKey.isNotEmpty()) {
            options.setDevelopmentAppKey(developmentAppKey)
        }

        val developmentAppSecret = config.optString(AirshipConstants.Config.DEVELOPMENT_APP_SECRET)
        if (developmentAppSecret.isNotEmpty()) {
            options.setDevelopmentAppSecret(developmentAppSecret)
        }

        val site = config.optString(AirshipConstants.Config.SITE)
        if (site.isNotEmpty()) {
            options.setSite(site)
        }

        if (config.has(AirshipConstants.Config.IS_DATA_COLLECTION_ENABLED)) {
            if (config.optBoolean(AirshipConstants.Config.IS_DATA_COLLECTION_ENABLED)) {
                options.enabledFeatures = PrivacyManager.FEATURE_ALL
            } else {
                options.enabledFeatures = PrivacyManager.FEATURE_NONE
            }
        }

        if (config.has(AirshipConstants.Config.IS_IN_PRODUCTION)) {
            val isInProduction = config.optBoolean(AirshipConstants.Config.IS_IN_PRODUCTION)
            options.setInProduction(isInProduction)
        }

        if (config.has(AirshipConstants.Config.DEVELOPMENT_LOG_LEVEL)) {
            val developmentLogLevel =
                config.optString(AirshipConstants.Config.DEVELOPMENT_LOG_LEVEL)
            options.setDevelopmentLogLevel(parseLogLevel(developmentLogLevel))
        }

        if (config.has(AirshipConstants.Config.PRODUCTION_LOG_LEVEL)) {
            val productionLogLevel = config.optString(AirshipConstants.Config.PRODUCTION_LOG_LEVEL)
            options.setProductionLogLevel(parseLogLevel(productionLogLevel))
        }

        if (config.has(AirshipConstants.Config.IS_ANALYTICS_ENABLED)) {
            if (config.optBoolean(AirshipConstants.Config.IS_ANALYTICS_ENABLED)) {
                options.enabledFeatures =
                    options.enabledFeatures or PrivacyManager.FEATURE_ANALYTICS
            } else {
                options.enabledFeatures =
                    options.enabledFeatures xor PrivacyManager.FEATURE_ANALYTICS
            }
        }

        if (config.has(AirshipConstants.Config.DEFAULT_CHANNEL)) {
            val defaultChannel = config.optString(AirshipConstants.Config.DEFAULT_CHANNEL)
            options.setNotificationChannel(defaultChannel)
            defaultChannelName = defaultChannel
        }

        return options.build()
    }

    internal fun parseLogLevel(level: String): Int {
        return when (level.toLowerCase(Locale.ROOT)) {
            "trace" -> Log.VERBOSE
            "debug" -> Log.DEBUG
            "info" -> Log.INFO
            "warn" -> Log.WARN
            "error" -> Log.ERROR
            "none" -> Int.MAX_VALUE
            else -> Log.ERROR
        }
    }

    override var channelTags: JSONArray
        get() = UAirship.shared().channel.tags.toJSONArray()
        set(value) {
            value.let {
                UAirship.shared().channel.tags = it.toStringList().toMutableSet()
            }
        }

    override fun setNamedUserTags(group: String, tags: JSONArray) {
        val editor = UAirship.shared().namedUser.editTagGroups()
        editor.setTags(group, tags.toStringList().toMutableSet())
        editor.apply()
    }

    override fun addTag(tagName: String) {
        UAirship.shared().channel.tags = setOf(tagName).union(UAirship.shared().channel.tags)
    }

    override fun removeTag(tagName: String) {
        UAirship.shared().channel.tags =
            UAirship.shared().channel.tags.apply { this.remove(tagName) }
    }

    override fun addTagGroup(group: String, tags: JSONArray, type: AirshipConstants.TagType) {
        val editor: TagGroupsEditor = when (type) {
            AirshipConstants.TagType.NAMED_USER_TAG -> UAirship.shared().namedUser.editTagGroups()
            AirshipConstants.TagType.CHANNEL_TAG -> UAirship.shared().channel.editTagGroups()
        }
        tags.toStringList().forEach {
            editor.addTag(group, it)
        }
        editor.apply()
    }

    override fun removeTagGroup(group: String, tags: JSONArray, type: AirshipConstants.TagType) {
        val editor: TagGroupsEditor = when (type) {
            AirshipConstants.TagType.NAMED_USER_TAG -> UAirship.shared().namedUser.editTagGroups()
            AirshipConstants.TagType.CHANNEL_TAG -> UAirship.shared().channel.editTagGroups()
        }
        tags.toStringList().forEach {
            editor.removeTag(group, it)
        }
        editor.apply()
    }

    override fun setAttributes(attributes: JSONObject) {
        val editor = UAirship.shared().channel.editAttributes()
        for (key in attributes.keys()) {
            when (val entry = attributes.get(key)) {
                is String -> {
                    try {
                        val date = attributeDateFormatter.parse(entry)
                        if (date != null) {
                            editor.setAttribute(key, date)
                        } else {
                            editor.setAttribute(key, entry)
                        }
                    } catch (ignore: ParseException) {
                        // Not a date; add as String
                        editor.setAttribute(key, entry)
                    }
                }
                is Int -> editor.setAttribute(key, entry)
                is Long -> editor.setAttribute(key, entry)
                is Double -> editor.setAttribute(key, entry)
                is Float -> editor.setAttribute(key, entry)
            }
        }
        editor.apply()
    }

    override fun identifyUser(id: String) {
        UAirship.shared().namedUser.id = id
    }

    override fun enablePushNotifications(options: JSONArray?, channelId: String?) {
        userPushNotificationsEnabled = true

        options?.let {
            setPushNotificationOptions(it, channelId)
        }
    }

    override var userPushNotificationsEnabled: Boolean?
        get() = UAirship.shared().pushManager.userNotificationsEnabled
        set(value) {
            value?.let {
                UAirship.shared().pushManager.userNotificationsEnabled = it
            }
        }

    override fun setPushNotificationOptions(options: JSONArray, channelId: String?) {
        val channelName = channelId ?: defaultChannelName
        if (!channelName.isNullOrEmpty())
            UAirship.shared().pushManager.notificationChannelRegistry.getNotificationChannel(
                channelName
            ).addResultCallback {
                // Create if not found.
                val notificationChannel = it ?: NotificationChannelCompat(
                    channelName,
                    channelName,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                ).also { newChannel ->
                    UAirship.shared().pushManager.notificationChannelRegistry.createNotificationChannel(
                        newChannel
                    )
                }
                for (i in 0 until options.length()) {
                    options.optString(i)?.let { opt ->
                        when (opt.toLowerCase(Locale.ROOT)) {
                            "light" -> notificationChannel.enableLights(true)
                            "badge" -> notificationChannel.showBadge = true
                            "vibrate" -> notificationChannel.enableVibration(true)
                            else -> Unit
                        }
                    }
                }
            }

    }

    override var quietTimeEnabled: Boolean?
        get() = UAirship.shared().pushManager.isQuietTimeEnabled
        set(value) {
            value?.let {
                // Deprecated but supported on iOS
                UAirship.shared().pushManager.isQuietTimeEnabled = it
            }
        }

    override fun setQuietTimeStartHour(hour: Int, minute: Int, endHour: Int, endMinute: Int) {

        val startDate: Date? = quietTimeDateFormatter.parse("$hour:$minute")
        val endDate: Date? = quietTimeDateFormatter.parse("$endHour:$endMinute")
        if (startDate != null && endDate != null) {
            UAirship.shared().pushManager.setQuietTimeInterval(startDate, endDate)
            quietTimeEnabled = true
        }
    }

    override fun setCustomIdentifiers(identifiers: JSONObject?) {
        identifiers?.let {
            val idMap = it.toStringMap()
            val editor = UAirship.shared().analytics.editAssociatedIdentifiers()
            idMap.forEach { (key, value) ->
                editor.addIdentifier(key, value)
            }
            editor.apply()
        }
    }

    override fun enableAdvertisingIDs(id: String, isLimitAdTracking: Boolean) {
        if (id.isNotEmpty()) {
            UAirship.shared().analytics.editAssociatedIdentifiers()
                .setAdvertisingId(id, isLimitAdTracking)
                .apply()
        }
    }

    override fun trackScreenView(screenName: String) {
        UAirship.shared().analytics.trackScreen(screenName)
    }

    override fun trackEvent(eventName: String, value: Double?, eventProperties: JSONObject?) {
        trackEvent(CustomEvent.Builder(eventName), value, eventProperties)
    }

    internal fun trackEvent(
        eventBuilder: CustomEvent.Builder,
        value: Double?,
        eventProperties: JSONObject?
    ) {
        value?.let {
            eventBuilder.setEventValue(value)
        }
        eventProperties?.let {
            val jsonMap = JsonMap.newBuilder()
            for (key in it.keys()) {
                when (val entry = it[key]) {
                    is String -> jsonMap.put(key, entry)
                    is Int -> jsonMap.put(key, entry)
                    is Double -> jsonMap.put(key, entry)
                    is Long -> jsonMap.put(key, entry)
                    is Char -> jsonMap.put(key, entry)
                    is Boolean -> jsonMap.put(key, entry)
                }
            }
            eventBuilder.setProperties(jsonMap.build())
        }
        eventBuilder.build().track()
    }

    override var analyticsEnabled: Boolean?
        get() = UAirship.shared().analytics.isEnabled
        set(value) {
            value?.let {
                if (it) {
                    UAirship.shared().privacyManager.enable(
                        PrivacyManager.FEATURE_ANALYTICS
                    )
                } else {
                    UAirship.shared().privacyManager.disable(
                        PrivacyManager.FEATURE_ANALYTICS
                    )
                }
            }
        }

    override var locationEnabled: Boolean?
        get() = AirshipLocationManager.shared().isLocationUpdatesEnabled
        set(value) {
            value?.let {
                AirshipLocationManager.shared().isLocationUpdatesEnabled = it
            }
        }

    override var backgroundLocationEnabled: Boolean?
        get() = AirshipLocationManager.shared().isBackgroundLocationAllowed
        set(value) {
            value?.let {
                AirshipLocationManager.shared().isBackgroundLocationAllowed = it
            }
        }

    override var dataCollectionEnabled: Boolean?
        get() = UAirship.shared().isDataCollectionEnabled
        set(value) {
            value?.let {
                UAirship.shared().isDataCollectionEnabled = it
            }
        }

    override var inAppMessagingEnabled: Boolean?
        get() = UAirship.shared().privacyManager.isEnabled(PrivacyManager.FEATURE_IN_APP_AUTOMATION)
        set(value) {
            value?.let {
                if (it) {
                    UAirship.shared().privacyManager.enable(PrivacyManager.FEATURE_IN_APP_AUTOMATION)
                } else {
                    UAirship.shared().privacyManager.disable(PrivacyManager.FEATURE_IN_APP_AUTOMATION)
                }
            }
        }

    override var inAppMessagingPaused: Boolean?
        get() = InAppAutomation.shared().isPaused
        set(value) {
            value?.let {
                InAppAutomation.shared().isPaused = it
            }
        }

    override var inAppMessagingDisplayInterval: Long?
        get() = InAppAutomation.shared().inAppMessageManager.displayInterval
        set(value) {
            value?.let {
                InAppAutomation.shared().inAppMessageManager.setDisplayInterval(
                    it,
                    TimeUnit.SECONDS
                )
            }
        }

    override fun displayMessageCenter(messageId: String?) {
        MessageCenter.shared().showMessageCenter(messageId);
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until this.length()) {
            val item = this.optString(i)
            if (item.isNotEmpty()) {
                list.add(item)
            }
        }
        return list.toList()
    }

    private fun JSONObject.toStringMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (key in this.keys()) {
            val item = this.optString(key)
            if (item.isNotEmpty()) {
                map[key] = item
            }
        }
        return map.toMap()
    }

    private fun Collection<String>.toJSONArray(): JSONArray {
        val json = JSONArray()
        this.forEach {
            json.put(it)
        }
        return json
    }
}