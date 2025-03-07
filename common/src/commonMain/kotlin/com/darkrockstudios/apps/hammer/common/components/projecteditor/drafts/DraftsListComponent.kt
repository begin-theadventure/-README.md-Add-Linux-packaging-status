package com.darkrockstudios.apps.hammer.common.components.projecteditor.drafts

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.getAndUpdate
import com.darkrockstudios.apps.hammer.common.components.ProjectComponentBase
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.data.drafts.DraftDef
import com.darkrockstudios.apps.hammer.common.data.drafts.SceneDraftRepository
import com.darkrockstudios.apps.hammer.common.data.projectInject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DraftsListComponent(
	componentContext: ComponentContext,
	private val sceneItem: SceneItem,
	private val closeDrafts: () -> Unit,
	private val compareDraft: (sceneDef: SceneItem, draftDef: DraftDef) -> Unit
) : ProjectComponentBase(sceneItem.projectDef, componentContext), DraftsList {

	private val draftsRepository: SceneDraftRepository by projectInject()

	private val _state = MutableValue(
		DraftsList.State(
			sceneItem = sceneItem,
			drafts = emptyList()
		)
	)
	override val state: Value<DraftsList.State> = _state

	override fun loadDrafts() {
		scope.launch(dispatcherDefault) {
			val drafts = draftsRepository.findDrafts(sceneItem.id)

			withContext(dispatcherMain) {
				_state.getAndUpdate {
					it.copy(drafts = drafts)
				}
			}
		}
	}

	override fun selectDraft(draftDef: DraftDef) {
		compareDraft(sceneItem, draftDef)
	}

	override fun cancel() {
		closeDrafts()
	}
}