package net.fawnoculus.warclaims.networking.messages;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fawnoculus.warclaims.claims.ClaimKey;
import net.fawnoculus.warclaims.claims.invade.InvasionInstance;
import net.fawnoculus.warclaims.claims.invade.InvasionKey;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class InvasionSyncMessage implements IMessage {
    private final Map<InvasionKey, InvasionInstance> invasions;
    private final Map<ClaimKey, InvasionKey> invasionsByPos;
    private @Nullable ByteBuf asBytes = null;

    public InvasionSyncMessage() {
        this.invasions = new HashMap<>();
        this.invasionsByPos = new HashMap<>();
    }

    public InvasionSyncMessage(Map<InvasionKey, InvasionInstance> invasions, Map<ClaimKey, InvasionKey> invasionsByPos) {
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

    public boolean isEmpty() {
        return this.invasions.isEmpty() && this.invasionsByPos.isEmpty();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (this.asBytes != null) {
            buf.writeBytes(this.asBytes);
            return;
        }
        ByteBuf buffer = Unpooled.buffer();

        // TODO

        this.asBytes = buffer;
        buf.writeBytes(buffer);
    }
}
