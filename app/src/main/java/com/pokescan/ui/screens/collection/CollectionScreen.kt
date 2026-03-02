package com.pokescan.ui.screens.collection

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pokescan.data.db.CardEntity
import com.pokescan.data.repository.SortOrder
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToStats: () -> Unit,
    onCardClick: (String) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (uiState.isSearching) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onClose = { viewModel.setSearching(false) },
                )
            } else {
                CollectionTopBar(
                    cardCount = uiState.cardCount,
                    totalValue = uiState.totalValue,
                    onSearchClick = { viewModel.setSearching(true) },
                    onSortClick = { showSortMenu = true },
                    onStatsClick = onNavigateToStats,
                    currentSort = uiState.sortOrder,
                    showSortMenu = showSortMenu,
                    onSortMenuDismiss = { showSortMenu = false },
                    onSortSelected = { order ->
                        viewModel.setSortOrder(order)
                        showSortMenu = false
                    },
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCamera,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan Card")
            }
        },
    ) { padding ->
        if (uiState.cards.isEmpty()) {
            EmptyCollectionPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onScanClick = onNavigateToCamera,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.cards, key = { it.id }) { card ->
                    CardGridItem(
                        card = card,
                        onClick = { onCardClick(card.id) },
                        onDelete = { viewModel.deleteCard(card.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionTopBar(
    cardCount: Int,
    totalValue: Double?,
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    onStatsClick: () -> Unit,
    currentSort: SortOrder,
    showSortMenu: Boolean,
    onSortMenuDismiss: () -> Unit,
    onSortSelected: (SortOrder) -> Unit,
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    TopAppBar(
        title = {
            Column {
                Text(
                    "PokeScan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (totalValue != null && cardCount > 0) {
                    Text(
                        "$cardCount cards · ${currencyFormat.format(totalValue)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                } else if (cardCount > 0) {
                    Text(
                        "$cardCount cards",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            Box {
                IconButton(onClick = onSortClick) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = onSortMenuDismiss) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = { onSortSelected(order) },
                            leadingIcon = {
                                if (order == currentSort) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onStatsClick) {
                Icon(Icons.Default.BarChart, contentDescription = "Stats")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search cards...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.surface,
            ),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onClose) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close search",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun CardGridItem(
    card: CardEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = card.imageUrlSmall,
                    contentDescription = card.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.716f) // Standard TCG card ratio
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                )
                if (card.quantity > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "×${card.quantity}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = card.setName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (card.marketPrice != null) {
                    Text(
                        text = currencyFormat.format(card.marketPrice),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Card") },
            text = { Text("Remove ${card.name} from your collection?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EmptyCollectionPlaceholder(
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Style,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No cards yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Scan your first Pokemon card\nwith the camera",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onScanClick) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan a Card")
        }
    }
}

private val SortOrder.label: String
    get() = when (this) {
        SortOrder.DATE_ADDED -> "Date Added"
        SortOrder.NAME -> "Name"
        SortOrder.VALUE -> "Value"
        SortOrder.SET -> "Set"
        SortOrder.TYPE -> "Type"
        SortOrder.RARITY -> "Rarity"
    }
