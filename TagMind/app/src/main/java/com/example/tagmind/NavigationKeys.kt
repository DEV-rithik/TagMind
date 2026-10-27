package com.example.tagmind

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data class ItemDetailKey(val itemId: String) : NavKey

@Serializable
data object ScanKey : NavKey

@Serializable
data object LeavingCheckKey : NavKey

@Serializable
data object SettingsKey : NavKey
