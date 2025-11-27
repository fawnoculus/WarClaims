package net.fawnoculus.warclaims.networking.messages;

import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimUpdateMessage implements IMessage {
    private final Map<ChunkPos, ClaimInstance> fromChunk;

    public ClaimUpdateMessage(){
        fromChunk = new HashMap<>();
    }

    public ClaimUpdateMessage(Map<ChunkPos, ClaimInstance> from){
        fromChunk = from;
    }

    public void addClaim(ClaimInstance addedClaim) {
        fromChunk.put(addedClaim.chunkPos, addedClaim);
    }

    public Map<ChunkPos, ClaimInstance> getFromChunk(){
        return this.fromChunk;
    }

    public boolean isEmpty() {
        return this.fromChunk.isEmpty();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int updatedClaims = buf.readInt();
        for (int i = 0; i < updatedClaims; i++) {
            UUID teamId = new UUID(buf.readLong(), buf.readLong());
            ChunkPos chunkPos = new ChunkPos(buf.readInt(), buf.readInt());
            int level = buf.readInt();

            this.addClaim(new ClaimInstance(teamId, chunkPos, level));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(fromChunk.size());
        for (ClaimInstance claim : fromChunk.values()) {
            buf.writeLong(claim.teamId.getMostSignificantBits());
            buf.writeLong(claim.teamId.getLeastSignificantBits());
            buf.writeInt(claim.chunkPos.x);
            buf.writeInt(claim.chunkPos.z);
            buf.writeInt(claim.level);
        }
    }

    @Override
    public String toString() {
        return "ClaimUpdateMessage{" +
                "fromChunk=" + fromChunk +
                '}';
    }
}
