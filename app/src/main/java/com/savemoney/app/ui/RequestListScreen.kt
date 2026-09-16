package com.savemoney.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.data.PurchaseRequest
import com.savemoney.app.data.RequestStatus
import com.savemoney.app.ui.theme.QTheme

@Composable
fun RequestListScreen(
    viewModel: AppViewModel,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<RequestStatus?>(null) }
    val shown = if (filter == null) requests else requests.filter { it.status == filter }
    val pendingForMe = requests.count { it.status == RequestStatus.PENDING && !it.mine }
    val waitingForList = !isReady
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QTheme.colors.screenGlow),
    ) {
        RefreshBar(visible = isRefreshing && isReady)
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Mascot(MascotKind.Cat, size = 64.dp)
        Spacer(Modifier.height(8.dp))
        PageHeader(
            title = "买买申请",
            subtitle = profile.partnerName?.let { "${profile.name} × $it · 想买？先过我这关哼" }
                ?: "${profile.name}，等另一半加入后一起把关每一笔开销",
        )
        if (profile.partnerName != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NameDot(profile.name, QTheme.colors.sky)
                NameDot(profile.partnerName!!, QTheme.colors.mint, modifier = Modifier.offset(x = (-8).dp))
                Text(
                    if (pendingForMe > 0) "$pendingForMe 笔在等你批哦" else "暂时没人闯关，哼",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QTheme.colors.muted,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChoiceChip("全部", filter == null, onClick = { filter = null })
            RequestStatus.entries.forEach { status ->
                ChoiceChip(status.label, filter == status, onClick = { filter = status })
            }
        }
        Spacer(Modifier.height(16.dp))
        if (waitingForList) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingHint("正在同步申请…")
            }
        } else if (shown.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyHint(
                    kind = MascotKind.Cat,
                    title = "还没有人来闯关",
                    subtitle = "想买就提申请，过了我这关再说",
                )
            }
            PillButton("新建申请", enabled = !isBusy, onClick = onCreate)
            Spacer(Modifier.height(12.dp))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(shown, key = { it.id }) { request ->
                    RequestCard(request, onClick = { onOpen(request.id) })
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    PillButton("＋  新建申请", enabled = !isBusy, onClick = onCreate)
                }
            }
        }
        }
    }
}

@Composable
private fun RequestCard(request: PurchaseRequest, onClick: () -> Unit) {
    val accent = categoryLook(request.category).accent
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, QTheme.colors.line, shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    request.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(request.status, partial = request.partial)
            }
            Spacer(Modifier.height(8.dp))
            CategoryChip(request.category)
            Spacer(Modifier.height(10.dp))
            MoneyText(request.totalCents, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append((request.approvedUnitPriceCents ?: request.unitPriceCents).toYuan())
                    append(" × ")
                    append(request.approvedQuantity ?: request.quantity)
                    if (request.partial) {
                        append(" · 申请 ${request.quantity} 个 ${request.askedCents.toYuan()}")
                    }
                    append(" · ")
                    append(if (request.mine) "我" else request.requesterName)
                    append(" · ")
                    append(request.createdAt.toDateTimeText())
                },
                style = MaterialTheme.typography.bodySmall,
                color = QTheme.colors.muted,
            )
            if (request.status == RequestStatus.PENDING) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (request.mine) "等 TA 看一眼" else "等你把关哼",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (request.mine) QTheme.colors.muted else QTheme.colors.coral,
                )
            }
        }
        if (!request.imagePath.isNullOrBlank()) {
            RequestThumb(
                request.category,
                request.imagePath,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}
