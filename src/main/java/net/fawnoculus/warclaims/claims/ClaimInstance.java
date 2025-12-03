package net.fawnoculus.warclaims.claims;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class ClaimInstance {
    public final UUID factionId;
    public final int level;

    public ClaimInstance(UUID factionId, int level) {
        this.factionId = factionId;
        this.level = level;
    }

    public static JsonObject toJson(ClaimInstance claim) {
        JsonObject json = new JsonObject();
        json.addProperty("factionId", claim.factionId.toString());
        json.addProperty("level", claim.level);
        return json;
    }

    public static ClaimInstance fromJson(JsonObject json) {
        UUID factionId = UUID.fromString(json.get("factionId").getAsString());
        int level = json.get("level").getAsInt();
        return new ClaimInstance(factionId, level);
    }

    public static void toByteBuff(ByteBuf buf, ClaimInstance claim) {
        buf.writeLong(claim.factionId.getMostSignificantBits());
        buf.writeLong(claim.factionId.getLeastSignificantBits());
        buf.writeInt(claim.level);
    }

    public static ClaimInstance fromBytes(ByteBuf buf) {
        UUID factionId = new UUID(buf.readLong(), buf.readLong());
        int level = buf.readInt();
        return new ClaimInstance(factionId, level);
    }

    @Override
    public String toString() {
        return "ClaimInstance{" +
                "factionId=" + factionId +
                ", level=" + level +
                '}';
    }
}
