# Progressive Stats

A clean, live stats overlay for Geometry Dash that helps you **see your grind**.

### What it shows while you play
- **Progress** — live % with decimals
- **Best** — your all-time best on this level (normal %)
- **Session Best** — best this session
- **Attempts** — total attempts and session attempts
- **Jumps** — jumps in current attempt (plus total jumps tracker)
- **Playtime** — total playtime on level + session playtime
- **Average %** — average of your last 50 runs (where you die)
- **Consistency** — how often you pass 25% / 50% / 75% from 0%
- **Progress Bar** — visual bar from 0-100% for current run
- **New Best Highlight** — flashes green and plays SFX when you beat your best

### Why it's useful
- Stop guessing if you're improving — see consistency go up
- Track how much time you've *really* spent on a level (not the game's buggy timer)
- Know if your session is good or you should take a break (session best vs avg)

### Customizable
All labels can be toggled. Move HUD to any corner or custom position, scale it, change opacity, compact mode.

### Tracking details
- Per-level save — stored in `geode/mods/minlol.progressive-stats/save/level_stats.json`
- Supports online levels, editor levels, platformer levels
- Optionally tracks practice mode (disabled by default)
- Uses LevelID API if installed for 100% stable IDs on copied/editor levels
- Automatically backs up on game exit

### Coming from Death Tracker / Playtime Tracker?
This mod intentionally merges the useful parts of both into a single lightweight HUD. If you need full death graphs, keep Death Tracker. If you want minimal distraction with just what matters during a session, this is it.

Made for grinders, by a grinder.
