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
import com.urbanairship.channel.NamedUser
import com.urbanairship.channel.TagGroupsEditor
import com.urbanairship.iam.InAppMessageManager
import com.urbanairship.json.JsonValue
import com.urbanairship.location.AirshipLocationManager
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
@Config(sdk = [21, 28])
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
        assertEquals(PrivacyManager.FEATURE_ALL, airshipConfig.enabledFeatures)
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
        assertEquals(PrivacyManager.FEATURE_ALL, airshipConfig.enabledFeatures)
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
        level = airshipInstance.parseLogLevel("invalid")
        assertEquals(Log.ERROR, level)
        level = airshipInstance.parseLogLevel("none")
        assertEquals(Int.MAX_VALUE, level)

        level = airshipInstance.parseLogLevel("DEbug")
        assertEquals(Log.DEBUG, level)
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
        val mockNamedUser: NamedUser = mockk(relaxed = true)
        every { mockUAirship.namedUser } returns mockNamedUser
        val mockEditor: TagGroupsEditor = mockk(relaxed = true)
        every { mockNamedUser.editTagGroups() } returns mockEditor

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
        val mockNamedUser: NamedUser = mockk(relaxed = true)
        every { mockUAirship.namedUser } returns mockNamedUser
        val mockEditor: TagGroupsEditor = mockk(relaxed = true)
        every { mockNamedUser.editTagGroups() } returns mockEditor

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
        val mockNamedUser: NamedUser = mockk(relaxed = true)
        every { mockUAirship.namedUser } returns mockNamedUser
        airshipInstance.identifyUser("user_1234")

        verify {
            mockNamedUser.id = "user_1234"
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
        // TODO("options possibly incomplete")
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
            mockPrivacyManager.enable(PrivacyManager.FEATURE_ANALYTICS)
        }
    }

    @Test
    fun locationEnabled_GetsEnabled() {
        val mockAirshipLocationManager: AirshipLocationManager = mockk(relaxed = true)
        mockkStatic(AirshipLocationManager::class)
        every { AirshipLocationManager.shared() } returns mockAirshipLocationManager

        val enabled = airshipInstance.locationEnabled

        verify {
            mockAirshipLocationManager.isLocationUpdatesEnabled
        }
    }

    @Test
    fun locationEnabled_SetsEnabled() {
        val mockAirshipLocationManager: AirshipLocationManager = mockk(relaxed = true)
        mockkStatic(AirshipLocationManager::class)
        every { AirshipLocationManager.shared() } returns mockAirshipLocationManager

        airshipInstance.locationEnabled = true

        verify {
            mockAirshipLocationManager.isLocationUpdatesEnabled = true
        }
    }

    @Test
    fun backgroundLocationEnabled_GetsEnabled() {
        val mockAirshipLocationManager: AirshipLocationManager = mockk(relaxed = true)
        mockkStatic(AirshipLocationManager::class)
        every { AirshipLocationManager.shared() } returns mockAirshipLocationManager

        val enabled = airshipInstance.backgroundLocationEnabled

        verify {
            mockAirshipLocationManager.isBackgroundLocationAllowed
        }
    }

    @Test
    fun backgroundLocationEnabled_SetsEnabled() {
        val mockAirshipLocationManager: AirshipLocationManager = mockk(relaxed = true)
        mockkStatic(AirshipLocationManager::class)
        every { AirshipLocationManager.shared() } returns mockAirshipLocationManager

        airshipInstance.backgroundLocationEnabled = true

        verify {
            mockAirshipLocationManager.isBackgroundLocationAllowed = true
        }
    }

    @Test
    fun dataCollectionEnabled_GetsEnabled() {
        val enabled = airshipInstance.dataCollectionEnabled

        verify {
            mockUAirship.isDataCollectionEnabled
        }
    }

    @Test
    fun dataCollectionEnabled_SetsEnabled() {
        airshipInstance.dataCollectionEnabled = true

        verify {
            mockUAirship.isDataCollectionEnabled = true
        }
    }

    @Test
    fun inAppMessagingEnabled_GetsEnabled() {
        val enabled = airshipInstance.inAppMessagingEnabled

        verify {
            mockPrivacyManager.isEnabled(PrivacyManager.FEATURE_IN_APP_AUTOMATION)
        }
    }

    @Test
    fun inAppMessagingEnabled_SetsEnabled() {
        airshipInstance.inAppMessagingEnabled = true

        verify {
            mockPrivacyManager.enable(PrivacyManager.FEATURE_IN_APP_AUTOMATION)
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
    fun inAppMessagingDisplayInterval_GetsInterval() {
        val mockInAppAutomation: InAppAutomation = mockk(relaxed = true)
        val mockInAppMessageManager: InAppMessageManager = mockk(relaxed = true)
        mockkStatic(InAppAutomation::class)
        every { InAppAutomation.shared() } returns mockInAppAutomation
        every { mockInAppAutomation.inAppMessageManager } returns mockInAppMessageManager

        val interval = airshipInstance.inAppMessagingDisplayInterval

        verify {
            mockInAppMessageManager.displayInterval
        }
    }

    @Test
    fun inAppMessagingDisplayInterval_SetsInterval() {
        val mockInAppAutomation: InAppAutomation = mockk(relaxed = true)
        val mockInAppMessageManager: InAppMessageManager = mockk(relaxed = true)
        mockkStatic(InAppAutomation::class)
        every { InAppAutomation.shared() } returns mockInAppAutomation
        every { mockInAppAutomation.inAppMessageManager } returns mockInAppMessageManager

        airshipInstance.inAppMessagingDisplayInterval = 100

        verify {
            mockInAppMessageManager.setDisplayInterval(100, TimeUnit.SECONDS)
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
}