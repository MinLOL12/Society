package io.github.minlol12.society.item;

import io.github.minlol12.society.SocietyManager;
import io.github.minlol12.society.core.data.Settlement;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The War Baton - a herald's stick for tipping two civilizations into war.
 *
 * <p>Right-click the ground near one village to mark it as the first
 * combatant (the choice rides on the stack). Right-click near a second,
 * different village to decree open war between the two. Right-click in
 * the air to clear the current mark.</p>
 */
public final class WarBatonItem extends Item {

    /** How close a click must land to a settlement centre to select it. */
    private static final double SELECT_RANGE = 128.0;
    /** NBT key remembering the first marked village. */
    private static final String NBT_FIRST = "society_war_first";

    public WarBatonItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        SocietyManager manager = SocietyManager.get();
        if (manager == null) {
            return ActionResult.PASS;
        }
        PlayerEntity user = context.getPlayer();
        if (!(user instanceof ServerPlayerEntity)) {
            return ActionResult.PASS;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) user;

        BlockPos pos = context.getBlockPos();
        Settlement clicked = manager.engine().findSettlementNear(
                pos.getX(), pos.getZ(), (int) SELECT_RANGE);
        if (clicked == null || clicked.isDestroyed()) {
            player.sendMessage(Text.literal("[Society] No settlement stands within "
                    + (int) SELECT_RANGE + " blocks to mark.").formatted(Formatting.RED), false);
            return ActionResult.SUCCESS;
        }

        ItemStack stack = context.getStack();
        NbtCompound nbt = stack.getOrCreateNbt();
        String firstName = nbt.getString(NBT_FIRST);

        if (firstName.isEmpty() || firstName.equalsIgnoreCase(clicked.name())) {
            nbt.putString(NBT_FIRST, clicked.name());
            player.sendMessage(Text.literal("[Society] " + clicked.name()
                    + " is marked as the first civilization. Click near a second village"
                    + " to set the two at war.").formatted(Formatting.GOLD), false);
            return ActionResult.SUCCESS;
        }

        // A second, different village: declare war between the two marks.
        Settlement first = manager.engine().findSettlementByName(firstName);
        nbt.remove(NBT_FIRST);
        if (first == null || first.isDestroyed()) {
            player.sendMessage(Text.literal("[Society] The first mark has faded; "
                    + clicked.name() + " is now marked instead.").formatted(Formatting.GOLD), false);
            nbt.putString(NBT_FIRST, clicked.name());
            return ActionResult.SUCCESS;
        }
        boolean declared = manager.engine().declareWar(first, clicked);
        if (declared) {
            player.sendMessage(Text.literal("[Society] War declared between "
                    + first.name() + " and " + clicked.name() + "!").formatted(Formatting.RED), false);
        } else {
            player.sendMessage(Text.literal("[Society] " + first.name() + " and "
                    + clicked.name() + " are already at war.").formatted(Formatting.YELLOW), false);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        // Right-click in the air clears the current selection.
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.contains(NBT_FIRST)) {
            nbt.remove(NBT_FIRST);
            if (player instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity) player).sendMessage(Text.literal(
                        "[Society] War Baton selection cleared.").formatted(Formatting.GRAY), false);
            }
        }
        return TypedActionResult.success(stack);
    }
}
