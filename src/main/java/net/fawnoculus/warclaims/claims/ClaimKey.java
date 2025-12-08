package net.fawnoculus.warclaims.claims;

import net.minecraft.util.math.ChunkPos;

import java.util.Objects;

public class ClaimKey {
    public final int dimension;
    public final ChunkPos pos;

    public ClaimKey(int dimension, int chunkX, int chunkZ) {
        this(dimension, new ChunkPos(chunkX, chunkZ));
    }

    public ClaimKey(int dimension, ChunkPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    public static ClaimKey fromString(String string) throws NumberFormatException, IndexOutOfBoundsException {
        String[] split = string.split(":");
        int dimension = Integer.parseInt(split[0]);
        ChunkPos pos = new ChunkPos(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        return new ClaimKey(dimension, pos);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ClaimKey)) return false;
        ClaimKey that = (ClaimKey) o;
        return this.dimension == that.dimension && Objects.equals(this.pos, that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, pos);
    }

    @Override
    public String toString() {
        return this.dimension + ":" + this.pos.x + ":" + this.pos.z;
    }
}
