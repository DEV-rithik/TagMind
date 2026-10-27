package com.example.tagmind.ui.main

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tagmind.MainActivity
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun scanButtonOpensNfcScreen() {
    composeTestRule.onNodeWithText("TagMind").assertExists()
    composeTestRule.onNodeWithText("Scan Item").performClick()
    composeTestRule.onNodeWithText("Hold near NFC tag").assertExists()
  }
}

