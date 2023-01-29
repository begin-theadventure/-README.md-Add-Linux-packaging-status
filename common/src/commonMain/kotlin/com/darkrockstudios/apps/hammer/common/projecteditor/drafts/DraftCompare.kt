package com.darkrockstudios.apps.hammer.common.projecteditor.drafts

import com.arkivanov.decompose.value.Value
import com.darkrockstudios.apps.hammer.common.data.PlatformRichText
import com.darkrockstudios.apps.hammer.common.data.SceneContent
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.data.drafts.DraftDef
import com.darkrockstudios.apps.hammer.common.dependencyinjection.HammerComponent

interface DraftCompare : HammerComponent {
	val sceneItem: SceneItem
	val draftDef: DraftDef

	val state: Value<State>

	fun loadContents()
	fun onContentChanged(content: PlatformRichText)
	fun pickDraft()
	fun pickMerged()

	fun cancel()

	data class State(
		val sceneItem: SceneItem,
		val draftDef: DraftDef,
		val sceneContent: SceneContent? = null,
		val draftContent: SceneContent? = null
	)
}