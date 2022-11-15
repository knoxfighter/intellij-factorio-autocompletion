package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.Parser;
import moe.knox.factorio.api.parser.data.RuntimeApi;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiFileWriterTest_1_1_70 {
    String fileOutput;

    @BeforeAll
    public void setup() throws IOException {
        RuntimeApi runtimeApi = Parser.fromWebsite("1.1.70");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(stream);
        ApiFileWriter apiFileWriter = ApiFileWriter.fromIoWriter(writer);
        apiFileWriter.writeRuntimeApi(runtimeApi);
        writer.flush();

        fileOutput = stream.toString();
        writer.close();
        stream.close();
    }

    @Disabled
    @Test
    void createAndOpenFile() throws IOException {
        Path path = Files.createTempFile("runtimeApi1170", ".out");
        File file = path.toFile();

        FileOutputStream stream = new FileOutputStream(file);
        stream.write(fileOutput.getBytes());
        stream.close();

        Desktop.getDesktop().open(file);
    }

    @Test
    void checkClass() {
        String expectedOutput = """
--- Collection of settings for overriding default ai behavior.
---@class LuaAISettings
local LuaAISettings = {}
                
--- If enabled, units that repeatedly fail to succeed at commands will be destroyed.
--- Read-Write
---@type boolean
LuaAISettings.allow_destroy_when_commands_fail = nil
                
--- If enabled, units that have nothing else to do will attempt to return to a spawner.
--- Read-Write
---@type boolean
LuaAISettings.allow_try_return_to_spawner = nil
                
--- If enabled, units will try to separate themselves from nearby friendly units.
--- Read-Write
---@type boolean
LuaAISettings.do_separation = nil
                
--- The class name of this object. Available even when `valid` is false. For LuaStruct objects it may also be suffixed with a dotted path to a member of the struct.
--- Read-Only
---@type string
LuaAISettings.object_name = nil
                
--- The pathing resolution modifier, must be between -8 and 8.
--- Read-Write
---@type int8
LuaAISettings.path_resolution_modifier = nil
                
--- Is this object valid? This Lua object holds a reference to an object within the game engine. It is possible that the game-engine object is removed whilst a mod still holds the corresponding Lua object. If that happens, the object becomes invalid, i.e. this attribute will be `false`. Mods are advised to check for object validity if any change to the game state might have occurred between the creation of the Lua object and its access.
--- Read-Only
---@type boolean
LuaAISettings.valid = nil
                
--- All methods and properties that this object supports.
---@return string
function LuaAISettings.help() end
""";

        MatcherAssert.assertThat(fileOutput, CoreMatchers.containsString(expectedOutput));
    }

    @Test
    void checkDefine() {
        String expectedOutput = """
---@type nil
defines.control_behavior.inserter.circuit_mode_of_operation.read_hand_contents = nil
""";
        MatcherAssert.assertThat(fileOutput, CoreMatchers.containsString(expectedOutput));
    }

    @Test
    void checkConceptTable() {
        String expectedOutput = """
--- A table used to define a manual shape for a piece of equipment.
---@class EquipmentPoint
local EquipmentPoint = {}

---@type uint
EquipmentPoint.x = nil

---@type uint
EquipmentPoint.y = nil
""";

        MatcherAssert.assertThat(fileOutput, CoreMatchers.containsString(expectedOutput));
    }

    // tuples are not written, cause tuples are not supported in EmmyLUA
    @Disabled
    @Test
    void checkConceptTuple() {
        String expectedOutput = """

""";

        MatcherAssert.assertThat(fileOutput, CoreMatchers.containsString(expectedOutput));
    }

    @Test
    void checkConceptStruct() {
        String expectedOutput = """
--- Parameters that affect the look and control of the game. Updating any of the member attributes here will immediately take effect in the game engine.
---@class GameViewSettings
local GameViewSettings = {}

--- Show the flashing alert icons next to the player's toolbar.
--- Read-Write
---@type boolean
GameViewSettings.show_alert_gui = nil

--- Show the controller GUI elements. This includes the toolbar, the selected tool slot, the armour slot, and the gun and ammunition slots.
--- Read-Write
---@type boolean
GameViewSettings.show_controller_gui = nil

--- Show overlay icons on entities. Also known as "alt-mode".
--- Read-Write
---@type boolean
GameViewSettings.show_entity_info = nil

--- Shows or hides the view options when map is opened.
--- Read-Write
---@type boolean
GameViewSettings.show_map_view_options = nil

--- Show the chart in the upper right-hand corner of the screen.
--- Read-Write
---@type boolean
GameViewSettings.show_minimap = nil

--- Shows or hides quickbar of shortcuts.
--- Read-Write
---@type boolean
GameViewSettings.show_quickbar = nil

--- When `true` (`false` is default), the rails will always show the rail block visualisation.
--- Read-Write
---@type boolean
GameViewSettings.show_rail_block_visualisation = nil

--- Show research progress and name in the upper right-hand corner of the screen.
--- Read-Write
---@type boolean
GameViewSettings.show_research_info = nil

--- Shows or hides the shortcut bar.
--- Read-Write
---@type boolean
GameViewSettings.show_shortcut_bar = nil

--- Shows or hides the buttons row.
--- Read-Write
---@type boolean
GameViewSettings.show_side_menu = nil

--- When `true` (the default), mousing over an entity will select it. Otherwise, moving the mouse won't update entity selection.
--- Read-Write
---@type boolean
GameViewSettings.update_entity_selection = nil
""";

        MatcherAssert.assertThat(fileOutput, CoreMatchers.containsString(expectedOutput));
    }

    @Test
    void checkConceptArray() {
        String expectedOutput = "--- Used to filter out irrelevant event callbacks in a performant way.\n" +
                "---\n" +
                "--- Filters are always used as an array of filters of a specific type. Every filter can only be used with its corresponding event, and different types of event filters can not be mixed.\n" +
                "---@alias EventFilter ";

        MatcherAssert.assertThat(fileOutput, CoreMatchers.containsString(expectedOutput));
    }

    @Test
    void checkConceptDictionary() {
        String expectedOutput = "--- A set of trigger target masks.\n" +
                "---@alias TriggerTargetMask table<string, boolean>";

        MatcherAssert.assertThat(fileOutput, CoreMatchers.containsString(expectedOutput));
    }

    @Test
    void checkConceptVariant() {
        String expectedOutput = "--- A fluid may be specified in one of three ways.\n" +
                "---@alias FluidIdentification string|LuaFluidPrototype|Fluid";

        MatcherAssert.assertThat(fileOutput, CoreMatchers.containsString(expectedOutput));
    }

    @Test
    void checkConceptSimple() {
        String expectedOutput = "--- The smooth orientation. It is a [float](float) in the range `[0, 1)` that covers a full circle, starting at the top and going clockwise. This means a value of `0` indicates \"north\", a value of `0.5` indicates \"south\".  For example then, a value of `0.625` would indicate \"south-west\", and a value of `0.875` would indicate \"north-west\".\n" +
                "---@alias RealOrientation float";

        MatcherAssert.assertThat(fileOutput, CoreMatchers.containsString(expectedOutput));
    }
}
