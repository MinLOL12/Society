package io.github.minlol12.society.command;

import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import io.github.minlol12.society.SocietyManager;
import io.github.minlol12.society.SocietyText;
import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Currency;
import io.github.minlol12.society.core.data.PlayerData;
import io.github.minlol12.society.core.data.PlayerStructure;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.system.PlayerRoleSystem;
import io.github.minlol12.society.core.types.PlayerRole;
import io.github.minlol12.society.core.types.PlayerStructureKind;
import io.github.minlol12.society.item.SetterStickItem;
import io.github.minlol12.society.item.SocietyItems;
import io.github.minlol12.society.world.CultureSamplerImpl;
import io.github.minlol12.society.world.PlayerStructurePlacer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The {@code /society} command tree - windows into the ledger, plus the
 * player-facing controls for wars, roles, the custom economy and the
 * Setter Stick:
 *
 * <pre>
 * /society                                  a short help
 * /society day | settlements | history      ledger windows
 * /society settlement &lt;name&gt; [section]     one settlement's page
 * /society villager &lt;entity&gt;               a villager's personal page
 * /society visit &lt;name&gt;                   teleport to a settlement
 * /society war &lt;a&gt; &lt;b&gt;                    decree war without the baton
 * /society structure ...                   the Setter Stick &amp; premade NBT list
 * /society build place &lt;type&gt;             alias for structure place
 * /society economy ...                     the player-managed currency
 * /society role ...                        roles from worker to king/queen
 * </pre>
 */
public final class SocietyCommands {

    private static final String[] SECTIONS = {
        "info", "economy", "tech", "culture", "diplomacy", "government", "buildings"
    };

    private SocietyCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("society");

            root.executes(context -> withPlayer(context.getSource(), (player, engine) -> {
                send(player, "--- Society ---", Formatting.GOLD);
                send(player, "/society day - the ledger's calendar", Formatting.GRAY);
                send(player, "/society settlements - every living settlement", Formatting.GRAY);
                send(player, "/society settlement <name> [info|economy|tech|culture|"
                        + "diplomacy|government|buildings]", Formatting.GRAY);
                send(player, "/society villager <entity> - a person's own page", Formatting.GRAY);
                send(player, "/society visit <name> - teleport to a settlement's newest building",
                        Formatting.GRAY);
                send(player, "/society war <a> <b> - decree war between two settlements",
                        Formatting.GRAY);
                send(player, "/society structure ... - claim structures with the Setter Stick",
                        Formatting.GRAY);
                send(player, "/society economy ... - print, burn and trade the world's currency",
                        Formatting.GRAY);
                send(player, "/society role ... - be a worker, a blacksmith, even king or queen",
                        Formatting.GRAY);
                send(player, "/society history [count] - the world chronicle", Formatting.GRAY);
                send(player, "Click a villager (sneak, or hold the Chronicle) to open their page.",
                        Formatting.DARK_GRAY);
                send(player, "Craft the War Baton (stick + iron + redstone) and click two villages"
                        + " to set them at war.", Formatting.DARK_GRAY);
                send(player, "Craft the Setter Stick (stick + name tag) to claim your own"
                        + " buildings.", Formatting.DARK_GRAY);
            }));

            root.then(CommandManager.literal("day").executes(context ->
                    withPlayer(context.getSource(), SocietyText::printDayLine)));

            root.then(CommandManager.literal("settlements").executes(context ->
                    withPlayer(context.getSource(), SocietyText::printSettlementList)));

            root.then(buildSettlementSubcommand());

            root.then(CommandManager.literal("visit")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(context -> visitSettlement(context))));

            root.then(CommandManager.literal("villager")
                    .then(CommandManager.argument("entity", EntityArgumentType.entity())
                            .executes(context -> {
                                Entity entity = EntityArgumentType.getEntity(context, "entity");
                                if (!(entity instanceof VillagerEntity)) {
                                    context.getSource().sendError(
                                            Text.literal("That is no villager."));
                                    return 0;
                                }
                                VillagerEntity villager = (VillagerEntity) entity;
                                return withPlayer(context.getSource(), (player, engine) -> {
                                    Citizen citizen = engine.citizenForEntity(
                                            villager.getUuidAsString());
                                    SocietyText.printCitizenCard(player, engine, citizen,
                                            villager.getVillagerData().getProfession().toString());
                                });
                            })));

            root.then(CommandManager.literal("history")
                    .executes(context -> withPlayer(context.getSource(),
                            (player, engine) -> SocietyText.printWorldHistory(player, engine, 10)))
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 30))
                            .executes(context -> withPlayer(context.getSource(),
                                    (player, engine) -> SocietyText.printWorldHistory(player, engine,
                                            IntegerArgumentType.getInteger(context, "count"))))));

            root.then(buildWarSubcommand());
            root.then(buildStructureSubcommand());
            root.then(buildBuildAliasSubcommand());
            root.then(buildEconomySubcommand());
            root.then(buildRoleSubcommand());

            dispatcher.register(root);
        });
    }

    // =====================================================================
    // War
    // =====================================================================

    private static LiteralArgumentBuilder<ServerCommandSource> buildWarSubcommand() {
        return CommandManager.literal("war")
                .then(CommandManager.argument("first", StringArgumentType.word())
                        .then(CommandManager.argument("second", StringArgumentType.word())
                                .executes(context -> {
                                    String first = StringArgumentType.getString(context, "first");
                                    String second = StringArgumentType.getString(context, "second");
                                    return withPlayer(context.getSource(), (player, engine) -> {
                                        Settlement a = engine.findSettlementByName(first);
                                        if (a == null) {
                                            send(player, "No settlement answers to '" + first + "'.",
                                                    Formatting.RED);
                                            return;
                                        }
                                        Settlement b = engine.findSettlementByName(second);
                                        if (b == null) {
                                            send(player, "No settlement answers to '" + second + "'.",
                                                    Formatting.RED);
                                            return;
                                        }
                                        if (engine.declareWar(a, b)) {
                                            send(player, "WAR! " + a.name() + " and " + b.name()
                                                    + " are now at war - their warriors will meet"
                                                    + " on the field.", Formatting.RED);
                                        } else {
                                            send(player, a.name() + " and " + b.name()
                                                    + " are already at war.", Formatting.YELLOW);
                                        }
                                    });
                                })));
    }

    // =====================================================================
    // Structures (Setter Stick + premade NBT catalogue)
    // =====================================================================

    private static LiteralArgumentBuilder<ServerCommandSource> buildStructureSubcommand() {
        return CommandManager.literal("structure")
                .then(CommandManager.literal("list").executes(context ->
                        withPlayer(context.getSource(), SocietyCommands::printStructureList)))
                .then(CommandManager.literal("mine").executes(context ->
                        withPlayer(context.getSource(), SocietyCommands::printMyStructures)))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .executes(context -> {
                                    String typeName = StringArgumentType.getString(context, "type");
                                    return withPlayer(context.getSource(),
                                            (player, engine) -> printStructureInfo(player, typeName));
                                })))
                .then(CommandManager.literal("kind")
                        .then(CommandManager.argument("kind", StringArgumentType.word())
                                .executes(context -> {
                                    String kindName = StringArgumentType.getString(context, "kind");
                                    return withPlayer(context.getSource(),
                                            (player, engine) -> setStructureKind(player, kindName));
                                })))
                .then(CommandManager.literal("name")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    return withPlayer(context.getSource(),
                                            (player, engine) -> setStructureName(player, name));
                                })))
                .then(CommandManager.literal("claim").executes(context ->
                        withPlayer(context.getSource(), SocietyCommands::claimStructure)))
                .then(CommandManager.literal("place")
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .executes(context -> placeStructure(context, ""))
                                .then(CommandManager.argument("nbt", StringArgumentType.greedyString())
                                        .executes(context -> placeStructure(context,
                                                StringArgumentType.getString(context, "nbt"))))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .executes(context -> {
                                    String id = StringArgumentType.getString(context, "id");
                                    return withPlayer(context.getSource(),
                                            (player, engine) -> removeStructure(player, engine, id));
                                })));
    }

    /** /society build place <type> - handy alias for the structure command. */
    private static LiteralArgumentBuilder<ServerCommandSource> buildBuildAliasSubcommand() {
        return CommandManager.literal("build")
                .then(CommandManager.literal("place")
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .executes(context -> placeStructure(context, ""))
                                .then(CommandManager.argument("nbt", StringArgumentType.greedyString())
                                        .executes(context -> placeStructure(context,
                                                StringArgumentType.getString(context, "nbt"))))));
    }

    private static void printStructureList(ServerPlayerEntity player, SocietyEngine engine) {
        send(player, "--- Premade structures (NBT catalogue) ---", Formatting.GOLD);
        send(player, "Place one with: /society structure place <type>", Formatting.DARK_GRAY);
        StringBuilder line = new StringBuilder();
        for (StructureType type : StructureType.values()) {
            line.setLength(0);
            line.append(type.display()).append(" [").append(type.category()).append(']');
            List<String> candidates =
                    io.github.minlol12.society.core.build.Blueprints.ctovCandidates(type,
                            io.github.minlol12.society.core.types.CultureOrigin.PLAINS);
            if (!candidates.isEmpty()) {
                line.append(" - ").append(candidates.get(0));
            }
            send(player, line.toString(), Formatting.WHITE);
        }
    }

    private static void printStructureInfo(ServerPlayerEntity player, String typeName) {
        StructureType type = StructureType.byName(typeName);
        if (type == null) {
            send(player, "No premade structure called '" + typeName + "'. See /society structure list.",
                    Formatting.RED);
            return;
        }
        send(player, "--- " + type.display() + " ---", Formatting.GOLD);
        send(player, "Category: " + type.category().name().toLowerCase()
                + " - footprint " + type.footprint() + "x" + type.footprint()
                + ", beds " + type.beds(), Formatting.WHITE);
        send(player, "Cost: " + Math.round(type.cost(io.github.minlol12.society.core.types.Good.WOOD))
                + " wood, " + Math.round(type.cost(io.github.minlol12.society.core.types.Good.STONE))
                + " stone, " + Math.round(type.cost(io.github.minlol12.society.core.types.Good.IRON))
                + " iron.", Formatting.WHITE);
        List<String> candidates = io.github.minlol12.society.core.build.Blueprints.ctovCandidates(
                type, io.github.minlol12.society.core.types.CultureOrigin.PLAINS);
        send(player, "NBT templates (" + candidates.size() + "):", Formatting.GRAY);
        for (int i = 0; i < Math.min(4, candidates.size()); i++) {
            send(player, "  ctov:" + candidates.get(i), Formatting.DARK_GRAY);
        }
        if (candidates.size() > 4) {
            send(player, "  ... and " + (candidates.size() - 4) + " more.", Formatting.DARK_GRAY);
        }
    }

    private static void printMyStructures(ServerPlayerEntity player, SocietyEngine engine) {
        List<PlayerStructure> mine = engine.playerStructuresByOwner(
                player.getGameProfile().getName());
        if (mine.isEmpty()) {
            send(player, "You have claimed no structures yet. Craft the Setter Stick"
                    + " (stick + name tag) or use /society structure place <type>.",
                    Formatting.GRAY);
            return;
        }
        send(player, "--- Your structures ---", Formatting.GOLD);
        for (PlayerStructure p : mine) {
            String where = p.settlementId().isEmpty() ? "wilderness"
                    : engine.settlements().get(p.settlementId()) == null ? "wilderness"
                    : engine.settlements().get(p.settlementId()).name();
            send(player, "- " + p.label() + " [" + p.kind().display() + "] at ("
                    + p.centerX() + ", " + p.centerZ() + ") near " + where
                    + " - id " + p.id(), Formatting.WHITE);
        }
    }

    private static void setStructureKind(ServerPlayerEntity player, String kindName) {
        if (!isKnownKind(kindName)) {
            send(player, "Unknown kind '" + kindName + "'. Try: government, housing, food,"
                    + " industry, trade, knowledge, defence, custom.", Formatting.RED);
            return;
        }
        PlayerStructureKind kind = PlayerStructureKind.byName(kindName);
        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(SocietyItems.SETTER_STICK)) {
            send(player, "Hold the Setter Stick to set a kind.", Formatting.RED);
            return;
        }
        NbtCompound nbt = stack.getOrCreateNbt();
        SetterStickItem.setKind(nbt, kind);
        stack.setCustomName(SetterStickItem.stickName(kind, SetterStickItem.currentLabel(nbt)));
        send(player, "This structure will be claimed as: " + kind.display() + ".",
                Formatting.LIGHT_PURPLE);
    }

    private static void setStructureName(ServerPlayerEntity player, String name) {
        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(SocietyItems.SETTER_STICK)) {
            send(player, "Hold the Setter Stick to name a structure.", Formatting.RED);
            return;
        }
        NbtCompound nbt = stack.getOrCreateNbt();
        SetterStickItem.setLabel(nbt, name);
        stack.setCustomName(SetterStickItem.stickName(SetterStickItem.currentKind(nbt), name));
        send(player, "This structure will be labelled '" + name + "'.",
                Formatting.LIGHT_PURPLE);
    }

    private static void claimStructure(ServerPlayerEntity player, SocietyEngine engine) {
        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(SocietyItems.SETTER_STICK)) {
            send(player, "Hold the Setter Stick to claim a structure: right-click two opposite"
                    + " corners of your build first.", Formatting.RED);
            return;
        }
        NbtCompound nbt = stack.getOrCreateNbt();
        int[] box = SetterStickItem.boxOf(nbt);
        if (box == null) {
            send(player, "No box marked yet. Right-click two opposite corners of your build.",
                    Formatting.RED);
            return;
        }
        int sizeX = box[3] - box[0] + 1;
        int sizeY = box[4] - box[1] + 1;
        int sizeZ = box[5] - box[2] + 1;
        if (sizeX > 128 || sizeY > 128 || sizeZ > 128) {
            send(player, "That box is too large (max 128 blocks each way).",
                    Formatting.RED);
            return;
        }
        PlayerStructureKind kind = SetterStickItem.currentKind(nbt);
        String label = SetterStickItem.currentLabel(nbt);
        if (label.isEmpty()) {
            label = kind.display();
        }

        PlayerStructure structure = new PlayerStructure(UUID.randomUUID().toString());
        structure.setOwnerName(player.getGameProfile().getName());
        structure.setLabel(label);
        structure.setKind(kind);
        structure.setBox(box[0], box[1], box[2], box[3], box[4], box[5]);
        structure.setCreatedDay(engine.day());
        Settlement near = engine.findSettlementNear(structure.centerX(), structure.centerZ(), 256);
        if (near != null) {
            structure.setSettlementId(near.id());
        }
        engine.addPlayerStructure(structure);
        SocietyManager manager = SocietyManager.get();
        if (manager != null) {
            PlayerStructurePlacer.markClaim(manager.overworld(), structure);
        }
        SetterStickItem.clear(nbt);
        stack.removeCustomName();
        send(player, "Claimed '" + label + "' as a " + kind.display().toLowerCase() + " ("
                + sizeX + "x" + sizeY + "x" + sizeZ + ") at ("
                + structure.centerX() + ", " + structure.centerZ() + ")."
                + (kind.isGovernment() ? " It is now a government building - sovereigns can"
                        + " be crowned here." : ""), Formatting.GOLD);
    }

    private static int placeStructure(
            com.mojang.brigadier.context.CommandContext<ServerCommandSource> context,
            String nbtPath) {
        String typeName = StringArgumentType.getString(context, "type");
        return withPlayer(context.getSource(), (player, engine) -> {
            StructureType type = StructureType.byName(typeName);
            if (type == null) {
                send(player, "No premade structure called '" + typeName + "'."
                        + " See /society structure list.", Formatting.RED);
                return;
            }
            SocietyManager manager = SocietyManager.get();
            if (manager == null) {
                send(player, "The society ledger is still waking up.", Formatting.RED);
                return;
            }
            ServerWorld world = manager.overworld();
            int x = (int) Math.floor(player.getX());
            int z = (int) Math.floor(player.getZ());
            CultureSamplerImpl sampler = new CultureSamplerImpl(world);
            boolean placed;
            if (!nbtPath.isEmpty()) {
                placed = PlayerStructurePlacer.stampNbt(world, nbtPath, x, z, 0);
            } else {
                placed = PlayerStructurePlacer.stampPremade(world, type,
                        sampler.sample(x, z), x, z, 0);
            }
            if (!placed) {
                send(player, "Could not place " + type.display() + " - is the NBT catalogue"
                        + " installed? (CTOV is required for premade structures.)",
                        Formatting.RED);
                return;
            }

            PlayerStructure structure = new PlayerStructure(UUID.randomUUID().toString());
            structure.setOwnerName(player.getGameProfile().getName());
            structure.setLabel(type.display());
            structure.setKind(kindForType(type));
            structure.setStructureType(type);
            structure.setBox(x - 1, player.getBlockY() - 1, z - 1, x + 1,
                    player.getBlockY() + 1, z + 1);
            structure.setCreatedDay(engine.day());
            Settlement near = engine.findSettlementNear(x, z, 256);
            if (near != null) {
                structure.setSettlementId(near.id());
                engine.attachPlayerBuildingToSettlement(near, type, x, player.getBlockY(), z);
            }
            engine.addPlayerStructure(structure);
            send(player, "Placed a " + type.display().toLowerCase()
                    + (near == null ? " in the wilds." : " at " + near.name() + " - the"
                            + " settlement counts it among its buildings."),
                    Formatting.GOLD);
        });
    }

    private static boolean isKnownKind(String name) {
        if (name == null) return false;
        for (PlayerStructureKind k : PlayerStructureKind.values()) {
            if (k.name().equalsIgnoreCase(name) || k.display().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static PlayerStructureKind kindForType(StructureType type) {
        switch (type.category()) {
            case HOUSING: return PlayerStructureKind.HOUSING;
            case FOOD: return PlayerStructureKind.FOOD;
            case INDUSTRY: return PlayerStructureKind.INDUSTRY;
            case TRADE: return PlayerStructureKind.TRADE;
            case KNOWLEDGE: return PlayerStructureKind.KNOWLEDGE;
            case DEFENCE: return PlayerStructureKind.DEFENCE;
            case CIVIC:
            default:
                if (type == StructureType.TOWN_HALL || type == StructureType.MEETING_HALL) {
                    return PlayerStructureKind.GOVERNMENT;
                }
                return PlayerStructureKind.CUSTOM;
        }
    }

    private static void removeStructure(ServerPlayerEntity player, SocietyEngine engine, String id) {
        PlayerStructure structure = engine.findPlayerStructure(id);
        if (structure == null) {
            send(player, "No structure with id '" + id + "'. See /society structure mine.",
                    Formatting.RED);
            return;
        }
        boolean owner = structure.ownerName().equalsIgnoreCase(
                player.getGameProfile().getName());
        if (!owner && !player.hasPermissionLevel(2)) {
            send(player, "Only " + structure.ownerName() + " (or an operator) can remove"
                    + " that structure.", Formatting.RED);
            return;
        }
        engine.removePlayerStructure(id);
        send(player, "Removed '" + structure.label() + "'.", Formatting.GOLD);
    }

    // =====================================================================
    // Economy (the player-managed currency)
    // =====================================================================

    private static LiteralArgumentBuilder<ServerCommandSource> buildEconomySubcommand() {
        return CommandManager.literal("economy")
                .executes(context -> withPlayer(context.getSource(),
                        SocietyCommands::printEconomy))
                .then(CommandManager.literal("print")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                .executes(context -> {
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    return withPlayer(context.getSource(),
                                            (player, engine) -> printMoney(player, engine, amount));
                                })))
                .then(CommandManager.literal("burn")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                .executes(context -> {
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    return withPlayer(context.getSource(),
                                            (player, engine) -> burnMoney(player, engine, amount));
                                })))
                .then(CommandManager.literal("name")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    return withPlayer(context.getSource(),
                                            (player, engine) -> renameCurrency(player, engine, name));
                                })))
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayerEntity target =
                                                    EntityArgumentType.getPlayer(context, "player");
                                            int amount =
                                                    IntegerArgumentType.getInteger(context, "amount");
                                            return withPlayer(context.getSource(),
                                                    (player, engine) -> giveMoney(player, engine,
                                                            target, amount));
                                        }))))
                .then(CommandManager.literal("balance").executes(context ->
                        withPlayer(context.getSource(), SocietyCommands::printBalance)))
                .then(CommandManager.literal("pay")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayerEntity target =
                                                    EntityArgumentType.getPlayer(context, "player");
                                            int amount =
                                                    IntegerArgumentType.getInteger(context, "amount");
                                            return withPlayer(context.getSource(),
                                                    (player, engine) -> payPlayer(player, engine,
                                                            target, amount));
                                        }))));
    }

    private static void printEconomy(ServerPlayerEntity player, SocietyEngine engine) {
        Currency currency = engine.currency();
        send(player, "--- The " + currency.name() + " of this world ---", Formatting.GOLD);
        send(player, "Supply: " + fmt(currency.supply()) + " notes (minted "
                + fmt(currency.mintedTotal()) + ", burned " + fmt(currency.burnedTotal()) + ")",
                Formatting.WHITE);
        send(player, "Backing: " + fmt(currency.computeBacking(engine))
                + " of real settlement wealth", Formatting.WHITE);
        send(player, "Value of one note: " + fmt(currency.valuePerUnit(engine))
                + " emerald-worth - the currency is " + currency.trendWord() + ".",
                Formatting.WHITE);
        double ratio = currency.supply() / Math.max(1.0, currency.backing() * 0.5);
        if (ratio >= 3.0) {
            send(player, "Too much money has been printed; the notes are nearly worthless.",
                    Formatting.RED);
        } else if (ratio <= 0.25) {
            send(player, "Notes are scarcer than the wealth behind them; each is worth"
                    + " a fortune.", Formatting.AQUA);
        } else {
            send(player, "The money supply is healthy.", Formatting.GREEN);
        }
        send(player, "Print with /society economy print <n>, burn with /society economy burn <n>,",
                Formatting.DARK_GRAY);
        send(player, "rename with /society economy name <name>.", Formatting.DARK_GRAY);
    }

    private static void printBalance(ServerPlayerEntity player, SocietyEngine engine) {
        PlayerData data = engine.playerDataFor(player.getUuidAsString(),
                player.getGameProfile().getName());
        double value = engine.currency().valuePerUnit(engine);
        send(player, "Your purse: " + fmt(data.currencyBalance()) + " "
                + engine.currency().name().toLowerCase() + " (worth "
                + fmt(data.currencyBalance() * value) + " emeralds).", Formatting.WHITE);
    }

    private static void printMoney(ServerPlayerEntity player, SocietyEngine engine, int amount) {
        Currency currency = engine.currency();
        double before = currency.valuePerUnit(engine);
        currency.print(amount);
        double after = currency.valuePerUnit(engine);
        double drop = (before - after) / Math.max(0.0001, before) * 100.0;
        send(player, "Printed " + amount + " new " + currency.name().toLowerCase() + "."
                + " The value of a note fell by " + fmt(drop) + "% - print too much and the"
                + " money becomes worthless.", Formatting.GOLD);
        engine.markDirty();
    }

    private static void burnMoney(ServerPlayerEntity player, SocietyEngine engine, int amount) {
        Currency currency = engine.currency();
        double before = currency.valuePerUnit(engine);
        double burned = currency.burn(amount);
        double after = currency.valuePerUnit(engine);
        double rise = (after - before) / Math.max(0.0001, before) * 100.0;
        send(player, "Burned " + fmt(burned) + " " + currency.name().toLowerCase()
                + " from circulation. The value of a note rose by " + fmt(rise)
                + "% - print too little and each note becomes a treasure.",
                Formatting.GOLD);
        engine.markDirty();
    }

    private static void renameCurrency(ServerPlayerEntity player, SocietyEngine engine, String name) {
        engine.currency().setName(name.trim());
        send(player, "The currency of this world is now called '" + engine.currency().name() + "'.",
                Formatting.GOLD);
        engine.markDirty();
    }

    private static void giveMoney(ServerPlayerEntity player, SocietyEngine engine,
                                  ServerPlayerEntity target, int amount) {
        Currency currency = engine.currency();
        double before = currency.valuePerUnit(engine);
        currency.print(amount); // minted on the spot: this inflates the world
        PlayerData data = engine.playerDataFor(target.getUuidAsString(),
                target.getGameProfile().getName());
        data.addCurrency(amount);
        double after = currency.valuePerUnit(engine);
        double drop = (before - after) / Math.max(0.0001, before) * 100.0;
        send(player, "Printed " + amount + " " + currency.name().toLowerCase() + " and gave them"
                + " to " + target.getGameProfile().getName() + ". The note's value fell by "
                + fmt(drop) + "%.", Formatting.GOLD);
        target.sendMessage(Text.literal("[Society] " + player.getGameProfile().getName()
                + " printed " + amount + " " + currency.name().toLowerCase()
                + " and gave them to you.").formatted(Formatting.GOLD), false);
        engine.markDirty();
    }

    private static void payPlayer(ServerPlayerEntity player, SocietyEngine engine,
                                  ServerPlayerEntity target, int amount) {
        PlayerData sender = engine.playerDataFor(player.getUuidAsString(),
                player.getGameProfile().getName());
        if (sender.currencyBalance() < amount) {
            send(player, "You only hold " + fmt(sender.currencyBalance()) + " "
                    + engine.currency().name().toLowerCase() + ".",
                    Formatting.RED);
            return;
        }
        PlayerData receiver = engine.playerDataFor(target.getUuidAsString(),
                target.getGameProfile().getName());
        sender.addCurrency(-amount);
        receiver.addCurrency(amount);
        send(player, "Paid " + amount + " " + engine.currency().name().toLowerCase() + " to "
                + target.getGameProfile().getName() + ".", Formatting.GOLD);
        target.sendMessage(Text.literal("[Society] " + player.getGameProfile().getName()
                + " paid you " + amount + " " + engine.currency().name().toLowerCase() + ".")
                .formatted(Formatting.GOLD), false);
        engine.markDirty();
    }

    // =====================================================================
    // Roles (worker, blacksmith, farmer ... king, queen)
    // =====================================================================

    private static LiteralArgumentBuilder<ServerCommandSource> buildRoleSubcommand() {
        return CommandManager.literal("role")
                .executes(context -> withPlayer(context.getSource(), SocietyCommands::printMyRole))
                .then(CommandManager.literal("list").executes(context ->
                        withPlayer(context.getSource(), SocietyCommands::printRoleList)))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("role", StringArgumentType.word())
                                .executes(context -> {
                                    String roleName = StringArgumentType.getString(context, "role");
                                    return withPlayer(context.getSource(),
                                            (player, engine) -> setRole(player, engine, roleName));
                                })))
                .then(CommandManager.literal("home")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    return withPlayer(context.getSource(),
                                            (player, engine) -> setRoleHome(player, engine, name));
                                })))
                .then(CommandManager.literal("crown")
                        .executes(context -> withPlayer(context.getSource(),
                                (player, engine) -> crownPlayer(player, engine, PlayerRole.KING)))
                        .then(CommandManager.literal("queen")
                                .executes(context -> withPlayer(context.getSource(),
                                        (player, engine) -> crownPlayer(player, engine,
                                                PlayerRole.QUEEN)))))
                .then(CommandManager.literal("abdicate").executes(context ->
                        withPlayer(context.getSource(), SocietyCommands::abdicatePlayer)));
    }

    private static void printMyRole(ServerPlayerEntity player, SocietyEngine engine) {
        PlayerData data = engine.playerDataFor(player.getUuidAsString(),
                player.getGameProfile().getName());
        Settlement home = data.homeSettlementId().isEmpty() ? null
                : engine.settlements().get(data.homeSettlementId());
        send(player, "--- Your role ---", Formatting.GOLD);
        send(player, "Role: " + data.role().display() + " - " + data.role().description(),
                Formatting.WHITE);
        send(player, home == null ? "Home: none yet - /society role home <settlement>"
                : "Home: " + home.name(), Formatting.WHITE);
        Settlement ruled = engine.settlementRuledBy(player.getUuidAsString());
        if (ruled != null) {
            send(player, "You sit on the throne of " + ruled.name() + ".",
                    Formatting.RED);
        }
        send(player, "Take a role with /society role set <role>; see /society role list.",
                Formatting.DARK_GRAY);
    }

    private static void printRoleList(ServerPlayerEntity player, SocietyEngine engine) {
        send(player, "--- Roles you can play ---", Formatting.GOLD);
        for (PlayerRole role : PlayerRole.values()) {
            if (role == PlayerRole.NONE || role.isSovereign()) {
                continue;
            }
            send(player, role.display() + " - " + role.description(), Formatting.WHITE);
        }
        send(player, "King / Queen - sovereigns are crowned at a government building:",
                Formatting.DARK_GRAY);
        send(player, "stand at one and run /society role crown (or crown queen).",
                Formatting.DARK_GRAY);
    }

    private static void setRole(ServerPlayerEntity player, SocietyEngine engine, String roleName) {
        PlayerRole role = PlayerRole.byName(roleName);
        if (role == PlayerRole.NONE) {
            send(player, "Unknown role '" + roleName + "'. See /society role list.",
                    Formatting.RED);
            return;
        }
        if (role.isSovereign()) {
            send(player, "The crown is not something you simply put on. Stand at a government"
                    + " building and run /society role crown.", Formatting.RED);
            return;
        }
        PlayerData data = engine.playerDataFor(player.getUuidAsString(),
                player.getGameProfile().getName());
        data.setRole(role, engine.day());
        engine.markDirty();
        send(player, "You are now a " + role.display().toLowerCase() + " of this world. Your craft"
                + " will strengthen your home settlement.", Formatting.GOLD);
    }

    private static void setRoleHome(ServerPlayerEntity player, SocietyEngine engine, String name) {
        Settlement settlement = engine.findSettlementByName(name);
        if (settlement == null) {
            send(player, "No settlement answers to '" + name + "'.", Formatting.RED);
            return;
        }
        PlayerData data = engine.playerDataFor(player.getUuidAsString(),
                player.getGameProfile().getName());
        data.setHomeSettlementId(settlement.id());
        engine.markDirty();
        send(player, "You now belong to " + settlement.name() + " - your craft will help it"
                + " while you play that role.", Formatting.GOLD);
    }

    private static void crownPlayer(ServerPlayerEntity player, SocietyEngine engine,
                                    PlayerRole role) {
        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());

        Settlement target = null;
        PlayerStructure government = engine.findGovernmentStructureNear(x, z, 32);
        if (government != null) {
            if (!government.settlementId().isEmpty()) {
                target = engine.settlements().get(government.settlementId());
            }
            if (target == null) {
                target = engine.findSettlementNear(government.centerX(), government.centerZ(), 256);
            }
        } else {
            Settlement near = engine.findSettlementNear(x, z, 96);
            if (near != null && hasGovernmentHall(engine, near)) {
                target = near;
            }
        }
        if (target == null) {
            send(player, "No government building stands near you. Claim one with the Setter"
                    + " Stick (kind: government) or build a Town Hall, then stand at it.",
                    Formatting.RED);
            return;
        }
        PlayerData data = engine.playerDataFor(player.getUuidAsString(),
                player.getGameProfile().getName());
        if (PlayerRoleSystem.crown(engine, target.id(), data.uuid(), role, engine.day())) {
            send(player, "You are crowned " + role.display() + " of " + target.name() + "!",
                    Formatting.RED);
        } else {
            String ruler = engine.rulerPlayers().get(target.id());
            PlayerData current = ruler == null ? null : engine.playerData().get(ruler);
            send(player, "The throne of " + target.name() + " is already taken"
                    + (current == null ? "." : " by " + current.playerName() + "."),
                    Formatting.YELLOW);
        }
    }

    private static boolean hasGovernmentHall(SocietyEngine engine, Settlement s) {
        for (io.github.minlol12.society.core.data.Building b : s.buildings()) {
            if (!b.isComplete() || b.isRuined()) continue;
            if (b.type() == StructureType.TOWN_HALL || b.type() == StructureType.MEETING_HALL) {
                return true;
            }
        }
        return false;
    }

    private static void abdicatePlayer(ServerPlayerEntity player, SocietyEngine engine) {
        PlayerData data = engine.playerDataFor(player.getUuidAsString(),
                player.getGameProfile().getName());
        for (String settlementId : new java.util.ArrayList<String>(engine.rulerPlayers().keySet())) {
            if (engine.rulerPlayers().get(settlementId).equals(data.uuid())) {
                if (PlayerRoleSystem.abdicate(engine, settlementId, data.uuid(), engine.day())) {
                    send(player, "You have abdicated the throne.", Formatting.GOLD);
                    return;
                }
            }
        }
        send(player, "You hold no crown to set down.", Formatting.YELLOW);
    }

    // =====================================================================
    // Plumbing
    // =====================================================================

    private static LiteralArgumentBuilder<ServerCommandSource> buildSettlementSubcommand() {
        RequiredArgumentBuilder<ServerCommandSource, String> nameArgument =
                CommandManager.argument("name", StringArgumentType.word())
                        .executes(context -> printSettlement(context, "info"));
        for (final String section : SECTIONS) {
            nameArgument.then(CommandManager.literal(section)
                    .executes(context -> printSettlement(context, section)));
        }
        return CommandManager.literal("settlement").then(nameArgument);
    }

    private static int printSettlement(
            com.mojang.brigadier.context.CommandContext<ServerCommandSource> context,
            String section) {
        String name = StringArgumentType.getString(context, "name");
        return withPlayer(context.getSource(), (player, engine) -> {
            Settlement settlement = engine.findSettlementByName(name);
            if (settlement == null) {
                context.getSource().sendError(Text.literal(
                        "No settlement answers to '" + name + "'."));
                return;
            }
            SocietyText.printSettlementSection(player, engine, settlement, section);
        });
    }

    /** Teleports the caller to a settlement's most recently finished building. */
    private static int visitSettlement(
            com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        return withPlayer(context.getSource(), (player, engine) -> {
            Settlement settlement = engine.findSettlementByName(name);
            if (settlement == null) {
                context.getSource().sendError(Text.literal(
                        "No settlement answers to '" + name + "'."));
                return;
            }
            SocietyManager manager = SocietyManager.get();
            if (manager == null) {
                context.getSource().sendError(Text.literal(
                        "The society ledger is still waking up."));
                return;
            }
            manager.teleportToConstruction(player, settlement);
        });
    }

    // =====================================================================
    // Plumbing
    // =====================================================================

    private interface EngineAction {
        void run(ServerPlayerEntity player, SocietyEngine engine);
    }

    /** Runs an action only when the ledger is awake and a player is asking. */
    private static int withPlayer(ServerCommandSource source, EngineAction action) {
        SocietyManager manager = SocietyManager.get();
        if (manager == null) {
            source.sendError(Text.literal("The society ledger is still waking up."));
            return 0;
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only a player can read the chronicle."));
            return 0;
        }
        action.run(player, manager.engine());
        return 1;
    }

    private static void send(ServerPlayerEntity player, String text, Formatting formatting) {
        player.sendMessage(Text.literal(text).formatted(formatting), false);
    }

    private static String fmt(double value) {
        if (value >= 1000.0) {
            return String.format(java.util.Locale.ROOT, "%.0f", value);
        }
        if (value >= 10.0) {
            return String.format(java.util.Locale.ROOT, "%.1f", value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
