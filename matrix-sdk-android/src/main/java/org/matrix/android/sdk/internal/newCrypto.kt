/*
 * Copyright (c) 2021 New Vector Ltd
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

package org.matrix.android.sdk.internal

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import uniffi.olm.OlmMachine as InnerMachine
import uniffi.olm.Device as InnerDevice
import uniffi.olm.Sas as InnerSas

class Device(inner: InnerDevice, machine: InnerMachine) {
    private val machine: InnerMachine = machine
    private val inner: InnerDevice = inner

    fun userId(): String {
        return this.inner.userId
    }

    fun deviceId(): String {
        return this.inner.deviceId
    }

    fun keys(): Map<String, String> {
        return this.inner.keys
    }

    fun startVerification(): InnerSas {
        return this.machine.startVerification(this.inner)
    }
}

class OlmMachine(user_id: String, device_id: String, path: String) {
    private val inner: InnerMachine = InnerMachine(user_id, device_id, path)

    fun userId(): String {
        return this.inner.userId()
    }

    fun deviceId(): String {
        return this.inner.deviceId()
    }

    fun identityKeys(): Map<String, String> {
        return this.inner.identityKeys()
    }

    suspend fun slowUserId(): String = withContext(Dispatchers.Default) {
        inner.slowUserId()
    }

    suspend fun getDevice(user_id: String, device_id: String): Device? = withContext(Dispatchers.IO) {
        when (val device: InnerDevice? = inner.getDevice(user_id, device_id)) {
            null -> null
            else -> Device(device, inner)
        }
    }
}
