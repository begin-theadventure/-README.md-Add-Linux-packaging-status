package com.darkrockstudios.apps.hammer.common.projectselection

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.darkrockstudios.apps.hammer.MR
import com.darkrockstudios.apps.hammer.common.components.projectselection.ProjectSelection
import com.darkrockstudios.apps.hammer.common.compose.moko.get

fun getLocationIcon(location: ProjectSelection.Locations): ImageVector {
	return when (location) {
		ProjectSelection.Locations.Projects -> Icons.Filled.LibraryBooks
		ProjectSelection.Locations.Settings -> Icons.Filled.Settings
	}
}

@ExperimentalMaterialApi
@ExperimentalComposeApi
@Composable
fun ProjectSelectionUi(
	component: ProjectSelection,
	modifier: Modifier = Modifier
) {
	val slot by component.slot.subscribeAsState()

	when (val destination = slot.child?.instance) {
		is ProjectSelection.Destination.AccountSettingsDestination -> AccountSettingsUi(destination.component, modifier)
		is ProjectSelection.Destination.ProjectsListDestination -> ProjectListUi(destination.component, modifier)
		else -> throw IllegalArgumentException("Child cannot be null")
	}
}

@ExperimentalMaterialApi
@ExperimentalComposeApi
@Composable
fun ProjectSelectionFab(
	component: ProjectSelection,
) {
	val slot by component.slot.subscribeAsState()

	when (val destination = slot.child?.instance) {
		is ProjectSelection.Destination.AccountSettingsDestination -> {
			/* none */
		}

		is ProjectSelection.Destination.ProjectsListDestination -> {
			FloatingActionButton(onClick = { destination.component.showCreate() }) {
				Icon(imageVector = Icons.Filled.Create, MR.strings.projects_list_create_button.get())
			}
		}

		else -> throw IllegalArgumentException("Child cannot be null")
	}
}