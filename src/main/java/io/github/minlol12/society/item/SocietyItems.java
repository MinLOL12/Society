package io.github.minlol12.society.item;

import io.github.minlol12.society.SocietyMod;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Registration point for the small number of Society items. */
public final class SocietyItems {

    public static final Item SOCIETY_CHRONICLE = new ChronicleItem(new Item.Settings().maxCount(1));

    /** A herald's stick for setting two civilizations at war. */
    public static final Item WAR_BATON = new WarBatonItem(new Item.Settings().maxCount(1));

    private SocietyItems() { }

    public static void register() {
        Registry.register(Registries.ITEM,
                new Identifier(SocietyMod.MOD_ID, "society_chronicle"), SOCIETY_CHRONICLE);
        Registry.register(Registries.ITEM,
                new Identifier(SocietyMod.MOD_ID, "war_baton"), WAR_BATON);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> {
                    entries.add(SOCIETY_CHRONICLE);
                    entries.add(WAR_BATON);
                });
    }
}
