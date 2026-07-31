package io.github.minlol12.society.gui;

import java.util.ArrayList;
import java.util.List;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.Building;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Household;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.Archetype;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Skill;
import io.github.minlol12.society.core.types.Trait;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The villager stat sheet: everything the ledger knows about one person,
 * laid out as a six-row chest screen. Click a villager and this is what
 * opens.
 *
 * <p>Built entirely out of named item stacks, so a completely vanilla
 * client renders it perfectly - no client mod, no resource pack.</p>
 */
public final class CitizenScreen {

    private CitizenScreen() { }

    /** Opens the stat page for one citizen in front of a player. */
    public static void open(net.minecraft.server.network.ServerPlayerEntity player,
                            SocietyEngine engine, Citizen citizen, String vanillaProfessionId) {
        ReadOnlyScreen.open(player, title(engine, citizen),
                build(engine, citizen, vanillaProfessionId));
        player.getWorld().playSound(null, player.getBlockPos(),
                net.minecraft.sound.SoundEvents.ITEM_BOOK_PAGE_TURN,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.7f, 1.2f);
    }

    /** Layout of the 54-slot page. */
    private static final int SLOT_PORTRAIT = 4;
    private static final int SLOT_IDENTITY = 10;
    private static final int SLOT_WORK = 12;
    private static final int SLOT_HOME = 14;
    private static final int SLOT_WEALTH = 16;
    private static final int SLOT_PERSONALITY = 19;
    private static final int SLOT_SKILLS = 21;
    private static final int SLOT_FAMILY = 23;
    private static final int SLOT_REPUTATION = 25;
    private static final int SLOT_MEMORIES = 31;
    private static final int SLOT_SETTLEMENT = 29;
    private static final int SLOT_RELATIONS = 33;
    private static final int SLOT_TRAITS_FIRST = 37;

    /**
     * Builds the page for one citizen. Returns 54 stacks laid out for a
     * 9x6 container.
     */
    public static List<ItemStack> build(SocietyEngine engine, Citizen citizen,
                                        String vanillaProfessionId) {
        List<ItemStack> slots = new ArrayList<ItemStack>(54);
        for (int i = 0; i < 54; i++) {
            slots.add(ItemStack.EMPTY);
        }
        if (citizen == null) return slots;

        int day = engine.day();
        Settlement home = engine.settlements().get(citizen.homeSettlementId());

        // Frame the page so it reads as a card rather than a chest.
        ItemStack filler = pane();
        for (int i = 0; i < 9; i++) slots.set(i, filler.copy());
        for (int i = 45; i < 54; i++) slots.set(i, filler.copy());
        slots.set(9, filler.copy());
        slots.set(17, filler.copy());
        slots.set(18, filler.copy());
        slots.set(26, filler.copy());
        slots.set(27, filler.copy());
        slots.set(35, filler.copy());
        slots.set(36, filler.copy());
        slots.set(44, filler.copy());

        slots.set(SLOT_PORTRAIT, portrait(citizen, day));
        slots.set(SLOT_IDENTITY, identity(engine, citizen, day));
        slots.set(SLOT_WORK, work(citizen, vanillaProfessionId, home));
        slots.set(SLOT_HOME, homeCard(engine, citizen, home));
        slots.set(SLOT_WEALTH, wealth(engine, citizen));
        slots.set(SLOT_PERSONALITY, personality(citizen));
        slots.set(SLOT_SKILLS, skills(citizen));
        slots.set(SLOT_FAMILY, family(engine, citizen));
        slots.set(SLOT_REPUTATION, reputation(engine, citizen, home));
        slots.set(SLOT_SETTLEMENT, settlementCard(engine, home));
        slots.set(SLOT_MEMORIES, memories(citizen));
        slots.set(SLOT_RELATIONS, relations(engine, citizen, home));

        // A bar of eight traits along the bottom, each a little gauge.
        Trait[] traits = Trait.values();
        for (int i = 0; i < traits.length && SLOT_TRAITS_FIRST + i < 44; i++) {
            slots.set(SLOT_TRAITS_FIRST + i, traitStack(citizen, traits[i]));
        }
        return slots;
    }

    /** The title shown at the top of the screen. */
    public static Text title(SocietyEngine engine, Citizen citizen) {
        if (citizen == null) {
            return Text.literal("An Unrecorded Stranger").formatted(Formatting.DARK_GRAY);
        }
        Settlement home = engine.settlements().get(citizen.homeSettlementId());
        MutableText text = Text.literal(citizen.fullName()).formatted(Formatting.DARK_AQUA);
        if (home != null) {
            text.append(Text.literal(" of " + home.name()).formatted(Formatting.DARK_GRAY));
        }
        return text;
    }

    // =====================================================================
    // Cards
    // =====================================================================

    private static ItemStack portrait(Citizen citizen, int day) {
        int age = citizen.ageYears(day);
        Item icon = age < 16 ? Items.EGG
                : citizen.isElder(day) ? Items.BOOK
                : Items.PLAYER_HEAD;
        ItemStack stack = new ItemStack(icon);
        name(stack, citizen.fullName(), Formatting.GOLD);
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey(lifeStage(citizen, day) + ", " + age + " years old"));
        lore.add(grey(citizen.personality().archetype().display() + " by nature"));
        if (!citizen.isAlive()) {
            lore.add(line("Died on day " + citizen.deathDay(), Formatting.DARK_RED));
        } else if (citizen.isManifested()) {
            lore.add(line("Standing here before you", Formatting.GREEN));
        } else {
            lore.add(line("Lives in the ledger for now", Formatting.DARK_GRAY));
        }
        lore(stack, lore);
        return stack;
    }

    private static String lifeStage(Citizen citizen, int day) {
        int age = citizen.ageYears(day);
        if (age < 6) return "Infant";
        if (age < 16) return "Child";
        if (age < 25) return "Young adult";
        if (age < 45) return "Adult";
        if (age < 60) return "Greying";
        return "Elder";
    }

    private static ItemStack identity(SocietyEngine engine, Citizen citizen, int day) {
        ItemStack stack = new ItemStack(Items.NAME_TAG);
        name(stack, "Who They Are", Formatting.YELLOW);
        List<Text> lore = new ArrayList<Text>();
        lore.add(pair("Name", citizen.fullName()));
        lore.add(pair("Age", citizen.ageYears(day) + " years (born day " + citizen.birthDay() + ")"));
        lore.add(pair("Nature", citizen.personality().archetype().display()));
        lore.add(pair("Calling", citizen.preferredProfession() == SimProfession.NONE
                ? "still searching" : citizen.preferredProfession().display()));
        lore.add(blank());
        lore.add(grey(archetypeBlurb(citizen.personality().archetype())));
        lore(stack, lore);
        return stack;
    }

    private static String archetypeBlurb(Archetype archetype) {
        switch (archetype) {
            case WORKER: return "Happiest with dirt on their hands.";
            case ARTISAN: return "Makes things, and makes them well.";
            case MERCHANT: return "Can smell a bargain three roads off.";
            case EXPLORER: return "Always looking at the horizon.";
            case WARRIOR: return "First to the wall when the horn sounds.";
            case SAGE: return "Asks the question everyone else forgot.";
            case LEADER: return "People find themselves listening.";
            case CARETAKER: return "Notices who has not eaten today.";
            default: return "";
        }
    }

    private static ItemStack work(Citizen citizen, String vanillaProfessionId, Settlement home) {
        SimProfession profession = citizen.profession();
        ItemStack stack = new ItemStack(professionIcon(profession));
        name(stack, "Work", Formatting.YELLOW);
        List<Text> lore = new ArrayList<Text>();
        lore.add(pair("Trade", profession == SimProfession.NONE
                ? "no fixed trade" : profession.display()));
        Skill primary = profession.primarySkill();
        if (primary != null) {
            lore.add(pair("Craft level", bar(citizen.skillLevel(primary))
                    + " " + citizen.skillLevel(primary) + "/100"));
        }
        if (vanillaProfessionId != null && !vanillaProfessionId.isEmpty()
                && !vanillaProfessionId.endsWith("none")) {
            lore.add(pair("At the workstation", prettify(vanillaProfessionId)));
        }
        if (home != null) {
            // Where they actually work, if the town has built such a place.
            StructureType site = worksiteFor(home, profession);
            if (site != null) {
                lore.add(pair("Works at", "the " + site.display().toLowerCase()));
            }
        }
        lore(stack, lore);
        return stack;
    }

    /** Finds a finished building in town that suits this profession. */
    private static StructureType worksiteFor(Settlement s, SimProfession profession) {
        if (profession == SimProfession.NONE) return null;
        for (Building b : s.completedBuildings()) {
            if (b.type().worksite() == profession) return b.type();
        }
        return null;
    }

    private static ItemStack homeCard(SocietyEngine engine, Citizen citizen, Settlement home) {
        ItemStack stack = new ItemStack(Items.OAK_DOOR);
        name(stack, "Home", Formatting.YELLOW);
        List<Text> lore = new ArrayList<Text>();
        if (home == null) {
            lore.add(grey("Belongs to no settlement - a wanderer."));
        } else {
            lore.add(pair("Settlement", home.name() + " (" + home.tier().display() + ")"));
            lore.add(pair("Folk", home.culture().origin().folkName()));
            lore.add(pair("Founded", "day " + home.foundedDay()));
            Household household = engine.households().get(citizen.householdId());
            if (household != null) {
                lore.add(pair("House", "House " + household.familyName()));
            }
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack wealth(SocietyEngine engine, Citizen citizen) {
        ItemStack stack = new ItemStack(Items.EMERALD);
        name(stack, "Means", Formatting.YELLOW);
        List<Text> lore = new ArrayList<Text>();
        lore.add(pair("Purse", fmt(citizen.personalWealth()) + " emeralds"));
        Household household = engine.households().get(citizen.householdId());
        if (household != null) {
            lore.add(pair("Family coffer", fmt(household.wealth()) + " emeralds"));
        }
        lore.add(blank());
        lore.add(grey(wealthBlurb(citizen.personalWealth())));
        lore(stack, lore);
        return stack;
    }

    private static String wealthBlurb(double wealth) {
        if (wealth < 5) return "Owns little more than their clothes.";
        if (wealth < 25) return "Gets by, week to week.";
        if (wealth < 80) return "Comfortable, by village standards.";
        if (wealth < 200) return "Well off; people ask them for loans.";
        return "Rich enough that it is discussed.";
    }

    private static ItemStack personality(Citizen citizen) {
        ItemStack stack = new ItemStack(Items.BOOK);
        name(stack, "Temperament", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey("A " + citizen.personality().archetype().display().toLowerCase()
                + " at heart."));
        lore.add(blank());
        for (Trait trait : citizen.personality().topTraits(3)) {
            lore.add(pair(trait.display(),
                    bar(citizen.personality().get(trait)) + " " + citizen.personality().get(trait)));
        }
        lore.add(blank());
        lore.add(grey("Full temperament shown along the bottom row."));
        lore(stack, lore);
        return stack;
    }

    private static ItemStack skills(Citizen citizen) {
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        name(stack, "Skills", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();
        boolean any = false;
        for (Skill skill : Skill.values()) {
            int level = citizen.skillLevel(skill);
            if (level <= 0) continue;
            any = true;
            lore.add(pair(skill.display(), bar(level) + " " + level));
        }
        if (!any) {
            lore.add(grey("Has not yet learned a trade."));
        } else {
            Skill best = citizen.bestSkill();
            if (best != null && citizen.bestSkillLevel() >= 75) {
                lore.add(blank());
                lore.add(line("A master of " + best.display().toLowerCase() + ".",
                        Formatting.GOLD));
            }
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack family(SocietyEngine engine, Citizen citizen) {
        ItemStack stack = new ItemStack(Items.CAKE);
        name(stack, "Family", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();

        Citizen spouse = engine.citizens().get(citizen.spouseId());
        lore.add(pair("Married to", spouse == null ? "unwed" : spouse.fullName()));

        Citizen mother = engine.citizens().get(citizen.motherId());
        Citizen father = engine.citizens().get(citizen.fatherId());
        if (mother != null || father != null) {
            lore.add(pair("Mother", mother == null ? "unknown" : mother.fullName()));
            lore.add(pair("Father", father == null ? "unknown" : father.fullName()));
        }

        List<String> children = citizen.childrenIds();
        if (!children.isEmpty()) {
            lore.add(blank());
            lore.add(pair("Children", String.valueOf(children.size())));
            int shown = 0;
            for (String id : children) {
                Citizen child = engine.citizens().get(id);
                if (child == null) continue;
                lore.add(grey("  " + child.firstName()
                        + (child.isAlive() ? "" : " (departed)")));
                if (++shown >= 4) break;
            }
        }

        Household household = engine.households().get(citizen.householdId());
        if (household != null && !household.tradition().isEmpty()) {
            lore.add(blank());
            lore.add(line(household.tradition(), Formatting.DARK_AQUA));
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack reputation(SocietyEngine engine, Citizen citizen, Settlement home) {
        ItemStack stack = new ItemStack(Items.GOLDEN_APPLE);
        name(stack, "Standing", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();
        lore.add(pair("Reputation", bar(citizen.reputation()) + " " + citizen.reputation() + "/100"));
        lore.add(grey(reputationBlurb(citizen.reputation())));
        if (home != null && home.government() != null) {
            if (citizen.id().equals(home.government().leaderId())) {
                lore.add(blank());
                lore.add(line(home.government().type().leaderTitle() + " of " + home.name(),
                        Formatting.GOLD));
            } else if (home.government().councilIds().contains(citizen.id())) {
                lore.add(blank());
                lore.add(line("Sits on the council of " + home.name(), Formatting.YELLOW));
            }
        }
        if (citizen.isFamous()) {
            lore.add(line("Their name is known beyond this valley.", Formatting.GOLD));
        }
        lore(stack, lore);
        return stack;
    }

    private static String reputationBlurb(int reputation) {
        if (reputation >= 85) return "Respected by everyone who knows them.";
        if (reputation >= 65) return "Well thought of.";
        if (reputation >= 40) return "An ordinary standing.";
        if (reputation >= 20) return "Somewhat mistrusted.";
        return "People cross the square to avoid them.";
    }

    private static ItemStack settlementCard(SocietyEngine engine, Settlement home) {
        ItemStack stack = new ItemStack(Items.BELL);
        name(stack, "Their Town", Formatting.AQUA);
        List<Text> lore = new ArrayList<Text>();
        if (home == null) {
            lore.add(grey("No town claims them."));
            lore(stack, lore);
            return stack;
        }
        lore.add(pair("Name", home.name()));
        lore.add(pair("Size", home.tier().display() + ", "
                + home.cachedPopulation() + " souls"));
        lore.add(pair("Beds", String.valueOf(home.bedCapacity())));
        lore.add(pair("Buildings", home.completedBuildings().size()
                + " standing, " + home.sitesUnderConstruction().size() + " rising"));
        lore.add(pair("Morale", bar((int) home.morale()) + " " + Math.round(home.morale())));
        lore.add(pair("Granary", ((int) home.stock(Good.FOOD)) + " food"));
        if (home.government() != null) {
            Citizen leader = engine.leaderOf(home);
            lore.add(pair(home.government().type().leaderTitle(),
                    leader == null ? "vacant" : leader.fullName()));
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack memories(Citizen citizen) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        name(stack, "What They Remember", Formatting.AQUA);
        List<Text> lore = new ArrayList<Text>();
        List<String> memories = citizen.memories();
        if (memories.isEmpty()) {
            lore.add(grey("Nothing worth telling has happened yet."));
        } else {
            int from = Math.max(0, memories.size() - 6);
            for (int i = from; i < memories.size(); i++) {
                lore.add(grey(memories.get(i)));
            }
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack relations(SocietyEngine engine, Citizen citizen, Settlement home) {
        ItemStack stack = new ItemStack(Items.COMPASS);
        name(stack, "Life So Far", Formatting.AQUA);
        List<Text> lore = new ArrayList<Text>();
        int day = engine.day();
        lore.add(pair("Born", "day " + citizen.birthDay()));
        if (citizen.lastChildBornDay() > 0) {
            lore.add(pair("Last child", "day " + citizen.lastChildBornDay()));
        }
        if (home != null) {
            lore.add(pair("Lives in", home.name()));
            lore.add(pair("Season", engine.season().display()));
        }
        lore.add(pair("Today", "day " + day));
        lore(stack, lore);
        return stack;
    }

    private static ItemStack traitStack(Citizen citizen, Trait trait) {
        int value = citizen.personality().get(trait);
        ItemStack stack = new ItemStack(traitIcon(trait));
        MutableText title = Text.literal(trait.display() + ": ")
                .formatted(Formatting.WHITE)
                .append(Text.literal(String.valueOf(value)).formatted(traitColour(value)));
        stack.setCustomName(title.styled(style -> style.withItalic(Boolean.FALSE)));
        stack.setCount(Math.max(1, Math.min(64, value)));
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey(bar(value)));
        lore.add(grey(trait.description()));
        lore.add(blank());
        lore.add(grey(traitBlurb(trait, value)));
        lore(stack, lore);
        return stack;
    }

    private static String traitBlurb(Trait trait, int value) {
        String strength = value >= 80 ? "Very high" : value >= 60 ? "High"
                : value >= 40 ? "Middling" : value >= 20 ? "Low" : "Very low";
        return strength + " - " + trait.description() + ".";
    }

    private static Formatting traitColour(int value) {
        if (value >= 75) return Formatting.GREEN;
        if (value >= 50) return Formatting.YELLOW;
        if (value >= 25) return Formatting.GOLD;
        return Formatting.RED;
    }

    private static Item traitIcon(Trait trait) {
        switch (trait) {
            case INDUSTRY: return Items.IRON_HOE;
            case SOCIABILITY: return Items.CAKE;
            case AMBITION: return Items.GOLD_INGOT;
            case CURIOSITY: return Items.SPYGLASS;
            case AGGRESSION: return Items.IRON_SWORD;
            case CAUTION: return Items.SHIELD;
            case GENEROSITY: return Items.BREAD;
            case WISDOM: return Items.ENCHANTED_BOOK;
            default: return Items.PAPER;
        }
    }

    private static Item professionIcon(SimProfession profession) {
        switch (profession) {
            case FARMER: return Items.WHEAT;
            case LUMBERJACK: return Items.IRON_AXE;
            case MINER: return Items.IRON_PICKAXE;
            case BUILDER: return Items.BRICKS;
            case CRAFTER: return Items.CRAFTING_TABLE;
            case TRADER: return Items.EMERALD;
            case SCHOLAR: return Items.BOOKSHELF;
            case HEALER: return Items.GLASS_BOTTLE;
            case GUARD: return Items.IRON_SWORD;
            case STEWARD: return Items.PAPER;
            default: return Items.STICK;
        }
    }

    // =====================================================================
    // Formatting helpers
    // =====================================================================

    /** A ten-segment gauge; reads at a glance without any client mod. */
    private static String bar(int value) {
        int filled = Math.max(0, Math.min(10, (value + 5) / 10));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? '\u25a0' : '\u25a1');
        }
        return sb.toString();
    }

    private static ItemStack pane() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.setCustomName(Text.literal(" "));
        return stack;
    }

    private static void name(ItemStack stack, String text, Formatting colour) {
        stack.setCustomName(Text.literal(text).formatted(colour)
                .styled(style -> style.withItalic(Boolean.FALSE)));
    }

    private static void lore(ItemStack stack, List<Text> lines) {
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (Text line : lines) {
            list.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(line)));
        }
        stack.getOrCreateSubNbt("display").put("Lore", list);
    }

    private static Text pair(String label, String value) {
        return Text.literal(label + ": ").formatted(Formatting.GRAY)
                .append(Text.literal(value).formatted(Formatting.WHITE))
                .styled(style -> style.withItalic(Boolean.FALSE));
    }

    private static Text grey(String text) {
        return Text.literal(text).formatted(Formatting.GRAY)
                .styled(style -> style.withItalic(Boolean.FALSE));
    }

    private static Text line(String text, Formatting colour) {
        return Text.literal(text).formatted(colour)
                .styled(style -> style.withItalic(Boolean.FALSE));
    }

    private static Text blank() {
        return Text.literal("");
    }

    private static String prettify(String id) {
        String name = id;
        int colon = name.indexOf(':');
        if (colon >= 0) name = name.substring(colon + 1);
        name = name.replace('_', ' ');
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String fmt(double value) {
        long rounded = Math.round(value * 10.0);
        return (rounded / 10) + "." + Math.abs(rounded % 10);
    }
}
