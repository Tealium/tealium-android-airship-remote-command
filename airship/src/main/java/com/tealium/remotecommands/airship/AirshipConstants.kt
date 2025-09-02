package com.tealium.remotecommands.airship
import com.urbanairship.PrivacyManager
import org.json.JSONArray
import java.util.Locale

object AirshipConstants {

    const val COMMAND_NAME = "command_name"
    const val SEPARATOR = ","
    const val DEFAULT_COMMAND_NAME = "airship"
    const val DEFAULT_COMMAND_DESCRIPTION = "Airship Remote Command"

    enum class TagType {
        CHANNEL_TAG, NAMED_USER_TAG
    }

    object Commands {
        const val INITIALIZE = "initialize"
        const val TRACK_EVENT = "trackevent"
        const val TRACK_SCREEN_VIEW = "trackscreenview"
        const val ENABLE_ANALYTICS = "enableanalytics"
        const val DISABLE_ANALYTICS = "disableanalytics"
        const val SET_NAMED_USER = "setnameduser"
        const val SET_CUSTOM_IDENTIFIERS = "setcustomidentifiers"
        const val ENABLE_ADVERTISING_IDENTIFIERS = "enableadvertisingidentifiers"
        const val ENABLE_IN_APP_MESSAGING = "enableinappmessaging"
        const val DISABLE_IN_APP_MESSAGING = "disableinappmessaging"
        const val PAUSE_IN_APP_MESSAGING = "pauseinappmessaging"
        const val UNPAUSE_IN_APP_MESSAGING = "unpauseinappmessaging"
        const val SET_IN_APP_MESSAGING_DISPLAY_INTERVAL = "setinappmessagingdisplayinterval"
        const val ENABLE_USER_PUSH_NOTIFICATIONS = "enableuserpushnotifications"
        const val DISABLE_USER_PUSH_NOTIFICATIONS = "disableuserpushnotifications"
        const val SET_PUSH_NOTIFICATION_OPTIONS = "setpushnotificationoptions"
        const val ENABLE_QUIET_TIME = "enablequiettime"
        const val DISABLE_QUIET_TIME = "disablequiettime"
        const val SET_QUIET_TIME_START = "setquiettimestart"
        const val SET_CHANNEL_TAGS = "setchanneltags"
        const val SET_NAMED_USER_TAGS = "setnamedusertags"
        const val ADD_TAG = "addtag"
        const val REMOVE_TAG = "removetag"
        const val ADD_TAG_GROUP = "addtaggroup"
        const val REMOVE_TAG_GROUP = "removetaggroup"
        const val SET_ATTRIBUTES = "setattributes"
        const val DISPLAY_MESSAGE_CENTER = "displaymessagecenter"
    }

    object Keys {
        const val EVENT_NAME = "event_name"
        const val EVENT_PROPERTIES = "event"
        const val EVENT_VALUE = "event_value"
        const val SCREEN_NAME = "screen_name"
        const val NAMED_USER_IDENTIFIER = "named_user_identifier"
        const val CUSTOM_IDENTIFIERS = "custom"
        const val IN_APP_MESSAGING_DISPLAY_INTERVAL = "in_app_messaging_display_interval"
        const val PUSH_NOTIFICATION_OPTIONS = "push_notification_options"
        const val QUIET = "quiet"
        const val START_HOUR = "start_hour"
        const val START_MINUTE = "start_minute"
        const val END_HOUR = "end_hour"
        const val END_MINUTE = "end_minute"
        const val NAMED_USER_TAGS = "named_user_tags"
        const val CHANNEL_TAGS = "channel_tags"
        const val TAG_GROUP = "tag_group"
        const val CHANNEL_TAG = "channel_tag"
        const val ATTRIBUTES = "attributes"
        const val CHANNEL_ID = "channel_id"
        const val MESSAGE_ID = "message_id"
        const val GOOGLE_ADID = "google_adid"
        const val GOOGLE_AD_TRACKING = "google_limit_ad_tracking"
    }
    
    /**
     * Configuration keys for Airship SDK initialization.
     * Reference: https://docs.airship.com/reference/libraries/android-kotlin/latest/urbanairship-core/com.urbanairship/-airship-config-options/-builder/index.html
     */
    object Config {
        const val PRODUCTION_APP_KEY = "productionAppKey"
        const val PRODUCTION_APP_SECRET = "productionAppSecret"
        const val DEVELOPMENT_APP_KEY = "developmentAppKey"
        const val DEVELOPMENT_APP_SECRET = "developmentAppSecret"
        const val SITE = "site"
        const val IS_DATA_COLLECTION_ENABLED = "isDataCollectionEnabled"
        const val IS_IN_PRODUCTION = "isInProduction"
        const val INITIAL_CONFIG_URL = "initialConfigUrl"
        const val REQUIRE_INITIAL_REMOTE_CONFIG_ENABLED = "requireInitialRemoteConfigEnabled"
        const val LOG_LEVEL = "logLevel"  
        const val DEVELOPMENT_LOG_LEVEL = "developmentLogLevel"
        const val PRODUCTION_LOG_LEVEL = "productionLogLevel" 
        const val IS_ANALYTICS_ENABLED = "isAnalyticsEnabled"
        const val DEFAULT_CHANNEL = "defaultChannel"
        const val CHANNEL_CREATION_DELAY_ENABLED = "channelCreationDelayEnabled"
        const val ALLOWED_TRANSPORTS = "allowedTransports" 
        const val FCM_FIREBASE_APP_NAME = "fcmFirebaseAppName"
        const val APP_STORE_URI = "appStoreUri"
        const val AUTO_PAUSE_IN_APP_AUTOMATION_ON_LAUNCH = "autoPauseInAppAutomationOnLaunch"
        const val BACKGROUND_REPORTING_INTERVAL_MS = "backgroundReportingIntervalMS"
        const val CHANNEL_CAPTURE_ENABLED = "channelCaptureEnabled"
        const val EXTENDED_BROADCASTS_ENABLED = "extendedBroadcastsEnabled" 
        const val URL_ALLOW_LIST = "urlAllowList"
        const val URL_ALLOW_LIST_SCOPE_JAVASCRIPT_INTERFACE = "urlAllowListScopeJavaScriptInterface"
        const val URL_ALLOW_LIST_SCOPE_OPEN_URL = "urlAllowListScopeOpenUrl"
        const val ENABLED_FEATURES = "enabledFeatures"
        const val RESET_ENABLED_FEATURES = "resetEnabledFeatures"
    }

    /**
     * Privacy Manager feature names for JSON configuration.
     * Reference: https://docs.airship.com/reference/libraries/android-kotlin/latest/urbanairship-core/com.urbanairship/-privacy-manager/-feature/-companion/index.html
     */
    object PrivacyFeatures {
        const val ALL = "ALL"
        const val NONE = "NONE"
        const val ANALYTICS = "ANALYTICS"
        const val CONTACTS = "CONTACTS"
        const val FEATURE_FLAGS = "FEATURE_FLAGS"
        const val IN_APP_AUTOMATION = "IN_APP_AUTOMATION"
        const val MESSAGE_CENTER = "MESSAGE_CENTER"
        const val PUSH = "PUSH"
        const val TAGS_AND_ATTRIBUTES = "TAGS_AND_ATTRIBUTES"
        
        /**
         * Converts JSON array of feature names to Airship PrivacyManager.Feature array.
         * 
         * @param featuresArray JSON array containing feature names
         * @return Array of PrivacyManager.Feature
         */
        fun fromJsonArray(featuresArray: JSONArray): Array<PrivacyManager.Feature> {
            val featureNames = (0 until featuresArray.length()).map { featuresArray.optString(it).uppercase(Locale.ROOT) }
            
            if (featureNames.contains(ALL)) {
                return arrayOf(PrivacyManager.Feature.ALL)
            }
            if (featureNames.contains(NONE)) {
                return arrayOf(PrivacyManager.Feature.NONE)
            }
            
            return featureNames.mapNotNull { fromString(it) }.toTypedArray()
        }
        
        /**
         * Converts single feature name string to PrivacyManager.Feature.
         * 
         * @param featureName Feature name (case-insensitive)
         * @return PrivacyManager.Feature or null if unknown
         */
        fun fromString(featureName: String): PrivacyManager.Feature? {
            return when (featureName.uppercase(Locale.ROOT)) {
                ALL -> PrivacyManager.Feature.ALL
                NONE -> PrivacyManager.Feature.NONE
                ANALYTICS -> PrivacyManager.Feature.ANALYTICS
                CONTACTS -> PrivacyManager.Feature.CONTACTS
                FEATURE_FLAGS -> PrivacyManager.Feature.FEATURE_FLAGS
                IN_APP_AUTOMATION -> PrivacyManager.Feature.IN_APP_AUTOMATION
                MESSAGE_CENTER -> PrivacyManager.Feature.MESSAGE_CENTER
                PUSH -> PrivacyManager.Feature.PUSH
                TAGS_AND_ATTRIBUTES -> PrivacyManager.Feature.TAGS_AND_ATTRIBUTES
                else -> null
            }
        }
    }
}