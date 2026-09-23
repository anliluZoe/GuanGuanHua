package com.guanguanhua.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guanguanhua.app.cycle.CycleCache
import com.guanguanhua.app.cycle.CycleMath
import com.guanguanhua.app.data.ApiConfig
import com.guanguanhua.app.data.CycleRecord
import com.guanguanhua.app.data.CycleSettings
import com.guanguanhua.app.data.CycleState
import com.guanguanhua.app.data.ExpenseRecord
import com.guanguanhua.app.data.HouseholdFullException
import com.guanguanhua.app.data.MemberDto
import com.guanguanhua.app.data.MonthlyBudget
import com.guanguanhua.app.data.PurchaseRequest
import com.guanguanhua.app.data.SessionDto
import com.guanguanhua.app.data.WidgetDto
import com.guanguanhua.app.notify.CycleReminder
import com.guanguanhua.app.notify.ReviewActivity
import com.guanguanhua.app.notify.ReviewActivityWorker
import com.guanguanhua.app.ui.theme.Appearance
import com.guanguanhua.app.widget.WidgetCache
import com.guanguanhua.app.widget.WidgetCopy
import com.guanguanhua.app.widget.WidgetRefreshWorker
import com.guanguanhua.app.widget.WidgetState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** 我叫什么、另一半叫什么。谁都能发申请，由对方来审。头像跟「我的头像」同步。 */
data class UserProfile(
    val name: String,
    val partnerName: String?,
    val avatarPreset: String? = null,
    val avatarUrl: String? = null,
    val partnerAvatarPreset: String? = null,
    val partnerAvatarUrl: String? = null,
)

data class HouseholdSession(
    val serverUrl: String,
    val token: String,
    val householdCode: String,
) {
    val joined: Boolean get() = token.isNotBlank()
}

data class JoinMemberPicker(
    val code: String,
    val members: List<MemberDto>,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as GuanGuanHuaApp).repository
    private val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(
        HouseholdSession(
            serverUrl = ApiConfig.resolvedServerUrl(prefs.getString("serverUrl", null)),
            token = prefs.getString("token", "")!!,
            householdCode = prefs.getString("householdCode", "")!!,
        )
    )
    val session: StateFlow<HouseholdSession> = _session.asStateFlow()

    private val _joinPicker = MutableStateFlow<JoinMemberPicker?>(null)
    val joinPicker: StateFlow<JoinMemberPicker?> = _joinPicker.asStateFlow()

    private val _profile = MutableStateFlow(
        UserProfile(
            name = prefs.getString("name", "")!!,
            partnerName = prefs.getString("partnerName", null),
            avatarPreset = prefs.getString("avatarPreset", null),
            avatarUrl = prefs.getString("avatarUrl", null),
            partnerAvatarPreset = prefs.getString("partnerAvatarPreset", null),
            partnerAvatarUrl = prefs.getString("partnerAvatarUrl", null),
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
    /** 拉列表/账本/资料。首次居中转圈，之后由下拉刷新指示器表示；后台 30 秒轮询走 quiet。 */
    val isRefreshing: StateFlow<Boolean> = _refreshCount
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _ready = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _ready.asStateFlow()

    private val _appearance = MutableStateFlow(Appearance.fromPref(prefs.getString(PREF_APPEARANCE, null)))
    val appearance: StateFlow<Appearance> = _appearance.asStateFlow()

    private val _widget = MutableStateFlow(WidgetCache.read(app))
    val widget: StateFlow<WidgetState> = _widget.asStateFlow()

    private val _cycle = MutableStateFlow(CycleState())
    val cycle: StateFlow<CycleState> = _cycle.asStateFlow()

    init {
        prefs.edit(commit = true) { putString("serverUrl", _session.value.serverUrl) }
        if (_session.value.joined) {
            val memberId = prefs.getLong(PREF_MEMBER_ID, 0L)
            val cached = CycleCache.read(app)?.takeIf { it.memberId == memberId }
            if (cached != null) _cycle.value = cached
            refresh()
        } else {
            _ready.value = true
        }
    }

    fun observeRequest(id: Long): Flow<PurchaseRequest?> =
        requests.map { list -> list.firstOrNull { it.id == id } }

    fun refresh(quiet: Boolean = false) {
        if (!_session.value.joined) {
            _ready.value = true
            return
        }
        viewModelScope.launch {
            track(_refreshCount, enabled = !quiet) {
                runCatching {
                    applyRemoteSession(repo.session())
                    syncRequests()
                    loadMonth(_selectedMonth.value)
                }.onFailure { _statusMessage.value = it.message ?: "同步失败" }
                runCatching { syncWidget() }
                runCatching { syncCycles() }.onFailure { error ->
                    if (!quiet && !_cycle.value.loaded) {
                        _statusMessage.value = error.message ?: "周期同步失败"
                    }
                }
                _ready.value = true
            }
        }
    }

    fun createHome(name: String) {
        viewModelScope.launch {
            track(_busyCount) { connect(_session.value.serverUrl) { repo.createHousehold(name.trim()) } }
        }
    }

    fun joinHome(code: String, name: String, memberId: Long? = null) {
        viewModelScope.launch {
            track(_busyCount) {
                connect(_session.value.serverUrl, householdCode = code.trim()) {
                    repo.joinHousehold(code, name.trim(), memberId)
                }
            }
        }
    }

    fun enterAsExistingMember(memberId: Long) {
        val picker = _joinPicker.value ?: return
        joinHome(picker.code, _profile.value.name, memberId)
    }

    fun dismissJoinPicker() {
        _joinPicker.value = null
    }

    fun setServerUrl(url: String) {
        val resolved = ApiConfig.resolvedServerUrl(url)
        prefs.edit(commit = true) { putString("serverUrl", resolved) }
        _session.update { it.copy(serverUrl = resolved) }
    }

    fun leaveHome(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { repo.leaveHousehold() }
                    .onSuccess {
                        ReviewActivityWorker.cancel(getApplication())
                        WidgetRefreshWorker.cancel(getApplication())
                        CycleReminder.cancel(getApplication())
                        WidgetCache.clear(getApplication())
                        CycleCache.clear(getApplication())
                        _widget.value = WidgetState()
                        _cycle.value = CycleState()
                        prefs.edit {
                            remove("token")
                            remove("householdCode")
                            remove(PREF_MEMBER_ID)
                            remove(ReviewActivity.PREF_SINCE)
                        }
                        _session.update { it.copy(token = "", householdCode = "") }
                        _joinPicker.value = null
                        _requests.value = emptyList()
                        _monthExpenses.value = emptyList()
                        _monthBudget.value = null
                        _ready.value = true
                        WidgetCache.publish(getApplication())
                        onSuccess()
                    }
                    .onFailure { _statusMessage.value = it.message ?: "退出失败，请检查网络后再试" }
            }
        }
    }

    fun updateName(name: String, onSuccess: () -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _statusMessage.value = "名字不能为空"
            return
        }
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { applyRemoteSession(repo.updateName(trimmed)) }
                    .onSuccess { onSuccess() }
                    .onFailure { _statusMessage.value = it.message ?: "保存失败" }
            }
        }
    }

    fun updateAvatarPreset(presetId: String) {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { applyRemoteSession(repo.updateAvatarPreset(_profile.value.name, presetId)) }
                    .onFailure { _statusMessage.value = it.message ?: "头像保存失败" }
            }
        }
    }

    fun updateAvatarPhoto(uri: Uri) {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { applyRemoteSession(repo.uploadAvatar(uri)) }
                    .onFailure { _statusMessage.value = it.message ?: "头像上传失败" }
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

    fun postStatus(message: String) {
        val text = message.trim()
        if (text.isNotEmpty()) _statusMessage.value = text
    }

    fun consumeStatus() {
        _statusMessage.value = null
    }

    fun refreshWidget() {
        if (!_session.value.joined) return
        viewModelScope.launch {
            track(_refreshCount) {
                runCatching { syncWidget() }
                    .onFailure { _statusMessage.value = it.message ?: "组件同步失败" }
            }
        }
    }

    fun saveWidgetCaption(caption: String, captionColor: String) {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching {
                    applyWidget(
                        repo.updateWidgetCaption(
                            WidgetCopy.clampCaption(caption),
                            WidgetCopy.normalizeCaptionColor(captionColor),
                        ),
                    )
                }.onFailure { _statusMessage.value = it.message ?: "说明保存失败" }
            }
        }
    }

    fun uploadWidgetPhoto(uri: Uri) {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { applyWidget(repo.uploadWidgetImage(uri)) }
                    .onFailure { _statusMessage.value = it.message ?: "照片上传失败" }
            }
        }
    }

    fun clearWidgetPhoto() {
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { applyWidget(repo.clearWidgetImage()) }
                    .onFailure { _statusMessage.value = it.message ?: "照片清空失败" }
            }
        }
    }

    fun recordCycleStart(iso: String) {
        if (!_session.value.joined) {
            _statusMessage.value = "先加入家庭再记"
            return
        }
        if (_cycle.value.cycles.any { it.start == iso }) {
            _statusMessage.value = "这一天已经是开始日啦"
            return
        }
        val start = CycleMath.parse(iso) ?: return
        val period = CycleMath.periodDays(_cycle.value.settings)
        val end = CycleMath.prefillEnd(start, period)
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { repo.createCycle(start.toString(), end.toString()) }
                    .onSuccess {
                        runCatching { syncCycles() }
                        _statusMessage.value = "已记开始 · 按经期 $period 天填到 ${CycleMath.formatCn(end)}（可再改）"
                    }
                    .onFailure { _statusMessage.value = it.message ?: "没记上" }
            }
        }
    }

    fun recordCycleEnd(iso: String) {
        val cycles = _cycle.value.cycles
        val open = cycles.filter { it.end.isNullOrBlank() }.maxByOrNull { it.start } ?: cycles.maxByOrNull { it.start }
        if (open == null) {
            _statusMessage.value = "先记一个开始日吧"
            return
        }
        if (iso < open.start) {
            _statusMessage.value = "结束日不能早于开始日哦"
            return
        }
        saveCycle(open.id, open.start, iso, "已记结束日 · ${CycleMath.formatCn(LocalDate.parse(iso))}")
    }

    fun saveCycle(id: Long, start: String, end: String?, success: String = "改好啦") {
        if (start.isBlank()) {
            _statusMessage.value = "开始日要填一下"
            return
        }
        if (!end.isNullOrBlank() && end < start) {
            _statusMessage.value = "结束日不能早于开始日哦"
            return
        }
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { repo.updateCycle(id, start, end?.takeIf { it.isNotBlank() }) }
                    .onSuccess {
                        runCatching { syncCycles() }
                        _statusMessage.value = success
                    }
                    .onFailure { _statusMessage.value = it.message ?: "没改成" }
            }
        }
    }

    fun setCycleReference(days: Int?) {
        val next = days?.coerceIn(CycleMath.REF_MIN, CycleMath.REF_MAX)
        patchCycleSettings(_cycle.value.settings.copy(referenceCycleDays = next))
    }

    fun setCyclePeriodDays(days: Int) {
        patchCycleSettings(_cycle.value.settings.copy(periodDays = days.coerceIn(CycleMath.PERIOD_MIN, CycleMath.PERIOD_MAX)))
    }

    fun setCycleRemind(enabled: Boolean) {
        patchCycleSettings(_cycle.value.settings.copy(remindEnabled = enabled))
    }

    fun setAppearance(value: Appearance) {
        prefs.edit { putString(PREF_APPEARANCE, value.prefValue) }
        _appearance.value = value
    }

    private suspend fun connect(serverUrl: String, householdCode: String? = null, action: suspend () -> SessionDto) {
        val url = ApiConfig.resolvedServerUrl(serverUrl)
        prefs.edit(commit = true) { putString("serverUrl", url) }
        _session.update { it.copy(serverUrl = url) }
        runCatching {
            val remote = action()
            prefs.edit(commit = true) { putString("token", remote.token) }
            applyRemoteSession(remote)
            _session.update { it.copy(token = remote.token, householdCode = remote.householdCode) }
            _joinPicker.value = null
            syncRequests()
            loadMonth(_selectedMonth.value)
            runCatching { syncWidget() }
            runCatching { syncCycles() }
            ReviewActivityWorker.schedule(getApplication())
            ReviewActivityWorker.enqueueSoon(getApplication())
            WidgetRefreshWorker.schedule(getApplication())
            WidgetRefreshWorker.enqueueSoon(getApplication())
        }.onFailure { error ->
            if (error is HouseholdFullException && error.members.isNotEmpty()) {
                _joinPicker.value = JoinMemberPicker(
                    code = householdCode.orEmpty(),
                    members = error.members,
                )
            } else {
                _statusMessage.value = error.message ?: "连接失败，请检查服务器地址"
            }
        }
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
            (getApplication() as GuanGuanHuaApp).inForeground -> {
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

    private suspend fun syncWidget() {
        val app = getApplication<Application>()
        val previous = WidgetCache.read(app)
        val next = WidgetCache.applyRemote(app, repo.getWidget())
        _widget.value = next
        if (next != previous) WidgetCache.publish(app)
    }

    private suspend fun applyWidget(remote: WidgetDto) {
        val app = getApplication<Application>()
        _widget.value = WidgetCache.applyRemote(app, remote)
        WidgetCache.publish(app)
    }

    private fun applyRemoteSession(remote: SessionDto) {
        val others = remote.members.filter { it.id != remote.memberId }
        val me = remote.members.firstOrNull { it.id == remote.memberId }
        val partner = others.firstOrNull()
        val partnerName = others.joinToString("、") { it.name }.ifBlank { null }
        _profile.value = UserProfile(
            name = remote.name,
            partnerName = partnerName,
            avatarPreset = me?.avatarPreset,
            avatarUrl = me?.avatarUrl,
            partnerAvatarPreset = partner?.avatarPreset,
            partnerAvatarUrl = partner?.avatarUrl,
        )
        prefs.edit {
            putString("name", remote.name)
            putString("partnerName", partnerName)
            putString("householdCode", remote.householdCode)
            putString("avatarPreset", me?.avatarPreset)
            putString("avatarUrl", me?.avatarUrl)
            putString("partnerAvatarPreset", partner?.avatarPreset)
            putString("partnerAvatarUrl", partner?.avatarUrl)
        }
        _session.update { it.copy(householdCode = remote.householdCode) }
        val previousId = prefs.getLong(PREF_MEMBER_ID, 0L)
        if (previousId != 0L && previousId != remote.memberId) {
            CycleCache.clear(getApplication())
            CycleReminder.cancel(getApplication())
            _cycle.value = CycleState()
        }
        prefs.edit { putLong(PREF_MEMBER_ID, remote.memberId) }
    }

    private suspend fun syncCycles() {
        if (!_session.value.joined) return
        publishCycles(repo.listCycles(), repo.getCycleSettings())
    }

    private fun publishCycles(cycles: List<CycleRecord>, settings: CycleSettings) {
        val memberId = prefs.getLong(PREF_MEMBER_ID, 0L)
        val state = CycleState(memberId, cycles, settings, loaded = true)
        _cycle.value = state
        if (memberId != 0L) {
            CycleCache.write(getApplication(), state)
            CycleReminder.schedule(getApplication(), state)
        }
    }

    private fun patchCycleSettings(next: CycleSettings) {
        if (!_session.value.joined) {
            _statusMessage.value = "先加入家庭再改"
            return
        }
        viewModelScope.launch {
            track(_busyCount) {
                runCatching { publishCycles(_cycle.value.cycles, repo.updateCycleSettings(next)) }
                    .onFailure { _statusMessage.value = it.message ?: "设置没保存上" }
            }
        }
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
        const val PREF_MEMBER_ID = "memberId"
    }
}
