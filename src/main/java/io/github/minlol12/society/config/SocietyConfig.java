package io.github.minlol12.society.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.minlol12.society.SocietyMod;
import io.github.minlol12.society.core.EngineConfig;

import net.fabricmc.loader.api.FabricLoader;

/** Player-facing JSON config; feeds {@link EngineConfig} at server start. */
public final class SocietyConfig {

    public int maxSettlements = 24;
    public int maxCitizensPerSettlement = 160;
    public int seasonLengthDays = 30;

    public boolean enableMigration = true;
    public boolean enableManifestSpawns = true;
    public boolean warCasualties = false;
    public boolean plagueCasualties = true;
    public boolean famineCasualties = true;
    public boolean announcements = true;
    public int announcementRadius = 160;
    public int dailyAnnouncementBudget = 4;

    /** Settlements raise real buildings in the world as they grow. */
    public boolean buildStructures = true;
    /** Clicking a villager opens the stat screen instead of chat text. */
    public boolean villagerScreen = true;
    /** Sneak-click a villager with an empty hand to open their page. */
    public boolean sneakToInspect = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static SocietyConfig loadOrCreate() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("society.json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                SocietyConfig config = GSON.fromJson(reader, SocietyConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException | RuntimeException e) {
                SocietyMod.LOGGER.warn("[Society] Could not read config, writing a fresh one: {}",
                        e.toString());
            }
        }
        SocietyConfig config = new SocietyConfig();
        config.save(path);
        return config;
    }

    public void save() {
        save(FabricLoader.getInstance().getConfigDir().resolve("society.json"));
    }

    private void save(Path path) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            SocietyMod.LOGGER.warn("[Society] Could not save config: {}", e.toString());
        }
    }

    public EngineConfig toEngineConfig() {
        EngineConfig engine = new EngineConfig();
        engine.maxSettlements = Math.max(1, maxSettlements);
        engine.maxCitizensPerSettlement = Math.max(1, maxCitizensPerSettlement);
        engine.seasonLengthDays = Math.max(4, seasonLengthDays);
        engine.enableMigration = enableMigration;
        engine.enableManifestSpawns = enableManifestSpawns;
        engine.warCasualties = warCasualties;
        engine.plagueCasualties = plagueCasualties;
        engine.famineCasualties = famineCasualties;
        engine.announcements = announcements;
        engine.announcementRadius = Math.max(16, announcementRadius);
        engine.dailyAnnouncementBudget = Math.max(1, dailyAnnouncementBudget);
        engine.buildStructures = buildStructures;
        return engine;
    }
}
