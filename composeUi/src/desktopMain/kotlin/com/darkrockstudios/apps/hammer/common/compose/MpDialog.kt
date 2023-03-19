package com.darkrockstudios.apps.hammer.common.compose

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import com.darkrockstudios.apps.hammer.common.compose.theme.AppTheme
import com.jthemedetecor.OsThemeDetector

@Composable
actual fun MpDialog(
	onCloseRequest: () -> Unit,
	visible: Boolean,
	title: String,
	content: @Composable () -> Unit
) {
	val osThemeDetector = remember { OsThemeDetector.getDetector() }

	// TODO This is crashing on desktop sometimes, look into it
	if (visible) {
		val state = rememberDialogState(size = DpSize.Unspecified)
		Dialog(
			onCloseRequest = onCloseRequest,
			visible = visible,
			title = title,
			state = state
		) {
			var darkMode by remember { mutableStateOf(osThemeDetector.isDark) }
			osThemeDetector.registerListener { isDarkModeEnabled ->
				darkMode = isDarkModeEnabled
			}

			AppTheme(useDarkTheme = darkMode) {
				Surface(
					modifier = Modifier.width(IntrinsicSize.Min)
						.height(IntrinsicSize.Min)
					//.wrapContentSize()
				) {
					content()
				}
			}
		}
	}
}