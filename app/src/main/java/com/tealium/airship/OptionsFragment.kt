package com.tealium.airship

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.navigation.fragment.findNavController
import com.urbanairship.PrivacyManager
import com.urbanairship.UAirship

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class OptionsFragment : Fragment(), RadioGroup.OnCheckedChangeListener {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        setupRadioButtons()

        listOf(
            R.id.radioGroup_analytics,
            R.id.radioGroup_messaging,
            R.id.radioGroup_push
        ).forEach {
            view.findViewById<RadioGroup>(it).setOnCheckedChangeListener(this);
        }

    }

    fun setupRadioButtons() {
        val privacyManager = UAirship.shared().privacyManager
        val analyticsEnabled = privacyManager.isEnabled(PrivacyManager.Feature.ANALYTICS)
        val messagingEnabled = privacyManager.isEnabled(PrivacyManager.Feature.MESSAGE_CENTER)
        val pushEnabled = privacyManager.isEnabled(PrivacyManager.Feature.PUSH)

        if (analyticsEnabled) {
            check(R.id.analytics_enabled)
        } else {
            check(R.id.analytics_disabled)
        }
        if (messagingEnabled) {
            check(R.id.messaging_enabled)
        } else {
            check(R.id.messaging_disabled)
        }
        if (pushEnabled) {
            check(R.id.push_enabled)
        } else {
            check(R.id.push_disabled)
        }
    }

    private fun check(id: Int) {
        view?.findViewById<RadioButton>(id)?.let {
            it.isChecked = true
        }
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        view?.findViewById<RadioButton>(checkedId)?.let {
            onChecked(it)?.apply {
                TealiumHelper.trackEvent(first, second)
            }
        }
    }

    private fun onChecked(v: View?): Pair<String, Map<String, Any>?>? {
        (v as? RadioButton)?.let {
            return when (it.id) {
                R.id.analytics_enabled -> Pair("enable_analytics", null)
                R.id.analytics_disabled -> Pair("disable_analytics", null)
                R.id.messaging_enabled -> Pair("enable_messaging", null)
                R.id.messaging_disabled -> Pair("disable_messaging", null)
                R.id.push_enabled -> Pair("enable_push", mapOf(
                    "channel_id" to "Default",
                    "push_opts" to listOf("vibrate", "badge")
                ))
                R.id.push_disabled -> Pair("disable_push", null)
                else -> null
            }
        }
        return null
    }
}