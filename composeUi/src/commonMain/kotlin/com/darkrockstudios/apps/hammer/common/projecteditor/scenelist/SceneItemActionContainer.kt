package com.darkrockstudios.apps.hammer.common.projecteditor.scenelist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.darkrockstudios.apps.hammer.common.data.SceneItem

@Composable
expect fun SceneItemActionContainer(
	scene: SceneItem,
	onSceneAltClick: (scene: SceneItem) -> Unit,
	itemContent: @Composable (modifier: Modifier) -> Unit
)