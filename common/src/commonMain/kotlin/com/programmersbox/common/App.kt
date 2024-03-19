package com.programmersbox.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import moe.tlaster.precompose.PreComposeApp

@Composable
internal fun App() {
    PreComposeApp {
        YahtzeeScreen()
    }
}