package com.pokescan.ui.screens.carddetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.pokescan.data.db.CardEntity
import com.pokescan.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: String,
    onNavigateBack: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val conditions = listOf("NM", "LP", "MP", "HP", "DMG")

    LaunchedEffect(cardId) { viewModel.initialize(cardId) }

    val card = uiState.card
    if (card == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        card.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setEditing(!uiState.isEditing) }) {
                        Icon(
                            if (uiState.isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (uiState.isEditing) "Done" else "Edit",
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Card image hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = card.imageUrlLarge,
                    contentDescription = card.name,
                    modifier = Modifier
                        .padding(24.dp)
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Name + type badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(card.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${card.setName} · #${card.number}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TypeBadges(typesJson = card.types ?: "[]")
                }

                Spacer(Modifier.height(16.dp))

                // Price card
                PriceCard(
                    card = card,
                    isRefreshing = uiState.isRefreshingPrice,
                    refreshError = uiState.priceRefreshError,
                    onRefresh = viewModel::refreshPrice,
                )

                Spacer(Modifier.height(16.dp))

                // Collection details
                CollectionDetailsCard(
                    card = card,
                    isEditing = uiState.isEditing,
                    conditions = conditions,
                    onQuantityChange = viewModel::updateQuantity,
                    onConditionChange = viewModel::updateCondition,
                    onFoilToggle = viewModel::toggleFoil,
                    onNotesChange = viewModel::updateNotes,
                )

                Spacer(Modifier.height(16.dp))

                // Card info
                CardInfoSection(card = card)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Card") },
            text = { Text("Remove ${card.name} from your collection? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCard(onDeleted = onNavigateBack)
                }) {
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
private fun PriceCard(
    card: CardEntity,
    isRefreshing: Boolean,
    refreshError: String?,
    onRefresh: () -> Unit,
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Market Price", style = MaterialTheme.typography.titleSmall)
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh price", modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (card.marketPrice != null) {
                Text(
                    currencyFormat.format(card.marketPrice),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (card.lowPrice != null) {
                        Column {
                            Text("Low", style = MaterialTheme.typography.labelSmall)
                            Text(currencyFormat.format(card.lowPrice), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (card.highPrice != null) {
                        Column {
                            Text("High", style = MaterialTheme.typography.labelSmall)
                            Text(currencyFormat.format(card.highPrice), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (card.priceUpdatedAt != null) {
                    Text(
                        "Updated ${dateFormat.format(Date(card.priceUpdatedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    "Price not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onRefresh, enabled = !isRefreshing) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Fetch Price")
                }
            }

            if (refreshError != null) {
                Spacer(Modifier.height(4.dp))
                Text(refreshError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CollectionDetailsCard(
    card: CardEntity,
    isEditing: Boolean,
    conditions: List<String>,
    onQuantityChange: (Int) -> Unit,
    onConditionChange: (String) -> Unit,
    onFoilToggle: () -> Unit,
    onNotesChange: (String) -> Unit,
) {
    var notesText by remember(card.notes) { mutableStateOf(card.notes) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("My Copy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            // Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Quantity")
                if (isEditing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onQuantityChange(card.quantity - 1) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(card.quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { onQuantityChange(card.quantity + 1) }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                } else {
                    Text(card.quantity.toString(), fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Condition
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Condition")
                if (isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        conditions.forEach { cond ->
                            FilterChip(
                                selected = card.condition == cond,
                                onClick = { onConditionChange(cond) },
                                label = { Text(cond, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                } else {
                    ConditionBadge(card.condition)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Foil
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Foil / Holo")
                if (isEditing) {
                    Switch(checked = card.isFoil, onCheckedChange = { onFoilToggle() })
                } else {
                    Text(if (card.isFoil) "Yes" else "No", fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Notes
            Text("Notes")
            if (isEditing) {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it; onNotesChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add notes about this card...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(8.dp),
                )
            } else {
                Text(
                    card.notes.ifBlank { "No notes" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (card.notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun CardInfoSection(card: CardEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Card Info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            InfoRow("Type", card.supertype)
            if (!card.hp.isNullOrBlank()) InfoRow("HP", card.hp)
            if (!card.rarity.isNullOrBlank()) InfoRow("Rarity", card.rarity)
            InfoRow("Set", "${card.setName} (${card.setSeries})")
            InfoRow("Number", "#${card.number}")
            if (card.artist.isNotBlank()) InfoRow("Artist", card.artist)
            InfoRow("Added", SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(card.addedAt)))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

@Composable
private fun TypeBadges(typesJson: String) {
    val types = remember(typesJson) {
        runCatching {
            Gson().fromJson(typesJson, Array<String>::class.java)?.toList() ?: emptyList()
        }.getOrDefault(emptyList())
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        types.forEach { type ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = typeColor(type).copy(alpha = 0.15f),
            ) {
                Text(
                    type,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = typeColor(type),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ConditionBadge(condition: String) {
    val color = when (condition) {
        "NM" -> Color(0xFF4CAF50)
        "LP" -> Color(0xFF8BC34A)
        "MP" -> Color(0xFFFFC107)
        "HP" -> Color(0xFFFF9800)
        "DMG" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            condition,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun typeColor(type: String): Color = when (type.lowercase()) {
    "fire" -> TypeFire
    "water" -> TypeWater
    "grass" -> TypeGrass
    "lightning" -> TypeLightning
    "psychic" -> TypePsychic
    "fighting" -> TypeFighting
    "darkness" -> TypeDarkness
    "metal" -> TypeMetal
    "dragon" -> TypeDragon
    "fairy" -> TypeFairy
    else -> TypeColorless
}
