package com.curio.notes

import android.app.Application
import com.curio.notes.di.AppContainer

class CurioApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
