package net.fawnoculus.warclaims.networking.messages;

import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

public class FactionSyncMessage implements IMessage {
    private final HashMap<UUID, FactionInstance> teams;

    public FactionSyncMessage(){
        teams = new HashMap<>();
    }

    public FactionSyncMessage(HashMap<UUID, FactionInstance> from){
        teams = from;
    }

    public void setTeam(UUID id, @Nullable FactionInstance team) {
        teams.put(id, team);
    }

    public HashMap<UUID, FactionInstance> getMap(){
        return this.teams;
    }

    public boolean isEmpty() {
        return this.teams.isEmpty();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int updatedClaims = buf.readInt();
        for (int i = 0; i < updatedClaims; i++) {
            UUID factionId = new UUID(buf.readLong(), buf.readLong());

            boolean isNull = buf.readBoolean();
            if(isNull) {
                this.setTeam(factionId, null);
                continue;
            }

            this.setTeam(factionId, FactionInstance.fromBytes(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(teams.size());
        for (UUID id : teams.keySet()) {
            FactionInstance team = teams.get(id);
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());

            if(team == null) {
                buf.writeBoolean(true);
                continue;
            }

            buf.writeBoolean(false);
            FactionInstance.toByteBuff(buf, team);
        }
    }
}
