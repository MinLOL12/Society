# Society

**Villagers wake up.** Society turns Minecraft's villagers from passive
trading kiosks into people - with personalities, families, jobs that emerge
from real needs, settlements that grow from campfires into cities, markets,
research, cultures, diplomacy, politics, and a written history of everything
that ever happened.

Server-side mod for **Minecraft 1.20.1 + Fabric**. Vanilla clients can join
a Society server without installing anything.

---

## How it works

Society keeps a *ledger* of the whole world and advances it one in-game day
at a time. Villager entities you can see are the embodied part of that ledger;
the rest keeps living even when no player is nearby. When you walk into a
town, ledger-only citizens who live there will step into the world to meet
you (up to a sane entity cap, and only where players can actually see).

The ledger lives in the world save (`society` persistent state) and survives
restarts. The simulation core under `core/` is deliberately free of any
Minecraft class, so it runs (and is tested) headlessly.

### 1. Villagers are individuals

Every villager you meet becomes a recorded citizen with a name, an age, a
reputation, personal wealth, up to eight vivid memories, and skills that
grow with practice. Tap one with the **Society Chronicle** to read their
personal page.

### 2. Personalities

Each person rolls values across eight traits (Bravery, Kindness, Industry,
Curiosity, Honor, Ambition, Sociability, Patience), which collapse into an
archetype - Dreamer, Grumbler, Stalwart, Firebrand, ... - with aptitudes
that steer which work they love. Children inherit a blend of their parents'
traits, nudged by their culture's virtue.

### 3. Families and generations

Citizens court, marry (chat announcement and all), move into households,
and have children who grow up, inherit, and remember. Houses accumulate
wealth and a *hereditary profession* - a line of masons really stays a line
of masons. Foundlings (baby villagers that appear on their own) are adopted
into nearby families when possible.

### 4. Professions emerge from needs

Nobody assigns jobs. Each settlement computes *needs* from its stocks and
population: empty granaries cry out for farmers, growth cries out for
builders and lumberjacks, research for scholars, threats for guards. Each
day a few citizens are matched to the most-needed work they have an
aptitude for. Elders retire; crises retrain people into food work.

### 5. Settlements grow

Clusters of villagers with beds and a meeting place are found as a
settlement and named after their land's folk. Population, housing, stock
and threats feed a tier ladder:

**Camp → Hamlet → Village → Town → City**

Tiers expand housing and storage capacity, change the government, and make
history when reached.

### 6. A living economy

Every day citizens produce goods (food, wood, stone, tools, ...) according
to profession, culture bonuses, technology and season; everyone eats;
surpluses are stockpiled. Prices float with supply and demand. Settlements
with a trade pact build routes and caravans move goods between them,
settling in emeralds. Depots run dry, famines bite (configurable
lethality), emigrants leave hopeless towns for better neighbours.

### 7. Knowledge and technology

Scholars turn settlement surplus into research, discovering practical
arts - Crop Rotation, Masonry, Medicine, Coinage, and more - each with
visible economic effects. Discoveries are recorded, celebrated, and slowly
diffuse to allied settlements. There are no universal crafts: what a town
knows is part of who it is.

### 8. Cultures

The biome a settlement grows in shapes its culture: Plainsfolk, Woodfolk,
Mountainfolk, Saltfolk, Sandfolk, Frostfolk, Leaffolk, Mirefolk each get
their own virtue, building style, dress, festival, names and production
specialties. Cultures accumulate facts - foundings, discoveries, survived
disasters - told on the culture page.

### 9. Diplomacy

Settlements within travel distance make first contact and start forming
opinions - from shared blood, complementary economies, trade volume and
old frictions. Warmth leads to trade pacts, routes, and alliances; cold,
competing neighbours can slide into war. Wars have battles, plunder,
scores, truces and tribute, and they are written into both towns'
chronicles and the world's.

### 10. Politics

Each town is ruled according to its size and spirit: an Elder Council, a
Chiefdom with hereditary succession, a Mayoralty, a Free Council or a
Merchant League in great trade cities. Leaders are elected, succeed, and
can be deposed when morale collapses; councils are drawn from respected
citizens; laws follow the culture's virtue and the government form.

### 11. History

Everything above is *written down*: each settlement keeps a chronicle, and
the world keeps a master chronicle of foundings, wars, disasters,
discoveries, deaths of heroes and migrations - up to 1500 entries, saved
with the world. Right-click anywhere with the Society Chronicle to read it.

---

## The Society Chronicle

Craft: **book + emerald** (shapeless).

* Right-click air/ground → the page of the settlement holding this land
  (or the world chronicle in the wilderness).
* Right-click a villager → that person's page: age, work, personality,
  family, wealth, reputation, and what they remember.

## Commands

```
/society                                help
/society day                            ledger calendar, soul & settlement counts
/society settlements                    every living settlement, briefly
/society settlement <name>              a settlement's overview page
/society settlement <name> economy      stocks, prices, workforce, needs, routes
/society settlement <name> tech         known arts and research within reach
/society settlement <name> culture      folk, styles, festival, accumulated facts
/society settlement <name> diplomacy    regards, treaties, trade, wars
/society settlement <name> government   ruler, laws, council
/society villager <entity>              a person's own page (entity selector)
/society history [count]                last entries of the world chronicle
```

## Configuration

`config/society.json` (created on first run):

| Key | Default | Meaning |
| --- | --- | --- |
| `maxSettlements` | 24 | cap on simultaneous living settlements |
| `maxCitizensPerSettlement` | 160 | ledger population cap per settlement |
| `seasonLengthDays` | 30 | in-game days per season (drives harvests & festivals) |
| `enableMigration` | true | unhappy citizens may emigrate to friendlier towns |
| `enableManifestSpawns` | true | ledger citizens may step into the world near players |
| `warCasualties` | false | whether battles can kill citizens outright |
| `plagueCasualties` | true | whether plague events can kill |
| `famineCasualties` | true | whether long famines can kill |
| `announcements` | true | enable `[Society]` chat announcements |
| `announcementRadius` | 160 | radius for local announcements |
| `dailyAnnouncementBudget` | 4 | max local announcements per settlement per day |

## Build

Requires JDK 17 and network access to the Fabric maven repositories:

```
./gradlew build
```

produces `build/libs/society-<version>.jar`. Drop it into a Fabric
1.20.1 server's `mods/` together with Fabric API.

## Headless simulation

The whole engine can run without Minecraft:

```
java -cp build/classes/java/main io.github.minlol12.society.core.headless.HeadlessSimulation
```

runs a 400-day, three-settlement scenario with save/load round-trips and a
strict determinism check - handy for tuning and regression testing.

## Credit

By MinLOL12. MIT licensed.
