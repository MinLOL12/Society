package io.github.minlol12.society.core;

import io.github.minlol12.society.core.types.CultureOrigin;

/** Implemented by the Minecraft side to classify the land at a position. */
public interface CultureSampler {

    CultureOrigin sample(int blockX, int blockZ);
}
