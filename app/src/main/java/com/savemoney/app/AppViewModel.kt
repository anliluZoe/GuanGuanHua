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
import com.savemoney.app.notify.ReviewActivity
import com.savemoney.app.notify.ReviewActivityWorker
import com.savemoney.app.ui.theme.Appearance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val _busyCount = MutableStateFlow(0)
    /** 提交、审核、建家等写操作进行中；界面用遮罩并禁用主按钮。 */
    val isBusy: StateFlow<Boolean> = _busyCount
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _refreshCount = MutableStateFlow(0)
    /** 拉列表/账本/资料。首次居中转圈，之后顶栏细进度；后台 30 秒轮询走 quiet。 */
    val isRefreshing: StateFlow<Boolean> = _refreshCount
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _ready = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _ready.asStateFlow()

    private val _appearance = MutableStateFlow(Appearance.fromPref(prefs.getString(PREF_APPEARANCE, null)))
    val appearance: StateFlow<Appearance> = _appearance.asStateFlow()

    init {
        if (_session.value.joined) refresh()
    }

    fun observeRequest(id: Long): Flow<PurchaseRequest?> =
        requests.map { list -> list.firstOrNull { it.id == id } }

    fun refresh(quiet: Boolean = false) {
        viewModelScope.launch {
            track(_refreshCount, enabled = !quiet) {
                runCatching {
                    applyRemoteSession(repo.session())
                    syncRequests()
                    loadMonth(_selectedMonth.value)
                }.onFailure { _statusMessage.value = it.message ?: "同步失败" }
                _ready.value = true
            }
        }
    }

    fun createHome(serverUrl: String, name: String) {
        viewModelScope.launch {
            track(_busyCount) { connect(serverUrl) { repo.createHousehold(name.trim()) } }
        }
    }

    fun joinHome(serverUrl: String, code: String, name: String) {
        viewModelScope.launch {
            track(_busyCount) { connect(serverUrl) { repo.joinHousehold(code, name.trim()) } }
        }
    }

    fun leaveHome() {
        ReviewActivityWorker.cancel(getApplication())
        prefs.edit {
            remove("token")
            remove("householdCode")
            remove(ReviewActivity.PREF_SINCE)
        }
        _session.update { it.copy(token = "", householdCode = "") }
        _requests.value = emptyList()
        _monthExpenses.value = emptyList()
        _monthBudget.value = null
        _ready.value = false
    }

    fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _statusMessage.value = "名字不能为空"
            return
        }
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { applyRemoteSession(repo.updateName(trimmed)) }
                    .onFailure { _statusMessage.value = it.message ?: "保存失败" }
            }
        }
    }

    fun submitRequest(
        itemName: String,
        category: String,
        unitPriceCents: Long,
        quantity: Int,
        reason: String,
        imageUri: String?,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching {
                    repo.createRequest(itemName.trim(), category, unitPriceCents, quantity, reason.trim(), imageUri?.let(Uri::parse))
                    syncRequests()
                }.onSuccess { onSuccess() }
                    .onFailure { _statusMessage.value = it.message ?: "提交失败" }
            }
        }
    }

    fun review(
        requestId: Long,
        approve: Boolean,
        comment: String,
        unitPriceCents: Long? = null,
        quantity: Int? = null,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching {
                    repo.review(requestId, approve, comment, unitPriceCents, quantity)
                    syncRequests()
                    loadMonth(_selectedMonth.value)
                }.onSuccess { onSuccess() }
                    .onFailure { _statusMessage.value = it.message ?: "审核失败" }
            }
        }
    }

    fun withdrawRequest(requestId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching {
                    repo.withdraw(requestId)
                    syncRequests()
                }.onSuccess { onSuccess() }
                    .onFailure { _statusMessage.value = it.message ?: "撤回失败" }
            }
        }
    }

    fun shiftMonth(delta: Long) {
        _selectedMonth.update { it.plusMonths(delta) }
        viewModelScope.launch {
            track(_refreshCount) { runCatching { loadMonth(_selectedMonth.value) } }
        }
    }

    fun setBudget(amountCents: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching {
                    _monthBudget.value = repo.setBudget(_selectedMonth.value.toString(), amountCents)
                }.onSuccess { onSuccess() }
                    .onFailure { _statusMessage.value = it.message ?: "预算保存失败" }
            }
        }
    }

    fun consumeStatus() {
        _statusMessage.value = null
    }

    fun setAppearance(value: Appearance) {
        prefs.edit { putString(PREF_APPEARANCE, value.prefValue) }
        _appearance.value = value
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
            ReviewActivityWorker.enqueueSoon(getApplication())
        }.onFailure { _statusMessage.value = it.message ?: "连接失败，请检查服务器地址" }
        _ready.value = _session.value.joined
    }

    /**
     * 拉取申请列表。前台只弹页内提示；若请求在退到后台之后才返回，改走系统通知，避免水位线被吃掉却没有状态栏提醒。
     */
    private suspend fun syncRequests() {
        val list = repo.listRequests()
        val since = prefs.getLong(ReviewActivity.PREF_SINCE, 0L)
        val items = if (since > 0L) ReviewActivity.newItems(list, since) else emptyList()
        val consume = when {
            since <= 0L || items.isEmpty() -> true
            (getApplication() as SaveMoneyApp).inForeground -> {
                _statusMessage.value = items.last().title
                true
            }
            else -> ReviewActivityWorker.notifyItems(getApplication(), items)
        }
        if (consume) {
            prefs.edit(commit = true) { putLong(ReviewActivity.PREF_SINCE, ReviewActivity.watermark(list, since)) }
        }
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

    private suspend fun track(count: MutableStateFlow<Int>, enabled: Boolean = true, block: suspend () -> Unit) {
        if (!enabled) {
            block()
            return
        }
        count.update { it + 1 }
        try {
            block()
        } finally {
            count.update { it - 1 }
        }
    }

    companion object {
        private const val PREF_APPEARANCE = "appearance"
    }
}
