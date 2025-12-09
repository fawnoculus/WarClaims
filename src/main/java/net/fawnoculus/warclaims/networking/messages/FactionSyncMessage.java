package net.fawnoculus.warclaims.networking.messages;

import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FactionSyncMessage implements IMessage {
    private final Map<UUID, FactionInstance> faction;

    public FactionSyncMessage() {
        faction = new HashMap<>();
    }

    public FactionSyncMessage(Map<UUID, FactionInstance> from) {
        faction = from;
    }

    public void setFaction(UUID id, @Nullable FactionInstance team) {
        faction.put(id, team);
    }

    public void removeFaction(UUID id) {
        faction.remove(id);
    }

    public Map<UUID, FactionInstance> getMap() {
        return this.faction;
    }

    public boolean isEmpty() {
        return this.faction.isEmpty();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int updatedClaims = buf.readInt();
        for (int i = 0; i < updatedClaims; i++) {
            UUID factionId = new UUID(buf.readLong(), buf.readLong());

            boolean isNull = buf.readBoolean();
            if (isNull) {
                this.setFaction(factionId, null);
                continue;
            }

            this.setFaction(factionId, FactionInstance.fromBytes(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(faction.size());
        for (UUID id : faction.keySet()) {
            FactionInstance team = faction.get(id);
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());

            if (team == null) {
                buf.writeBoolean(true);
                continue;
            }

            buf.writeBoolean(false);
            FactionInstance.toByteBuff(buf, team);
        }
    }
}
