package com.programmersbox.common

import androidx.compose.runtime.Composable
import moe.tlaster.precompose.PreComposeApp

@Composable
public fun App() {
    PreComposeApp {
        YahtzeeScreen()
    }
}