package com.docscanner.ui.common

import android.content.Context
import androidx.annotation.StringRes

/**
 * A user-facing message emitted by a ViewModel as a string resource reference,
 * so the text is resolved (and localized) at display time in the UI layer.
 * ViewModels must not bake English literals into UiState.
 */
data class UiMessage(
    @StringRes val resId: Int,
    val args: List<Any> = emptyList()
) {
    fun resolve(context: Context): String =
        if (args.isEmpty()) context.getString(resId)
        else context.getString(resId, *args.toTypedArray())
}
