package com.savemoney.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseRequestDao {
    @Insert
    suspend fun insert(request: PurchaseRequest): Long

    @Update
    suspend fun update(request: PurchaseRequest)

    @Query("SELECT * FROM purchase_requests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PurchaseRequest>>

    @Query("SELECT * FROM purchase_requests WHERE id = :id")
    fun observeById(id: Long): Flow<PurchaseRequest?>

    @Query("SELECT * FROM purchase_requests WHERE id = :id")
    suspend fun getById(id: Long): PurchaseRequest?

    @Query("DELETE FROM purchase_requests WHERE id = :id AND status = 'PENDING'")
    suspend fun deletePending(id: Long)
}

@Dao
interface ExpenseRecordDao {
    @Insert
    suspend fun insert(record: ExpenseRecord): Long

    @Query("SELECT * FROM expense_records WHERE spentAt >= :startInclusive AND spentAt < :endExclusive ORDER BY spentAt DESC")
    fun observeBetween(startInclusive: Long, endExclusive: Long): Flow<List<ExpenseRecord>>
}

@Dao
interface MonthlyBudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: MonthlyBudget)

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    fun observe(yearMonth: String): Flow<MonthlyBudget?>
}

/**
 * 审核操作需要同时更新申请状态并写入消费记录，放在一个事务中保证一致性。
 */
@Dao
abstract class ReviewDao {
    @Query("SELECT * FROM purchase_requests WHERE id = :id")
    protected abstract suspend fun requestById(id: Long): PurchaseRequest?

    @Update
    protected abstract suspend fun updateRequest(request: PurchaseRequest)

    @Insert
    protected abstract suspend fun insertExpense(record: ExpenseRecord)

    @Transaction
    open suspend fun review(
        requestId: Long,
        approve: Boolean,
        reviewerName: String,
        comment: String,
        now: Long,
    ): Boolean {
        val request = requestById(requestId) ?: return false
        if (request.status != RequestStatus.PENDING) return false
        updateRequest(
            request.copy(
                status = if (approve) RequestStatus.APPROVED else RequestStatus.REJECTED,
                reviewedAt = now,
                reviewerName = reviewerName,
                reviewComment = comment.ifBlank { null },
            )
        )
        if (approve) {
            insertExpense(
                ExpenseRecord(
                    requestId = request.id,
                    itemName = request.itemName,
                    category = request.category,
                    amountCents = request.totalCents,
                    spentAt = now,
                    requesterName = request.requesterName,
                    reviewerName = reviewerName,
                )
            )
        }
        return true
    }
}
