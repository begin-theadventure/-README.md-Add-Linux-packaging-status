package com.darkrockstudios.apps.hammer.common.util

import android.content.Context
import dev.icerock.moko.resources.StringResource

actual class StrRes(private val context: Context) {
	private val res = context.resources

	actual fun get(str: StringResource) = res.getString(str.resourceId)
	actual fun get(str: StringResource, vararg args: Any) = res.getString(str.resourceId, *args)
}