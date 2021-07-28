package com.tealium.remotecommands.airship

import android.app.Application
import com.tealium.remotecommands.RemoteCommand
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 28])
class AirshipRemoteCommandTests {

    @RelaxedMockK
    lateinit var mockApplication: Application

    @RelaxedMockK
    lateinit var mockCommand: AirshipCommand

    @RelaxedMockK
    lateinit var mockResponse: RemoteCommand.Response

    lateinit var airshipRemoteCommand: AirshipRemoteCommand
    lateinit var payload: JSONObject

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        payload = JSONObject()
        every { mockResponse.requestPayload } returns payload

        airshipRemoteCommand = AirshipRemoteCommand(mockApplication, airshipCommand = mockCommand)
    }

    @Test
    fun onInvoke_Initialize_Calls_Initialize() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.INITIALIZE)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.initialize(mockApplication, payload)
        }
    }

    @Test
    fun onInvoke_TrackEvent_DoesNotCall_TrackEvent_WithoutEventName() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.TRACK_EVENT)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.trackEvent("my_event", null, null)
        }
    }

    @Test
    fun onInvoke_TrackEvent_Calls_TrackEvent() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.TRACK_EVENT)
        payload.put(AirshipConstants.Keys.EVENT_NAME, "my_event")

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.trackEvent("my_event", null, null)
        }
    }

    @Test
    fun onInvoke_TrackEvent_Calls_TrackEvent_WithValue_AndParams() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.TRACK_EVENT)
        payload.put(AirshipConstants.Keys.EVENT_NAME, "my_event")
        payload.put(AirshipConstants.Keys.EVENT_VALUE, 10.5)
        val eventProperties = JSONObject()
        payload.put(AirshipConstants.Keys.EVENT_PROPERTIES, eventProperties)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.trackEvent("my_event", 10.5, eventProperties)
        }
    }

    @Test
    fun onInvoke_TrackScreenView_Calls_TrackScreenView() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.TRACK_SCREEN_VIEW)
        payload.put(AirshipConstants.Keys.SCREEN_NAME, "my_screen")

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.trackScreenView("my_screen")
        }
    }

    @Test
    fun onInvoke_TrackScreenView_DoesNotCall_TrackScreenView_WithoutScreenName() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.TRACK_SCREEN_VIEW)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.trackScreenView("my_screen")
        }
    }

    @Test
    fun onInvoke_SetNamedUser_Calls_SetNamedUser() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_NAMED_USER)
        payload.put(AirshipConstants.Keys.NAMED_USER_IDENTIFIER, "my_user")

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.identifyUser("my_user")
        }
    }

    @Test
    fun onInvoke_SetNamedUser_DoesNotCall_SetNamedUser_WithoutUserName() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_NAMED_USER)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.identifyUser("my_user")
        }
    }

    @Test
    fun onInvoke_SetCustomIdentifiers_Calls_SetCustomIdentifiers() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_CUSTOM_IDENTIFIERS)
        val identifiers = JSONObject("{ \"id_1\":\"12345\"}")
        payload.put(AirshipConstants.Keys.CUSTOM_IDENTIFIERS, identifiers)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.setCustomIdentifiers(identifiers)
        }
    }

    @Test
    fun onInvoke_SetCustomIdentifiers_DoesNotCall_SetNamedUser_WithoutCustomIdentifiers() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_CUSTOM_IDENTIFIERS)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.setCustomIdentifiers(any())
        }
    }

    @Test
    fun onInvoke_SetInAppMessagingDisplayInterval_Calls_SetInAppMessagingDisplayInterval() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_IN_APP_MESSAGING_DISPLAY_INTERVAL)
        payload.put(AirshipConstants.Keys.IN_APP_MESSAGING_DISPLAY_INTERVAL, 100)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.inAppMessagingDisplayInterval = 100
        }
    }

    @Test
    fun onInvoke_SetInAppMessagingDisplayInterval_DoesNotCall_SetInAppMessagingDisplayInterval_WithoutInterval() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_IN_APP_MESSAGING_DISPLAY_INTERVAL)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.inAppMessagingDisplayInterval = any()
        }
    }

    @Test
    fun onInvoke_SetPushNotificationOptions_Calls_SetPushNotificationOptions() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_PUSH_NOTIFICATION_OPTIONS)
        val options = JSONArray(arrayOf("opt1", "opt2"))
        payload.put(AirshipConstants.Keys.PUSH_NOTIFICATION_OPTIONS, options)
        payload.put(AirshipConstants.Keys.CHANNEL_ID, "channel_id")

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.setPushNotificationOptions(options,"channel_id")
        }
    }

    @Test
    fun onInvoke_SetPushNotificationOptions_DoesNotCall_SetPushNotificationOptions_WithoutOptions() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_PUSH_NOTIFICATION_OPTIONS)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.setPushNotificationOptions(any(), null)
        }
    }

    @Test
    fun onInvoke_SetQuietTime_Calls_SetQuiteTime() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_QUIET_TIME_START)
        val quiet = JSONObject()
        quiet.put(AirshipConstants.Keys.START_HOUR, 1)
        quiet.put(AirshipConstants.Keys.START_MINUTE, 2)
        quiet.put(AirshipConstants.Keys.END_HOUR, 3)
        quiet.put(AirshipConstants.Keys.END_MINUTE, 4)
        payload.put(AirshipConstants.Keys.QUIET, quiet)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.setQuietTimeStartHour(1, 2, 3, 4)
        }
    }

    @Test
    fun onInvoke_SetQuietTime_DoesNotCall_SetQuiteTime_WithoutAllValues() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_QUIET_TIME_START)
        val quiet = JSONObject()
        payload.put(AirshipConstants.Keys.QUIET, quiet)
        airshipRemoteCommand.onInvoke(mockResponse)

        quiet.put(AirshipConstants.Keys.START_MINUTE, 1)
        airshipRemoteCommand.onInvoke(mockResponse)

        quiet.put(AirshipConstants.Keys.START_HOUR, 2)
        airshipRemoteCommand.onInvoke(mockResponse)

        quiet.put(AirshipConstants.Keys.END_MINUTE, 3)
        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.setQuietTimeStartHour(1, 2, 3, 4)
        }
    }

    @Test
    fun onInvoke_SetChannelTags_Calls_SetChannelTags() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_CHANNEL_TAGS)
        val tags = JSONArray(arrayOf("opt1", "opt2"))
        payload.put(AirshipConstants.Keys.CHANNEL_TAGS, tags)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.channelTags = tags
        }
    }

    @Test
    fun onInvoke_SetChannelTags_DoesNotCall_SetChannelTags_WithoutTags() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_CHANNEL_TAGS)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.channelTags = any()
        }
    }

    @Test
    fun onInvoke_SetNamedUserTags_Calls_SetNamedUserTags() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_NAMED_USER_TAGS)
        payload.put(AirshipConstants.Keys.TAG_GROUP, "group")
        val tags = JSONArray(arrayOf("opt1", "opt2"))
        payload.put(AirshipConstants.Keys.NAMED_USER_TAGS, tags)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.setNamedUserTags("group", tags)
        }
    }

    @Test
    fun onInvoke_SetNamedUserTags_DoesNotCall_SetNamedUserTags_WithoutTags() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_NAMED_USER_TAGS)

        airshipRemoteCommand.onInvoke(mockResponse)
        payload.put(AirshipConstants.Keys.TAG_GROUP, "group")
        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.setNamedUserTags(any(), any())
        }
    }

    @Test
    fun onInvoke_AddTag_Calls_AddTag() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.ADD_TAG)
        payload.put(AirshipConstants.Keys.CHANNEL_TAG, "tag")

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.addTag("tag")
        }
    }

    @Test
    fun onInvoke_AddTag_DoesNotCall_AddTag_WithoutTag() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.ADD_TAG)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.addTag(any())
        }
    }

    @Test
    fun onInvoke_RemoveTag_Calls_RemoveTag() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.REMOVE_TAG)
        payload.put(AirshipConstants.Keys.CHANNEL_TAG, "tag")

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.removeTag("tag")
        }
    }

    @Test
    fun onInvoke_RemoveTag_DoesNotCall_RemoveTag_WithoutTag() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.REMOVE_TAG)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.removeTag(any())
        }
    }

    @Test
    fun onInvoke_AddTagGroup_Calls_AddTagGroup() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.ADD_TAG_GROUP)
        payload.put(AirshipConstants.Keys.TAG_GROUP, "group")

        val tags = JSONArray(arrayOf("tag1", "tag2"))
        payload.put(AirshipConstants.Keys.CHANNEL_TAGS, tags)
        payload.put(AirshipConstants.Keys.NAMED_USER_TAGS, tags)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.addTagGroup("group", tags, AirshipConstants.TagType.CHANNEL_TAG)
            mockCommand.addTagGroup("group", tags, AirshipConstants.TagType.NAMED_USER_TAG)
        }
    }

    @Test
    fun onInvoke_AddTagGroup_DoesNotCall_AddTagGroup_WithoutGroupOrTags() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.ADD_TAG_GROUP)
        airshipRemoteCommand.onInvoke(mockResponse)

        payload.put(AirshipConstants.Keys.TAG_GROUP, "group")
        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.addTagGroup("group", any(), AirshipConstants.TagType.CHANNEL_TAG)
            mockCommand.addTagGroup("group", any(), AirshipConstants.TagType.NAMED_USER_TAG)
        }
    }

    @Test
    fun onInvoke_RemoveTagGroup_Calls_RemoveTagGroup() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.REMOVE_TAG_GROUP)
        payload.put(AirshipConstants.Keys.TAG_GROUP, "group")

        val tags = JSONArray(arrayOf("tag1", "tag2"))
        payload.put(AirshipConstants.Keys.CHANNEL_TAGS, tags)
        payload.put(AirshipConstants.Keys.NAMED_USER_TAGS, tags)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.removeTagGroup("group", tags, AirshipConstants.TagType.CHANNEL_TAG)
            mockCommand.removeTagGroup("group", tags, AirshipConstants.TagType.NAMED_USER_TAG)
        }
    }

    @Test
    fun onInvoke_RemoveTagGroup_DoesNotCall_RemoveTagGroup_WithoutGroupOrTags() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.REMOVE_TAG_GROUP)
        airshipRemoteCommand.onInvoke(mockResponse)

        payload.put(AirshipConstants.Keys.TAG_GROUP, "group")
        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.removeTagGroup("group", any(), AirshipConstants.TagType.CHANNEL_TAG)
            mockCommand.removeTagGroup("group", any(), AirshipConstants.TagType.NAMED_USER_TAG)
        }
    }

    @Test
    fun onInvoke_SetAttributes_Calls_SetAttributes() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_ATTRIBUTES)
        val attributes = JSONObject("{\"key\":\"value\"}")
        payload.put(AirshipConstants.Keys.ATTRIBUTES, attributes)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.setAttributes(attributes)
        }
    }

    @Test
    fun onInvoke_SetAttributes_DoesNotCall_SetAttributes_WithoutAttributes() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.SET_ATTRIBUTES)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify (exactly = 0) {
            mockCommand.setAttributes(any())
        }
    }

    @Test
    fun onInvoke_Enable_Calls_EnableTrue() {
        payload.put(AirshipConstants.COMMAND_NAME, arrayOf(
            AirshipConstants.Commands.ENABLE_ANALYTICS,
            AirshipConstants.Commands.ENABLE_BACKGROUND_LOCATION,
            AirshipConstants.Commands.ENABLE_IN_APP_MESSAGING,
            AirshipConstants.Commands.PAUSE_IN_APP_MESSAGING,
            AirshipConstants.Commands.ENABLE_LOCATION,
            AirshipConstants.Commands.ENABLE_QUIET_TIME,
            ).joinToString(AirshipConstants.SEPARATOR)
        )

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.analyticsEnabled = true
            mockCommand.inAppMessagingEnabled = true
            mockCommand.inAppMessagingPaused = true
            mockCommand.quietTimeEnabled = true
            mockCommand.locationEnabled = true
        }
    }

    @Test
    fun onInvoke_Disable_Calls_EnableFalse() {
        payload.put(AirshipConstants.COMMAND_NAME, arrayOf(
            AirshipConstants.Commands.DISABLE_ANALYTICS,
            AirshipConstants.Commands.DISABLE_BACKGROUND_LOCATION,
            AirshipConstants.Commands.DISABLE_IN_APP_MESSAGING,
            AirshipConstants.Commands.UNPAUSE_IN_APP_MESSAGING,
            AirshipConstants.Commands.DISABLE_LOCATION,
            AirshipConstants.Commands.DISABLE_QUIET_TIME,
            AirshipConstants.Commands.DISABLE_USER_PUSH_NOTIFICATIONS,
        ).joinToString(AirshipConstants.SEPARATOR)
        )

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.analyticsEnabled = false
            mockCommand.inAppMessagingEnabled = false
            mockCommand.inAppMessagingPaused = false
            mockCommand.userPushNotificationsEnabled = false
            mockCommand.quietTimeEnabled = false
            mockCommand.locationEnabled = false
        }
    }

    @Test
    fun setEnableAdvertisingIds_SetsAdIdAndTrackingInfo() {
        payload.put(AirshipConstants.COMMAND_NAME, AirshipConstants.Commands.ENABLE_ADVERTISING_IDENTIFIERS)
        payload.put(AirshipConstants.Keys.GOOGLE_ADID, "my_id")
        payload.put(AirshipConstants.Keys.GOOGLE_AD_TRACKING, true)

        airshipRemoteCommand.onInvoke(mockResponse)

        verify {
            mockCommand.enableAdvertisingIDs("my_id", true)
        }
    }
}