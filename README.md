# Progressive Stats — Geode mod for Geometry Dash

Live in-game HUD that shows your **progress, consistency, playtime, attempts** and more — while you play.

> This repository was converted from a Minecraft mod (Society) into a Geode mod for Geometry Dash. The mod is now a full C++/Geode project.

## Features

### In-Game HUD (during PlayLayer)
- **Current %** — live progress with decimals + run time
- **Best %** — all-time best for this level
- **Session Best** — best this session
- **Attempts** — `total (session)` format
- **Jumps** — jumps this attempt + total jumps ever
- **Playtime** — total time spent on level + session time
- **Average** — average of last 50 runs (and session avg)
- **Consistency** — % of runs that reached 25% / 50% / 75% / 90% from 0%
- **Progress Bar** — visual bar that turns green when above best
- **New Best FX** — green flash + SFX when you beat best

### Where stats show
- **In-level HUD** — top-left by default, movable, scalable, opacity
- **Pause Menu** — info button (clock icon) left side → full stats popup
- **Level Info Page** — clock button → detailed stats before entering

### Tracking
- Per-level JSON save: `.../geode/mods/minlol.progressive-stats/save/level_stats.json`
- Supports **online levels** (`online_12345`) and **editor levels** (`editor_Name_hash`)
- Optional LevelID API support for stable editor IDs if `cvolton.level-id-api` is installed
- Tracks up to 1000 recent runs per level, calculates:
  - best, average, consistency thresholds, deaths above threshold, total jumps, total playtime
- Practice mode toggle (off by default)
- Session resets on exit toggle

### Settings (mod.json)
All HUD elements can be toggled:
- Enable HUD, Position (Top Left/Right, Bottom Left/Right, Custom), Custom X/Y, Scale, BG opacity, Compact mode, Decimals
- Show: Current, Best, Session Best, Attempts, Jumps, Playtime, Session Time, Average, Consistency, Progress Bar
- Tracking: Track practice, count practice as attempts, reset session on exit, colored best, SFX on new best

## Build

### Requirements
- [Geode SDK](https://docs.geode-sdk.org/getting-started/) (installed separately)
- CMake 3.21+
- C++23 compiler (MSVC / clang)

CMake needs to know where the **Geode SDK source directory** is (the directory
containing its `CMakeLists.txt`). You can provide it with the `GEODE_SDK`
environment variable or the `GEODE_SDK` CMake option.

#### Windows PowerShell

For the current PowerShell window, set the SDK path before configuring:

```powershell
$env:GEODE_SDK = "C:\path\to\geode"
cmake -B build -DCMAKE_BUILD_TYPE=RelWithDebInfo
cmake --build build --config RelWithDebInfo
```

Alternatively, pass the path directly (this is useful when using a fresh
PowerShell window):

```powershell
cmake -B build -DCMAKE_BUILD_TYPE=RelWithDebInfo -DGEODE_SDK="C:\path\to\geode"
cmake --build build --config RelWithDebInfo
```

Replace `C:\path\to\geode` with your SDK directory. Do not run the build
command after a failed configure: CMake does not create `ALL_BUILD.vcxproj`
until it has found the SDK and configured successfully.

#### Other platforms / Geode CLI

```bash
# Standard build (with GEODE_SDK exported in your shell)
cmake -B build -DCMAKE_BUILD_TYPE=RelWithDebInfo
cmake --build build --config RelWithDebInfo

# Or via Geode CLI
geode build
```

The `.geode` file will be in `build/` or installed via CLI.

### Install
1. Install [Geode](https://geode-sdk.org) for GD 2.2074
2. Drag the built `.geode` into `geode/mods/` or use `geode build --install`
3. Enable mod in Geode's mod list in-game
4. Open level → HUD appears
5. Pause → clock button → detailed stats

## File Layout
```
mod.json
CMakeLists.txt
about.md
changelog.md
src/
  main.cpp          — PlayLayer, PauseLayer, LevelInfoLayer hooks
  StatsManager.hpp/cpp — JSON persistence, per-level/session logic
  HUD.hpp/cpp       — overlay UI, bars, labels
  Utils.hpp         — level key, formatting
resources/
```

## How tracking works (simplified)
- On `PlayLayer::init` → first attempt + session attempt counted
- On `destroyPlayer` → capture death % as run, update best, save
- On `resetLevel` → if manual reset without death, capture current % as run; then count new attempt
- On `update(dt)` → accumulate playtime to total + session + run, update HUD every frame
- On `incrementJumps` → count jumps per run + total
- On `levelComplete` → 100% run
- On `onQuit` → save JSON; optional reset session

## Save format
```json
{
  "version": 1,
  "levels": {
    "online_123456": {
      "totalAttempts": 423,
      "totalJumps": 8234,
      "totalPlaytime": 4523.5,
      "bestPercent": 87.34,
      "runs": [12.3, 45.6,  ...]
    }
  }
}
```

## Future ideas
- Graph of runs over time (like Death Tracker)
- Export to CSV
- Startpos tracking
- Platformer distance tracking
- Per-copy stats sharing via LevelID API linking

## Credits
- Idea inspired by Death Tracker, Playtime Tracker, Blitzkrieg
- Built with Geode SDK

MIT — by MinLOL12
