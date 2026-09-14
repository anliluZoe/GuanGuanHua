package com.savemoney.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.savemoney.app.UserRole
import com.savemoney.app.data.RequestStatus

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("购买申请")
                        Text(
                            text = "${profile.role.label} · ${profile.currentName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (profile.role == UserRole.REQUESTER) {
                ExtendedFloatingActionButton(
                    onClick = onCreate,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("新建申请") },
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("全部") },
                )
                RequestStatus.entries.forEach { status ->
                    FilterChip(
                        selected = filter == status,
                        onClick = { filter = status },
                        label = {
                            Text(
                                if (status == RequestStatus.PENDING && pendingCount > 0)
                                    "${status.label} $pendingCount"
                                else status.label
                            )
                        },
                    )
                }
            }

            if (shown.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (profile.role == UserRole.REQUESTER) "还没有申请，点击右下角新建" else "暂无申请",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.id }) { request ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(request.id) },
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = request.itemName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatusBadge(request.status)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = request.totalCents.toYuan(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${request.unitPriceCents.toYuan()} × ${request.quantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${request.category} · ${request.requesterName} · ${request.createdAt.toDateTimeText()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
