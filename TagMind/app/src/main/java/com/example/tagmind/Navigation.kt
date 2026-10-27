package com.example.tagmind

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.tagmind.ui.home.HomeScreen
import com.example.tagmind.ui.itemdetail.ItemDetailScreen
import com.example.tagmind.ui.leaving.LeavingCheckScreen
import com.example.tagmind.ui.scan.ScanScreen

@Composable
fun MainNavigation(
    initialAlarmItemId: String? = null,
    pendingNfcUid: String? = null,
    onAlarmOpened: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(HomeKey)

    // If launched from an alarm notification, navigate to the relevant item detail
    LaunchedEffect(initialAlarmItemId) {
        if (initialAlarmItemId != null) {
            val top = backStack.lastOrNull()
            if (!(top is ItemDetailKey && top.itemId == initialAlarmItemId)) {
                backStack.add(ItemDetailKey(itemId = initialAlarmItemId))
            }
            onAlarmOpened()
        }
    }

    LaunchedEffect(pendingNfcUid) {
        val top = backStack.lastOrNull()
        if (pendingNfcUid != null && top !is ItemDetailKey && top != ScanKey && top != LeavingCheckKey) {
            backStack.add(ScanKey)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeScreen(
                    onItemClick = { itemId -> backStack.add(ItemDetailKey(itemId = itemId)) },
                    onScanClick = { backStack.add(ScanKey) },
                    onLeavingCheckClick = { backStack.add(LeavingCheckKey) },
                )
            }
            entry<ScanKey> {
                ScanScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onItemRegistered = { itemId ->
                        backStack.removeLastOrNull()
                        backStack.add(ItemDetailKey(itemId = itemId))
                    },
                )
            }
            entry<ItemDetailKey> { key ->
                ItemDetailScreen(
                    itemId = key.itemId,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<LeavingCheckKey> {
                LeavingCheckScreen(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
