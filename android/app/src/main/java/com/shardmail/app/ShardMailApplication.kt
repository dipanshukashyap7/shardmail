package com.shardmail.app

import android.app.Application
import com.shardmail.app.data.local.AppDatabase
import com.shardmail.app.data.local.SecurePreferences
import com.shardmail.app.data.repository.MailRepository

class ShardMailApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securePreferences: SecurePreferences
        private set

    lateinit var repository: MailRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        securePreferences = SecurePreferences(this)
        repository = MailRepository(
            messageDao = database.messageDao(),
            aliasDao = database.aliasDao(),
            securePreferences = securePreferences
        )
    }
}
