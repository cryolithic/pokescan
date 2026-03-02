package com.pokescan.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.pokescan.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection Stats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Overview summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Style,
                    label = "Total Cards",
                    value = uiState.totalCards.toString(),
                    color = MaterialTheme.colorScheme.primary,
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Inventory2,
                    label = "Unique Sets",
                    value = uiState.uniqueSets.toString(),
                    color = Color(0xFF1976D2),
                )
            }

            if (uiState.totalValue != null && uiState.totalValue > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Collection Value",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                currencyFormat.format(uiState.totalValue),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Cards by set
            if (uiState.cardsBySet.isNotEmpty()) {
                DistributionCard(
                    title = "Cards by Set",
                    icon = Icons.Default.CollectionsBookmark,
                    items = uiState.cardsBySet.map { it.setName to it.count },
                    totalCount = uiState.totalCards,
                    colorProvider = { index ->
                        listOf(
                            Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00),
                            Color(0xFF7B1FA2), Color(0xFF00796B), Color(0xFFD32F2F),
                        ).getOrElse(index) { Color(0xFF607D8B) }
                    },
                )
            }

            // Cards by type
            if (uiState.cardsByType.isNotEmpty()) {
                DistributionCard(
                    title = "Cards by Type",
                    icon = Icons.Default.Whatshot,
                    items = uiState.cardsByType.map { tc ->
                        val types = runCatching {
                            Gson().fromJson(tc.types, Array<String>::class.java)?.joinToString(", ") ?: tc.types
                        }.getOrDefault(tc.types)
                        types to tc.count
                    },
                    totalCount = uiState.totalCards,
                    colorProvider = { index ->
                        val typeColors = listOf(
                            TypeFire, TypeWater, TypeGrass, TypeLightning, TypePsychic,
                            TypeFighting, TypeDarkness, TypeMetal, TypeDragon, TypeFairy,
                        )
                        typeColors.getOrElse(index) { TypeColorless }
                    },
                )
            }

            // Cards by rarity
            if (uiState.cardsByRarity.isNotEmpty()) {
                DistributionCard(
                    title = "Cards by Rarity",
                    icon = Icons.Default.Star,
                    items = uiState.cardsByRarity.map { it.rarity to it.count },
                    totalCount = uiState.totalCards,
                    colorProvider = { index ->
                        listOf(
                            Color(0xFFFFD700), Color(0xFFC0C0C0), Color(0xFFCD7F32),
                            Color(0xFF9C27B0), Color(0xFF2196F3),
                        ).getOrElse(index) { Color(0xFF607D8B) }
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DistributionCard(
    title: String,
    icon: ImageVector,
    items: List<Pair<String, Int>>,
    totalCount: Int,
    colorProvider: (Int) -> Color,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))

            items.take(10).forEachIndexed { index, (label, count) ->
                val fraction = if (totalCount > 0) count.toFloat() / totalCount else 0f
                val color = colorProvider(index)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label.take(24),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(8.dp))
                    // Bar
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp),
                    )
                }
            }
        }
    }
}
