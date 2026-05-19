package net.justlime.limeframegui.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.sql.Timestamp
import java.util.*

object FrameConverter {
    private val gson = GsonBuilder().registerTypeAdapter(Timestamp::class.java, JsonSerializer<Timestamp> { src, _, _ ->
        JsonPrimitive(src.toInstant().toString())
    }).create()

    fun serializeItemStackList(items: List<ItemStack>): String {
        return items.joinToString(",") { serializeItemStack(it) }
    }

    fun deserializeItemStackList(data: String?): List<ItemStack>? {
        return data?.split(",")?.mapNotNull { deserializeItemStack(it) }?.toMutableList()
    }

    fun serializeItemStack(item: ItemStack): String {
        val outputStream = ByteArrayOutputStream()
        val bukkitOut = BukkitObjectOutputStream(outputStream)
        bukkitOut.use { bukkitOut ->
            bukkitOut.writeObject(item)
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    fun deserializeItemStack(data: String?): ItemStack? {
        if (data.isNullOrEmpty()) return null
        return try {
            val bytes = Base64.getDecoder().decode(data)
            val inputStream = ByteArrayInputStream(bytes)
            val bukkitIn = BukkitObjectInputStream(inputStream)
            bukkitIn.use { bukkitIn ->
                bukkitIn.readObject() as? ItemStack
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun serializeInventory(inventory: Inventory): String {
        val outputStream = ByteArrayOutputStream()
        val dataOutput = BukkitObjectOutputStream(outputStream)
        dataOutput.writeInt(inventory.size)
        for (i in 0 until inventory.size) {
            dataOutput.writeObject(inventory.getItem(i))
        }
        dataOutput.close()
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    fun deserializeInventory(data: String?): Inventory? {
        if (data.isNullOrEmpty()) return null
        return try {
            val bytes = Base64.getDecoder().decode(data)
            val inputStream = ByteArrayInputStream(bytes)
            val dataInput = BukkitObjectInputStream(inputStream)
            val size = dataInput.readInt()
            val inventory = Bukkit.createInventory(null, size)
            for (i in 0 until size) {
                inventory.setItem(i, dataInput.readObject() as? ItemStack)
            }
            dataInput.close()
            inventory
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}