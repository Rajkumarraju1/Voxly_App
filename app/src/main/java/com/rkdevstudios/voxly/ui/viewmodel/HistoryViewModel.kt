package com.rkdevstudios.voxly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkdevstudios.voxly.data.model.Call
import com.rkdevstudios.voxly.data.repository.CallRepository
import com.rkdevstudios.voxly.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val callRepository: com.rkdevstudios.voxly.data.repository.CallRepositoryImpl, // Use Impl for new methods
    private val userRepository: UserRepository
) : ViewModel() {

    private val _history = MutableStateFlow<List<Call>>(emptyList())
    val history: StateFlow<List<Call>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Pagination State
    private var lastVisible: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isEndReached = false

    // Selection State
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()

    init {
        // Auto-Prune on init to keep DB clean
        pruneOldHistory()
        loadHistory(reset = true)
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedItems.value = emptySet()
        }
    }

    fun toggleSelection(callId: String) {
        val current = _selectedItems.value.toMutableSet()
        if (current.contains(callId)) {
            current.remove(callId)
        } else {
            current.add(callId)
        }
        _selectedItems.value = current
        
        // Auto-exit selection mode if empty? Optional.
        if (current.isEmpty() && _isSelectionMode.value) {
            // _isSelectionMode.value = false // Keep active or close? Let's keep active for now.
        }
    }

    fun deleteSelectedItems() {
        val toDelete = _selectedItems.value.toList()
        if (toDelete.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                callRepository.deleteCalls(toDelete)
                // Remove from local list
                _history.value = _history.value.filter { it.id !in toDelete }
                toggleSelectionMode() // Exit selection mode
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHistory(reset: Boolean = false) {
        if (_isLoading.value) return
        if (reset) {
            lastVisible = null
            isEndReached = false
            _history.value = emptyList()
        }
        if (isEndReached && !reset) return

        val userId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Use new Paged method
                val result = callRepository.getCallHistoryPaged(userId, lastVisible)
                val newCalls = result.first
                lastVisible = result.second
                
                if (newCalls.isEmpty() || lastVisible == null) {
                    isEndReached = true
                } else {
                    if (reset) {
                        _history.value = newCalls
                    } else {
                        // Append and Distinct
                        val current = _history.value
                        val combined = (current + newCalls).distinctBy { it.id }
                        _history.value = combined
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMore() {
        loadHistory(reset = false)
    }
    
    fun clearAllHistory() {
        val userId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
             _isLoading.value = true
             try {
                 callRepository.clearHistory(userId)
                 _history.value = emptyList()
                 lastVisible = null
                 isEndReached = true
             } catch (e: Exception) {
                 e.printStackTrace()
             } finally {
                 _isLoading.value = false
             }
        }
    }
    
    private fun pruneOldHistory() {
        val userId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                callRepository.pruneOldHistory(userId)
            } catch (e: Exception) {
                // Ignore silent failures
            }
        }
    }
}
