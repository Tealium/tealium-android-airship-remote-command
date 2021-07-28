package com.tealium.airship

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_user).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_userFragment)
        }

        view.findViewById<Button>(R.id.button_options).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        view.findViewById<Button>(R.id.button_track_view).setOnClickListener {
            TealiumHelper.trackView("screen_view", mapOf())
        }

        view.findViewById<Button>(R.id.button_track_event).setOnClickListener {
            TealiumHelper.trackEvent("click", mapOf(
                "event_value" to 100.0,
                "string_prop" to "string",
                "int_prop" to 10,
                "bool_prop" to false,
                "double_prop" to 10.5
            ))
        }
    }
}