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
import org.matrix.android.sdk.internal.database.KotlinRealmMigrator

/**
 * This migration adds a new field into MyDeviceLastSeenInfoEntity corresponding to the last seen user agent.
 */
internal class MigrateCryptoTo020(context: AutomaticSchemaMigration.MigrationContext) : KotlinRealmMigrator(context, 20) {

    override fun doMigrate(migrationContext: AutomaticSchemaMigration.MigrationContext) {
        // Nothing to do, this is an automatic migration now.
    }
}