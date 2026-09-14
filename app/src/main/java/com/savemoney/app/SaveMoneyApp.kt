package com.savemoney.app

import android.app.Application
import androidx.room.Room
import com.savemoney.app.data.AppDatabase
import com.savemoney.app.data.MIGRATION_1_2

class SaveMoneyApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "save_money.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
