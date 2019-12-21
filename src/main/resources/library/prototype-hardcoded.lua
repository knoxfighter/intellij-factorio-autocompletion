---@alias ForceCondition string|'"all"'|'"enemy"'|'"ally"'|'"friend"'|'"not-friend"'|'"same"'|'"not-same"'

---@alias AnimationVariations Animation[]

---@alias Trigger TriggerClass[]

--- An array of TriggerItems. The tables are each loaded as one of the Types/TriggerItem extensions, depending on the value of the type key in the table.
---@class TriggerClass : TriggerItem
local TriggerClass = {}

---@type string|'"direct"'|'"area"'|'"line"'|'"cluster"'
TriggerClass.type = nil

---@class Vector3D
local Vector3D = {}

---@type float
Vector3D.x = nil

---@type float
Vector3D.y = nil

---@type float
Vector3D.z = nil

--- Refers to an existing circuit network signal.
---@class SignalIDConnector
local SignalIDConnector = {}

---@type string|'"virtual"'|'"item"'|'"fluid"'
SignalIDConnector.type = nil

--- The name of a circuit network signal, it has to have the type that is specified in this definition.
--- For example `name = "iron-plate"` would only work with `type = "item"`, and `type = "fluid"` would need a fluid signal as the name, for example `crude-oil` or `water`.
---@type string
SignalIDConnector.name = nil

--- https://wiki.factorio.com/Types/Order
---@alias Order string

---@alias SpriteSizeType int16

--- Table of red, green, blue, and alpha float values between 0 and 1. All values are optional, default optional value for colors is 0, for alpha 1.
--- Alternatively, values can be from 0-255, they are interpreted as such if at least one of r,g or b is > 1.
---@class Color
local Color = {}

--- [optional, default 0] red value
---@type float
Color.r = nil

--- [optional, default 0] green value
---@type float
Color.g = nil

--- [optional, default 0] blue value
---@type float
Color.b = nil

--- [optional, default 1] alpha value - transparency
---@type float
Color.a = nil

--- Definition of a point where circuit network wires can be connected to an entity.
---@class WireConnectionPoint
local WireConnectionPoint = {}

---@type WirePosition
WireConnectionPoint.wire = nil

---@type WirePosition
WireConnectionPoint.shadow = nil

---@alias ItemCountType uint32

--- An array of TriggerEffectItem, or just one TriggerEffectItem.
--- The tables are each loaded as one of the Types/TriggerEffectItem extensions, depending on the value of the type key in the table.
---@class TriggerEffect : TriggerEffectItem
local TriggerEffect = {}

---@type string|'"damage"'|'"create-entity"'|'"create-explosion"'|'"create-fire"'|'"create-smoke"'|'"create-trivial-smoke"'|'"create-particle"'|'"create-sticker"'|'"nested-result"'|'"play-sound"'|'"push-back"'|'"destroy-cliffs"|'"show-explosion-on-chart"'
TriggerEffect.type = nil

---@alias ItemStackIndex uint16

---@alias MaterialAmountType double

---@alias EffectTypeLimitation string[]

---@alias AttackReaction AttackReactionItem[]

--- Uses `string` to specify the amount of electric energy in joules or electric energy per time in watts.
--- Internally, the input in Watt or `Joule/second` is always converted into Joule/tick or `Joule/(1/60)second`, using the following formula: `Power in Joule/tick = Power in Watt / 60`.
---@alias Energy string

--- Class used for Prototype/Recipe ingredients, it loads as Types/ItemIngredientPrototype or Types/FluidIngredientPrototype, depending on the #type.
--- THIS IS NOT COMPLETE!!
---@class IngredientPrototype
local IngredientPrototype = {}

---@type string|'"item"'|'"fluid"'
IngredientPrototype.type = nil

---@type string
IngredientPrototype.name = nil

---@alias BoundingBox float[]

---@alias vector float[]

---@alias ItemPrototypeFlags string[]

--- Class used for Prototype/Recipe prodcuts, it loads as Types/ItemProductPrototype or Types/FluidProductPrototype, depending on the #type.
---@class ProductPrototype
local ProductPrototype = {}

---@type string|'"item"'|'"fluid"'
ProductPrototype.type = nil

--- When hovering over a recipe in the crafting menu the recipe tooltip will be shown.
--- An additional item tooltip will be shown for every product, as a separate tooltip, if the item tooltip has a description and/or properties to show and if show_details_in_recipe_tooltip is true.
--- THIS IS NOT COMPLETE!!
---@type boolean
ProductPrototype.show_details_in_recipe_tooltip = nil

---@alias RotatedAnimationVariations RotatedAnimation|RotatedAnimation[]

--- The definition of a spawnable unit for a Prototype/EnemySpawner.
--- It can be specified as a table with named or numbered keys, but not a mix of both.
---@class UnitSpawnDefinition
local UnitSpawnDefinition = {}

--- The name of a Unit
---@type string
UnitSpawnDefinition.unit = nil

--- Array of evolution and probability info. The evolution_factor must be ascending from entry to entry.
---@type SpawnPoint[]
UnitSpawnDefinition.spawn_points = nil

---@alias RenderLayer string

---@alias SpriteVariations SpriteVariationsSprite[]

--- A sprite. Does not use the layers property. Has the following extra optional properties:
---@class SpriteVariationsSprite : Sprite
local SpriteVariationsSprite = {}

---@type uint32
SpriteVariationsSprite.variation_count = nil

---@type uint32
SpriteVariationsSprite.repeat_count = nil

---@type uint32
SpriteVariationsSprite.line_length = nil

---@alias Loot LootTable[]

--- Type for `loot` for Prototype/EntityWithHealth: The loot is generated when the entity is killed.
---@class LootTable
local LootTable = {}

--- The item to spawn.
---@type string
LootTable.item = nil

--- 0 is 0% and 1 is 100%.
---@type double
LootTable.probability = nil

---@type double
LootTable.count_min = nil

---@type double
LootTable.count_max = nil

---@alias CapsuleAction table

---@alias AttackParameters table
