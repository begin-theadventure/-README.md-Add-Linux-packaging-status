package com.darkrockstudios.apps.hammer.common.data.projectsync.synchronizers

import com.darkrockstudios.apps.hammer.base.http.ApiProjectEntity
import com.darkrockstudios.apps.hammer.base.http.EntityHash
import com.darkrockstudios.apps.hammer.base.http.EntityType
import com.darkrockstudios.apps.hammer.base.http.synchronizer.EntityHasher
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.projectsync.EntitySynchronizer
import com.darkrockstudios.apps.hammer.common.data.projectsync.OnSyncLog
import com.darkrockstudios.apps.hammer.common.data.projectsync.syncLogE
import com.darkrockstudios.apps.hammer.common.data.projectsync.syncLogI
import com.darkrockstudios.apps.hammer.common.data.timelinerepository.TimeLineEvent
import com.darkrockstudios.apps.hammer.common.data.timelinerepository.TimeLineRepository
import com.darkrockstudios.apps.hammer.common.server.ServerProjectApi
import kotlinx.coroutines.flow.first

class ClientTimelineSynchronizer(
	projectDef: ProjectDef,
	serverProjectApi: ServerProjectApi,
	private val timeLineRepository: TimeLineRepository,
) : EntitySynchronizer<ApiProjectEntity.TimelineEventEntity>(projectDef, serverProjectApi) {

	override suspend fun prepareForSync() {
		timeLineRepository.loadTimeline()
		// Collect one to make sure its loaded
		timeLineRepository.timelineFlow.first()
	}

	override suspend fun ownsEntity(id: Int): Boolean {
		return timeLineRepository.getTimelineEvent(id) != null
	}

	override suspend fun getEntityHash(id: Int): String? {
		val event = timeLineRepository.getTimelineEvent(id)
		return if (event != null) {
			EntityHasher.hashTimelineEvent(
				id = event.id,
				order = event.order,
				date = event.date,
				content = event.content,
			)
		} else {
			null
		}
	}

	override suspend fun createEntityForId(id: Int): ApiProjectEntity.TimelineEventEntity {
		val event =
			timeLineRepository.getTimelineEvent(id) ?: throw IllegalStateException("Timeline event $id not found")

		return ApiProjectEntity.TimelineEventEntity(
			id = id,
			order = event.order,
			date = event.date,
			content = event.content
		)
	}

	override suspend fun reIdEntity(oldId: Int, newId: Int) {
		timeLineRepository.reIdEvent(oldId, newId)
	}

	override suspend fun storeEntity(
		serverEntity: ApiProjectEntity.TimelineEventEntity,
		syncId: String,
		onLog: OnSyncLog
	) {
		val event = TimeLineEvent(
			id = serverEntity.id,
			order = serverEntity.order,
			date = serverEntity.date,
			content = serverEntity.content,
		)

		timeLineRepository.updateEvent(event, false)
	}

	override suspend fun finalizeSync() {
		timeLineRepository.loadTimeline()
	}

	override fun getEntityType() = EntityType.TimelineEvent

	override suspend fun deleteEntityLocal(id: Int, onLog: OnSyncLog) {
		val event = timeLineRepository.getTimelineEvent(id)
		if (event != null) {
			if (timeLineRepository.deleteEvent(event)) {
				onLog(syncLogI("Deleted Timeline Event $id", projectDef))
			} else {
				onLog(syncLogE("Failed to delete timeline event $id", projectDef))
			}
		} else {
			onLog(syncLogE("Timeline event $id not found", projectDef))
		}
	}

	override suspend fun hashEntities(newIds: List<Int>): Set<EntityHash> {
		return timeLineRepository.timelineFlow.first().events
			.filter { newIds.contains(it.id).not() }
			.mapNotNull { event ->
				getEntityHash(event.id)?.let { hash ->
					EntityHash(event.id, hash)
				}
			}
			.toSet()
	}
}