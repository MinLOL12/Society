# Society

**Villagers wake up.** Society turns Minecraft's villagers from passive
trading kiosks into people - with personalities, families, jobs that emerge
from real needs, and settlements that **physically build themselves** from a
campfire circle into a city, block by block, while you watch.

Click any villager to open their full stat sheet. Walk away for a week and
come back to find new houses, a granary, a marketplace and a watchtower that
weren't there before - raised by builders, paid for in real timber and stone.

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
grow with practice.

**Click a villager to open their page** - a full screen showing who they
are, their work and workplace, their temperament as eight labelled gauges,
every skill they have learned, their family tree, their standing in town,
and what they remember. Sneak-click with an empty hand, or right-click
holding the **Society Chronicle**. It is drawn entirely with vanilla items,
so it works on an unmodified client.

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

Guards are deliberately scarce: a town keeps a small standing watch
(around twenty-five for an ordinary village) and is hard-capped at sixty,
so the rest of its people farm, build and trade instead of all taking up
swords.

When monsters threaten, a village reacts like people, not a swarm. Only
the brave and the aggressive charge; the cautious turn and run. Children
never engage - they flee to safety.

### 5. Settlements really build

This is not a counter going up. Each day a settlement works out what it most
needs - a roof, a field, a forge, a wall - stakes out a plot for it, pays for
it out of its own timber and stone, and puts its builders on the site. The
world places the blocks course by course as the work is done, so you can
stand in a village and watch a house go up over several days.

There are **55 structures** across seven purposes:

| Purpose | Buildings |
| --- | --- |
| Civic | Well, Bell Plaza, Notice Board, Meeting Hall, Town Hall, Fountain, Public Garden, Graveyard |
| Housing | Lean-to Shelter, Cottage, Family House, Longhouse, Townhouse, Manor |
| Food | Farm Plot, Great Field, Orchard, Animal Pen, Barn, Granary, Windmill, Bakery, Apiary, Fishing Hut |
| Industry | Lumber Camp, Sawmill, Mine Head, Quarry, Smithy, Foundry, Carpenter, Mason's Yard, Weaver, Tannery, Pottery Kiln, Brewery |
| Trade | Market Stall, Marketplace, Warehouse, Trading Post, Inn, Stable, Dock |
| Knowledge | Shrine, Library, School, Apothecary, Infirmary, Observatory, Bathhouse |
| Defence | Guard Post, Watchtower, Barracks, Gatehouse, Wall |

Every one of them **does something**. A sawmill means more planks; a library
means faster research; a granary widens the stores; a fountain lifts spirits;
a watchtower adds to the town's defence; an infirmary means fewer deaths.
Nothing in the catalogue is scenery.

Society does not fabricate substitute houses from hard-coded blocks. Village
buildings are stamped from the authored NBT templates supplied by CTOV, with
AdoraBuild: Structures adding its own authored structures to world generation.
A Society site is rejected unless its entire plot is dry, level, clear, and
solid; it will not fill water, cut into a hill, or overlap an existing build.
Homes also keep six clear blocks from other homes.

Crucially, **housing is now the beds that actually exist**. A settlement can
only grow as far as it has built houses to sleep in, so the tier ladder -

**Camp → Hamlet → Village → Town → City**

is something a town has to earn with timber and labour, not something that
happens to it. Buildings are laid out in real culture styles too: the same
cottage blueprint becomes a mossy log lodge among the Woodfolk, a granite
hall among the Stonefolk and a flat-roofed sandstone house among the
Sandfolk.

Fire and war leave real ruins, and the builders have to come back and raise
them again.

Towns put expansion first. They keep several sites rising at once, builders
make faster progress the more hands are on a plot, and blocks go up briskly
- so a village visibly grows rather than while away its days on festivals.
Festivals are now occasional affairs, thrown only by thriving towns in high
spirits.

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
old frictions. The elders do not act the moment a score crosses a line:
every day there is only a *chance* they act on it, so nothing ever feels
scripted. Warmth may ripen into a trade pact, then an alliance; cold,
competing neighbours can slide into war. And once in a great while two
states decide to become one:

* **Peaceful union** - two sworn allies that have trusted one another for
  a long time, where one town has clearly outgrown the other, may simply
  fold the small state into the large one.
* **Conquest** - a decisive, quick victory in war can end in outright
  annexation instead of tribute: the victor swallows the vanquished town,
  its people, its buildings and its treasury, and the map redraws itself
  in a single day.

Wars have battles, plunder, scores, truces and tribute, and they are
written into both towns' chronicles and the world's.

Trade also reaches **across the map**. Established merchant towns with a
marketplace, trading post or dock send caravans down long roads and, over
time, hear of distant towns far beyond a scout's range - so complementary
civilizations find each other and trade even across thousands of blocks.

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

* Right-click air/ground → the settlement page: population, stores, every
  building standing, everything under construction and how far along it is,
  what the town is waiting on materials for, government, culture, known
  arts, neighbours and its chronicle.
* Right-click a villager → their personal page.

You do not need the Chronicle to read people: **sneak-click any villager
with an empty hand** and their page opens.

Any `[Society]` chat message about a settlement ends in a clickable
**[Teleport]** that drops you at that settlement's most recently finished
building - so the moment a town raises something, you can go and stand in
it. The same trip is available on demand with `/society visit <name>`.

## The War Baton

Craft: **stick + iron ingot + redstone** (shapeless). It looks like a
plain stick, but it carries a herald's authority.

* Right-click (the ground, or just the air) near one village to **mark**
  it. The baton's name shows the mark, and it is judged from where you
  stand - no need to click a specific block.
* Right-click near a second, different village to **declare war** between
  the two - instantly, no matter how far apart they are or how friendly
  they were. The decree is written into both chronicles and the world's.
* Right-click in the wilderness (or at the marked village) to **clear**
  the mark; sneak-right-click in the air to read it back.

Wars started this way play out like any other: battles, plunder, war
scores, and a truce or tribute at the end. When the decree lands, the two
towns' soldiers actually take the field - visible raiders march out of one
town and fight the defenders of the other - and every battle is written
into both chronicles and the world's. The same decree can be issued
without the baton via `/society war <a> <b>`.

## The Political Map

Run `/society map` to open a political map of every city-state in the
world. Each state's land is painted in its own colour - the larger the
town, the wider its claim reaches - with white borders drawn where one
state's ground meets another's, and the wilderness between them left
black. A banner marks every capital, and the legend below names each
power, its government, its population and its treaties. Your home
settlement is marked "(home)". Hover any province to read which
city-state holds it and how far you are from its capital. The map
redraws itself live: annex a neighbour and its colour disappears from
the map in the same day.

## The Setter Stick & player structures

Craft: **stick + name tag** (shapeless). Players can raise their own
buildings alongside the villagers':

* **Premade NBT catalogue.** `/society structure list` shows all 55
  authored structures (houses, smithies, town halls, watchtowers, ...)
  with their CTOV NBT templates. Stand where you want it and run
  `/society structure place <type>` - the building is stamped into the
  world from its NBT file, centred on you, and the nearest settlement
  counts it among its real buildings (beds, defence, research and all).
  You can also stamp any specific NBT file with
  `/society structure place <type> <namespace:path>`.
* **Claim your own build.** Right-click two opposite corners of a
  structure you built with the Setter Stick, sneak-right-click a block to
  cycle what kind of structure it is (government building, housing, food,
  industry, trade, knowledge, defence, or custom), name it with
  `/society structure name <label>`, then `/society structure claim`.
  A bell rings onto the claim so everyone can see the ground is spoken
  for. `/society structure mine` lists your claims;
  `/society structure remove <id>` unclaims one.

## Government buildings & multiplayer

A structure labelled a **government building** (or a Town Hall or Meeting
Hall) is the seat of rule for its settlement. Players claim government
buildings with the Setter Stick and are crowned there - see roles below.
Player-built structures, government buildings and sovereigns all appear on
the settlement page and in `/society settlement <name> government`.

## Roles: worker, blacksmith, farmer ... king & queen

`/society role set <role>` makes you a **Worker, Farmer, Blacksmith,
Miner, Builder, Crafter, Trader, Scholar, Healer, Guard or Steward** of
the world. While you hold a role and belong to a settlement (your home is
picked from where you stand, or set with `/society role home <name>`), the
settlement quietly benefits from your craft: a blacksmith's tools, a
farmer's food, a scholar's research.

The **King and Queen** are different. They are not chosen - they are
*crowned*. Stand at a government building (claimed with the Setter Stick)
or a Town Hall and run `/society role crown` (or `crown queen`). The
crowned player rules that settlement in the ledger: morale and taxes rise
under their reign, and the throne shows on every page of the town. One
player per throne; `/society role abdicate` steps down.

## The custom economy

The world's currency - **Coins** by default - is yours and your friends'
to manage. It is backed by the real wealth of every settlement on the
map, and its value is honest about the printing press:

* `/society economy print <n>` mints new notes. **Print too much and the
  currency becomes worthless** - the world stops believing in notes it
  cannot back, and every treasury's purchasing power quietly erodes.
* `/society economy burn <n>` destroys notes. **Print too little and each
  note becomes extremely valuable** - scarcer than the wealth behind it,
  a single coin buys the town.
* `/society economy name <name>` renames the currency.
* `/society economy give <player> <n>` mints and hands over (inflating the
  world as it does); `/society economy pay <player> <n>` is a straight
  transfer between friends; `/society economy balance` shows your purse.
* `/society economy` shows the supply, backing, and the current value of
  one note, plus how healthy the money supply is.

The ledger's treasuries are denominated in this currency, so inflation
bites everywhere at once - and the whole world hears when the money starts
to rot.

## The kid update

Baby villagers finally act their age. They run around playfully, chase
each other and play tag around the settlement, swapping chaser and chased
when they catch one another. But the moment anything bad happens - a war
is declared, raiders march, monsters threaten, famine bites - the children
stop playing, run inside the nearest house, and stay there until the
trouble passes.

## Commands

```
/society                                help
/society day                            ledger calendar, soul & settlement counts
/society settlements                    every living settlement, briefly
/society map                            the political map of the city-states
/society settlement <name>              a settlement's overview page
/society settlement <name> economy      stocks, prices, workforce, needs, routes
/society settlement <name> tech         known arts and research within reach
/society settlement <name> culture      folk, styles, festival, accumulated facts
/society settlement <name> diplomacy    regards, treaties, trade, wars
/society settlement <name> government   ruler, laws, council, player sovereigns
/society settlement <name> buildings    what stands, what is rising, what is blocked
/society villager <entity>              a person's own page (entity selector)
/society visit <name>                   teleport to a settlement's newest building
/society history [count]                last entries of the world chronicle

/society war <a> <b>                    decree war between two settlements

/society structure list                 the premade NBT structure catalogue
/society structure place <type>         stamp a premade structure where you stand
/society structure place <type> <nbt>   stamp any namespace:path NBT file
/society structure claim                claim the box marked with the Setter Stick
/society structure kind <kind>          set the kind of the pending claim
/society structure name <label>         label the pending claim
/society structure mine                 your claimed structures
/society structure info <type>          details and NBT paths of one structure
/society structure remove <id>          unclaim a structure
/society build place <type>             alias for structure place

/society economy                        supply, backing and value of the currency
/society economy print <n>              mint notes (inflation)
/society economy burn <n>               destroy notes (deflation)
/society economy name <name>            rename the currency
/society economy give <player> <n>      print notes and give them away
/society economy balance                your purse
/society economy pay <player> <n>       transfer notes between players

/society role                           your role, home and throne
/society role list                      every role you can play
/society role set <role>                become a worker, farmer, blacksmith, ...
/society role home <name>               belong to a settlement
/society role crown [queen]             be crowned at a government building
/society role abdicate                  step down from the throne
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
| `buildStructures` | true | settlements plan and raise real buildings |
| `villagerScreen` | true | clicking a villager opens the stat screen (false = chat text) |
| `sneakToInspect` | true | sneak-click with an empty hand to open a villager's page |

## Build

Requires JDK 17 and network access to the Fabric maven repositories:

```
./gradlew build
```

produces `build/libs/society-<version>.jar`. Drop it into a Fabric
1.20.1 server's `mods/` together with Fabric API. For authored village and
cooking content, also install the declared runtime mods: CTOV (6459787),
AdoraBuild: Structures (5714699), Chef's Delight [Fabric] (7335194), and its
required Farmer's Delight dependency (8547913).

**Note on Chef's Delight:** Chef's Delight 1.0.4+ (Fabric) depends on
Structurized Reborn (https://modrinth.com/mod/sructurized-reborn) to register
its village structures, but does not list it in its own mod metadata.
Society therefore pulls it automatically as a runtime dependency when Chef's
Delight is present in the dev environment (and you should install it too
if you are using Chef's Delight in a modpack or client). The Maven coordinate
used is `maven.modrinth:Wd844r7Q:9bAkNJm5` (file/version ID for the 1.20.1 Fabric build).

## Headless simulation

The whole engine can run without Minecraft:

```
java -cp build/classes/java/main io.github.minlol12.society.core.headless.HeadlessSimulation
```

runs a 400-day, three-settlement scenario with save/load round-trips and a
strict determinism check - handy for tuning and regression testing. It
asserts that the towns really build: that buildings of several kinds go up,
that houses supply the beds people sleep in, and that housing capacity is
exactly the beds that exist.

The building catalogue has its own audit:

```
java -cp build/classes/java/main io.github.minlol12.society.core.headless.BlueprintAudit
```

which checks every one of the 55 blueprints - that each fits its plot, that
anything enclosed has a door, a roof and light, that houses contain the beds
they promise, that workshops contain a workstation, and that a settlement of
each size can plan enough beds for its people.

## Credit

By MinLOL12. MIT licensed.
