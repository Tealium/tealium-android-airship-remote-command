package com.tealium.airship

import android.R
import android.app.Application


class App : Application() {

    override fun onCreate() {
        super.onCreate()

        TealiumHelper.init(this)
    }
}