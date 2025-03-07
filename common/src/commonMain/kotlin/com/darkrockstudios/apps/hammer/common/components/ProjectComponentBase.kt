package com.darkrockstudios.apps.hammer.common.components

import com.arkivanov.decompose.ComponentContext
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.ProjectScoped
import com.darkrockstudios.apps.hammer.common.dependencyinjection.ProjectDefScope

abstract class ProjectComponentBase(
	val projectDef: ProjectDef,
	componentContext: ComponentContext
) : ComponentBase(componentContext), ProjectScoped {
	override val projectScope = ProjectDefScope(projectDef)
}