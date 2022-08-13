package com.darkrockstudios.apps.hammer.common.projecteditor.sceneeditor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.darkrockstudios.apps.hammer.common.data.PlatformRichText
import com.darkrockstudios.apps.hammer.common.data.SceneBuffer
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.di.HammerComponent

interface SceneEditor : HammerComponent {
    val state: Value<State>
    var lastDiscarded: MutableValue<Long>

    fun addEditorMenu()
    fun removeEditorMenu()
    fun loadSceneContent()
    fun storeSceneContent(): Boolean
    fun onContentChanged(content: PlatformRichText)
    fun beginSceneNameEdit()
    fun endSceneNameEdit()
    fun changeSceneName(newName: String)

    data class State(
        val sceneItem: SceneItem,
        val sceneBuffer: SceneBuffer? = null,
        val isEditingName: Boolean = false
    )
}