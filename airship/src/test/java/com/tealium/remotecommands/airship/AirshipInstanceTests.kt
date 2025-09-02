package com.tealium.remotecommands.airship

import android.app.Application
import android.util.Log
import com.urbanairship.AirshipConfigOptions
import com.urbanairship.PrivacyManager
import com.urbanairship.UAirship
import com.urbanairship.analytics.Analytics
import com.urbanairship.analytics.AssociatedIdentifiers
import com.urbanairship.analytics.CustomEvent
import com.urbanairship.automation.InAppAutomation
import com.urbanairship.channel.AirshipChannel
import com.urbanairship.channel.AttributeEditor
import com.urbanairship.channel.TagGroupsEditor
import com.urbanairship.contacts.Contact
import com.urbanairship.json.JsonValue
import com.urbanairship.messagecenter.MessageCenter
import com.urbanairship.push.PushManager
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 35])
class AirshipInstanceTests {

    @RelaxedMockK
    lateinit var mockApplication: Application

    @RelaxedMockK
    lateinit var mockUAirship: UAirship

    @RelaxedMockK
    lateinit var mockPrivacyManager: PrivacyManager

    lateinit var airshipInstance: AirshipInstance
    lateinit var config: JSONObject

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        config = JSONObject()

        every { mockUAirship.privacyManager } returns mockPrivacyManager

        mockkStatic(UAirship::class)
        every { UAirship.shared() } returns mockUAirship

        airshipInstance = AirshipInstance()
    }

    @Test
    fun parseOptions_SetsCorrectProductionValues() {
        config.put(AirshipConstants.Config.PRODUCTION_APP_KEY, "prod_key")
        config.put(AirshipConstants.Config.PRODUCTION_APP_SECRET, "prod_secret")
        config.put(AirshipConstants.Config.DEVELOPMENT_APP_KEY, "dev_key")
        config.put(AirshipConstants.Config.DEVELOPMENT_APP_SECRET, "dev_secret")
        config.put(AirshipConstants.Config.PRODUCTION_LOG_LEVEL, "warn")
        config.put(AirshipConstants.Config.DEVELOPMENT_LOG_LEVEL, "debug")

        config.put(AirshipConstants.Config.IS_IN_PRODUCTION, true)

        config.put(AirshipConstants.Config.SITE, "site")
        config.put(AirshipConstants.Config.IS_DATA_COLLECTION_ENABLED, true)
        config.put(AirshipConstants.Config.IS_ANALYTICS_ENABLED, true)

        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)

        assertEquals("prod_key", airshipConfig.appKey)
        assertEquals("prod_secret", airshipConfig.appSecret)
        assertTrue(airshipConfig.inProduction)
        assertTrue(airshipConfig.analyticsEnabled)
        assertEquals(PrivacyManager.Feature.ALL, airshipConfig.enabledFeatures)
        assertTrue(airshipConfig.analyticsEnabled)
    }

    @Test
    fun parseOptions_SetsCorrectDevelopmentValues() {
        config.put(AirshipConstants.Config.PRODUCTION_APP_KEY, "prod_key")
        config.put(AirshipConstants.Config.PRODUCTION_APP_SECRET, "prod_secret")
        config.put(AirshipConstants.Config.DEVELOPMENT_APP_KEY, "dev_key")
        config.put(AirshipConstants.Config.DEVELOPMENT_APP_SECRET, "dev_secret")
        config.put(AirshipConstants.Config.PRODUCTION_LOG_LEVEL, "warn")
        config.put(AirshipConstants.Config.DEVELOPMENT_LOG_LEVEL, "debug")

        config.put(AirshipConstants.Config.IS_IN_PRODUCTION, false)

        config.put(AirshipConstants.Config.SITE, "site")
        config.put(AirshipConstants.Config.IS_DATA_COLLECTION_ENABLED, true)
        config.put(AirshipConstants.Config.IS_ANALYTICS_ENABLED, true)

        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)

        assertEquals("dev_key", airshipConfig.appKey)
        assertEquals("dev_secret", airshipConfig.appSecret)
        assertFalse(airshipConfig.inProduction)
        assertTrue(airshipConfig.analyticsEnabled)
        assertEquals(PrivacyManager.Feature.ALL, airshipConfig.enabledFeatures)
        assertTrue(airshipConfig.analyticsEnabled)
    }

    @Test
    fun parseLogLevel_ReturnsCorrectLogLevel() {
        var level = airshipInstance.parseLogLevel("trace")
        assertEquals(Log.VERBOSE, level)
        level = airshipInstance.parseLogLevel("debug")
        assertEquals(Log.DEBUG, level)
        level = airshipInstance.parseLogLevel("info")
        assertEquals(Log.INFO, level)
        level = airshipInstance.parseLogLevel("warn")
        assertEquals(Log.WARN, level)
        level = airshipInstance.parseLogLevel("error")
        assertEquals(Log.ERROR, level)
        level = airshipInstance.parseLogLevel("none")
        assertEquals(Int.MAX_VALUE, level)

        // Test case-insensitive parsing
        level = airshipInstance.parseLogLevel("DEbug")
        assertEquals(Log.DEBUG, level)

        // Test invalid input returns null
        level = airshipInstance.parseLogLevel("invalid")
        assertNull(level)
        level = airshipInstance.parseLogLevel("")
        assertNull(level)
        level = airshipInstance.parseLogLevel("unknown")
        assertNull(level)
    }

    @Test
    fun channelTags_SetsNewSetOfTags() {
        val mockChannel: AirshipChannel = mockk(relaxed = true)
        every { mockUAirship.channel } returns mockChannel
        every { mockChannel.tags } returns setOf("tag1", "tag2")

        airshipInstance.channelTags = JSONArray().apply {
            put("tag3")
            put("tag4")
        }

        verify {
            mockChannel.tags = match {
                it.contains("tag3")
                        && it.contains("tag4")
                        && !it.contains("tag1")
                        && !it.contains("tag2")
            }
        }
    }

    @Test
    fun channelTags_GetsExistingSetOfTags() {
        val mockChannel: AirshipChannel = mockk(relaxed = true)
        every { mockUAirship.channel } returns mockChannel
        every { mockChannel.tags } returns setOf("tag1", "tag2")

        val tags = airshipInstance.channelTags

        assertEquals("tag1", tags.getString(0))
        assertEquals("tag2", tags.getString(1))
    }

    @Test
    fun addTag_AddsToExistingTags() {
        val mockChannel: AirshipChannel = mockk(relaxed = true)
        every { mockUAirship.channel } returns mockChannel
        every { mockChannel.tags } returns setOf("tag1", "tag2")

        airshipInstance.addTag("tag3")

        verify {
            mockChannel.tags = match {
                it.contains("tag1")
                        && it.contains("tag2")
                        && it.contains("tag3")

            }
        }
    }

    @Test
    fun removeTag_RemovesFromExistingTags() {
        val mockChannel: AirshipChannel = mockk(relaxed = true)
        every { mockUAirship.channel } returns mockChannel
        every { mockChannel.tags } returns setOf("tag1", "tag2")

        airshipInstance.removeTag("tag1")

        verify {
            mockChannel.tags = match {
                !it.contains("tag1")
                        && it.contains("tag2")
            }
        }
    }

    @Test
    fun addTagGroup_AddsUserTagGroups() {
        val mockContact = mockk<Contact>(relaxed = true)
        every { mockUAirship.contact } returns mockContact
        val mockEditor: TagGroupsEditor = mockk(relaxed = true)
        every { mockContact.editTagGroups() } returns mockEditor

        airshipInstance.addTagGroup("group", JSONArray().apply {
            put("tag1")
            put("tag2")
        }, AirshipConstants.TagType.NAMED_USER_TAG)

        verify {
            mockEditor.addTag("group", "tag1")
            mockEditor.addTag("group", "tag2")
        }
    }

    @Test
    fun addTagGroup_AddsChannelTagGroups() {
        val mockChannel: AirshipChannel = mockk(relaxed = true)
        every { mockUAirship.channel } returns mockChannel
        val mockEditor: TagGroupsEditor = mockk(relaxed = true)
        every { mockChannel.editTagGroups() } returns mockEditor

        airshipInstance.addTagGroup("group", JSONArray().apply {
            put("tag1")
            put("tag2")
        }, AirshipConstants.TagType.CHANNEL_TAG)

        verify {
            mockEditor.addTag("group", "tag1")
            mockEditor.addTag("group", "tag2")
        }
    }

    @Test
    fun removeTagGroup_RemovesUserTagGroups() {
        val mockContact = mockk<Contact>(relaxed = true)
        every { mockUAirship.contact } returns mockContact
        val mockEditor: TagGroupsEditor = mockk(relaxed = true)
        every { mockContact.editTagGroups() } returns mockEditor

        airshipInstance.removeTagGroup("group", JSONArray().apply {
            put("tag1")
            put("tag2")
        }, AirshipConstants.TagType.NAMED_USER_TAG)

        verify {
            mockEditor.removeTag("group", "tag1")
            mockEditor.removeTag("group", "tag2")
        }
    }

    @Test
    fun removeTagGroup_RemovesChannelTagGroups() {
        val mockChannel: AirshipChannel = mockk(relaxed = true)
        every { mockUAirship.channel } returns mockChannel
        val mockEditor: TagGroupsEditor = mockk(relaxed = true)
        every { mockChannel.editTagGroups() } returns mockEditor

        airshipInstance.removeTagGroup("group", JSONArray().apply {
            put("tag1")
            put("tag2")
        }, AirshipConstants.TagType.CHANNEL_TAG)

        verify {
            mockEditor.removeTag("group", "tag1")
            mockEditor.removeTag("group", "tag2")
        }
    }

    @Test
    fun identifyUser_SetsUserId() {
        val mockContact = mockk<Contact>(relaxed = true)
        every { mockUAirship.contact } returns mockContact
        
        airshipInstance.identifyUser("user_1234")

        verify {
            mockContact.identify("user_1234")
        }
    }

    @Test
    fun enablePushNotifications_EnablesNotifications() {
        val mockPushManager: PushManager = mockk(relaxed = true)
        every { mockUAirship.pushManager } returns mockPushManager

        airshipInstance.userPushNotificationsEnabled = true

        verify {
            mockPushManager.userNotificationsEnabled = true
        }
    }

    @Test
    fun userPushNotificationsEnabled_GetsEnabled() {
        val mockPushManager: PushManager = mockk(relaxed = true)
        every { mockUAirship.pushManager } returns mockPushManager

        val enabled = airshipInstance.userPushNotificationsEnabled

        verify {
            mockPushManager.userNotificationsEnabled
        }
    }

    @Test
    fun userPushNotificationsEnabled_SetsEnabled() {
        val mockPushManager: PushManager = mockk(relaxed = true)
        every { mockUAirship.pushManager } returns mockPushManager

        airshipInstance.userPushNotificationsEnabled = true

        verify {
            mockPushManager.userNotificationsEnabled = true
        }
    }

    @Test
    fun customIdentifiers_AddsGivenIdentifiers() {
        val mockAnalytics: Analytics = mockk(relaxed = true)
        every { mockUAirship.analytics } returns mockAnalytics
        val mockEditor: AssociatedIdentifiers.Editor = mockk(relaxed = true)
        every { mockAnalytics.editAssociatedIdentifiers() } returns mockEditor

        val identifiers = JSONObject().apply {
            put("id_1", "1234")
            put("id_2", "5678")
            put("id_3", 91011)
        }

        airshipInstance.setCustomIdentifiers(identifiers)

        verify {
            mockEditor.addIdentifier("id_1", "1234")
            mockEditor.addIdentifier("id_2", "5678")
            mockEditor.addIdentifier("id_3", "91011")
            mockEditor.apply()
        }
    }

    @Test
    fun trackScreenView_SendsScreenName() {
        val mockAnalytics: Analytics = mockk(relaxed = true)
        every { mockUAirship.analytics } returns mockAnalytics

        airshipInstance.trackScreenView("screen")

        verify {
            mockAnalytics.trackScreen("screen")
        }
    }

    @Test
    fun trackEvent_SendsEventValue() {
        val mockBuilder: CustomEvent.Builder = mockk(relaxed = true)

        airshipInstance.trackEvent(mockBuilder, 100.4, null)

        verify {
            mockBuilder.setEventValue(100.4)
        }
    }

    @Test
    fun trackEvent_SendsEventProperties() {
        val mockBuilder: CustomEvent.Builder = mockk(relaxed = true)

        val jsonObject = JSONObject().apply {
            put("string", "value")
            put("int", 10)
            put("double", 10.5)
            put("long", Long.MAX_VALUE)
            put("boolean", false)
        }
        airshipInstance.trackEvent(mockBuilder, null, jsonObject)

        verify {
            mockBuilder.setProperties(match {
                it.opt("string") == JsonValue.wrap("value")
                        && it.opt("int") == JsonValue.wrap(10)
                        && it.opt("double") == JsonValue.wrap(10.5)
                        && it.opt("long") == JsonValue.wrap(Long.MAX_VALUE)
                        && it.opt("boolean") == JsonValue.wrap(false)
            })
        }
    }

    @Test
    fun analyticsEnabled_GetsEnabled() {
        val mockAnalytics: Analytics = mockk(relaxed = true)
        every { mockUAirship.analytics } returns mockAnalytics

        val enabled = airshipInstance.analyticsEnabled

        verify {
            mockAnalytics.isEnabled
        }
    }

    @Test
    fun analyticsEnabled_SetsEnabled() {
        airshipInstance.analyticsEnabled = true

        verify {
            mockPrivacyManager.enable(PrivacyManager.Feature.ANALYTICS)
        }
    }

    @Test
    fun inAppMessagingEnabled_GetsEnabled() {
        val enabled = airshipInstance.inAppMessagingEnabled

        verify {
            mockPrivacyManager.isEnabled(PrivacyManager.Feature.IN_APP_AUTOMATION)
        }
    }

    @Test
    fun inAppMessagingEnabled_SetsEnabled() {
        airshipInstance.inAppMessagingEnabled = true

        verify {
            mockPrivacyManager.enable(PrivacyManager.Feature.IN_APP_AUTOMATION)
        }
    }

    @Test
    fun inAppMessagingPaused_GetsPaused() {
        val mockInAppAutomation: InAppAutomation = mockk(relaxed = true)
        mockkStatic(InAppAutomation::class)
        every { InAppAutomation.shared() } returns mockInAppAutomation

        val enabled = airshipInstance.inAppMessagingPaused

        verify {
            mockInAppAutomation.isPaused
        }
    }

    @Test
    fun inAppMessagingPaused_SetsPaused() {
        val mockInAppAutomation: InAppAutomation = mockk(relaxed = true)
        mockkStatic(InAppAutomation::class)
        every { InAppAutomation.shared() } returns mockInAppAutomation

        airshipInstance.inAppMessagingPaused = true

        verify {
            mockInAppAutomation.isPaused = true
        }
    }

    @Test
    fun displayMessageCenter_ShowsMessageCenter() {
        val mockMessageCenter: MessageCenter = mockk(relaxed = true)
        mockkStatic(MessageCenter::class)
        every { MessageCenter.shared() } returns mockMessageCenter

        airshipInstance.displayMessageCenter(null)
        airshipInstance.displayMessageCenter("message")

        verify {
            mockMessageCenter.showMessageCenter(null)
            mockMessageCenter.showMessageCenter("message")
        }
    }

    @Test
    fun setAttributes_SetsGivenAttributes() {
        val mockChannel: AirshipChannel = mockk(relaxed = true)
        every { mockUAirship.channel } returns mockChannel
        val mockEditor: AttributeEditor = mockk(relaxed = true)
        every { mockChannel.editAttributes() } returns mockEditor

        val firstOfJan2021 = Date(1609459200000)
        val date = SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", Locale.ROOT).format(firstOfJan2021)

        val attributes = JSONObject().apply {
            put("date_key", date)
            put("string_key", "string_value")
            put("int_key", 100)
            put("double_key", 100.50)
            put("float_key", 100.50f)
            put("long_key", Long.MAX_VALUE)
        }
        airshipInstance.setAttributes(attributes)

        verify {
            mockEditor.setAttribute("date_key", firstOfJan2021)
            mockEditor.setAttribute("string_key", "string_value")
            mockEditor.setAttribute("int_key", 100)
            mockEditor.setAttribute("double_key", 100.50)
            mockEditor.setAttribute("float_key", 100.50f)
            mockEditor.setAttribute("long_key", Long.MAX_VALUE)
            mockEditor.apply()
        }
    }

    @Test
    fun parseOptions_SetsChannelCreationDelayEnabled() {
        config.put(AirshipConstants.Config.CHANNEL_CREATION_DELAY_ENABLED, true)
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertTrue(airshipConfig.channelCreationDelayEnabled)
    }

    @Test
    fun parseOptions_SetsAllowedTransports() {
        val transports = JSONArray().apply {
            put("FCM")
            put("ADM")
            put("HMS")
        }
        config.put(AirshipConstants.Config.ALLOWED_TRANSPORTS, transports)
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertEquals(listOf("FCM", "ADM", "HMS"), airshipConfig.allowedTransports)
    }

    @Test
    fun parseOptions_SetsAppStoreUri() {
        config.put(AirshipConstants.Config.APP_STORE_URI, "market://details?id=com.example.app")
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertEquals("market://details?id=com.example.app", airshipConfig.appStoreUri.toString())
    }

    @Test
    fun parseOptions_SetsAutoPauseInAppAutomationOnLaunch() {
        config.put(AirshipConstants.Config.AUTO_PAUSE_IN_APP_AUTOMATION_ON_LAUNCH, true)
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertTrue(airshipConfig.autoPauseInAppAutomationOnLaunch)
    }

    @Test
    fun parseOptions_SetsBackgroundReportingIntervalMS() {
        config.put(AirshipConstants.Config.BACKGROUND_REPORTING_INTERVAL_MS, 60000L)
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertEquals(60000L, airshipConfig.backgroundReportingIntervalMS)
    }

    @Test
    fun parseOptions_SetsChannelCaptureEnabled() {
        config.put(AirshipConstants.Config.CHANNEL_CAPTURE_ENABLED, true)
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertTrue(airshipConfig.channelCaptureEnabled)
    }

    @Test
    fun parseOptions_SetsExtendedBroadcastsEnabled() {
        config.put(AirshipConstants.Config.EXTENDED_BROADCASTS_ENABLED, true)
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertTrue(airshipConfig.extendedBroadcastsEnabled)
    }

    @Test
    fun parseOptions_SetsFcmFirebaseAppName() {
        config.put(AirshipConstants.Config.FCM_FIREBASE_APP_NAME, "custom-firebase-app")
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertEquals("custom-firebase-app", airshipConfig.fcmFirebaseAppName)
    }

    @Test
    fun parseOptions_SetsInitialConfigUrl() {
        config.put(AirshipConstants.Config.INITIAL_CONFIG_URL, "https://custom-domain.com/config")
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertEquals("https://custom-domain.com/config", airshipConfig.initialConfigUrl)
    }

    @Test
    fun parseOptions_SetsRequireInitialRemoteConfigEnabled() {
        config.put(AirshipConstants.Config.REQUIRE_INITIAL_REMOTE_CONFIG_ENABLED, true)
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertTrue(airshipConfig.requireInitialRemoteConfigEnabled)
    }

    @Test
    fun parseOptions_SetsLogLevel() {
        config.put(AirshipConstants.Config.LOG_LEVEL, "info")
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertEquals(Log.INFO, airshipConfig.logLevel)
    }

    @Test
    fun parseOptions_SetsUrlAllowLists() {
        val urlAllowList = JSONArray().apply {
            put("https://example.com/*")
            put("https://trusted.com/*")
        }
        val jsInterfaceList = JSONArray().apply {
            put("https://api.company.com/*")
        }
        val openUrlList = JSONArray().apply {
            put("https://support.company.com/*")
        }
        
        config.put(AirshipConstants.Config.URL_ALLOW_LIST, urlAllowList)
        config.put(AirshipConstants.Config.URL_ALLOW_LIST_SCOPE_JAVASCRIPT_INTERFACE, jsInterfaceList)
        config.put(AirshipConstants.Config.URL_ALLOW_LIST_SCOPE_OPEN_URL, openUrlList)
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        assertEquals(listOf("https://example.com/*", "https://trusted.com/*"), airshipConfig.urlAllowList)
        assertEquals(listOf("https://api.company.com/*"), airshipConfig.urlAllowListScopeJavaScriptInterface)
        assertEquals(listOf("https://support.company.com/*"), airshipConfig.urlAllowListScopeOpenUrl)
    }

    @Test
    fun parseOptions_SetsEnabledFeatures() {
        val features = JSONArray().apply {
            put("ANALYTICS")
            put("PUSH")
            put("IN_APP_AUTOMATION")
        }
        config.put(AirshipConstants.Config.ENABLED_FEATURES, features)
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        // Verify that the enabled features were set correctly
        val enabledFeatures = airshipConfig.enabledFeatures
        assertEquals(3, enabledFeatures.size)
        assertTrue(enabledFeatures.contains(PrivacyManager.Feature.ANALYTICS))
        assertTrue(enabledFeatures.contains(PrivacyManager.Feature.PUSH))
        assertTrue(enabledFeatures.contains(PrivacyManager.Feature.IN_APP_AUTOMATION))
    }

    @Test 
    fun parseOptions_HandlesMissingLogLevelGracefully() {
        config.put(AirshipConstants.Config.LOG_LEVEL, "invalid_level")
        
        val airshipConfig: AirshipConfigOptions = airshipInstance.parseOptions(config)
        
        // Should default to ERROR when invalid level is provided
        assertEquals(Log.ERROR, airshipConfig.logLevel)
    }

    @Test
    fun privacyFeatures_fromJsonArray_HandlesALL() {
        val featuresArray = JSONArray().apply {
            put("ALL")
        }
        
        val result = AirshipConstants.PrivacyFeatures.fromJsonArray(featuresArray)
        
        assertEquals(1, result.size)
        assertEquals(PrivacyManager.Feature.ALL, result[0])
    }

    @Test
    fun privacyFeatures_fromJsonArray_HandlesNONE() {
        val featuresArray = JSONArray().apply {
            put("NONE")
        }
        
        val result = AirshipConstants.PrivacyFeatures.fromJsonArray(featuresArray)
        
        assertEquals(1, result.size)
        assertEquals(PrivacyManager.Feature.NONE, result[0])
    }

    @Test
    fun privacyFeatures_fromJsonArray_HandlesMultipleFeatures() {
        val featuresArray = JSONArray().apply {
            put("ANALYTICS")
            put("PUSH")
            put("IN_APP_AUTOMATION")
            put("MESSAGE_CENTER")
        }
        
        val result = AirshipConstants.PrivacyFeatures.fromJsonArray(featuresArray)
        
        assertEquals(4, result.size)
        assertTrue(result.contains(PrivacyManager.Feature.ANALYTICS))
        assertTrue(result.contains(PrivacyManager.Feature.PUSH))
        assertTrue(result.contains(PrivacyManager.Feature.IN_APP_AUTOMATION))
        assertTrue(result.contains(PrivacyManager.Feature.MESSAGE_CENTER))
    }

    @Test
    fun privacyFeatures_fromJsonArray_IgnoresInvalidFeatures() {
        val featuresArray = JSONArray().apply {
            put("ANALYTICS")
            put("INVALID_FEATURE")
            put("PUSH")
            put("")
        }
        
        val result = AirshipConstants.PrivacyFeatures.fromJsonArray(featuresArray)
        
        assertEquals(2, result.size)
        assertTrue(result.contains(PrivacyManager.Feature.ANALYTICS))
        assertTrue(result.contains(PrivacyManager.Feature.PUSH))
    }

    @Test
    fun privacyFeatures_fromJsonArray_IsCaseInsensitive() {
        val featuresArray = JSONArray().apply {
            put("analytics")
            put("Push")
            put("IN_app_AUTOMATION")
        }
        
        val result = AirshipConstants.PrivacyFeatures.fromJsonArray(featuresArray)
        
        assertEquals(3, result.size)
        assertTrue(result.contains(PrivacyManager.Feature.ANALYTICS))
        assertTrue(result.contains(PrivacyManager.Feature.PUSH))
        assertTrue(result.contains(PrivacyManager.Feature.IN_APP_AUTOMATION))
    }

    @Test
    fun privacyFeatures_fromString_ReturnsCorrectFeature() {
        assertEquals(PrivacyManager.Feature.ANALYTICS, AirshipConstants.PrivacyFeatures.fromString("ANALYTICS"))
        assertEquals(PrivacyManager.Feature.PUSH, AirshipConstants.PrivacyFeatures.fromString("PUSH"))
        assertEquals(PrivacyManager.Feature.IN_APP_AUTOMATION, AirshipConstants.PrivacyFeatures.fromString("IN_APP_AUTOMATION"))
        assertEquals(PrivacyManager.Feature.MESSAGE_CENTER, AirshipConstants.PrivacyFeatures.fromString("MESSAGE_CENTER"))
        assertEquals(PrivacyManager.Feature.CONTACTS, AirshipConstants.PrivacyFeatures.fromString("CONTACTS"))
        assertEquals(PrivacyManager.Feature.FEATURE_FLAGS, AirshipConstants.PrivacyFeatures.fromString("FEATURE_FLAGS"))
        assertEquals(PrivacyManager.Feature.TAGS_AND_ATTRIBUTES, AirshipConstants.PrivacyFeatures.fromString("TAGS_AND_ATTRIBUTES"))
        assertEquals(PrivacyManager.Feature.ALL, AirshipConstants.PrivacyFeatures.fromString("ALL"))
        assertEquals(PrivacyManager.Feature.NONE, AirshipConstants.PrivacyFeatures.fromString("NONE"))
    }

    @Test
    fun privacyFeatures_fromString_ReturnsNullForInvalid() {
        assertNull(AirshipConstants.PrivacyFeatures.fromString("INVALID_FEATURE"))
        assertNull(AirshipConstants.PrivacyFeatures.fromString(""))
        assertNull(AirshipConstants.PrivacyFeatures.fromString("unknown"))
    }
}