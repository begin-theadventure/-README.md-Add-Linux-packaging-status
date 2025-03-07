package com.darkrockstudios.apps.hammer.common.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darkrockstudios.apps.hammer.MR
import com.darkrockstudios.apps.hammer.common.compose.moko.get
import com.darkrockstudios.apps.hammer.common.data.MenuItemDescriptor

@Composable
fun TopBar(
	modifier: Modifier = Modifier,
	title: State<String>,
	onClose: () -> Unit,
	menuItems: Set<MenuItemDescriptor>
) {
	Row(
		modifier = modifier.heightIn(min = Ui.TOP_BAR_HEIGHT).fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
	) {
		IconButton(
			modifier = Modifier.size(Ui.MIN_TOUCH_SIZE).padding(Ui.Padding.S),
			onClick = onClose,
		) {
			Icon(imageVector = Icons.Default.Close, contentDescription = MR.strings.scene_editor_menu_item_close.get())
		}

		Text(
			modifier = Modifier.weight(1f),
			text = title.value,
			textAlign = TextAlign.Center,
			fontWeight = FontWeight.Bold
		)

		TopAppBarDropdownMenu(menuItems = menuItems)
	}
}