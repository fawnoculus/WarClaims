package net.fawnoculus.warclaims.claims;

import com.google.gson.JsonObject;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.Objects;
import java.util.UUID;

public class ClaimInstance {
    public final UUID factionId;
    public final int level;

    public ClaimInstance(UUID factionId, int level) {
        this.factionId = factionId;
        this.level = level;
    }

    public static ClaimInstance fromJson(JsonObject json) {
        UUID factionId = UUID.fromString(json.get("factionId").getAsString());
        int level = json.get("level").getAsInt();
        return new ClaimInstance(factionId, level);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("factionId", this.factionId.toString());
        json.addProperty("level", this.level);
        return json;
    }

    public ITextComponent makeTooltip(FactionInstance team) {
        return new TextComponentString(team.name + " (" + this.level + ")");
    }


    @Override
    public String toString() {
        return "ClaimInstance{" +
                "factionId=" + factionId +
                ", level=" + level +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimInstance)) return false;
        ClaimInstance that = (ClaimInstance) o;
        return level == that.level && Objects.equals(factionId, that.factionId);
    }
}
