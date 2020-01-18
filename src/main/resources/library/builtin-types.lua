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
