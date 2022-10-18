/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.create

import io.realm.kotlin.MutableRealm
import kotlinx.coroutines.TimeoutCancellationException
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.sync.model.RoomSyncSummary
<<<<<<< HEAD
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.internal.database.RealmInstance
=======
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
>>>>>>> develop
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomMembersLoadStatusType
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.events.getFixedRoomMemberContent
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberEventHandler
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface CreateLocalRoomTask : Task<CreateRoomParams, String>

internal class DefaultCreateLocalRoomTask @Inject constructor(
<<<<<<< HEAD
        @UserId private val userId: String,
        @SessionDatabase private val realmInstance: RealmInstance,
=======
        @SessionDatabase private val monarchy: Monarchy,
>>>>>>> develop
        private val roomMemberEventHandler: RoomMemberEventHandler,
        private val roomSummaryUpdater: RoomSummaryUpdater,
        private val createRoomBodyBuilder: CreateRoomBodyBuilder,
        private val cryptoService: DefaultCryptoService,
        private val clock: Clock,
        private val createLocalRoomStateEventsTask: CreateLocalRoomStateEventsTask,
) : CreateLocalRoomTask {

    override suspend fun execute(params: CreateRoomParams): String {
<<<<<<< HEAD
        val createRoomBody = createRoomBodyBuilder.build(params.withDefault())
        val invitedUsers = createRoomBody.invitedUserIds.orEmpty()
                .mapNotNull { tryOrNull { userService.resolveUser(it) } }

        val roomId = RoomLocalEcho.createLocalEchoId()
        realmInstance.write {
            createLocalRoomEntity(this, roomId, createRoomBody, invitedUsers)
            createLocalRoomSummaryEntity(this, roomId, createRoomBody)
=======
        val createRoomBody = createRoomBodyBuilder.build(params)
        val roomId = RoomLocalEcho.createLocalEchoId()
        monarchy.awaitTransaction { realm ->
            createLocalRoomEntity(realm, roomId, createRoomBody)
            createLocalRoomSummaryEntity(realm, roomId, params, createRoomBody)
>>>>>>> develop
        }

        // Wait for room to be created in DB
        try {
            awaitNotEmptyResult(realmInstance, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                RoomSummaryEntity.where(realm, roomId = roomId)
                        .query("membershipStr == $0", Membership.JOIN.name)
            }
        } catch (exception: TimeoutCancellationException) {
            throw CreateRoomFailure.CreatedWithTimeout(roomId)
        }
        return roomId
    }

    /**
     * Create a local room entity from the given room creation params.
     * This will also generate and store in database the chunk and the events related to the room params in order to retrieve and display the local room.
     */
    private fun createLocalRoomEntity(realm: MutableRealm, roomId: String, createRoomBody: CreateRoomBody, invitedUsers: List<User>) {
        RoomEntity.getOrCreate(realm, roomId).apply {
            membership = Membership.JOIN
            chunks.add(createLocalRoomChunk(realm, roomId, createRoomBody, invitedUsers))
            membersLoadStatus = RoomMembersLoadStatusType.LOADED
        }
    }

<<<<<<< HEAD
    private fun createLocalRoomSummaryEntity(realm: MutableRealm, roomId: String, createRoomBody: CreateRoomBody) {
        val otherUserId = createRoomBody.getDirectUserId()
        if (otherUserId != null) {
            RoomSummaryEntity.getOrCreate(realm, roomId).apply {
=======
    private fun createLocalRoomSummaryEntity(realm: Realm, roomId: String, createRoomParams: CreateRoomParams, createRoomBody: CreateRoomBody) {
        // Create the room summary entity
        val roomSummaryEntity = realm.createObject<RoomSummaryEntity>(roomId).apply {
            val otherUserId = createRoomBody.getDirectUserId()
            if (otherUserId != null) {
>>>>>>> develop
                isDirect = true
                directUserId = otherUserId
            }
        }

        // Update the createRoomParams from the potential feature preset before saving
        createRoomParams.featurePreset?.let { featurePreset ->
            featurePreset.updateRoomParams(createRoomParams)
            createRoomParams.initialStates.addAll(featurePreset.setupInitialStates().orEmpty())
        }

        // Create a LocalRoomSummaryEntity decorated by the related RoomSummaryEntity and the updated CreateRoomParams
        realm.createObject<LocalRoomSummaryEntity>(roomId).also {
            it.roomSummaryEntity = roomSummaryEntity
            it.createRoomParams = createRoomParams
        }

        // Update the RoomSummaryEntity by simulating a fake sync response
        roomSummaryUpdater.update(
                realm = realm,
                roomId = roomId,
                membership = Membership.JOIN,
                roomSummary = RoomSyncSummary(
                        heroes = createRoomBody.invitedUserIds.orEmpty().take(5),
                        joinedMembersCount = 1,
                        invitedMembersCount = createRoomBody.invitedUserIds?.size ?: 0
                ),
                updateMembers = !createRoomBody.invitedUserIds.isNullOrEmpty()
        )
    }

    /**
     * Create a single chunk containing the necessary events to display the local room.
     *
     * @param realm the current instance of realm
     * @param roomId the id of the local room
     * @param createRoomBody the room creation params
     *
     * @return a chunk entity
     */
    private fun createLocalRoomChunk(realm: MutableRealm, roomId: String, createRoomBody: CreateRoomBody, invitedUsers: List<User>): ChunkEntity {
        val chunkEntity = ChunkEntity().apply {
            this.roomId = roomId
            isLastBackward = true
            isLastForward = true
        }

<<<<<<< HEAD
        val eventList = createLocalRoomEvents(createRoomBody, invitedUsers)
=======
        val eventList = createLocalRoomStateEventsTask.execute(CreateLocalRoomStateEventsTask.Params(createRoomBody))
>>>>>>> develop
        val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()

        for (event in eventList) {
            if (event.eventId == null || event.senderId == null || event.type == null) {
                continue
            }

            val now = clock.epochMillis()
            val eventEntity = event.toEntity(roomId, SendState.SYNCED, now).copyToRealmOrIgnore(realm, EventInsertType.INCREMENTAL_SYNC)
            if (event.stateKey != null) {
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
                if (event.type == EventType.STATE_ROOM_MEMBER) {
                    roomMemberContentsByUser[event.stateKey] = event.getFixedRoomMemberContent()
                    roomMemberEventHandler.handle(realm, roomId, event, false)
                }

                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event)
            }

            roomMemberContentsByUser.getOrPut(event.senderId) {
                // If we don't have any new state on this user, get it from db
                val rootStateEvent = CurrentStateEventEntity.getOrNull(realm, roomId, event.senderId, EventType.STATE_ROOM_MEMBER)?.root
                rootStateEvent?.asDomain()?.getFixedRoomMemberContent()
            }

            chunkEntity.addTimelineEvent(
                    realm = realm,
                    roomId = roomId,
                    eventEntity = eventEntity,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = roomMemberContentsByUser
            )
        }

        return chunkEntity
    }
<<<<<<< HEAD

    /**
     * Build the list of the events related to the room creation params.
     *
     * @param createRoomBody the room creation params
     *
     * @return the list of events
     */
    private fun createLocalRoomEvents(createRoomBody: CreateRoomBody, invitedUsers: List<User>): List<Event> {
        val myUser = userService.getUser(userId) ?: User(userId)

        val createRoomEvent = createLocalEvent(
                type = EventType.STATE_ROOM_CREATE,
                content = RoomCreateContent(
                        creator = userId
                ).toContent()
        )
        val myRoomMemberEvent = createLocalEvent(
                type = EventType.STATE_ROOM_MEMBER,
                content = RoomMemberContent(
                        membership = Membership.JOIN,
                        displayName = myUser.displayName,
                        avatarUrl = myUser.avatarUrl
                ).toContent(),
                stateKey = userId
        )
        val roomMemberEvents = invitedUsers.map {
            createLocalEvent(
                    type = EventType.STATE_ROOM_MEMBER,
                    content = RoomMemberContent(
                            isDirect = createRoomBody.isDirect.orFalse(),
                            membership = Membership.INVITE,
                            displayName = it.displayName,
                            avatarUrl = it.avatarUrl
                    ).toContent(),
                    stateKey = it.userId
            )
        }

        return buildList {
            add(createRoomEvent)
            add(myRoomMemberEvent)
            addAll(createRoomBody.initialStates.orEmpty().map { createLocalEvent(it.type, it.content, it.stateKey) })
            addAll(roomMemberEvents)
        }
    }

    /**
     * Generate a local event from the given parameters.
     *
     * @param type the event type, see [EventType]
     * @param content the content of the Event
     * @param stateKey the stateKey, if any
     *
     * @return a fake event
     */
    private fun createLocalEvent(type: String?, content: Content?, stateKey: String? = ""): Event {
        return Event(
                type = type,
                senderId = userId,
                stateKey = stateKey,
                content = content,
                originServerTs = clock.epochMillis(),
                eventId = LocalEcho.createLocalEchoId()
        )
    }

    /**
     * Setup default values to the CreateRoomParams as the room is created locally (the default values will not be defined by the server).
     */
    private fun CreateRoomParams.withDefault() = this.apply {
        if (visibility == null) visibility = RoomDirectoryVisibility.PRIVATE
        if (historyVisibility == null) historyVisibility = RoomHistoryVisibility.SHARED
        if (guestAccess == null) guestAccess = GuestAccess.Forbidden
    }
=======
>>>>>>> develop
}
