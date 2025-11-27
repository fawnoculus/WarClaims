package net.fawnoculus.warclaims.claims;

import net.minecraft.util.math.ChunkPos;

import java.util.UUID;

public class ClaimInstance {
    public final UUID teamId;
    public final ChunkPos chunkPos;
    public final int level;

    public ClaimInstance(UUID teamId, ChunkPos chunkPos, int level) {
        this.teamId = teamId;
        this.chunkPos = chunkPos;
        this.level = level;
    }
}
