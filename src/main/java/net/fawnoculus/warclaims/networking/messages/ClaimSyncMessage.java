package net.fawnoculus.warclaims.networking.messages;

import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.HashMap;

public class ClaimSyncMessage implements IMessage {
    private final HashMap<Integer, HashMap<ChunkPos, ClaimInstance>> claims;

    public ClaimSyncMessage(){
        claims = new HashMap<>();
    }

    public ClaimSyncMessage(HashMap<Integer, HashMap<ChunkPos, ClaimInstance>> claims){
        this.claims = claims;
    }

    public void setClaim(int dimension, int chunkX, int chunkZ, @Nullable ClaimInstance claim) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = claims.get(dimension);
        if(dimensionClaims == null) {
            dimensionClaims = new HashMap<>();
        }
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        dimensionClaims.put(pos, claim);
        claims.put(dimension, dimensionClaims);
    }

    public HashMap<Integer, HashMap<ChunkPos, ClaimInstance>> getMap(){
        return this.claims;
    }

    public boolean isEmpty() {
        return this.claims.isEmpty();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int updatedDimensions = buf.readInt();
        for (int i = 0; i < updatedDimensions; i++) {
            int dimensionId = buf.readInt();
            int updatedClaims = buf.readInt();

            for (int j = 0; j < updatedClaims; j++) {
                int chunkX = buf.readInt();
                int chunkZ = buf.readInt();
                boolean isNull = buf.readBoolean();

                if(isNull) {
                    this.setClaim(dimensionId, chunkX, chunkZ, null);
                    continue;
                }

                this.setClaim(dimensionId, chunkX, chunkZ, ClaimInstance.fromBytes(buf));
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(claims.size());
        for (Integer dimensionId : claims.keySet()) {
            HashMap<ChunkPos, ClaimInstance> dimensionClaims = claims.get(dimensionId);

            buf.writeInt(dimensionId);
            buf.writeInt(dimensionClaims.size());

            for (ChunkPos pos : dimensionClaims.keySet()) {
                ClaimInstance claim = dimensionClaims.get(pos);
                buf.writeInt(pos.x);
                buf.writeInt(pos.z);

                if(claim == null) {
                    buf.writeBoolean(true);
                    continue;
                }

                buf.writeBoolean(false);
                ClaimInstance.toByteBuff(buf, claim);
            }
        }
    }
}
