package io.github.minlol12.society.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import io.github.minlol12.society.SocietyManager;
import io.github.minlol12.society.SocietyText;
import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Settlement;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The {@code /society} command tree - read-only windows into the ledger:
 *
 * <pre>
 * /society                                  a short help
 * /society day                              where the ledger's calendar stands
 * /society settlements                      every living settlement, briefly
 * /society settlement &lt;name&gt; [section]     one settlement's page (info|economy|
 *                                          tech|culture|diplomacy|government)
 * /society villager &lt;entity&gt;               a villager's personal page
 * /society history [count]                  the world chronicle's last lines
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
                send(player, "/society history [count] - the world chronicle", Formatting.GRAY);
                send(player, "Click a villager (sneak, or hold the Chronicle) to open their page.",
                        Formatting.DARK_GRAY);
                send(player, "Craft the Society Chronicle (book + emerald) to browse it in-world.",
                        Formatting.DARK_GRAY);
                send(player, "Craft the War Baton (stick + iron + redstone) and click two villages"
                        + " to set them at war.", Formatting.DARK_GRAY);
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

            dispatcher.register(root);
        });
    }

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
}
