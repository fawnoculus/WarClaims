package net.fawnoculus.warclaims.networking.messages;

import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.claims.ClaimKey;
import net.fawnoculus.warclaims.claims.invade.ClientInvasionInstance;
import net.fawnoculus.warclaims.claims.invade.InvasionInstance;
import net.fawnoculus.warclaims.claims.invade.InvasionKey;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InvasionSyncMessage implements IMessage {
    private final Map<InvasionKey, ClientInvasionInstance> clientInvasions;
    private final Map<InvasionKey, InvasionInstance> invasions;
    private final Map<ClaimKey, InvasionKey> invasionsByPos;

    public InvasionSyncMessage() {
        this.clientInvasions = new HashMap<>();
        this.invasions = new HashMap<>();
        this.invasionsByPos = new HashMap<>();
    }

    public InvasionSyncMessage(Map<InvasionKey, InvasionInstance> invasions, Map<ClaimKey, InvasionKey> invasionsByPos) {
        this.clientInvasions = new HashMap<>();
        this.invasions = invasions;
        this.invasionsByPos = invasionsByPos;
    }

    public void setInvasion(InvasionKey key, @Nullable InvasionInstance instance) {
        this.invasions.put(key, instance);
    }

    public void removeInvasion(InvasionKey key) {
        this.invasions.remove(key);
    }

    public void setInvasionPos(ClaimKey claimKey, @Nullable InvasionKey invasionKey) {
        this.invasionsByPos.put(claimKey, invasionKey);
    }

    public void removeInvasionPos(ClaimKey claimKey) {
        this.invasionsByPos.remove(claimKey);
    }

    public Map<InvasionKey, ClientInvasionInstance> getClientInvasions() {
        return this.clientInvasions;
    }

    public Map<ClaimKey, InvasionKey> getInvasionsByPos() {
        return this.invasionsByPos;
    }

    public boolean isEmpty() {
        return this.invasions.isEmpty() && this.invasionsByPos.isEmpty();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int invasionsSize = buf.readInt();
        for (int i = 0; i < invasionsSize; i++) {
            UUID attackingFaction = new UUID(buf.readLong(), buf.readLong());
            UUID defendingFaction = new UUID(buf.readLong(), buf.readLong());
            InvasionKey invasionKey = new InvasionKey(attackingFaction, defendingFaction);

            boolean isNull = buf.readBoolean();
            if (isNull) {
                this.clientInvasions.put(invasionKey, null);
                continue;
            }

            ClientInvasionInstance clientInvasion = ClientInvasionInstance.fromByteBuff(buf);

            this.clientInvasions.put(invasionKey, clientInvasion);

            int posSize = buf.readInt();
            for (int j = 0; j < posSize; j++) {
                this.invasionsByPos.put(new ClaimKey(buf.readInt(), buf.readInt(), buf.readInt()), invasionKey);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        Set<Map.Entry<InvasionKey, InvasionInstance>> entrySet = this.invasions.entrySet();
        buf.writeInt(entrySet.size());
        for (Map.Entry<InvasionKey, InvasionInstance> entry : entrySet) {
            buf.writeLong(entry.getKey().attackingFaction.getMostSignificantBits());
            buf.writeLong(entry.getKey().attackingFaction.getLeastSignificantBits());
            buf.writeLong(entry.getKey().defendingFaction.getMostSignificantBits());
            buf.writeLong(entry.getKey().defendingFaction.getLeastSignificantBits());

            if (entry.getValue() == null) {
                buf.writeBoolean(true);
                continue;
            }

            buf.writeBoolean(false);
            entry.getValue().writeClientInstance(buf);

            buf.writeInt(entry.getValue().invadingChunks.size());
            for (ClaimKey claimKey : entry.getValue().invadingChunks.keySet()) {
                buf.writeInt(claimKey.dimension);
                buf.writeInt(claimKey.pos.x);
                buf.writeInt(claimKey.pos.z);
            }
        }
    }
}
