package io.github.minlol12.society;

import io.github.minlol12.society.command.SocietyCommands;
import io.github.minlol12.society.config.SocietyConfig;
import io.github.minlol12.society.item.SocietyItems;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Society - living settlements, families, economies and histories for
 * Minecraft villagers. Server-side simulation; clients can join a Society
 * server with a completely vanilla client.
 */
public final class SocietyMod implements ModInitializer {

    public static final String MOD_ID = "society";
    public static final String MOD_NAME = "Society";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static SocietyConfig config;

    @Override
    public void onInitialize() {
        config = SocietyConfig.loadOrCreate();
        SocietyItems.register();
        SocietyCommands.register();
        SocietyManager.register();
        LOGGER.info("[Society] Villagers are waking up: personalities, families, "
                + "markets, politics and history enabled.");
    }

    public static SocietyConfig config() {
        return config;
    }
}
