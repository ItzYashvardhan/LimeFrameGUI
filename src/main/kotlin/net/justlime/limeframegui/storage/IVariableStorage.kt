package net.justlime.limeframegui.storage

import java.util.UUID
import java.util.concurrent.CompletableFuture

interface IVariableStorage {
    fun loadValue(playerUuid: UUID, key: String): CompletableFuture<String?>
    fun saveValue(playerUuid: UUID, key: String, value: String): CompletableFuture<Void>
    fun loadAll(playerUuid: UUID): CompletableFuture<Map<String, String>>
}