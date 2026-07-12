package net.justlime.limeframegui.config


object FrameKeys {

    object Default{
        const val DEFAULT = "default"
        const val DEFAULT_CHEST_TITLE = "LimeFrame Chest Inventory"
        const val DEFAULT_CHEST_ROWS = 6
        const val DEFAULT_ANVIL_TITLE = "LimeFrame Anvil Inventory"
        const val DEFAULT_ANVIL_LABEL = "Type Here"
    }

    object Main {
        const val SECTION = "main"
        const val TITLE = "title"
        const val LABEL = "label"
        const val ROWS = "rows"
        const val PATTERN = "pattern"
        const val TYPE = "type"
        const val TEMPLATE = "template"
        const val INTERFACE = "interface"
        const val INHERIT = "inherit"
        const val INGREDIENTS = "ingredients"
        const val DYNAMIC_LIST = "dynamic_list"
        const val PREVENT_CLOSE = "prevent_close"
    }


    object Type {
        const val GENERIC = "generic"
        const val ABSTRACT = "abstract"
        const val TEMPLATE = "template"
        const val INTERFACE = "interface"
    }

    object Item {
        const val SECTION = "items"
        const val COMPONENT = "component"
        const val VIEW_REQUIREMENT = "view-requirement"
        const val PRIORITY = "priority"
        const val MATERIAL = "material"
        const val NAME = "name"
        const val LORE = "lore"
        const val DISPLAY = "display"
        const val UPDATE_INTERVAL = "update-interval"
        const val AMOUNT = "amount"
        const val GLOW = "glow"
        const val FLAGS = "flags"
        const val MODEL = "model"
        const val TEXTURE = "texture"
        const val UNBREAKABLE = "unbreakable"
        const val DAMAGE = "damage"
        const val SLOT = "slot"
        const val SLOTS = "slots"
        const val CHAR = "char"
        const val BASE64_DATA = "data"
        const val ACTION = "action"
    }

    object Text {
        const val SECTION = "text"
        const val FONT = "font"
        const val WEIGHT = "weight"
        const val PREFIX = "prefix"
        const val SUFFIX = "suffix"
        const val CASE = "case"
        const val WRAP_LENGTH = "wrap-length"
    }

    object Sound {
        const val SOUND_CLICK = "click-sound"
        const val SOUND_OPEN = "open-sound"
        const val SOUND_CLOSE = "close-sound"
    }

    object Anvil {
        const val TYPE = "type"
        const val TYPE_LEFT = "left"
        const val TYPE_RIGHT = "right"
        const val TYPE_OUTPUT = "output"
    }

    object Context {
        const val OPEN_REQUIREMENTS = "requirement.condition"
        const val DENY_ACTIONS = "requirement.on-deny"
        const val LOCAL_VARIABLES = "variables"
        const val LOCAL_PLACEHOLDERS = "placeholders"
    }

    object State {
        const val STATES = "states"
        const val STATE_ID = "state-id"
    }
}