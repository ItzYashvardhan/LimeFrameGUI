package net.justlime.limeframegui.models

data class GuiBuffer(
    var renderLimit: Int = 10,   // Chunk size to load
    var margin: Int = 3,  // Distance to load ahead
    var cleanupMargin: Int = 0 //  Distance to keep behind (Set 0 to disable)
)
