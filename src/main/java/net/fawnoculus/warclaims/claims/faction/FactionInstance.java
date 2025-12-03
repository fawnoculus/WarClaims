package net.fawnoculus.warclaims.claims.faction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.utils.ColorUtil;
import net.minecraft.entity.player.EntityPlayerMP;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

public class FactionInstance {
    public final UUID owner;
    public final int color;
    public final String name;
    public final HashSet<UUID> officers;
    public final HashSet<UUID> members;
    public final HashSet<UUID> allies;

    public FactionInstance(EntityPlayerMP owner, String name) {
        this.owner = owner.getGameProfile().getId();
        this.color = randomColor(owner.getRNG());
        this.name = name;
        this.officers = new HashSet<>();
        this.members = new HashSet<>();
        this.allies = new HashSet<>();
    }

    public FactionInstance(
            UUID owner,
            int color,
            String name,
            HashSet<UUID> officers,
            HashSet<UUID> members,
            HashSet<UUID> allies
    ) {
        this.owner = owner;
        this.color = color;
        this.name = name;
        this.officers = officers;
        this.members = members;
        this.allies = allies;
    }

    public boolean isAllied(EntityPlayerMP playerMP) {
        return this.isAllied(playerMP.getGameProfile().getId());
    }

    public boolean isMember(EntityPlayerMP playerMP) {
        return this.isMember(playerMP.getGameProfile().getId());
    }

    public boolean isOfficer(EntityPlayerMP playerMP) {
        return this.isOfficer(playerMP.getGameProfile().getId());
    }

    public boolean isOwner(EntityPlayerMP playerMP) {
        return this.isOwner(playerMP.getGameProfile().getId());
    }

    public boolean isAllied(UUID id) {
        if (this.isMember(id)) {
            return true;
        }

        for (UUID allyTeam : this.allies) {
            FactionInstance instance = FactionManager.getFaction(allyTeam);
            if(instance != null && instance.isMember(id)) {
                return true;
            }
        }

        return false;
    }

    public boolean isMember(UUID id) {
        return this.members.contains(id) || this.officers.contains(id) || this.owner.equals(id);
    }

    public boolean isOfficer(UUID id) {
        return this.officers.contains(id) || this.owner.equals(id);
    }

    public boolean isOwner(UUID id) {
        return this.owner.equals(id);
    }

    private static int randomColor(Random random) {
        return ColorUtil.fromHSV(random.nextFloat(), .9f, .9f);
    }

    public static JsonObject toJson(FactionInstance faction) {
        JsonObject json = new JsonObject();
        json.add("owner", new JsonPrimitive(faction.owner.toString()));
        json.add("color", new JsonPrimitive(faction.color));
        json.add("name", new JsonPrimitive(faction.name));

        JsonArray members = new JsonArray();
        faction.members.forEach(uuid -> members.add(uuid.toString()));
        json.add("members", members);

        JsonArray officers = new JsonArray();
        faction.officers.forEach(uuid -> officers.add(uuid.toString()));
        json.add("officers", officers);

        JsonArray allies = new JsonArray();
        faction.allies.forEach(uuid -> allies.add(uuid.toString()));
        json.add("allies", allies);

        return json;
    }

    public static FactionInstance fromJson(JsonObject json) {
        UUID owner = UUID.fromString(json.get("owner").getAsString());
        int color = json.get("color").getAsInt();
        String name = json.get("name").getAsString();

        HashSet<UUID> officers = new HashSet<>();
        for (JsonElement element : json.get("officers").getAsJsonArray()) {
            officers.add(UUID.fromString(element.getAsString()));
        }

        HashSet<UUID> members = new HashSet<>();
        for (JsonElement element : json.get("members").getAsJsonArray()) {
            members.add(UUID.fromString(element.getAsString()));
        }

        HashSet<UUID> allies = new HashSet<>();
        for (JsonElement element : json.get("allies").getAsJsonArray()) {
            allies.add(UUID.fromString(element.getAsString()));
        }

        return new FactionInstance(owner, color, name, officers, members, allies);
    }

    public static void toByteBuff(ByteBuf buf, FactionInstance faction) {
        buf.writeLong(faction.owner.getMostSignificantBits());
        buf.writeLong(faction.owner.getLeastSignificantBits());

        buf.writeInt(faction.color);

        buf.writeInt(faction.name.length());
        buf.writeCharSequence(faction.name, StandardCharsets.UTF_8);

        buf.writeInt(faction.officers.size());
        for (UUID officer : faction.officers) {
            buf.writeLong(officer.getMostSignificantBits());
            buf.writeLong(officer.getLeastSignificantBits());
        }

        buf.writeInt(faction.members.size());
        for (UUID member : faction.members) {
            buf.writeLong(member.getMostSignificantBits());
            buf.writeLong(member.getLeastSignificantBits());
        }

        buf.writeInt(faction.allies.size());
        for (UUID ally : faction.allies) {
            buf.writeLong(ally.getMostSignificantBits());
            buf.writeLong(ally.getLeastSignificantBits());
        }
    }

    public static FactionInstance fromBytes(ByteBuf buf) {
        UUID owner = new UUID(buf.readLong(), buf.readLong());
        int color = buf.readInt();

        int nameLength = buf.readInt();
        CharSequence charSequence = buf.readCharSequence(nameLength, StandardCharsets.UTF_8);
        String name = charSequence.toString();

        HashSet<UUID> officers = new HashSet<>();
        int officerCount = buf.readInt();
        for (int i = 0; i < officerCount; i++) {
            officers.add(new UUID(buf.readLong(), buf.readLong()));
        }

        HashSet<UUID> members = new HashSet<>();
        int memberCount = buf.readInt();
        for (int i = 0; i < memberCount; i++) {
            members.add(new UUID(buf.readLong(), buf.readLong()));
        }

        HashSet<UUID> allies = new HashSet<>();
        int allyCount = buf.readInt();
        for (int i = 0; i < allyCount; i++) {
            allies.add(new UUID(buf.readLong(), buf.readLong()));
        }

        return new FactionInstance(owner, color, name, officers, members, allies);
    }
}
