/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.draft

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import io.realm.kotlin.MutableRealm
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.clearWith
import org.matrix.android.sdk.internal.database.mapper.DraftMapper
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.UserDraftsEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.util.mapOptional
import timber.log.Timber
import javax.inject.Inject

internal class DraftRepository @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
) {

    suspend fun saveDraft(roomId: String, userDraft: UserDraft) {
        realmInstance.write {
            saveDraftInDb(this, userDraft, roomId)
        }
    }

    suspend fun deleteDraft(roomId: String) {
        realmInstance.write {
            deleteDraftFromDb(this, roomId)
        }
    }

    fun getDraft(roomId: String): UserDraft? {
        val realm = realmInstance.getBlockingRealm()
        return UserDraftsEntity.where(realm, roomId).first()
                .find()
                ?.let { mapUserDrafts(it) }
    }

    fun getDraftsLive(roomId: String): LiveData<Optional<UserDraft>> {
        return realmInstance.queryFirst {
            UserDraftsEntity.where(it, roomId).first()
        }
                .mapOptional(::mapUserDrafts)
                .asLiveData()
    }

    private fun mapUserDrafts(userDraftsEntity: UserDraftsEntity): UserDraft? {
        return userDraftsEntity.userDrafts.firstOrNull()?.let { draft ->
            DraftMapper.map(draft)
        }
    }

    private fun deleteDraftFromDb(realm: MutableRealm, roomId: String) {
        UserDraftsEntity.where(realm, roomId).first().find()?.userDrafts?.clearWith {
            realm.delete(it)
        }
    }

    private fun saveDraftInDb(realm: MutableRealm, draft: UserDraft, roomId: String) {
        val roomSummaryEntity = RoomSummaryEntity.where(realm, roomId).first().find()
                ?: realm.copyToRealm(
                        RoomSummaryEntity().apply {
                            this.roomId = roomId
                        }
                )

        val userDraftsEntity = roomSummaryEntity.userDrafts
                ?: realm.copyToRealm(UserDraftsEntity()).also {
                    roomSummaryEntity.userDrafts = it
                }

        userDraftsEntity.let { userDraftEntity ->
            // Save only valid draft
            if (draft.isValid()) {
                // Replace the current draft
                val newDraft = DraftMapper.map(draft)
                Timber.d("Draft: create a new draft ${privacySafe(draft)}")
                userDraftEntity.userDrafts.clear()
                userDraftEntity.userDrafts.add(newDraft)
            } else {
                // There is no draft to save, so the composer was clear
                Timber.d("Draft: delete a draft")
                val topDraft = userDraftEntity.userDrafts.lastOrNull()
                if (topDraft == null) {
                    Timber.d("Draft: nothing to do")
                } else {
                    // Remove the top draft
                    Timber.d("Draft: remove the top draft")
                    userDraftEntity.userDrafts.remove(topDraft)
                }
            }
        }
    }

    private fun privacySafe(o: Any): Any {
        if (BuildConfig.LOG_PRIVATE_DATA) {
            return o
        }
        return ""
    }
}