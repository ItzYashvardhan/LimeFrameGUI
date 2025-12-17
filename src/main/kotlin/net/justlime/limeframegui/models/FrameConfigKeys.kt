package net.justlime.limeframegui.models

import net.justlime.limeframegui.enums.RenderMode

data class FrameConfigKeys(
    var inventoryTitle: String = "title",
    var inventoryRows: String = "rows",
    var inventoryItemSection: String = "items",
    var defaultInventoryTitle: String = "LimeFrame Inventory",
    var defaultInventoryRows: Int = 6,
    var defaultRenderMode: RenderMode = RenderMode.EAGER,
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
    /**Play Sound on ItemClick
     * - Syntax: name, pitch, volume
     * - Default Value: null,1.0,1.0**/
    var stylishItemSound: String = "click-sound",
    /** Play Sound on GUI Open
     * - Syntax: name, pitch, volume
     * - Default Value: null,1.0,1.0
     **/
    var stylishOpenSound: String = "open-sound",//GUI Open Sound
    /** Play Sound on GUI Close
     * - Syntax: name, pitch, volume
     * - Default Value: null,1.0,1.0
     **/
    var stylishCloseSound: String = "close-sound",

    var clickSound: GuiSound = GuiSound(),
    var openSound: GuiSound = GuiSound(),
    var closeSound: GuiSound = GuiSound()

)



