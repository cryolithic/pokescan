package com.pokescan.ui.screens.carddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokescan.data.db.CardEntity
import com.pokescan.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardDetailUiState(
    val card: CardEntity? = null,
    val isRefreshingPrice: Boolean = false,
    val priceRefreshError: String? = null,
    val isEditing: Boolean = false,
)

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val repository: CardRepository,
) : ViewModel() {

    private val _isRefreshingPrice = MutableStateFlow(false)
    private val _priceRefreshError = MutableStateFlow<String?>(null)
    private val _isEditing = MutableStateFlow(false)

    private var cardId: String = ""

    private lateinit var _cardFlow: Flow<CardEntity?>

    val uiState: StateFlow<CardDetailUiState> get() = _uiState
    private val _uiState = MutableStateFlow(CardDetailUiState())

    fun initialize(cardId: String) {
        if (this.cardId == cardId) return
        this.cardId = cardId

        viewModelScope.launch {
            combine(
                repository.getCardById(cardId),
                _isRefreshingPrice,
                _priceRefreshError,
                _isEditing,
            ) { card, refreshing, error, editing ->
                CardDetailUiState(card, refreshing, error, editing)
            }.collect { _uiState.value = it }
        }
    }

    fun refreshPrice() {
        _isRefreshingPrice.value = true
        _priceRefreshError.value = null
        viewModelScope.launch {
            repository.refreshCardPrices(cardId)
                .onFailure { _priceRefreshError.value = "Failed to fetch latest price." }
            _isRefreshingPrice.value = false
        }
    }

    fun updateQuantity(quantity: Int) {
        val card = _uiState.value.card ?: return
        viewModelScope.launch {
            repository.updateCard(card.copy(quantity = quantity.coerceAtLeast(1)))
        }
    }

    fun updateCondition(condition: String) {
        val card = _uiState.value.card ?: return
        viewModelScope.launch {
            repository.updateCard(card.copy(condition = condition))
        }
    }

    fun updateNotes(notes: String) {
        val card = _uiState.value.card ?: return
        viewModelScope.launch {
            repository.updateCard(card.copy(notes = notes))
        }
    }

    fun toggleFoil() {
        val card = _uiState.value.card ?: return
        viewModelScope.launch {
            repository.updateCard(card.copy(isFoil = !card.isFoil))
        }
    }

    fun setEditing(editing: Boolean) {
        _isEditing.value = editing
    }

    fun deleteCard(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
            onDeleted()
        }
    }
}
