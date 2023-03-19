package com.darkrockstudios.apps.hammer.common.compose

import androidx.compose.runtime.Composable

@Composable
expect fun MpDialog(
	onCloseRequest: () -> Unit,
	visible: Boolean,
	title: String,
	content: @Composable () -> Unit
)