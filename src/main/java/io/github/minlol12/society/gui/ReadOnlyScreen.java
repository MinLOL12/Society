package io.github.minlol12.society.gui;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * A chest-shaped information screen that cannot be touched: clicks are
 * swallowed, nothing can be taken out and nothing can be put in. This is
 * what lets Society show rich, clickable pages to a completely vanilla
 * client without handing the player a pile of free items.
 */
public final class ReadOnlyScreen {

    private ReadOnlyScreen() { }

    /** Opens a 9x6 page of display stacks for a player. */
    public static void open(ServerPlayerEntity player, Text title, List<ItemStack> contents) {
        SimpleInventory inventory = new SimpleInventory(54);
        for (int i = 0; i < Math.min(54, contents.size()); i++) {
            inventory.setStack(i, contents.get(i));
        }
        player.openHandledScreen(new SimpleNamedFactory(title, inventory));
    }

    /** A factory that hands out locked handlers over a fixed inventory. */
    private static final class SimpleNamedFactory implements NamedScreenHandlerFactory {

        private final Text title;
        private final Inventory inventory;

        private SimpleNamedFactory(Text title, Inventory inventory) {
            this.title = title;
            this.inventory = inventory;
        }

        @Override
        public Text getDisplayName() {
            return title;
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory,
                                        PlayerEntity player) {
            return new LockedHandler(syncId, playerInventory, inventory);
        }
    }

    /** A generic 9x6 container with every mutation refused. */
    private static final class LockedHandler extends GenericContainerScreenHandler {

        private LockedHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, 6);
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType,
                                PlayerEntity player) {
            // Every click is a no-op: the page is a display, not a chest.
            // Re-sync so a laggy client never renders a ghost item.
            if (player instanceof ServerPlayerEntity) {
                sendContentUpdates();
                ((ServerPlayerEntity) player).currentScreenHandler.syncState();
            }
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }

        @Override
        public boolean canInsertIntoSlot(ItemStack stack, net.minecraft.screen.slot.Slot slot) {
            return false;
        }

        @Override
        public void onClosed(PlayerEntity player) {
            // Deliberately does not call super: the display inventory is
            // ours and must never be dropped on the floor.
            setCursorStack(ItemStack.EMPTY);
        }
    }
}
