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
 * <p>Click the ground (or just the air) near one village to mark it as the
 * first combatant; click near a second, different village to decree open
 * war between the two. Clicking air where the marked village itself stands
 * clears the mark; sneak-clicking in the air reads it back. The selection
 * is generous on purpose - it is judged from where the herald stands and
 * reaches out a long way - and every step tells you plainly which
 * settlement was chosen, so a decree never silently fails.</p>
 */
public final class WarBatonItem extends Item {

    /** How close a player must stand to a settlement centre to select it. */
    private static final double SELECT_RANGE = 384.0;
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
        ServerPlayerEntity player = playerOf(context.getPlayer());
        if (player == null) {
            return ActionResult.PASS;
        }
        SocietyManager manager = SocietyManager.get();
        if (manager == null) {
            return ActionResult.PASS;
        }
        BlockPos pos = context.getBlockPos();
        Settlement clicked = manager.engine().findSettlementNear(
                (int) Math.floor(player.getX()), (int) Math.floor(player.getZ()), SELECT_RANGE);
        if (clicked == null) {
            clicked = manager.engine().findSettlementNear(pos.getX(), pos.getZ(), SELECT_RANGE);
        }
        handleSelection(player, context.getStack(), clicked);
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        ServerPlayerEntity player = playerOf(user);
        if (player == null) {
            return TypedActionResult.success(stack);
        }
        NbtCompound nbt = stack.getOrCreateNbt();

        if (user.isSneaking()) {
            // Reading the herald's orders back. To clear the mark, click air
            // in the wilderness (no settlement near) or at the marked village.
            String first = nbt.getString(NBT_FIRST);
            if (first.isEmpty()) {
                player.sendMessage(Text.literal(
                        "[Society] The War Baton carries no mark yet. Click near a village"
                                + " (air is fine) to mark it.")
                        .formatted(Formatting.GRAY), false);
            } else {
                player.sendMessage(Text.literal("[Society] " + first + " is marked."
                        + " Click near a second, different village to declare war.")
                        .formatted(Formatting.GOLD), false);
            }
            return TypedActionResult.success(stack);
        }

        SocietyManager manager = SocietyManager.get();
        if (manager == null) {
            return TypedActionResult.success(stack);
        }
        // Air clicks work exactly like ground clicks, so a chest or a door
        // never swallows the herald's authority: judge from where we stand.
        Settlement clicked = manager.engine().findSettlementNear(
                (int) Math.floor(player.getX()), (int) Math.floor(player.getZ()), SELECT_RANGE);
        handleSelection(player, stack, clicked);
        return TypedActionResult.success(stack);
    }

    // =====================================================================
    // Marking / declaring / clearing
    // =====================================================================

    /**
     * The one decision the baton ever makes:
     * <ul>
     *   <li>no mark yet and a settlement nearby - mark it;</li>
     *   <li>a mark and a <em>different</em> settlement nearby - declare war;</li>
     *   <li>a mark and only the marked settlement (or nothing) nearby - clear.</li>
     * </ul>
     */
    private void handleSelection(ServerPlayerEntity player, ItemStack stack, Settlement clicked) {
        SocietyManager manager = SocietyManager.get();
        if (manager == null) {
            return;
        }
        NbtCompound nbt = stack.getOrCreateNbt();
        String firstName = nbt.getString(NBT_FIRST);

        if (firstName.isEmpty()) {
            if (clicked == null || clicked.isDestroyed()) {
                player.sendMessage(Text.literal("[Society] No settlement stands within "
                        + (int) SELECT_RANGE + " blocks to mark. You are at ("
                        + (int) Math.floor(player.getX()) + ", "
                        + (int) Math.floor(player.getZ()) + ").").formatted(Formatting.RED), false);
                return;
            }
            nbt.putString(NBT_FIRST, clicked.name());
            stack.setCustomName(Text.literal("War Baton - marked: " + clicked.name())
                    .formatted(Formatting.RED));
            player.sendMessage(Text.literal("[Society] " + clicked.name()
                    + " (at " + clicked.centerX() + ", " + clicked.centerZ() + ") is marked as the"
                    + " first civilization. Click near a second village to set the two at war.")
                    .formatted(Formatting.GOLD), false);
            return;
        }

        // A mark exists. Same village (or no village at hand): clear the mark.
        if (clicked == null || clicked.isDestroyed()
                || firstName.equalsIgnoreCase(clicked.name())) {
            nbt.remove(NBT_FIRST);
            stack.removeCustomName();
            player.sendMessage(Text.literal("[Society] War Baton selection cleared.")
                    .formatted(Formatting.GRAY), false);
            return;
        }

        // A second, different village: declare war between the two marks.
        Settlement first = manager.engine().findSettlementByName(firstName);
        nbt.remove(NBT_FIRST);
        stack.removeCustomName();
        if (first == null || first.isDestroyed()) {
            player.sendMessage(Text.literal("[Society] The first mark has faded; "
                    + clicked.name() + " is now marked instead.").formatted(Formatting.GOLD), false);
            nbt.putString(NBT_FIRST, clicked.name());
            stack.setCustomName(Text.literal("War Baton - marked: " + clicked.name())
                    .formatted(Formatting.RED));
            return;
        }
        boolean declared = manager.engine().declareWar(first, clicked);
        if (declared) {
            player.sendMessage(Text.literal("[Society] WAR! " + first.name()
                    + " and " + clicked.name() + " are now at war - the decree is written"
                    + " into both chronicles, and their warriors will meet on the field.")
                    .formatted(Formatting.RED), false);
        } else {
            player.sendMessage(Text.literal("[Society] " + first.name() + " and "
                    + clicked.name() + " are already at war.").formatted(Formatting.YELLOW), false);
        }
    }

    private static ServerPlayerEntity playerOf(PlayerEntity player) {
        return player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null;
    }
}
