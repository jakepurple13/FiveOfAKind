package com.programmersbox.common

import androidx.compose.runtime.Composable

public actual fun getPlatformName(): String {
    return "FiveOfAKind"
}

@Composable
public fun UIShow() {
    App()
}