package io.github.minlol12.society.item;

import io.github.minlol12.society.core.types.PlayerStructureKind;

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
 * The Setter Stick - a builder's tool for claiming structures the players
 * themselves raise, and for stamping the premade NBT structures from the
 * Society catalogue onto the land.
 *
 * <ul>
 *   <li>Right-click two opposite corners of your build to mark its box.</li>
 *   <li>Sneak-right-click a block to cycle what kind of structure it is
 *       (government building, housing, food, industry, trade, knowledge,
 *       defence or custom).</li>
 *   <li>Then run {@code /society structure claim} to claim it, or
 *       {@code /society structure name <label>} to give it a name first.</li>
 *   <li>Right-click air clears the box; sneak-right-click air reads it back.</li>
 * </ul>
 */
public final class SetterStickItem extends Item {

    private static final String NBT_C1X = "society_setter_c1x";
    private static final String NBT_C1Y = "society_setter_c1y";
    private static final String NBT_C1Z = "society_setter_c1z";
    private static final String NBT_C2X = "society_setter_c2x";
    private static final String NBT_C2Y = "society_setter_c2y";
    private static final String NBT_C2Z = "society_setter_c2z";
    private static final String NBT_KIND = "society_setter_kind";
    private static final String NBT_LABEL = "society_setter_label";

    public SetterStickItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        PlayerEntity user = context.getPlayer();
        if (!(user instanceof ServerPlayerEntity)) {
            return ActionResult.PASS;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ItemStack stack = context.getStack();
        NbtCompound nbt = stack.getOrCreateNbt();
        BlockPos pos = context.getBlockPos();

        if (player.isSneaking()) {
            // Cycle the structure kind.
            PlayerStructureKind next = cycleKind(currentKind(nbt));
            nbt.putString(NBT_KIND, next.name());
            stack.setCustomName(stickName(next, nbt.getString(NBT_LABEL)));
            player.sendMessage(Text.literal("[Society] This structure will be claimed as: "
                    + next.display() + ". (" + next.description() + ")")
                    .formatted(Formatting.LIGHT_PURPLE), false);
            return ActionResult.SUCCESS;
        }

        if (!nbt.contains(NBT_C1X)) {
            nbt.putInt(NBT_C1X, pos.getX());
            nbt.putInt(NBT_C1Y, pos.getY());
            nbt.putInt(NBT_C1Z, pos.getZ());
            stack.setCustomName(stickName(currentKind(nbt), nbt.getString(NBT_LABEL)));
            player.sendMessage(Text.literal("[Society] Corner 1 set at ("
                    + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "). "
                    + "Click the opposite corner of your structure.")
                    .formatted(Formatting.GOLD), false);
            return ActionResult.SUCCESS;
        }

        nbt.putInt(NBT_C2X, pos.getX());
        nbt.putInt(NBT_C2Y, pos.getY());
        nbt.putInt(NBT_C2Z, pos.getZ());
        int sizeX = Math.abs(pos.getX() - nbt.getInt(NBT_C1X)) + 1;
        int sizeY = Math.abs(pos.getY() - nbt.getInt(NBT_C1Y)) + 1;
        int sizeZ = Math.abs(pos.getZ() - nbt.getInt(NBT_C1Z)) + 1;
        PlayerStructureKind kind = currentKind(nbt);
        String label = nbt.getString(NBT_LABEL);
        stack.setCustomName(stickName(kind, label));
        player.sendMessage(Text.literal("[Society] Box set: " + sizeX + "x" + sizeY + "x" + sizeZ
                + " (" + kind.display() + (label.isEmpty() ? "" : ", '" + label + "'") + "). "
                + "Run /society structure claim to claim it.")
                .formatted(Formatting.GOLD), false);
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        NbtCompound nbt = stack.getOrCreateNbt();
        if (player.isSneaking()) {
            if (player instanceof ServerPlayerEntity) {
                PlayerStructureKind kind = currentKind(nbt);
                String label = nbt.getString(NBT_LABEL);
                if (!nbt.contains(NBT_C1X)) {
                    ((ServerPlayerEntity) player).sendMessage(Text.literal(
                            "[Society] The Setter Stick carries no box yet. Kind: "
                                    + kind.display() + (label.isEmpty() ? "" : ", label: " + label)
                                    + ". Right-click two opposite corners of your structure.")
                            .formatted(Formatting.GRAY), false);
                } else if (!nbt.contains(NBT_C2X)) {
                    ((ServerPlayerEntity) player).sendMessage(Text.literal(
                            "[Society] Corner 1 at (" + nbt.getInt(NBT_C1X) + ", "
                                    + nbt.getInt(NBT_C1Y) + ", " + nbt.getInt(NBT_C1Z) + "). "
                                    + "Kind: " + kind.display()
                                    + (label.isEmpty() ? "" : ", label: " + label)
                                    + ". Click the opposite corner.")
                            .formatted(Formatting.GRAY), false);
                } else {
                    int sizeX = Math.abs(nbt.getInt(NBT_C2X) - nbt.getInt(NBT_C1X)) + 1;
                    int sizeY = Math.abs(nbt.getInt(NBT_C2Y) - nbt.getInt(NBT_C1Y)) + 1;
                    int sizeZ = Math.abs(nbt.getInt(NBT_C2Z) - nbt.getInt(NBT_C1Z)) + 1;
                    ((ServerPlayerEntity) player).sendMessage(Text.literal(
                            "[Society] Box: " + sizeX + "x" + sizeY + "x" + sizeZ + " as "
                                    + kind.display() + (label.isEmpty() ? "" : ", label: " + label)
                                    + ". Run /society structure claim to claim it.")
                            .formatted(Formatting.GRAY), false);
                }
            }
            return TypedActionResult.success(stack);
        }
        // Right-click in the air clears the selection.
        clear(nbt);
        stack.removeCustomName();
        if (player instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) player).sendMessage(Text.literal(
                    "[Society] Setter Stick selection cleared.").formatted(Formatting.GRAY), false);
        }
        return TypedActionResult.success(stack);
    }

    // =====================================================================
    // Shared helpers (also used by the claim command)
    // =====================================================================

    /** The working name shown on the stack while a claim is being prepared. */
    public static Text stickName(PlayerStructureKind kind, String label) {
        StringBuilder sb = new StringBuilder("The Setter Stick - ");
        sb.append(kind.display());
        if (label != null && !label.isEmpty()) {
            sb.append(' ');
            sb.append('"');
            sb.append(label);
            sb.append('"');
        }
        return Text.literal(sb.toString()).formatted(Formatting.LIGHT_PURPLE);
    }

    public static PlayerStructureKind currentKind(NbtCompound nbt) {
        return PlayerStructureKind.byName(nbt.getString(NBT_KIND));
    }

    public static String currentLabel(NbtCompound nbt) {
        return nbt.getString(NBT_LABEL);
    }

    /** The label currently set on the stick, or the kind's display as fallback. */
    public static String labelOrDefault(NbtCompound nbt) {
        String label = nbt.getString(NBT_LABEL);
        return label.isEmpty() ? currentKind(nbt).display() : label;
    }

    public static void setKind(NbtCompound nbt, PlayerStructureKind kind) {
        nbt.putString(NBT_KIND, kind.name());
    }

    public static void setLabel(NbtCompound nbt, String label) {
        nbt.putString(NBT_LABEL, label == null ? "" : label);
    }

    /** True when both corners are set. */
    public static boolean hasBox(NbtCompound nbt) {
        return nbt.contains(NBT_C1X) && nbt.contains(NBT_C2X);
    }

    /** The claimed box as {minX, minY, minZ, maxX, maxY, maxZ}, or null. */
    public static int[] boxOf(NbtCompound nbt) {
        if (!hasBox(nbt)) return null;
        return new int[]{
                Math.min(nbt.getInt(NBT_C1X), nbt.getInt(NBT_C2X)),
                Math.min(nbt.getInt(NBT_C1Y), nbt.getInt(NBT_C2Y)),
                Math.min(nbt.getInt(NBT_C1Z), nbt.getInt(NBT_C2Z)),
                Math.max(nbt.getInt(NBT_C1X), nbt.getInt(NBT_C2X)),
                Math.max(nbt.getInt(NBT_C1Y), nbt.getInt(NBT_C2Y)),
                Math.max(nbt.getInt(NBT_C1Z), nbt.getInt(NBT_C2Z))};
    }

    public static void clear(NbtCompound nbt) {
        nbt.remove(NBT_C1X);
        nbt.remove(NBT_C1Y);
        nbt.remove(NBT_C1Z);
        nbt.remove(NBT_C2X);
        nbt.remove(NBT_C2Y);
        nbt.remove(NBT_C2Z);
    }

    private static PlayerStructureKind cycleKind(PlayerStructureKind current) {
        PlayerStructureKind[] values = PlayerStructureKind.values();
        int index = current == null ? -1 : current.ordinal();
        return values[(index + 1) % values.length];
    }
}
