package com.tealium.remotecommands.airship

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.urbanairship.AirshipConfigOptions
import com.urbanairship.PrivacyManager
import com.urbanairship.UAirship
import com.urbanairship.analytics.CustomEvent
import com.urbanairship.automation.InAppAutomation
import com.urbanairship.channel.TagGroupsEditor
import com.urbanairship.json.JsonMap
import com.urbanairship.messagecenter.MessageCenter
import com.urbanairship.push.notifications.NotificationChannelCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri


class AirshipInstance : AirshipCommand {

    private val quietTimeDateFormatter = SimpleDateFormat("hh:mm", Locale.US)
    private val attributeDateFormatter = SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", Locale.ROOT)
    private var defaultChannelName: String? = null

    override fun initialize(application: Application, config: JSONObject) {
        // UAirship.takeOff() must be called on the main thread
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            UAirship.takeOff(application, parseOptions(config))
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                UAirship.takeOff(application, parseOptions(config))
            }
        }
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

        if (config.has(AirshipConstants.Config.ENABLED_FEATURES)) {
            val enabledFeaturesArray = config.getJSONArray(AirshipConstants.Config.ENABLED_FEATURES)
            val features = AirshipConstants.PrivacyFeatures.fromJsonArray(enabledFeaturesArray)
            options.setEnabledFeatures(*features)
        } else if (config.has(AirshipConstants.Config.IS_DATA_COLLECTION_ENABLED)) {
            // Backward compatibility
            if (config.optBoolean(AirshipConstants.Config.IS_DATA_COLLECTION_ENABLED)) {
                options.enabledFeatures = PrivacyManager.Feature.ALL
            } else {
                options.enabledFeatures = PrivacyManager.Feature.NONE
            }
        }

        if (config.has(AirshipConstants.Config.RESET_ENABLED_FEATURES)) {
            options.setResetEnabledFeatures(config.optBoolean(AirshipConstants.Config.RESET_ENABLED_FEATURES))
        }

        if (config.has(AirshipConstants.Config.IS_IN_PRODUCTION)) {
            val isInProduction = config.optBoolean(AirshipConstants.Config.IS_IN_PRODUCTION)
            options.setInProduction(isInProduction)
        }

        if (config.has(AirshipConstants.Config.INITIAL_CONFIG_URL)) {
            val initialConfigUrl = config.optString(AirshipConstants.Config.INITIAL_CONFIG_URL)
            if (initialConfigUrl.isNotEmpty()) {
                options.setInitialConfigUrl(initialConfigUrl)
            }
        }

        if (config.has(AirshipConstants.Config.REQUIRE_INITIAL_REMOTE_CONFIG_ENABLED)) {
            val requireInitialRemoteConfig = config.optBoolean(AirshipConstants.Config.REQUIRE_INITIAL_REMOTE_CONFIG_ENABLED) 
            options.setRequireInitialRemoteConfigEnabled(requireInitialRemoteConfig)
        }

        if (config.has(AirshipConstants.Config.LOG_LEVEL)) {
            val logLevel = parseLogLevel(config.optString(AirshipConstants.Config.LOG_LEVEL))
            if (logLevel != null) {
                options.setLogLevel(logLevel)
            } else {
                Log.w(BuildConfig.TAG, "Invalid logLevel: '${config.optString(AirshipConstants.Config.LOG_LEVEL)}'. Using ERROR as default. Valid values: trace, debug, info, warn, error, none")
                options.setLogLevel(Log.ERROR)
            }
        }

        if (config.has(AirshipConstants.Config.DEVELOPMENT_LOG_LEVEL)) {
            val developmentLogLevel = config.optString(AirshipConstants.Config.DEVELOPMENT_LOG_LEVEL)
            val logLevel = parseLogLevel(developmentLogLevel)
            if (logLevel != null) {
                options.setDevelopmentLogLevel(logLevel)
            } else {
                Log.w(BuildConfig.TAG, "Invalid developmentLogLevel: '$developmentLogLevel'. Using ERROR as default. Valid values: trace, debug, info, warn, error, none")
                options.setDevelopmentLogLevel(Log.ERROR)
            }
        }

        if (config.has(AirshipConstants.Config.PRODUCTION_LOG_LEVEL)) {
            val productionLogLevel = config.optString(AirshipConstants.Config.PRODUCTION_LOG_LEVEL)
            val logLevel = parseLogLevel(productionLogLevel)
            if (logLevel != null) {
                options.setProductionLogLevel(logLevel)
            } else {
                Log.w(BuildConfig.TAG, "Invalid productionLogLevel: '$productionLogLevel'. Using ERROR as default. Valid values: trace, debug, info, warn, error, none")
                options.setProductionLogLevel(Log.ERROR)
            }
        }

        if (config.has(AirshipConstants.Config.IS_ANALYTICS_ENABLED)) {
            val analyticsEnabled = config.optBoolean(AirshipConstants.Config.IS_ANALYTICS_ENABLED)
            options.setAnalyticsEnabled(analyticsEnabled)
        }

        if (config.has(AirshipConstants.Config.DEFAULT_CHANNEL)) {
            val defaultChannel = config.optString(AirshipConstants.Config.DEFAULT_CHANNEL)
            options.setNotificationChannel(defaultChannel)
            defaultChannelName = defaultChannel
        }

        if (config.has(AirshipConstants.Config.CHANNEL_CREATION_DELAY_ENABLED)) {
            val channelCreationDelayEnabled = config.optBoolean(AirshipConstants.Config.CHANNEL_CREATION_DELAY_ENABLED) 
            options.setChannelCreationDelayEnabled(channelCreationDelayEnabled)
        }

        if (config.has(AirshipConstants.Config.ALLOWED_TRANSPORTS)) {
            val allowedTransportsArray = config.getJSONArray(AirshipConstants.Config.ALLOWED_TRANSPORTS)
            val transports = allowedTransportsArray.toStringList().toTypedArray() 
            options.setAllowedTransports(transports)
        }

        if (config.has(AirshipConstants.Config.FCM_FIREBASE_APP_NAME)) {
            val fcmFirebaseAppName = config.optString(AirshipConstants.Config.FCM_FIREBASE_APP_NAME)
            if (fcmFirebaseAppName.isNotEmpty()) {
                options.setFcmFirebaseAppName(fcmFirebaseAppName)
            }
        }

        if (config.has(AirshipConstants.Config.APP_STORE_URI)) {
            val appStoreUriString = config.optString(AirshipConstants.Config.APP_STORE_URI)
            if (appStoreUriString.isNotEmpty()) {
                try {
                    val appStoreUri = appStoreUriString.toUri()
                    options.setAppStoreUri(appStoreUri)
                } catch (e: Exception) {
                    Log.w(BuildConfig.TAG, "Invalid app store URI: $appStoreUriString", e)
                }
            }
        }

        if (config.has(AirshipConstants.Config.AUTO_PAUSE_IN_APP_AUTOMATION_ON_LAUNCH)) {
            val autoPauseInAppAutomationOnLaunch = config.optBoolean(AirshipConstants.Config.AUTO_PAUSE_IN_APP_AUTOMATION_ON_LAUNCH) 
            options.setAutoPauseInAppAutomationOnLaunch(autoPauseInAppAutomationOnLaunch)
        }

        if (config.has(AirshipConstants.Config.BACKGROUND_REPORTING_INTERVAL_MS)) {
            val backgroundReportingIntervalMS = config.optLong(AirshipConstants.Config.BACKGROUND_REPORTING_INTERVAL_MS)
            if (backgroundReportingIntervalMS > 0) {
                options.setBackgroundReportingIntervalMS(backgroundReportingIntervalMS)
            }
        }

        if (config.has(AirshipConstants.Config.CHANNEL_CAPTURE_ENABLED)) {
            val channelCaptureEnabled = config.optBoolean(AirshipConstants.Config.CHANNEL_CAPTURE_ENABLED)
            options.setChannelCaptureEnabled(channelCaptureEnabled)
        }

        if (config.has(AirshipConstants.Config.EXTENDED_BROADCASTS_ENABLED)) {
            val extendedBroadcastsEnabled = config.optBoolean(AirshipConstants.Config.EXTENDED_BROADCASTS_ENABLED)
            options.setExtendedBroadcastsEnabled(extendedBroadcastsEnabled)
        }

        if (config.has(AirshipConstants.Config.URL_ALLOW_LIST)) {
            val urlAllowListArray = config.optJSONArray(AirshipConstants.Config.URL_ALLOW_LIST)
            if (urlAllowListArray != null && urlAllowListArray.length() > 0) {
                val urlAllowList = Array(urlAllowListArray.length()) { i ->
                    urlAllowListArray.optString(i)
                }
                options.setUrlAllowList(urlAllowList)
            }
        }

        if (config.has(AirshipConstants.Config.URL_ALLOW_LIST_SCOPE_JAVASCRIPT_INTERFACE)) {
            val urlAllowListArray = config.optJSONArray(AirshipConstants.Config.URL_ALLOW_LIST_SCOPE_JAVASCRIPT_INTERFACE)
            if (urlAllowListArray != null && urlAllowListArray.length() > 0) {
                val urlAllowList = Array(urlAllowListArray.length()) { i ->
                    urlAllowListArray.optString(i)
                }
                options.setUrlAllowListScopeJavaScriptInterface(urlAllowList)
            }
        }

        if (config.has(AirshipConstants.Config.URL_ALLOW_LIST_SCOPE_OPEN_URL)) {
            val urlAllowListArray = config.optJSONArray(AirshipConstants.Config.URL_ALLOW_LIST_SCOPE_OPEN_URL)
            if (urlAllowListArray != null && urlAllowListArray.length() > 0) {
                val urlAllowList = Array(urlAllowListArray.length()) { i ->
                    urlAllowListArray.optString(i)
                }
                options.setUrlAllowListScopeOpenUrl(urlAllowList)
            }
        }

        return options.build()
    }

    internal fun parseLogLevel(level: String): Int? {
        return when (level.lowercase(Locale.ROOT)) {
            "trace" -> Log.VERBOSE
            "debug" -> Log.DEBUG
            "info" -> Log.INFO
            "warn" -> Log.WARN
            "error" -> Log.ERROR
            "none" -> Int.MAX_VALUE
            else -> null
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
        val editor = UAirship.shared().contact.editTagGroups()
        editor.setTags(group, tags.toStringList().toMutableSet())
        editor.apply()
    }

    override fun addTag(tagName: String) {
        UAirship.shared().channel.tags = setOf(tagName).union(UAirship.shared().channel.tags)
    }

    override fun removeTag(tagName: String) {
        val currentTags = UAirship.shared().channel.tags.toMutableSet()
        currentTags.remove(tagName)
        UAirship.shared().channel.tags = currentTags
    }

    override fun addTagGroup(group: String, tags: JSONArray, type: AirshipConstants.TagType) {
        val editor: TagGroupsEditor = when (type) {
            AirshipConstants.TagType.NAMED_USER_TAG -> UAirship.shared().contact.editTagGroups()
            AirshipConstants.TagType.CHANNEL_TAG -> UAirship.shared().channel.editTagGroups()
        }
        tags.toStringList().forEach {
            editor.addTag(group, it)
        }
        editor.apply()
    }

    override fun removeTagGroup(group: String, tags: JSONArray, type: AirshipConstants.TagType) {
        val editor: TagGroupsEditor = when (type) {
            AirshipConstants.TagType.NAMED_USER_TAG -> UAirship.shared().contact.editTagGroups()
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
        UAirship.shared().contact.identify(id)
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
                        when (opt.lowercase(Locale.ROOT)) {
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
                        PrivacyManager.Feature.ANALYTICS
                    )
                } else {
                    UAirship.shared().privacyManager.disable(
                        PrivacyManager.Feature.ANALYTICS
                    )
                }
            }
        }

    override var inAppMessagingEnabled: Boolean?
        get() = UAirship.shared().privacyManager.isEnabled(PrivacyManager.Feature.IN_APP_AUTOMATION)
        set(value) {
            value?.let {
                if (it) {
                    UAirship.shared().privacyManager.enable(PrivacyManager.Feature.IN_APP_AUTOMATION)
                } else {
                    UAirship.shared().privacyManager.disable(PrivacyManager.Feature.IN_APP_AUTOMATION)
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
        get() = InAppAutomation.shared().inAppMessaging.displayInterval
        set(value) {
            value?.let {
                InAppAutomation.shared().inAppMessaging.displayInterval = it
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