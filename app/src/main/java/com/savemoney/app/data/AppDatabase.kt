package com.savemoney.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PurchaseRequest::class, ExpenseRecord::class, MonthlyBudget::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun requestDao(): PurchaseRequestDao
    abstract fun expenseDao(): ExpenseRecordDao
    abstract fun budgetDao(): MonthlyBudgetDao
    abstract fun reviewDao(): ReviewDao
}
