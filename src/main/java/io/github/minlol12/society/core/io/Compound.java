package io.github.minlol12.society.core.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal self-describing serialisation container (string-keyed tagged union).
 * Supports: {@code String}, {@code Integer}, {@code Long}, {@code Double},
 * {@code Boolean}, {@code Compound}, {@code List<Compound>} and
 * {@code List<String>}.
 *
 * <p>The Minecraft adapter layer converts this to and from NBT
 * (see {@code NbtBridge}); the simulation core itself never depends on
 * Minecraft classes.</p>
 */
public final class Compound {

    private final Map<String, Object> values = new LinkedHashMap<String, Object>();

    public Map<String, Object> raw() {
        return values;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Compound put(String key, String value) {
        values.put(key, value);
        return this;
    }

    public Compound put(String key, int value) {
        values.put(key, Integer.valueOf(value));
        return this;
    }

    public Compound put(String key, long value) {
        values.put(key, Long.valueOf(value));
        return this;
    }

    public Compound put(String key, double value) {
        values.put(key, Double.valueOf(value));
        return this;
    }

    public Compound put(String key, boolean value) {
        values.put(key, Boolean.valueOf(value));
        return this;
    }

    public Compound put(String key, Compound value) {
        values.put(key, value);
        return this;
    }

    public Compound putCompoundList(String key, List<Compound> value) {
        values.put(key, value);
        return this;
    }

    public Compound putStringList(String key, List<String> value) {
        values.put(key, new ArrayList<String>(value));
        return this;
    }

    public String getString(String key, String fallback) {
        Object v = values.get(key);
        return v instanceof String ? (String) v : fallback;
    }

    public int getInt(String key, int fallback) {
        Object v = values.get(key);
        return v instanceof Integer ? ((Integer) v).intValue() : fallback;
    }

    public long getLong(String key, long fallback) {
        Object v = values.get(key);
        return v instanceof Long ? ((Long) v).longValue() : fallback;
    }

    public double getDouble(String key, double fallback) {
        Object v = values.get(key);
        return v instanceof Double ? ((Double) v).doubleValue() : fallback;
    }

    public boolean getBool(String key, boolean fallback) {
        Object v = values.get(key);
        return v instanceof Boolean ? ((Boolean) v).booleanValue() : fallback;
    }

    public Compound getCompound(String key) {
        Object v = values.get(key);
        return v instanceof Compound ? (Compound) v : new Compound();
    }

    @SuppressWarnings("unchecked")
    public List<Compound> getCompoundList(String key) {
        Object v = values.get(key);
        if (v instanceof List<?>) {
            List<Object> list = (List<Object>) v;
            List<Compound> out = new ArrayList<Compound>(list.size());
            for (Object o : list) {
                if (o instanceof Compound) {
                    out.add((Compound) o);
                }
            }
            return out;
        }
        return new ArrayList<Compound>();
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object v = values.get(key);
        if (v instanceof List<?>) {
            List<Object> list = (List<Object>) v;
            List<String> out = new ArrayList<String>(list.size());
            for (Object o : list) {
                if (o instanceof String) {
                    out.add((String) o);
                }
            }
            return out;
        }
        return new ArrayList<String>();
    }
}
