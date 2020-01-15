# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Added
- Autocompletion for require statement. (autocompletes the path to lua files)
- Download the factorio lualib (https://github.com/wube/factorio-data/tree/master/core/lualib)
- Type infer is followed the require statements correctly to files. This is based on the two points above :)
- Autocomplete Prototypes for tables (e.g. `LuaForce.recipe[String]LuaRecipe`)
- Autocomplete Prototypes in data.raw table.

### Fixed
- Removed weird indexing of files (only happens now, when finished downloading)

### Changed
- Settings will reload Prototypes and Lualib too.
- Check for updates is only done, when the Project is opened.

## [1.1.1] - 30.12.2019
### Fixed
- Global Variable where defined `local`

## [1.1.0] - 28.12.2019
### Added
- JSON Schema for info.json
- Autocompletion for Prototypes (other autocompletion mostly deactivated)
    - completion for prototype field (only names)
    - completion for the type literal
- Compatibility with Jetbrains 2019.3 IDEs

## [1.0.0] - Initial Release - 16.11.2019
### Added
- Code completion for additional LUA files
- Download and parse the factorio-lua-api
- bultin-types as hardcoded library
