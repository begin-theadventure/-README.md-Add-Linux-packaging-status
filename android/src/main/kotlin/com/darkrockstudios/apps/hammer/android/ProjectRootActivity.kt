package com.darkrockstudios.apps.hammer.android

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.getAndUpdate
import com.darkrockstudios.apps.hammer.common.components.projectroot.CloseConfirm
import com.darkrockstudios.apps.hammer.common.components.projectroot.ProjectRoot
import com.darkrockstudios.apps.hammer.common.components.projectroot.ProjectRootComponent
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.compose.moko.get
import com.darkrockstudios.apps.hammer.common.compose.theme.AppTheme
import com.darkrockstudios.apps.hammer.common.data.MenuDescriptor
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.closeProjectScope
import com.darkrockstudios.apps.hammer.common.data.globalsettings.GlobalSettingsRepository
import com.darkrockstudios.apps.hammer.common.data.globalsettings.UiTheme
import com.darkrockstudios.apps.hammer.common.data.openProjectScope
import com.darkrockstudios.apps.hammer.common.dependencyinjection.ProjectDefScope
import com.darkrockstudios.apps.hammer.common.injectMainDispatcher
import com.darkrockstudios.apps.hammer.common.projectroot.ProjectRootUi
import com.darkrockstudios.apps.hammer.common.projectroot.getDestinationIcon
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.LocalImageLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.component.getScopeId
import org.koin.java.KoinJavaComponent.getKoin

class ProjectRootActivity : AppCompatActivity() {

	private val imageLoader: ImageLoader by inject()
	private val globalSettingsRepository: GlobalSettingsRepository by inject()
	private val mainDispatcher by injectMainDispatcher()
	private val globalSettings = MutableValue(globalSettingsRepository.globalSettings)
	private var settingsUpdateJob: Job? = null

	private val viewModel: ProjectRootViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val projectDef = intent.getParcelableExtra<ProjectDef>(EXTRA_PROJECT)
		if (projectDef == null) {
			finish()
		} else {
			viewModel.setProjectDef(projectDef)

			val menu = MutableValue(setOf<MenuDescriptor>())
			val component = ProjectRootComponent(
				componentContext = defaultComponentContext(),
				projectDef = projectDef,
				addMenu = { menuDescriptor ->
					menu.value = mutableSetOf(menuDescriptor).apply { add(menuDescriptor) }
				},
				removeMenu = { menuId ->
					menu.value = menu.value.filter { it.id != menuId }.toSet()
				}
			)

			setContent {
				CompositionLocalProvider(LocalImageLoader provides imageLoader) {
					val settingsState by globalSettings.subscribeAsState()
					val isDark = when (settingsState.uiTheme) {
						UiTheme.Light -> false
						UiTheme.Dark -> true
						UiTheme.FollowSystem -> isSystemInDarkTheme()
					}

					AppTheme(isDark) {
						Content(projectDef, component, menu)
					}
				}
			}
		}
	}

	override fun onStart() {
		super.onStart()

		settingsUpdateJob = lifecycleScope.launch {
			globalSettingsRepository.globalSettingsUpdates.collect { settings ->
				withContext(mainDispatcher) {
					globalSettings.getAndUpdate { settings }
				}
			}
		}
	}

	override fun onStop() {
		super.onStop()
		settingsUpdateJob?.cancel()
		settingsUpdateJob = null
	}

	@OptIn(ExperimentalMaterial3Api::class)
	@Composable
	private fun Content(
		projectDef: ProjectDef,
		component: ProjectRootComponent,
		menu: MutableValue<Set<MenuDescriptor>>
	) {
		val scope = rememberCoroutineScope()
		val drawerState = rememberDrawerState(DrawerValue.Closed)

		val router by component.routerState.subscribeAsState()
		val showBack = !component.isAtRoot()
		val shouldConfirmClose by component.closeRequestHandlers.subscribeAsState()
		val backEnabled by component.backEnabled.subscribeAsState()
		val destinationTypes = remember { ProjectRoot.DestinationTypes.values() }

		BackHandler(enabled = backEnabled) {
			component.requestClose()
		}

		Scaffold(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.background),
			topBar = {
				SetStatusBar()
				TopBar(
					title = projectDef.name,
					drawerOpen = drawerState,
					showBack = showBack,
					onButtonClicked = {
						if (showBack) {
							onBackPressed()
						} else {
							scope.launch {
								if (drawerState.isOpen) {
									drawerState.close()
								} else {
									drawerState.open()
								}
							}
						}
					},
					actions = {
						if (menu.value.isNotEmpty()) {
							TopAppBarDropdownMenu(menu.value.toList())
						}
					}
				)
			},
			content = { innerPadding ->
				ModalNavigationDrawer(
					modifier = Modifier.padding(innerPadding),
					drawerState = drawerState,
					drawerContent = {
						ModalDrawerSheet(modifier = Modifier.width(Ui.NAV_DRAWER)) {
							Spacer(Modifier.height(12.dp))
							destinationTypes.forEach { item ->
								NavigationDrawerItem(
									icon = {
										Icon(
											imageVector = getDestinationIcon(item),
											contentDescription = item.text.get()
										)
									},
									label = { Text(item.text.get()) },
									selected = router.active.instance.getLocationType() == item,
									onClick = {
										scope.launch { drawerState.close() }
										component.showDestination(item)
									}
								)
							}
						}
					},
					content = {
						ProjectRootUi(component, R.drawable::class)
					}
				)
			}
		)

		if (shouldConfirmClose.isNotEmpty()) {
			val item = shouldConfirmClose.first()
			when (item) {
				CloseConfirm.Scenes -> {
					confirmUnsavedScenesDialog(component)
				}

				CloseConfirm.Notes -> {
					confirmCloseUnsavedNotesDialog(component)
				}

				CloseConfirm.Encyclopedia -> {
					confirmCloseUnsavedEncyclopediaDialog(component)
				}

				CloseConfirm.Sync -> {
					component.showProjectSync()
				}

				CloseConfirm.Complete -> {
					finish()
				}
			}
		}
	}

	companion object {
		const val EXTRA_PROJECT = "project"
	}
}

class ProjectRootViewModel : ViewModel() {

	private var projectDef: ProjectDef? = null
	fun setProjectDef(project: ProjectDef) {
		if (projectDef == null) {
			projectDef = project
			runBlocking { openProjectScope(project) }
		}
	}

	override fun onCleared() {
		projectDef?.let {
			closeProjectScope(getKoin().getScope(ProjectDefScope(it).getScopeId()), it)
		}
	}
}