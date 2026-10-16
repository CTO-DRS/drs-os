/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Checklist/summary screens. The screens render exactly the four
 * honest states produced by security-core; loading and failure are
 * real UI states, never masked with stale or fake content.
 *
 * State is never communicated by color alone: every row pairs the
 * color with the localized state word and an icon
 * (docs/ui/native-system-surfaces.md §9 a11y rule).
 */
package com.drs.os.securitycenter.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drs.os.security.CheckDataProvider
import com.drs.os.security.CheckState
import com.drs.os.security.ChecklistSummaryUi
import com.drs.os.security.CheckItemUi
import com.drs.os.securitycenter.R
import com.drs.os.securitycenter.ui.DashboardPresentation
import com.drs.os.securitycenter.ui.DashboardUiState
import com.drs.os.securitycenter.ui.ResKeys
import com.drs.os.securitycenter.ui.theme.DarkDanger
import com.drs.os.securitycenter.ui.theme.DarkSuccess
import com.drs.os.securitycenter.ui.theme.DarkWarning
import com.drs.os.securitycenter.ui.theme.LightDanger
import com.drs.os.securitycenter.ui.theme.LightSuccess
import com.drs.os.securitycenter.ui.theme.LightWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChecklistViewModel(
    private val providerFactory: () -> CheckDataProvider
) : ViewModel() {

    private val _state = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state: StateFlow<DashboardUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.value = DashboardUiState.Loading
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.Default) {
                DashboardPresentation.evaluate(providerFactory())
            }
        }
    }
}

@Composable
private fun stateColor(state: CheckState) = when (state) {
    CheckState.PASS -> if (isSystemInDarkTheme()) DarkSuccess else LightSuccess
    CheckState.WARNING -> if (isSystemInDarkTheme()) DarkWarning else LightWarning
    CheckState.UNAVAILABLE, CheckState.NOT_CHECKED ->
        if (isSystemInDarkTheme()) DarkDanger else LightDanger
}

@Composable
fun DashboardScreen(
    viewModel: ChecklistViewModel,
    onOpenChecklist: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenStatus: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val s = state) {
            DashboardUiState.Loading -> LoadingBox()
            DashboardUiState.Failed -> ErrorBox(onRetry = viewModel::refresh)
            is DashboardUiState.Ready -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_drs_launcher),
                            contentDescription = stringResource(R.string.a11y_state_icon),
                            tint = stateColor(s.summary.overall),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(ResKeys.bannerRes(s.summary.overall)),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                SummaryCounts(s.summary)
                Button(onClick = onOpenChecklist, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.nav_checklist))
                }
                Button(onClick = onOpenStatus, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.nav_status))
                }
                Button(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.nav_privacy))
                }
            }
        }
    }
}

@Composable
private fun SummaryCounts(summary: ChecklistSummaryUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CountRow(stringResource(R.string.state_pass), summary.pass)
            CountRow(stringResource(R.string.state_warning), summary.warning)
            CountRow(stringResource(R.string.state_unavailable), summary.unavailable)
            CountRow(stringResource(R.string.state_notchecked), summary.notChecked)
        }
    }
}

@Composable
private fun CountRow(label: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(count.toString(), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ChecklistScreen(viewModel: ChecklistViewModel) {
    val state by viewModel.state.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.nav_checklist),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(12.dp))
        when (val s = state) {
            DashboardUiState.Loading -> LoadingBox()
            DashboardUiState.Failed -> ErrorBox(onRetry = viewModel::refresh)
            is DashboardUiState.Ready -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.items, key = { it.id }) { item ->
                    CheckRow(item)
                }
            }
        }
    }
}

@Composable
private fun CheckRow(item: CheckItemUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_drs_launcher),
                    contentDescription = stringResource(R.string.a11y_state_icon),
                    tint = stateColor(item.state),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    stringResource(ResKeys.titleRes(item.id)),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(ResKeys.stateRes(item.state)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(
                stringResource(ResKeys.detailResFor(item.id, detailSuffix(item.state))),
                style = MaterialTheme.typography.bodyMedium
            )
            item.evidenceLines.forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun detailSuffix(state: CheckState): String = when (state) {
    CheckState.PASS -> "pass"
    CheckState.WARNING -> "warning"
    CheckState.UNAVAILABLE -> "unavailable"
    CheckState.NOT_CHECKED -> "notchecked"
}

@Composable
fun LoadingBox() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ErrorBox(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.error_generic), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
