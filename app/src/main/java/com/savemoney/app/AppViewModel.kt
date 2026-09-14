package com.savemoney.app

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.savemoney.app.data.MonthlyBudget
import com.savemoney.app.data.PurchaseRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

/** 当前使用者的角色。同一台手机上可以在设置页切换。 */
enum class UserRole(val label: String) {
    REQUESTER("申请人"),
    APPROVER("审核人"),
}

data class UserProfile(
    val role: UserRole,
    val requesterName: String,
    val approverName: String,
) {
    val currentName: String
        get() = if (role == UserRole.REQUESTER) requesterName else approverName
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as SaveMoneyApp).database
    private val prefs = app.getSharedPreferences("profile", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(
        UserProfile(
            role = UserRole.valueOf(prefs.getString("role", UserRole.REQUESTER.name)!!),
            requesterName = prefs.getString("requesterName", "申请人")!!,
            approverName = prefs.getString("approverName", "审核人")!!,
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    val requests: StateFlow<List<PurchaseRequest>> = db.requestDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    val monthExpenses = _selectedMonth.flatMapLatest { month ->
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        db.expenseDao().observeBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthBudget: StateFlow<MonthlyBudget?> = _selectedMonth.flatMapLatest { month ->
        db.budgetDao().observe(month.toString())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun observeRequest(id: Long): Flow<PurchaseRequest?> = db.requestDao().observeById(id)

    fun updateProfile(role: UserRole, requesterName: String, approverName: String) {
        val next = UserProfile(
            role = role,
            requesterName = requesterName.trim().ifBlank { "申请人" },
            approverName = approverName.trim().ifBlank { "审核人" },
        )
        _profile.value = next
        prefs.edit {
            putString("role", next.role.name)
            putString("requesterName", next.requesterName)
            putString("approverName", next.approverName)
        }
    }

    fun submitRequest(
        itemName: String,
        category: String,
        unitPriceCents: Long,
        quantity: Int,
        reason: String,
    ) {
        viewModelScope.launch {
            db.requestDao().insert(
                PurchaseRequest(
                    itemName = itemName.trim(),
                    category = category,
                    unitPriceCents = unitPriceCents,
                    quantity = quantity,
                    reason = reason.trim(),
                    requesterName = _profile.value.requesterName,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun review(requestId: Long, approve: Boolean, comment: String) {
        viewModelScope.launch {
            db.reviewDao().review(
                requestId = requestId,
                approve = approve,
                reviewerName = _profile.value.approverName,
                comment = comment,
                now = System.currentTimeMillis(),
            )
        }
    }

    fun withdrawRequest(requestId: Long) {
        viewModelScope.launch { db.requestDao().deletePending(requestId) }
    }

    fun shiftMonth(delta: Long) {
        _selectedMonth.update { it.plusMonths(delta) }
    }

    fun setBudget(amountCents: Long) {
        viewModelScope.launch {
            db.budgetDao().upsert(MonthlyBudget(_selectedMonth.value.toString(), amountCents))
        }
    }
}
