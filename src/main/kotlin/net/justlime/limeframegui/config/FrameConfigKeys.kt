package net.justlime.limeframegui.config

import net.justlime.limeframegui.models.registry.GuiSound

data class FrameConfigKeys(
    //MAIN
    var main: String = "main",
    var inventoryTitle: String = "title",
    var inventoryRows: String = "rows",
    var pattern: String = "main.pattern",
    var openRequirements: String = "requirement.condition",
    var denyActions: String = "requirement.on-deny",
    var inventoryItemSection: String = "items",
    var defaultInventoryTitle: String = "LimeFrame Inventory",
    var defaultInventoryRows: Int = 6,

    //ITEMS
    var viewRequirements: String = "view-requirement",
    var priority: String = "priority",
    var material: String = "material",
    var name: String = "name",
    var lore: String = "lore",
    var display: String = "display",
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

    // Advanced Text Engine Keys
    var textSection: String = "text",
    var textFont: String = "font",
    var textWeight: String = "weight",
    var textPrefix: String = "prefix",
    var textSuffix: String = "suffix",
    var textCase: String = "case",
    var textWrapLength: String = "wrap-length",

    // Stylish Settings
    var stylishTitle: Boolean = false,
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

    // Anvil Gui
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
    var closeSound: GuiSound = GuiSound(),

    // Scoped Context Keys
    var localVariables: String = "variables",
    var localPlaceholders: String = "placeholders",

    //states
    var states: String = "states",
    var stateId: String = "state-id"
)