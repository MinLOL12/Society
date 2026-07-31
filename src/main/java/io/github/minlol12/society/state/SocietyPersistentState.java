package io.github.minlol12.society.state;

import io.github.minlol12.society.core.io.Compound;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

/** The ledger, bound into the world save as the "society" data file. */
public final class SocietyPersistentState extends PersistentState {

    public static final String KEY = "society";

    private Compound data = new Compound();
    private boolean hasData;

    public Compound data() { return data; }

    public boolean hasData() { return hasData; }

    public void setData(Compound data) {
        this.data = data;
        this.hasData = true;
        markDirty();
    }

    public static SocietyPersistentState loadFromNbt(NbtCompound nbt) {
        SocietyPersistentState state = new SocietyPersistentState();
        state.data = NbtBridge.fromNbt(nbt.getCompound("Data"));
        state.hasData = true;
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("Data", NbtBridge.toNbt(data));
        return nbt;
    }
}
