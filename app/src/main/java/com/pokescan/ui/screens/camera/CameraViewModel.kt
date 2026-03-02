package com.pokescan.ui.screens.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokescan.data.api.ApiCard
import com.pokescan.data.db.CardEntity
import com.pokescan.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CameraUiState {
    object Scanning : CameraUiState()
    object Processing : CameraUiState()
    data class SearchResults(val query: String, val results: List<ApiCard>) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
    data class CardAdded(val cardId: String) : CameraUiState()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: CardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Scanning)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _manualQuery = MutableStateFlow("")
    val manualQuery: StateFlow<String> = _manualQuery.asStateFlow()

    fun onTextDetected(text: String) {
        if (_uiState.value != CameraUiState.Scanning) return
        val cleaned = cleanOcrText(text)
        if (cleaned.length >= 3) {
            searchCards(cleaned)
        }
    }

    fun searchCards(query: String) {
        _manualQuery.value = query
        _uiState.value = CameraUiState.Processing
        viewModelScope.launch {
            repository.searchOnlineCards(query)
                .onSuccess { cards ->
                    _uiState.value = if (cards.isEmpty()) {
                        CameraUiState.Error("No cards found for \"$query\". Try a different search.")
                    } else {
                        CameraUiState.SearchResults(query = query, results = cards)
                    }
                }
                .onFailure { e ->
                    _uiState.value = CameraUiState.Error(e.message ?: "Failed to search cards.")
                }
        }
    }

    fun addCard(apiCard: ApiCard, quantity: Int = 1, condition: String = "NM", isFoil: Boolean = false) {
        viewModelScope.launch {
            val entity = repository.apiCardToEntity(apiCard, quantity, condition, isFoil)
            repository.addCard(entity)
            _uiState.value = CameraUiState.CardAdded(entity.id)
        }
    }

    fun resetToScanning() {
        _uiState.value = CameraUiState.Scanning
        _manualQuery.value = ""
    }

    fun setManualQuery(query: String) {
        _manualQuery.value = query
    }

    private fun cleanOcrText(text: String): String {
        // Extract the most likely card name from OCR text
        // Card names typically appear on the first line or in large text
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length >= 3 }

        // Heuristic: first non-empty line that looks like a name (not all digits, not HP, etc.)
        val nameLine = lines.firstOrNull { line ->
            !line.matches(Regex("^[0-9/HP ]+$")) &&
            !line.startsWith("HP") &&
            line.length <= 30
        }
        return nameLine ?: lines.firstOrNull() ?: text.take(30)
    }
}
