package com.appmaze

import android.app.Application
import com.appmaze.data.AppDatabase
import com.appmaze.data.SettingsRepository

class ShuffleApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ShuffleApplication
            private set
    }
}
