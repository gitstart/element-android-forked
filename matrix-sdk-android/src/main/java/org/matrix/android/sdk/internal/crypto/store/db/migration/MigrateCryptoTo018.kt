/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.kotlin.migration.AutomaticSchemaMigration
import org.matrix.android.sdk.internal.crypto.model.InboundGroupSessionData
import org.matrix.android.sdk.internal.database.KotlinRealmMigrator
import org.matrix.android.sdk.internal.database.safeEnumerate
import org.matrix.android.sdk.internal.di.MoshiProvider

/**
 * This migration is adding support for trusted flags on megolm sessions.
 * We can't really assert the trust of existing keys, so for the sake of simplicity we are going to
 * mark existing keys as safe.
 * This migration can take long depending on the account
 */
internal class MigrateCryptoTo018(context: AutomaticSchemaMigration.MigrationContext) : KotlinRealmMigrator(context, 18) {

    private val moshiAdapter = MoshiProvider.providesMoshi().adapter(InboundGroupSessionData::class.java)

    override fun doMigrate(migrationContext: AutomaticSchemaMigration.MigrationContext) {
        migrationContext.safeEnumerate("OlmInboundGroupSessionEntity") { oldObject, newObject ->
            if (newObject == null) return@safeEnumerate
            oldObject.getNullableValue("inboundGroupSessionDataJson", String::class)?.let { oldData ->
                moshiAdapter.fromJson(oldData)?.let { dataToMigrate ->
                    dataToMigrate.copy(trusted = true).let {
                        newObject.set("inboundGroupSessionDataJson", moshiAdapter.toJson(it))
                    }
                }
            }
        }
    }
}