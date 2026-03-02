package com.pokescan.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokescan.data.db.RarityCount
import com.pokescan.data.db.SetCount
import com.pokescan.data.db.TypeCount
import com.pokescan.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class StatsUiState(
    val totalCards: Int = 0,
    val totalValue: Double? = null,
    val uniqueSets: Int = 0,
    val cardsBySet: List<SetCount> = emptyList(),
    val cardsByType: List<TypeCount> = emptyList(),
    val cardsByRarity: List<RarityCount> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: CardRepository,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.getCardCount(),
        repository.getTotalCollectionValue(),
        repository.getUniqueSetCount(),
        repository.getCardCountBySet(),
        repository.getCardCountByType(),
        repository.getCardCountByRarity(),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        StatsUiState(
            totalCards = values[0] as Int,
            totalValue = values[1] as Double?,
            uniqueSets = values[2] as Int,
            cardsBySet = values[3] as List<SetCount>,
            cardsByType = values[4] as List<TypeCount>,
            cardsByRarity = values[5] as List<RarityCount>,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}
