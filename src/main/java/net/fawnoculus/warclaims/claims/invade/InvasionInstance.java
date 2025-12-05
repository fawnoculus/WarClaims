package net.fawnoculus.warclaims.claims.invade;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.ChunkPos;

import java.util.Objects;
import java.util.UUID;

public class InvasionInstance {
    public final UUID factionId;
    public final long ticks;

    public InvasionInstance(UUID factionId) {
        this.factionId = factionId;
        this.ticks = 0;
    }

    public InvasionInstance(UUID factionId, long ticks) {
        this.factionId = factionId;
        this.ticks = ticks;
    }

    public static JsonObject toJson(InvasionInstance claim) {
        JsonObject json = new JsonObject();
        json.addProperty("factionId", claim.factionId.toString());
        json.addProperty("ticks", claim.ticks);
        return json;
    }

    public static InvasionInstance fromJson(JsonObject json) {
        UUID factionId = UUID.fromString(json.get("factionId").getAsString());
        long level = json.get("ticks").getAsLong();
        return new InvasionInstance(factionId, level);
    }

    public static void toByteBuff(ByteBuf buf, InvasionInstance claim) {
        buf.writeLong(claim.factionId.getMostSignificantBits());
        buf.writeLong(claim.factionId.getLeastSignificantBits());
        buf.writeLong(claim.ticks);
    }

    public static InvasionInstance fromBytes(ByteBuf buf) {
        UUID factionId = new UUID(buf.readLong(), buf.readLong());
        long ticks = buf.readLong();
        return new InvasionInstance(factionId, ticks);
    }

    public InvasionInstance onTick(int dimension, ChunkPos pos) {
        return new InvasionInstance(this.factionId, this.ticks + 1);
    }

    public boolean isDifferent(InvasionInstance other) {
        return !this.equals(other);
    }

    @Override
    public String toString() {
        return "InvasionInstance{" +
                "factionId=" + factionId +
                ", ticks=" + ticks +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InvasionInstance)) return false;
        InvasionInstance that = (InvasionInstance) o;
        return ticks == that.ticks && Objects.equals(factionId, that.factionId);
    }
}
