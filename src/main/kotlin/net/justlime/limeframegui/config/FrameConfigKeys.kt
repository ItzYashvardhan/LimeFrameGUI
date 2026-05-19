package net.justlime.limeframegui.config

import net.justlime.limeframegui.models.registry.GuiSound

data class FrameConfigKeys(
    var main: String = "main",
    var pattern: String = "main.pattern",
    var background: String = "background",
    var inventoryTitle: String = "title",
    var inventoryRows: String = "rows",
    var inventoryItemSection: String = "items",
    var defaultInventoryTitle: String = "LimeFrame Inventory",
    var defaultInventoryRows: Int = 6,
    var material: String = "material",
    var name: String = "name",
    var lore: String = "lore",
    var updateInterval: String = "update-interval",
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
    var action: String = "action",
    /**Play Sound on ItemClick
     * - Syntax: name, pitch, volume
     * - Default Value: null,1.0,1.0**/
    var stylishItemSound: String = "click-sound",

    /** Play Sound on GUI Open
     * - Syntax: name, pitch, volume
     * - Default Value: null,1.0,1.0
     **/
    var stylishOpenSound: String = "open-sound",
    /** Play Sound on GUI Close
     * - Syntax: name, pitch, volume
     * - Default Value: null,1.0,1.0
     **/
    var stylishCloseSound: String = "close-sound",

    var anvilTitle: String = "title",
    var anvilLabel: String = "label",
    var anvilLeftItem: String = "left-item",
    var anvilRightItem: String = "right-item",
    var anvilOutputItem: String = "output-item",
    var anvilPreventClose: String = "prevent-close",
    var anvilCancelSound: String = "cancel-sound",
    var anvilSubmitSound: String = "submit-sound",
    var defaultAnvilTitle: String = "Anvil Gui",
    var defaultAnvilLabel: String = "Type here",


    var clickSound: GuiSound = GuiSound(),
    var openSound: GuiSound = GuiSound(),
    var closeSound: GuiSound = GuiSound()

)