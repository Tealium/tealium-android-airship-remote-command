package com.tealium.airship

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class UserFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_back).setOnClickListener {
            findNavController().navigate(R.id.action_userFragment_to_FirstFragment)
        }

        view.findViewById<Button>(R.id.button_login).setOnClickListener {
            view.findViewById<EditText>(R.id.editTextTextEmailAddress)?.text?.let {
                if (it.isNotEmpty()) {
                    TealiumHelper.trackView("login", mapOf(
                        "email" to it
                    ))
                }
            }
        }

        view.findViewById<Button>(R.id.button_add_user_tag).setOnClickListener {
            val grp = group
            userTags?.let { tags ->
                if (grp.isNotEmpty()) {
                    TealiumHelper.trackEvent("add_tag_group", mapOf(
                        "user_tags" to tags,
                        "group" to grp
                    ))
                }
            }
        }

        view.findViewById<Button>(R.id.button_add_channel_tag).setOnClickListener {
            val grp = group
            channelTags?.let { tags ->
                if (grp.isNotEmpty()) {
                    TealiumHelper.trackEvent("add_tag_group", mapOf(
                        "channel_tags" to tags,
                        "group" to grp
                    ))
                } else {
                    for (tag in tags) {
                        TealiumHelper.trackEvent("add_tag", mapOf(
                            "channel_tag" to tag
                        ))
                    }
                }
            }
        }

        view.findViewById<Button>(R.id.button_remove_user_tag).setOnClickListener {
            val grp = group
            userTags?.let { tags ->
                if (grp.isNotEmpty()) {

                    TealiumHelper.trackEvent("remove_tag_group", mapOf(
                        "user_tags" to tags,
                        "group" to grp
                    ))
                }
            }
        }

        view.findViewById<Button>(R.id.button_remove_channel_tag).setOnClickListener {
            val grp = group
            channelTags?.let { tags ->
                if (grp.isNotEmpty()) {
                    TealiumHelper.trackEvent("remove_tag_group", mapOf(
                        "channel_tags" to tags,
                        "group" to grp
                    ))
                } else {
                    for (tag in tags) {
                        TealiumHelper.trackEvent("remove_tag", mapOf(
                            "channel_tag" to tag
                        ))
                    }
                }
            }
        }
    }

    private val group: String
        get() = view?.findViewById<EditText>(R.id.editTextText_group)?.text.toString()
    private val channelTags : List<String>?
        get() = view?.findViewById<EditText>(R.id.editText_channel_tag)?.text?.split(";")
    private val userTags : List<String>?
        get() = view?.findViewById<EditText>(R.id.editText_user_tag)?.text?.split(";")

}