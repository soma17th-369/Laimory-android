package com.soma369.laimory.feature.feature1.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.Feature1Item
import com.soma369.laimory.feature.feature1.state.Feature1UiIntent
import com.soma369.laimory.feature.feature1.viewmodel.Feature1ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Feature1Screen(
    onBack: () -> Unit,
    viewModel: Feature1ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbar.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Feature 1") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← 뒤로") }
                },
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(contentPadding = paddingValues) {
                items(state.items) { item ->
                    Feature1ItemCard(item = item)
                }
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.sendIntent(Feature1UiIntent.TriggerServerError) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("서버 오류")
                        }
                        Button(
                            onClick = { viewModel.sendIntent(Feature1UiIntent.TriggerUnauthorizedError) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("인증 오류")
                        }
                        Button(
                            onClick = { viewModel.sendIntent(Feature1UiIntent.TriggerNetworkError) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("네트워크 오류")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Feature1ItemCard(item: Feature1Item) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
