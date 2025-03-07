package com.darkrockstudios.apps.hammer.common.projectselection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.darkrockstudios.apps.hammer.MR
import com.darkrockstudios.apps.hammer.common.components.projectselection.accountsettings.AccountSettings
import com.darkrockstudios.apps.hammer.common.compose.MpDialog
import com.darkrockstudios.apps.hammer.common.compose.SimpleConfirm
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.compose.moko.get
import com.darkrockstudios.apps.hammer.common.compose.moveFocusOnTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSetupDialog(
	component: AccountSettings,
	scope: CoroutineScope,
) {
	val state by component.state.subscribeAsState()

	val focusManager = LocalFocusManager.current

	var passwordVisible by rememberSaveable(state.serverSetup) { mutableStateOf(false) }
	var confirmDeleteLocal by rememberSaveable(state.serverSetup) { mutableStateOf<Boolean?>(null) }
	val existingServer = rememberSaveable(state.serverSetup) { state.serverWorking.not() && state.currentUrl != null }

	MpDialog(
		onCloseRequest = {
			component.cancelServerSetup()
		},
		visible = state.serverSetup,
		title = MR.strings.settings_server_setup_title.get(),
		size = DpSize(400.dp, 460.dp)
	) {
		Box(
			modifier = Modifier.padding(Ui.Padding.XL),
			contentAlignment = Alignment.Center
		) {
			if (state.serverWorking) {
				CircularProgressIndicator(
					modifier = Modifier.align(Alignment.Center).size(128.dp)
				)
			}

			Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
				Text(
					MR.strings.settings_server_setup_header.get(),
					style = MaterialTheme.typography.headlineMedium
				)

				//Divider(modifier = Modifier.fillMaxWidth())

				Spacer(modifier = Modifier.size(Ui.Padding.L))

				Row(verticalAlignment = Alignment.CenterVertically) {
					Checkbox(
						checked = state.serverSsl,
						onCheckedChange = { component.updateServerSsl(it) },
						enabled = state.serverWorking.not() && existingServer.not()
					)
					Text(MR.strings.settings_server_setup_ssl_label.get())
				}

				OutlinedTextField(
					value = state.serverUrl ?: "",
					onValueChange = { component.updateServerUrl(it) },
					label = { Text(MR.strings.settings_server_setup_url_hint.get()) },
					modifier = Modifier.moveFocusOnTab(),
					keyboardOptions = KeyboardOptions(
						autoCorrect = false,
						imeAction = ImeAction.Next,
						keyboardType = KeyboardType.Uri
					),
					keyboardActions = KeyboardActions(
						onNext = { focusManager.moveFocus(FocusDirection.Down) }
					),
					enabled = state.serverWorking.not() && existingServer.not()
				)

				OutlinedTextField(
					value = state.serverEmail ?: "",
					onValueChange = { component.updateServerEmail(it) },
					label = { Text(MR.strings.settings_server_setup_email_hint.get()) },
					modifier = Modifier.moveFocusOnTab(),
					keyboardOptions = KeyboardOptions(
						autoCorrect = false,
						imeAction = ImeAction.Next,
						keyboardType = KeyboardType.Email
					),
					keyboardActions = KeyboardActions(
						onNext = { focusManager.moveFocus(FocusDirection.Down) }
					),
					enabled = state.serverWorking.not() && existingServer.not()
				)

				OutlinedTextField(
					value = state.serverPassword ?: "",
					onValueChange = { component.updateServerPassword(it) },
					label = { Text(MR.strings.settings_server_setup_password_hint.get()) },
					singleLine = true,
					placeholder = { Text(MR.strings.settings_server_setup_password_hint.get()) },
					visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
					modifier = Modifier.moveFocusOnTab(),
					keyboardOptions = KeyboardOptions(
						autoCorrect = false,
						imeAction = ImeAction.Done,
						keyboardType = KeyboardType.Password
					),
					keyboardActions = KeyboardActions(
						onNext = { focusManager.moveFocus(FocusDirection.Down) },
					),
					trailingIcon = {
						val image = if (passwordVisible)
							Icons.Filled.Visibility
						else Icons.Filled.VisibilityOff

						// Please provide localized description for accessibility services
						val description = if (passwordVisible)
							MR.strings.settings_server_setup_password_hide.get()
						else
							MR.strings.settings_server_setup_password_show.get()

						IconButton(onClick = { passwordVisible = !passwordVisible }) {
							Icon(imageVector = image, description)
						}
					},
					enabled = state.serverWorking.not()
				)

				state.serverError?.let { error ->
					Text(
						error,
						color = MaterialTheme.colorScheme.error,
						style = MaterialTheme.typography.bodySmall,
						fontStyle = FontStyle.Italic
					)
				}

				Spacer(modifier = Modifier.size(Ui.Padding.L))

				Row(
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Button(
						onClick = {
							if (state.serverIsLoggedIn.not()) {
								confirmDeleteLocal = false
							} else {
								component.setupServer(
									ssl = state.serverSsl,
									url = state.serverUrl ?: "",
									email = state.serverEmail ?: "",
									password = state.serverPassword ?: "",
									create = false,
									removeLocalContent = false
								)
							}
						},
						enabled = state.serverWorking.not()
					) {
						Text(MR.strings.settings_server_setup_login_button.get())
					}

					if (state.serverIsLoggedIn.not()) {
						Button(
							onClick = { confirmDeleteLocal = true },
							enabled = state.serverWorking.not() && state.currentUrl == null
						) {
							Text(MR.strings.settings_server_setup_create_button.get())
						}
					}

					Button(
						onClick = {
							scope.launch {
								component.cancelServerSetup()
							}
						},
						enabled = state.serverWorking.not()
					) {
						Text(MR.strings.settings_server_setup_cancel_button.get())
					}
				}
			}
		}

		confirmDeleteLocal?.let { create ->
			fun setupServer(create: Boolean, removeLocal: Boolean) {
				component.setupServer(
					ssl = state.serverSsl,
					url = state.serverUrl ?: "",
					email = state.serverEmail ?: "",
					password = state.serverPassword ?: "",
					create = create,
					removeLocalContent = removeLocal
				)
			}

			SimpleConfirm(
				title = MR.strings.remove_local_dialog_title.get(),
				message = MR.strings.remove_local_dialog_message.get(),
				implicitCancel = false,
				onDismiss = {
					setupServer(create, false)
					confirmDeleteLocal = null
				},
				onConfirm = {
					setupServer(create, true)
					confirmDeleteLocal = null
				}
			)
		}
	}
}