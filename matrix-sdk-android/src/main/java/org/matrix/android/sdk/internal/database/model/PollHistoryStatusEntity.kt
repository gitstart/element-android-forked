/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.internal.session.room.poll.PollConstants

/**
 * Keeps track of the loading process of the poll history.
 */
internal open class PollHistoryStatusEntity(
        /**
         * The related room id.
         */
        @PrimaryKey
        var roomId: String = "",

        /**
         * Timestamp of the in progress poll sync target in backward direction in milliseconds.
         */
        var currentTimestampTargetBackwardMs: Long? = null,

        /**
         * Timestamp of the oldest event synced in milliseconds.
         */
        var oldestTimestampReachedMs: Long? = null,

        /**
         * Indicate whether all polls in a room have been synced in backward direction.
         */
        var isEndOfPollsBackward: Boolean = false,

        /**
         * Token of the end of the last synced chunk in backward direction.
         */
        var tokenEndBackward: String? = null,

        /**
         * Token of the start of the last synced chunk in forward direction.
         */
        var tokenStartForward: String? = null,
) : RealmObject() {

    companion object

    /**
     * Create a new instance of the entity with the same content.
     */
    fun copy(): PollHistoryStatusEntity {
        return PollHistoryStatusEntity(
                roomId = roomId,
                currentTimestampTargetBackwardMs = currentTimestampTargetBackwardMs,
                oldestTimestampReachedMs = oldestTimestampReachedMs,
                isEndOfPollsBackward = isEndOfPollsBackward,
                tokenEndBackward = tokenEndBackward,
                tokenStartForward = tokenStartForward,
        )
    }

    /**
     * Indicate whether at least one poll sync has been fully completed backward for the given room.
     */
    val hasCompletedASyncBackward: Boolean
        get() = oldestTimestampReachedMs != null

    /**
     * Indicate whether all polls in a room have been synced for the current timestamp target in backward direction.
     */
    val currentTimestampTargetBackwardReached: Boolean
        get() = checkIfCurrentTimestampTargetBackwardIsReached()

    private fun checkIfCurrentTimestampTargetBackwardIsReached(): Boolean {
        val currentTarget = currentTimestampTargetBackwardMs
        val lastTarget = oldestTimestampReachedMs
        // last timestamp target should be older or equal to the current target
        return currentTarget != null && lastTarget != null && lastTarget <= currentTarget
    }

    /**
     * Compute the number of days of history currently synced.
     */
    fun getNbSyncedDays(currentMs: Long): Int {
        val oldestTimestamp = oldestTimestampReachedMs
        return if (oldestTimestamp == null) {
            0
        } else {
            ((currentMs - oldestTimestamp).coerceAtLeast(0) / PollConstants.MILLISECONDS_PER_DAY).toInt()
        }
    }
}