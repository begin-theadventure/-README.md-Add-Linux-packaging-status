package com.darkrockstudios.apps.hammer.projects

import com.darkrockstudios.apps.hammer.base.http.BeginProjectsSyncResponse
import com.darkrockstudios.apps.hammer.base.http.HEADER_SYNC_ID
import com.darkrockstudios.apps.hammer.base.http.HttpResponseError
import com.darkrockstudios.apps.hammer.plugins.ServerUserIdPrincipal
import com.darkrockstudios.apps.hammer.plugins.USER_AUTH
import com.darkrockstudios.apps.hammer.project.InvalidSyncIdException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

fun Application.projectsRoutes() {
	routing {
		authenticate(USER_AUTH) {
			route("/projects/{userId}") {
				beginProjectsSync()
				endProjectSync()
				deleteProject()
				createProject()
			}
		}
	}
}

private fun Route.beginProjectsSync() {
	val projectsRepository: ProjectsRepository = get()

	get("/begin_sync") {
		val principal = call.principal<ServerUserIdPrincipal>()!!

		val result = projectsRepository.beginProjectsSync(principal.id)
		if (result.isSuccess) {
			val syncData = result.getOrThrow()
			val responseData = BeginProjectsSyncResponse(
				syncId = syncData.syncId,
				projects = syncData.projects.map { it.name }.toSet(),
				deletedProjects = syncData.deletedProjects
			)
			call.respond(responseData)
		} else {
			val message = result.exceptionOrNull()?.message
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Header", message = message ?: "Unknown Error")
			)
		}
	}
}

private fun Route.endProjectSync() {
	val projectsRepository: ProjectsRepository = get()

	get("/end_sync") {
		val principal = call.principal<ServerUserIdPrincipal>()!!

		val syncId = call.request.headers[HEADER_SYNC_ID]

		if (syncId == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Header", message = "syncId was missing")
			)
		} else {
			projectsRepository.endProjectsSync(principal.id, syncId)
			call.respond("Okay")
		}
	}
}

private fun Route.deleteProject() {
	val projectsRepository: ProjectsRepository = get()

	get("/{projectName}/delete") {
		val principal = call.principal<ServerUserIdPrincipal>()!!
		val projectName = call.parameters["projectName"]
		val syncId = call.request.headers[HEADER_SYNC_ID]

		if (projectName == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Parameter", message = "projectName was missing")
			)
		} else if (syncId == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Header", message = "syncId was missing")
			)
		} else {
			val result = projectsRepository.deleteProject(principal.id, syncId, projectName)
			if (result.isSuccess) {
				call.respond("Success")
			} else {
				when (val e = result.exceptionOrNull()) {
					is InvalidSyncIdException -> {
						call.respond(
							status = HttpStatusCode.BadRequest,
							HttpResponseError(error = "Error", message = "Invalid sync ID")
						)
					}

					else -> {
						call.respond(
							status = HttpStatusCode.InternalServerError,
							HttpResponseError(error = "Error", message = e?.message ?: "Unknown failure")
						)
					}
				}
			}
		}
	}
}

private fun Route.createProject() {
	val projectsRepository: ProjectsRepository = get()

	get("/{projectName}/create") {
		val principal = call.principal<ServerUserIdPrincipal>()!!
		val projectName = call.parameters["projectName"]
		val syncId = call.request.headers[HEADER_SYNC_ID]

		if (projectName == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Parameter", message = "projectName was missing")
			)
		} else if (syncId == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Header", message = "syncId was missing")
			)
		} else {
			val result = projectsRepository.createProject(principal.id, syncId, projectName)
			if (result.isSuccess) {
				call.respond("Success")
			} else {
				when (val e = result.exceptionOrNull()) {
					is InvalidSyncIdException -> {
						call.respond(
							status = HttpStatusCode.BadRequest,
							HttpResponseError(error = "Error", message = "Invalid sync ID")
						)
					}

					else -> {
						call.respond(
							status = HttpStatusCode.InternalServerError,
							HttpResponseError(error = "Error", message = e?.message ?: "Unknown failure")
						)
					}
				}
			}
		}
	}
}