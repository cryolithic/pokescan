package com.pokescan.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokescan.data.db.CardEntity
import com.pokescan.data.repository.CardRepository
import com.pokescan.data.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val cards: List<CardEntity> = emptyList(),
    val sortOrder: SortOrder = SortOrder.DATE_ADDED,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val totalValue: Double? = null,
    val cardCount: Int = 0,
)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: CardRepository,
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val cards: StateFlow<List<CardEntity>> = combine(_sortOrder, _searchQuery) { sort, query ->
        sort to query
    }.flatMapLatest { (sort, query) ->
        if (query.isBlank()) repository.getCards(sort)
        else repository.searchCards(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<CollectionUiState> = combine(
        cards,
        _sortOrder,
        _searchQuery,
        _isSearching,
        repository.getTotalCollectionValue(),
        repository.getCardCount(),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CollectionUiState(
            cards = values[0] as List<CardEntity>,
            sortOrder = values[1] as SortOrder,
            searchQuery = values[2] as String,
            isSearching = values[3] as Boolean,
            totalValue = values[4] as Double?,
            cardCount = values[5] as Int,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionUiState())

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearching(searching: Boolean) {
        _isSearching.value = searching
        if (!searching) _searchQuery.value = ""
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }
}
