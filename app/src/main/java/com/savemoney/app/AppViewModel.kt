package com.savemoney.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.savemoney.app.data.ExpenseRecord
import com.savemoney.app.data.MonthlyBudget
import com.savemoney.app.data.PurchaseRequest
import com.savemoney.app.data.SessionDto
import com.savemoney.app.notify.ReviewActivity
import com.savemoney.app.notify.ReviewActivityWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

/** 我叫什么、另一半叫什么。谁都能发申请，由对方来审。 */
data class UserProfile(
    val name: String,
    val partnerName: String?,
)

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
            name = prefs.getString("name", "")!!,
            partnerName = prefs.getString("partnerName", null),
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
                applyRemoteSession(repo.session())
                syncRequests()
                loadMonth(_selectedMonth.value)
            }.onFailure { _statusMessage.value = it.message ?: "同步失败" }
        }
    }

    fun createHome(serverUrl: String, name: String) {
        viewModelScope.launch { connect(serverUrl) { repo.createHousehold(name.trim()) } }
    }

    fun joinHome(serverUrl: String, code: String, name: String) {
        viewModelScope.launch { connect(serverUrl) { repo.joinHousehold(code, name.trim()) } }
    }

    fun leaveHome() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(ReviewActivityWorker.WORK_NAME)
        prefs.edit {
            remove("token")
            remove("householdCode")
            remove(ReviewActivity.PREF_SINCE)
        }
        _session.update { it.copy(token = "", householdCode = "") }
        _requests.value = emptyList()
        _monthExpenses.value = emptyList()
        _monthBudget.value = null
    }

    fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _statusMessage.value = "名字不能为空"
            return
        }
        viewModelScope.launch {
            runCatching { applyRemoteSession(repo.updateName(trimmed)) }
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
                syncRequests()
            }.onFailure { _statusMessage.value = it.message ?: "提交失败" }
        }
    }

    fun review(
        requestId: Long,
        approve: Boolean,
        comment: String,
        unitPriceCents: Long? = null,
        quantity: Int? = null,
    ) {
        viewModelScope.launch {
            runCatching {
                repo.review(requestId, approve, comment, unitPriceCents, quantity)
                syncRequests()
                loadMonth(_selectedMonth.value)
            }.onFailure { _statusMessage.value = it.message ?: "审核失败" }
        }
    }

    fun withdrawRequest(requestId: Long) {
        viewModelScope.launch {
            runCatching {
                repo.withdraw(requestId)
                syncRequests()
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
            applyRemoteSession(remote)
            _session.update { it.copy(token = remote.token, householdCode = remote.householdCode) }
            syncRequests()
            loadMonth(_selectedMonth.value)
            ReviewActivityWorker.schedule(getApplication())
        }.onFailure { _statusMessage.value = it.message ?: "连接失败，请检查服务器地址" }
    }

    /** 拉取申请列表；如果对方有新动作（新申请 / 审核结果），顺手在页面上提示一句。 */
    private suspend fun syncRequests() {
        val list = repo.listRequests()
        val since = prefs.getLong(ReviewActivity.PREF_SINCE, 0L)
        if (since > 0L) {
            ReviewActivity.newItems(list, since).lastOrNull()?.let { _statusMessage.value = it.title }
        }
        prefs.edit(commit = true) { putLong(ReviewActivity.PREF_SINCE, ReviewActivity.watermark(list, since)) }
        _requests.value = list
    }

    private suspend fun loadMonth(month: YearMonth) {
        _monthExpenses.value = repo.listExpenses(month.toString())
        _monthBudget.value = repo.getBudget(month.toString())
    }

    private fun applyRemoteSession(remote: SessionDto) {
        val partner = remote.members
            .filter { it.id != remote.memberId }
            .joinToString("、") { it.name }
            .ifBlank { null }
        _profile.value = UserProfile(name = remote.name, partnerName = partner)
        prefs.edit {
            putString("name", remote.name)
            putString("partnerName", partner)
            putString("householdCode", remote.householdCode)
        }
        _session.update { it.copy(householdCode = remote.householdCode) }
    }
}
