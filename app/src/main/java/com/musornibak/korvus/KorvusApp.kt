package com.musornibak.korvus

import android.app.Application

class KorvusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: KorvusApp
            private set
    }
}
