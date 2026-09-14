package com.savemoney.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE purchase_requests ADD COLUMN imagePath TEXT")
    }
}

@Database(
    entities = [PurchaseRequest::class, ExpenseRecord::class, MonthlyBudget::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun requestDao(): PurchaseRequestDao
    abstract fun expenseDao(): ExpenseRecordDao
    abstract fun budgetDao(): MonthlyBudgetDao
    abstract fun reviewDao(): ReviewDao
}
