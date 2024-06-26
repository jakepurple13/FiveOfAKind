package com.programmersbox.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

public actual fun getPlatformName(): String {
    return "FiveOfAKind"
}

@Composable
public fun UIShow() {
    val context = LocalContext.current

    App(
        settings = remember { Settings { context.filesDir.resolve(Settings.DATASTORE_FILE_NAME).absolutePath } }
    )
}
