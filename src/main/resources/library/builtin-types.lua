---@alias float number
---@alias double number
---@alias int number
---@alias int8 number
---@alias int16 number
---@alias int32 number
---@alias int64 number
---@alias uint number
---@alias uint8 number
---@alias uint16 number
---@alias uint32 number
---@alias uint64 number
---@alias bool boolean

--- Alias for capilization
---@alias Any any

---This function allows to log LocalisedStrings to the Factorio log file.
---This, in combination with serpent, makes debugging in the data stage easier, because it allows to simply inspect entire prototype tables.
---For example, this allows to see all properties of the sulfur item prototype: log(serpent.block(data.raw["item"]["sulfur"]))
---@param str string|LocalisedString
function log(str) end

--- Factorio provides the function table_size() as a simple way to find the size of tables with non-continuous keys, because the standard # does not work correctly for these.
---The function is a C++-side implementation of the following Lua code, thus it is faster than doing it in LUA!
---Note that table_size() does not work correctly for LuaCustomTables, their size has to be determined with LuaCustomTable::operator # instead.
---@param t table
function table_size(t) end


--[[
---@shape T1
---@field baum number

---@shape T2
---@field type "haus"
---@field haus string

---@type T1|T2
local hu = {
    baum = 5,
    type = "haus" -- This should be an error, but is not
}

hu.type = "haus" -- Error: No such member 'type' found on type 'T1'

local var = hu.haus -- Error: No such member 'haus' found on type 'T1'

hu.baum = 5 -- Error: No such member 'baum' found on type 'T2'

-- also there is no code-completion for the var names
]]

---@shape T1
---@field type string
---@field num number

---@type T1
local elem

elem.type = "huhu"
elem.num = 5

---@type T1
local elem2 = {
    type = "baum",
    num = 5
}
