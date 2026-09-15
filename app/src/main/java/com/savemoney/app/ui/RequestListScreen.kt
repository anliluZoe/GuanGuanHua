package com.savemoney.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.data.PurchaseRequest
import com.savemoney.app.data.RequestStatus
import com.savemoney.app.ui.theme.Cute

@Composable
fun RequestListScreen(
    viewModel: AppViewModel,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<RequestStatus?>(null) }
    val shown = if (filter == null) requests else requests.filter { it.status == filter }
    val pendingCount = requests.count { it.status == RequestStatus.PENDING }
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        PageHeader(
            title = "买买申请",
            subtitle = profile.partnerName?.let { "${profile.name} 和 $it，一起把关每一笔开销" }
                ?: "${profile.name}，等另一半加入后一起把关每一笔开销",
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChoiceChip("全部", filter == null, onClick = { filter = null })
            RequestStatus.entries.forEach { status ->
                val label = if (status == RequestStatus.PENDING && pendingCount > 0)
                    "${status.label} $pendingCount" else status.label
                ChoiceChip(label, filter == status, onClick = { filter = status })
            }
        }
        Spacer(Modifier.height(16.dp))
        if (shown.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyHint(
                    kind = MascotKind.Coin,
                    title = "还没有想买的东西",
                    subtitle = "谁想买都可以发起申请，另一个人来把关",
                )
            }
            CharcoalPillButton("新建申请", onClick = onCreate)
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
                    CharcoalPillButton("＋  新建申请", onClick = onCreate)
                }
            }
        }
    }
}

@Composable
private fun RequestCard(request: PurchaseRequest, onClick: () -> Unit) {
    SoftCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RequestThumb(request.category, request.imagePath)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        request.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(request.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    request.totalCents.toYuan(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Cute.Peach,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${request.unitPriceCents.toYuan()} × ${request.quantity} · ${if (request.mine) "我" else request.requesterName} · ${request.createdAt.toDateTimeText()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cute.Muted,
                )
                if (request.status == RequestStatus.PENDING) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (request.mine) "等 TA 看一眼" else "👀 等你把关",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (request.mine) Cute.Muted else Cute.Peach,
                    )
                }
            }
        }
    }
}
