package com.example.personalassistant

import android.app.Application
import com.example.personalassistant.database.DatabaseHelper
import com.example.personalassistant.manager.CloudSyncManager
import com.example.personalassistant.manager.NotificationManager

class PersonalAssistantApp : Application() {
    
    companion object {
        lateinit var instance: PersonalAssistantApp
            private set
    }
    
    val databaseHelper: DatabaseHelper by lazy { DatabaseHelper(this) }
    val cloudSyncManager: CloudSyncManager by lazy { CloudSyncManager(this) }
    val notificationManager: NotificationManager by lazy { NotificationManager(this) }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize managers
        cloudSyncManager.initialize()
        notificationManager.createNotificationChannels()
    }
}