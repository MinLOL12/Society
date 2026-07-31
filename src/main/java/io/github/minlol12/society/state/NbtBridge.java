package io.github.minlol12.society.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.minlol12.society.core.io.Compound;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

/**
 * Two-way conversion between the engine's Minecraft-free {@link Compound}
 * tree and vanilla NBT, so the ledger persists inside the world save.
 */
public final class NbtBridge {

    private NbtBridge() { }

    public static NbtCompound toNbt(Compound compound) {
        NbtCompound out = new NbtCompound();
        for (Map.Entry<String, Object> entry : compound.raw().entrySet()) {
            NbtElement element = elementOf(entry.getValue());
            if (element != null) {
                out.put(entry.getKey(), element);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static NbtElement elementOf(Object value) {
        if (value instanceof String) {
            return NbtString.of((String) value);
        }
        if (value instanceof Integer) {
            return net.minecraft.nbt.NbtInt.of(((Integer) value).intValue());
        }
        if (value instanceof Long) {
            return net.minecraft.nbt.NbtLong.of(((Long) value).longValue());
        }
        if (value instanceof Double) {
            return net.minecraft.nbt.NbtDouble.of(((Double) value).doubleValue());
        }
        if (value instanceof Boolean) {
            return net.minecraft.nbt.NbtByte.of((byte) (((Boolean) value).booleanValue() ? 1 : 0));
        }
        if (value instanceof Compound) {
            return toNbt((Compound) value);
        }
        if (value instanceof List<?>) {
            List<Object> list = (List<Object>) value;
            NbtList out = new NbtList();
            for (Object item : list) {
                NbtElement element = elementOf(item);
                if (element != null) {
                    out.add(element);
                }
            }
            return out;
        }
        return null;
    }

    public static Compound fromNbt(NbtCompound nbt) {
        Compound out = new Compound();
        for (String key : nbt.getKeys()) {
            NbtElement element = nbt.get(key);
            Object value = valueOf(element);
            if (value != null) {
                putTyped(out, key, value);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void putTyped(Compound out, String key, Object value) {
        if (value instanceof String) {
            out.put(key, (String) value);
        } else if (value instanceof Integer) {
            out.put(key, ((Integer) value).intValue());
        } else if (value instanceof Long) {
            out.put(key, ((Long) value).longValue());
        } else if (value instanceof Double) {
            out.put(key, ((Double) value).doubleValue());
        } else if (value instanceof Boolean) {
            out.put(key, ((Boolean) value).booleanValue());
        } else if (value instanceof Compound) {
            out.put(key, (Compound) value);
        } else if (value instanceof List<?>) {
            List<Object> list = (List<Object>) value;
            if (!list.isEmpty() && list.get(0) instanceof String) {
                List<String> strings = new ArrayList<String>();
                for (Object o : list) {
                    if (o instanceof String) strings.add((String) o);
                }
                out.putStringList(key, strings);
            } else {
                List<Compound> compounds = new ArrayList<Compound>();
                for (Object o : list) {
                    if (o instanceof Compound) compounds.add((Compound) o);
                }
                out.putCompoundList(key, compounds);
            }
        }
    }

    private static Object valueOf(NbtElement element) {
        switch (element.getType()) {
            case NbtElement.STRING_TYPE: {
                NbtString text = (NbtString) element;
                return text.asString();
            }
            case NbtElement.INT_TYPE: {
                return Integer.valueOf(((net.minecraft.nbt.NbtInt) element).intValue());
            }
            case NbtElement.LONG_TYPE: {
                return Long.valueOf(((net.minecraft.nbt.NbtLong) element).longValue());
            }
            case NbtElement.DOUBLE_TYPE: {
                return Double.valueOf(((net.minecraft.nbt.NbtDouble) element).doubleValue());
            }
            case NbtElement.BYTE_TYPE: {
                return Boolean.valueOf(((net.minecraft.nbt.NbtByte) element).byteValue() != 0);
            }
            case NbtElement.COMPOUND_TYPE: {
                return fromNbt((NbtCompound) element);
            }
            case NbtElement.LIST_TYPE: {
                NbtList list = (NbtList) element;
                List<Object> out = new ArrayList<Object>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    Object value = valueOf(list.get(i));
                    if (value != null) out.add(value);
                }
                return out;
            }
            default:
                return null;
        }
    }
}
