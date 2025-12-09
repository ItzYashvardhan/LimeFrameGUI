package net.justlime.limeframegui.models

data class BufferSettings(
    val enabled: Boolean = false, //
    val renderLimit: Int = 10,   // Chunk size to load
    val renderMargin: Int = 5,  //
    val unRenderPages: Boolean = false, //
    val cleanupMargin: Int = 20 //  Distance to keep behind (Require unRenderPages must be true)
)
