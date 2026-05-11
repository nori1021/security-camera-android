package com.securitycamera.app

import android.app.Application
import com.securitycamera.app.data.PrefsRepository

class SecurityCameraApplication : Application() {
    lateinit var prefsRepository: PrefsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        prefsRepository = PrefsRepository(applicationContext)
    }
}
