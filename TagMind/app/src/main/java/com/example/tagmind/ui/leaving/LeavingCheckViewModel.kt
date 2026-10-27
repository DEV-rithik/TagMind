package com.example.tagmind.ui.leaving

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tagmind.TagMindApplication
import com.example.tagmind.data.db.entity.ItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class LeavingCheckUiState(
    val items: List<ItemEntity> = emptyList(),
    val packedIds: Set<String> = emptySet(),
    /** Name of the item whose tag was scanned last, shown as brief feedback. */
    val lastScannedName: String? = null,
) {
    val allPacked: Boolean get() = items.isNotEmpty() && items.all { it.id in packedIds }
}

/**
 * Drives the "Leaving?" checklist. An item is ticked either by tapping it
 * or by tapping its NFC tag with the phone. State is kept only while the
 * screen is open, so every departure starts with a fresh list.
 */
class LeavingCheckViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as TagMindApplication).repository

    private val packed = MutableStateFlow<Set<String>>(emptySet())
    private val lastScanned = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LeavingCheckUiState> = combine(
        repo.getAllItems(),
        packed,
        lastScanned,
    ) { items, packedIds, last ->
        LeavingCheckUiState(
            items = items,
            packedIds = packedIds.intersect(items.map { it.id }.toSet()),
            lastScannedName = last,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeavingCheckUiState())

    fun toggle(itemId: String) {
        packed.update { if (itemId in it) it - itemId else it + itemId }
    }

    fun reset() {
        packed.value = emptySet()
        lastScanned.value = null
    }

    fun onTagScanned(uid: String) {
        val item = uiState.value.items.firstOrNull { it.nfcUid == uid }
        if (item == null) {
            lastScanned.value = null
            return
        }
        packed.update { it + item.id }
        lastScanned.value = item.name
    }
}
