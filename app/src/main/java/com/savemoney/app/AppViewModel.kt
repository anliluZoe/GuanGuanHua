package com.savemoney.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.savemoney.app.data.ExpenseRecord
import com.savemoney.app.data.MonthlyBudget
import com.savemoney.app.data.PurchaseRequest
import com.savemoney.app.data.SessionDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

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

data class HouseholdSession(
    val serverUrl: String,
    val token: String,
    val householdCode: String,
) {
    val joined: Boolean get() = token.isNotBlank()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as SaveMoneyApp).repository
    private val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(
        HouseholdSession(
            serverUrl = prefs.getString("serverUrl", "http://10.0.2.2:8080")!!,
            token = prefs.getString("token", "")!!,
            householdCode = prefs.getString("householdCode", "")!!,
        )
    )
    val session: StateFlow<HouseholdSession> = _session.asStateFlow()

    private val _profile = MutableStateFlow(
        UserProfile(
            role = runCatching { UserRole.valueOf(prefs.getString("role", UserRole.REQUESTER.name)!!) }.getOrDefault(UserRole.REQUESTER),
            requesterName = prefs.getString("requesterName", "申请人")!!,
            approverName = prefs.getString("approverName", "审核人")!!,
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _requests = MutableStateFlow<List<PurchaseRequest>>(emptyList())
    val requests: StateFlow<List<PurchaseRequest>> = _requests.asStateFlow()

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _monthExpenses = MutableStateFlow<List<ExpenseRecord>>(emptyList())
    val monthExpenses: StateFlow<List<ExpenseRecord>> = _monthExpenses.asStateFlow()

    private val _monthBudget = MutableStateFlow<MonthlyBudget?>(null)
    val monthBudget: StateFlow<MonthlyBudget?> = _monthBudget.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        if (_session.value.joined) refresh()
    }

    fun observeRequest(id: Long): Flow<PurchaseRequest?> =
        requests.map { list -> list.firstOrNull { it.id == id } }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                val remote = repo.session()
                applyRemoteSession(remote.role, remote.requesterName, remote.approverName, remote.householdCode)
                _requests.value = repo.listRequests()
                loadMonth(_selectedMonth.value)
            }.onFailure { _statusMessage.value = it.message ?: "同步失败" }
        }
    }

    fun createHome(serverUrl: String, name: String, role: UserRole) {
        viewModelScope.launch { connect(serverUrl) { repo.createHousehold(name, role.name) } }
    }

    fun joinHome(serverUrl: String, code: String, name: String, role: UserRole) {
        viewModelScope.launch { connect(serverUrl) { repo.joinHousehold(code, name, role.name) } }
    }

    fun leaveHome() {
        prefs.edit {
            remove("token")
            remove("householdCode")
        }
        _session.update { it.copy(token = "", householdCode = "") }
        _requests.value = emptyList()
        _monthExpenses.value = emptyList()
        _monthBudget.value = null
    }

    fun updateProfile(role: UserRole, requesterName: String, approverName: String) {
        val next = UserProfile(role, requesterName.trim().ifBlank { "申请人" }, approverName.trim().ifBlank { "审核人" })
        _profile.value = next
        persistProfile(next)
        viewModelScope.launch {
            runCatching { repo.updateSession(next.role.name, next.requesterName, next.approverName) }
                .onFailure { _statusMessage.value = it.message ?: "保存失败" }
        }
    }

    fun submitRequest(
        itemName: String,
        category: String,
        unitPriceCents: Long,
        quantity: Int,
        reason: String,
        imageUri: String?,
    ) {
        viewModelScope.launch {
            runCatching {
                repo.createRequest(itemName.trim(), category, unitPriceCents, quantity, reason.trim(), imageUri?.let(Uri::parse))
                _requests.value = repo.listRequests()
            }.onFailure { _statusMessage.value = it.message ?: "提交失败" }
        }
    }

    fun review(requestId: Long, approve: Boolean, comment: String) {
        viewModelScope.launch {
            runCatching {
                repo.review(requestId, approve, comment)
                _requests.value = repo.listRequests()
                loadMonth(_selectedMonth.value)
            }.onFailure { _statusMessage.value = it.message ?: "审核失败" }
        }
    }

    fun withdrawRequest(requestId: Long) {
        viewModelScope.launch {
            runCatching {
                repo.withdraw(requestId)
                _requests.value = repo.listRequests()
            }.onFailure { _statusMessage.value = it.message ?: "撤回失败" }
        }
    }

    fun shiftMonth(delta: Long) {
        _selectedMonth.update { it.plusMonths(delta) }
        viewModelScope.launch { runCatching { loadMonth(_selectedMonth.value) } }
    }

    fun setBudget(amountCents: Long) {
        viewModelScope.launch {
            runCatching {
                _monthBudget.value = repo.setBudget(_selectedMonth.value.toString(), amountCents)
            }.onFailure { _statusMessage.value = it.message ?: "预算保存失败" }
        }
    }

    fun consumeStatus() {
        _statusMessage.value = null
    }

    private suspend fun connect(serverUrl: String, action: suspend () -> SessionDto) {
        val url = serverUrl.trim().trimEnd('/')
        prefs.edit(commit = true) { putString("serverUrl", url) }
        _session.update { it.copy(serverUrl = url) }
        runCatching {
            val remote = action()
            prefs.edit(commit = true) { putString("token", remote.token) }
            applyRemoteSession(remote.role, remote.requesterName, remote.approverName, remote.householdCode)
            _session.update { it.copy(token = remote.token, householdCode = remote.householdCode) }
            _requests.value = repo.listRequests()
            loadMonth(_selectedMonth.value)
        }.onFailure { _statusMessage.value = it.message ?: "连接失败，请检查服务器地址" }
    }

    private suspend fun loadMonth(month: YearMonth) {
        _monthExpenses.value = repo.listExpenses(month.toString())
        _monthBudget.value = repo.getBudget(month.toString())
    }

    private fun applyRemoteSession(role: String, requester: String, approver: String, code: String) {
        val next = UserProfile(
            role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.REQUESTER),
            requesterName = requester,
            approverName = approver,
        )
        _profile.value = next
        persistProfile(next)
        prefs.edit { putString("householdCode", code) }
        _session.update { it.copy(householdCode = code) }
    }

    private fun persistProfile(next: UserProfile) {
        prefs.edit {
            putString("role", next.role.name)
            putString("requesterName", next.requesterName)
            putString("approverName", next.approverName)
        }
    }
}
