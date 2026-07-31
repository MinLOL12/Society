package io.github.minlol12.society.item;

import io.github.minlol12.society.SocietyManager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * The Society Chronicle. Right-click to read of the nearest settlement - or
 * of the whole world when none is near. Right-click a villager to read
 * their personal page instead (handled by the entity interact hook).
 */
public final class ChronicleItem extends Item {

    public ChronicleItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        SocietyManager manager = SocietyManager.get();
        if (manager == null) {
            return TypedActionResult.pass(stack);
        }
        if (player instanceof ServerPlayerEntity) {
            manager.printLocalChronicle((ServerPlayerEntity) player);
            if (world instanceof ServerWorld) {
                ((ServerWorld) world).playSound(null, player.getBlockPos(),
                        SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.8f, 1.1f);
            }
        }
        return TypedActionResult.success(stack);
    }
}
