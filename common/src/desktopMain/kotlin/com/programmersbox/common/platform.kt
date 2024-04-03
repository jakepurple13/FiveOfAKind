package com.programmersbox.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

public actual fun getPlatformName(): String {
    return "FiveOfAKind"
}

@Composable
public fun UIShow() {
    App(
        settings = remember { Settings { Settings.DATASTORE_FILE_NAME } }
    )
}