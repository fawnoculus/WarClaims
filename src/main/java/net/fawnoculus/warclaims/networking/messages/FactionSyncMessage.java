package net.fawnoculus.warclaims.networking.messages;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

public class FactionSyncMessage implements IMessage {
    private final HashMap<UUID, FactionInstance> faction;
    private @Nullable ByteBuf asBytes = null;

    public FactionSyncMessage() {
        faction = new HashMap<>();
    }

    public FactionSyncMessage(HashMap<UUID, FactionInstance> from) {
        faction = from;
    }

    public void setFaction(UUID id, @Nullable FactionInstance team) {
        faction.put(id, team);
        this.asBytes = null;
    }

    public void removeFaction(UUID id) {
        faction.remove(id);
        this.asBytes = null;
    }

    public HashMap<UUID, FactionInstance> getMap() {
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
        if (this.asBytes != null) {
            buf.writeBytes(this.asBytes);
            return;
        }
        ByteBuf buffer = Unpooled.buffer();

        buffer.writeInt(faction.size());
        for (UUID id : faction.keySet()) {
            FactionInstance team = faction.get(id);
            buffer.writeLong(id.getMostSignificantBits());
            buffer.writeLong(id.getLeastSignificantBits());

            if (team == null) {
                buffer.writeBoolean(true);
                continue;
            }

            buffer.writeBoolean(false);
            FactionInstance.toByteBuff(buffer, team);
        }

        this.asBytes = buffer;
        buf.writeBytes(buffer);
    }
}
