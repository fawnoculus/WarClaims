package net.fawnoculus.warclaims.networking.messages;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.fawnoculus.warclaims.claims.ClaimKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.*;

public class ClaimSyncMessage implements IMessage {
    private final Map<ClaimKey, ClaimInstance> claims;
    private @Nullable ByteBuf asBytes = null;

    public ClaimSyncMessage() {
        claims = new HashMap<>();
    }

    public ClaimSyncMessage(HashMap<ClaimKey, ClaimInstance> claims) {
        this.claims = claims;
    }

    public void setClaim(int dimension, int chunkX, int chunkZ, @Nullable ClaimInstance claim) {
        this.setClaim(new ClaimKey(dimension, chunkX, chunkZ), claim);
    }

    public void setClaim(int dimension, ChunkPos pos, @Nullable ClaimInstance claim) {
        this.setClaim(new ClaimKey(dimension, pos), claim);
    }

    public void setClaim(ClaimKey key, @Nullable ClaimInstance claim) {
        this.asBytes = null;
        claims.put(key, claim);
    }

    public void removeClaim(ClaimKey key) {
        this.asBytes = null;
        claims.remove(key);
    }

    public Map<ClaimKey, ClaimInstance> getMap() {
        return this.claims;
    }

    public boolean isEmpty() {
        return this.claims.isEmpty();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int removedClaims = buf.readInt();
        for (int i = 0; i < removedClaims; i++) {
            int dimension = buf.readInt();
            int dimensionRemovedClaims = buf.readInt();
            for (int j = 0; j < dimensionRemovedClaims; j++) {
                this.setClaim(dimension, buf.readInt(), buf.readInt(), null);
            }
        }

        int changedClaims = buf.readInt();
        for (int i = 0; i < changedClaims; i++) {
            UUID factionId = new UUID(buf.readLong(), buf.readLong());
            int teamChangedDimensions = buf.readInt();
            for (int j = 0; j < teamChangedDimensions; j++) {
                int dimension = buf.readInt();
                int dimensionChangedClaims = buf.readInt();
                for (int k = 0; k < dimensionChangedClaims; k++) {
                    ChunkData data = ChunkData.fromByteBuff(buf);
                    this.setClaim(dimension, data.pos, new ClaimInstance(factionId, data.level));
                }
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (this.asBytes != null) {
            buf.writeBytes(this.asBytes);
            return;
        }
        ByteBuf buffer = Unpooled.buffer();

        Map<Integer, List<ChunkPos>> removedClaims = new HashMap<>();
        Map<UUID, Map<Integer, List<ChunkData>>> changedClaims = new HashMap<>();
        for (Map.Entry<ClaimKey, ClaimInstance> entry : this.claims.entrySet()) {
            if (entry.getValue() == null) {
                removedClaims.computeIfAbsent(entry.getKey().dimension, ignored -> new ArrayList<>())
                        .add(entry.getKey().pos);
                continue;
            }

            changedClaims.computeIfAbsent(entry.getValue().factionId, ignored -> new HashMap<>())
                    .computeIfAbsent(entry.getKey().dimension, ignored -> new ArrayList<>())
                    .add(new ChunkData(entry.getKey().pos, entry.getValue().level));
        }

        buffer.writeInt(removedClaims.size());
        for (Map.Entry<Integer, List<ChunkPos>> entry : removedClaims.entrySet()) {
            buffer.writeInt(entry.getKey());
            buffer.writeInt(entry.getValue().size());
            for (ChunkPos pos : entry.getValue()) {
                buffer.writeInt(pos.x);
                buffer.writeInt(pos.z);
            }
        }

        buffer.writeInt(changedClaims.size());
        for (Map.Entry<UUID, Map<Integer, List<ChunkData>>> entry : changedClaims.entrySet()) {
            buffer.writeLong(entry.getKey().getMostSignificantBits());
            buffer.writeLong(entry.getKey().getLeastSignificantBits());
            buffer.writeInt(entry.getValue().size());
            for (Map.Entry<Integer, List<ChunkData>> factionEntry : entry.getValue().entrySet()) {
                buffer.writeInt(factionEntry.getKey());
                buffer.writeInt(factionEntry.getValue().size());
                for (ChunkData data : factionEntry.getValue()) {
                    data.toByteBuff(buffer);
                }
            }
        }

        this.asBytes = buffer;
        buf.writeBytes(buffer);
    }

    private static class ChunkData {
        private final ChunkPos pos;
        private final int level;

        private ChunkData(ChunkPos pos, int level) {
            this.pos = pos;
            this.level = level;
        }

        private void toByteBuff(ByteBuf buf) {
            buf.writeInt(this.pos.x);
            buf.writeInt(this.pos.z);
            buf.writeInt(this.level);
        }

        private static ChunkData fromByteBuff(ByteBuf buf) {
            return new ChunkData(new ChunkPos(buf.readInt(), buf.readInt()), buf.readInt());
        }
    }
}
