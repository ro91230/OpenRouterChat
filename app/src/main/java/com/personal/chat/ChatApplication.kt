package com.personal.chat

import android.app.Application
import com.personal.chat.di.AppContainer

class ChatApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
