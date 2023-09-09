# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

## [1.4.0-IDEA231-eap-1] - 12.08.2022
### Added
- Compatibility with jetbrains IDEs 2023.*

## [1.3.5] - 12.08.2022
### Added
- Compatibility with jetbrains IDEs 2022.*

## [1.3.4] - 02.09.2021
### Added
- Compatibility with jetbrains IDEs 2021.2.*

## [1.3.3] - 08.07.2021
### Added
- Compatibility with jetbrains IDEs 2021.1.*

## [1.3.2] - 10.12.2020
### Added
- Compatibility with jetbrains IDEs 2020.3.*

## [1.3.1] - 03.08.2020
### Added
- Compatibility with jetbrains IDEs 2020.2.*

## [1.3.0] - 22.04.2020
### Added
- Compatibility with jetbrains IDEs 2020.1.*

## [1.2.2] - 11.02.2020
### Fixed
- endless "Download Factorio LuaApi" on startup
- download of lualib and core prototypes failed

## [1.2.1] - 18.01.2020
### Fixed
- Added missing log() and table_size() functions
- Defines-Table is now available again (was defined local)
- functions and variables inside base/core Prototype definitions are not shown in autocompletion anymore.

## [1.2.0] - 15.01.2020
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
