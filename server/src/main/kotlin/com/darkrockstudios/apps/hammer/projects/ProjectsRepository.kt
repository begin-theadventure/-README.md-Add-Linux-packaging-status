package com.darkrockstudios.apps.hammer.projects

import com.darkrockstudios.apps.hammer.dependencyinjection.PROJECTS_SYNC_MANAGER
import com.darkrockstudios.apps.hammer.getRootDataDirectory
import com.darkrockstudios.apps.hammer.project.InvalidSyncIdException
import com.darkrockstudios.apps.hammer.project.ProjectDefinition
import com.darkrockstudios.apps.hammer.project.ProjectRepository
import com.darkrockstudios.apps.hammer.project.ProjectsSyncData
import com.darkrockstudios.apps.hammer.syncsessionmanager.SyncSessionManager
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject

class ProjectsRepository(
	private val fileSystem: FileSystem,
	private val json: Json,
	private val clock: Clock
) {
	private val syncSessionManager: SyncSessionManager<Long, ProjectsSynchronizationSession> by inject(
		clazz = SyncSessionManager::class.java,
		qualifier = named(PROJECTS_SYNC_MANAGER)
	)

	fun createUserData(userId: Long) {
		val userDir = getUserDirectory(userId)

		fileSystem.createDirectories(userDir)

		val dataFile = userDir / DATA_FILE
		val data = defaultData(userId)
		val dataJson = json.encodeToString(data)
		fileSystem.write(dataFile) {
			writeUtf8(dataJson)
		}

		/*
		val projectsFile = userDir / PROJECT_FILE
		// Create the zip file with a place holder entry
		//val zipFs = fileSystem.openZip(projectsFile)
		val zipFile = File(projectsFile.toString())
		val out = ZipOutputStream(FileOutputStream(zipFile))
		val e = ZipEntry(".")
		out.putNextEntry(e)
		val placeHolderData = "\n".toByteArray()
		out.write(placeHolderData, 0, placeHolderData.size)
		out.closeEntry()
		out.close()
		*/
	}

	private fun getUserDirectory(userId: Long): Path {
		return getUserDirectory(userId, fileSystem)
	}

	suspend fun beginProjectsSync(userId: Long): Result<ProjectsBeginSyncData> {
		return if (syncSessionManager.hasActiveSyncSession(userId)) {
			Result.failure(IllegalStateException("User $userId already has a synchronization session"))
		} else {
			val newSyncId = syncSessionManager.createNewSession(userId) { user, sync ->
				ProjectsSynchronizationSession(
					userId = user,
					started = clock.now(),
					syncId = sync
				)
			}

			val projects = getProjects(userId)
			val deletedProjects = getDeletedProjects(userId)

			val data = ProjectsBeginSyncData(
				syncId = newSyncId,
				projects = projects,
				deletedProjects = deletedProjects
			)

			Result.success(data)
		}
	}

	suspend fun endProjectsSync(userId: Long, syncId: String) {
		val session = syncSessionManager.findSession(userId)
		if (session == null) {
			Result.failure(IllegalStateException("User $userId does not have a synchronization session"))
		} else {
			if (session.syncId != syncId) {
				Result.failure(IllegalStateException("Invalid sync id"))
			} else {
				syncSessionManager.terminateSession(userId)
				Result.success(true)
			}
		}
	}

	private fun getProjects(userId: Long): Set<ProjectDefinition> {
		val projectsDir = getUserDirectory(userId)
		return fileSystem.list(projectsDir)
			.filter { fileSystem.metadata(it).isDirectory }
			.filter { it.name.startsWith('.').not() }
			.map { path -> ProjectDefinition(path.name) }
			.toSet()
	}

	private fun getDeletedProjects(userId: Long): Set<String> {
		return loadSyncData(userId).deletedProjects
	}

	private fun loadSyncData(userId: Long): ProjectsSyncData {
		val path = getSyncDataPath(userId)
		return if (fileSystem.exists(path)) {
			fileSystem.read(path) {
				val syncDataJson = readUtf8()
				json.decodeFromString(syncDataJson)
			}
		} else {
			val data = defaultData(userId)
			saveSyncData(userId, data)
			data
		}
	}

	private fun saveSyncData(userId: Long, data: ProjectsSyncData) {
		val path = getSyncDataPath(userId)
		fileSystem.write(path) {
			val syncDataJson = json.encodeToString(data)
			writeUtf8(syncDataJson)
		}
	}

	private fun updateSyncData(userId: Long, action: (ProjectsSyncData) -> ProjectsSyncData): ProjectsSyncData {
		val data = loadSyncData(userId)
		val update = action(data)
		saveSyncData(userId, update)
		return update
	}

	private fun getSyncDataPath(userId: Long): Path = getUserDirectory(userId) / DATA_FILE

	suspend fun deleteProject(userId: Long, syncId: String, projectName: String): Result<Unit> {
		if (syncSessionManager.validateSyncId(userId, syncId, true)
				.not()
		) return Result.failure(InvalidSyncIdException())

		val projectDef = ProjectDefinition(projectName)
		val projectDir = ProjectRepository.getProjectDirectory(userId, projectDef, fileSystem)
		fileSystem.deleteRecursively(projectDir)

		updateSyncData(userId) { data ->
			data.copy(
				deletedProjects = data.deletedProjects + projectName
			)
		}

		return Result.success(Unit)
	}

	suspend fun createProject(userId: Long, syncId: String, projectName: String): Result<Unit> {
		if (syncSessionManager.validateSyncId(userId, syncId, true)
				.not()
		) return Result.failure(InvalidSyncIdException())

		updateSyncData(userId) { data ->
			data.copy(
				deletedProjects = data.deletedProjects - projectName
			)
		}

		return Result.success(Unit)
	}

	companion object {
		private const val DATA_DIRECTORY = "user_data"
		private const val DATA_FILE = "syncData.json"

		fun defaultData(userId: Long): ProjectsSyncData {
			return ProjectsSyncData(
				lastSync = Instant.DISTANT_PAST,
				deletedProjects = emptySet()
			)
		}

		fun getRootDirectory(fileSystem: FileSystem): Path = getRootDataDirectory(fileSystem) / DATA_DIRECTORY

		fun getUserDirectory(userId: Long, fileSystem: FileSystem): Path {
			val dir = getRootDirectory(fileSystem)
			return dir / userId.toString()
		}

		fun getSyncDataPath(userId: Long, fileSystem: FileSystem): Path =
			getUserDirectory(userId, fileSystem) / DATA_FILE
	}
}