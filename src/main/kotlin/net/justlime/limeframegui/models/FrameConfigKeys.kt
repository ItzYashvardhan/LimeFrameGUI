package net.justlime.limeframegui.models

data class FrameConfigKeys(
    var inventoryTitle: String = "title",
    var inventoryRows: String = "rows",
    var inventoryItemSection: String = "items",
    var defaultInventoryTitle: String = "LimeFrame Inventory",
    var defaultInventoryRows: Int = 6,
    var material: String = "material",
    var name: String = "name",
    var lore: String = "lore",
    var amount: String = "amount",
    var glow: String = "glow",
    var flags: String = "flags",
    var model: String = "model",
    var texture: String = "texture",
    var unbreakable: String = "unbreakable",
    var damage: String = "damage",
    var slot: String = "slot",
    var slotList: String = "slots",
    var base64Data: String = "data",
    var stylishFontTitle: String = "font-title",
    var stylishFontName: String = "font-name",
    var stylishFontLore: String = "font-lore",
    var stylishTitle: Boolean = false,//Set to try to use small caps font.
    var stylishName: Boolean = false,
    var stylishLore: Boolean = false,
)



