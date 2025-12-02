package net.fawnoculus.warclaims.claims.faction;

import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.utils.ColorUtil;
import net.fawnoculus.warclaims.utils.FileUtil;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
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

    public boolean isAlly(EntityPlayerMP playerMP) {
        return this.isAlly(playerMP.getGameProfile().getId());
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

    public boolean isAlly(UUID id) {
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

    public static void toWriter(Writer writer, FactionInstance faction) throws IOException {
        FileUtil.writeUUID(writer, faction.owner);
        WarClaims.LOGGER.info("Writing color = R: {} G: {} B: {}", ColorUtil.getRed(faction.color), ColorUtil.getGreen(faction.color), ColorUtil.getGreen(faction.color));
        writer.write(ColorUtil.getRed(faction.color));
        writer.write(ColorUtil.getGreen(faction.color));
        writer.write(ColorUtil.getGreen(faction.color));
        writer.write(faction.name.length());
        writer.write(faction.name);

        writer.write(faction.officers.size());
        for (UUID officer : faction.officers) {
            FileUtil.writeUUID(writer, officer);
        }

        writer.write(faction.members.size());
        for (UUID member : faction.members) {
            FileUtil.writeUUID(writer, member);
        }

        writer.write(faction.allies.size());
        for (UUID ally : faction.allies) {
            FileUtil.writeUUID(writer, ally);
        }
    }

    public static FactionInstance fromReader(Reader reader) throws IOException {
        UUID owner = FileUtil.readUUID(reader);
        int color = ColorUtil.fromRGB(reader.read(), reader.read(), reader.read());
        WarClaims.LOGGER.info("Read color = R: {} G: {} B: {}", ColorUtil.getRed(color), ColorUtil.getGreen(color), ColorUtil.getGreen(color));

        int nameLength = reader.read();
        char[] nameData = new char[nameLength];
        int ignored = reader.read(nameData);
        String name = new String(nameData);

        HashSet<UUID> officers = new HashSet<>();
        int officerCount = reader.read();
        for (int i = 0; i < officerCount; i++) {
            officers.add(FileUtil.readUUID(reader));
        }

        HashSet<UUID> members = new HashSet<>();
        int memberCount = reader.read();
        for (int i = 0; i < memberCount; i++) {
            members.add(FileUtil.readUUID(reader));
        }

        HashSet<UUID> allies = new HashSet<>();
        int allyCount = reader.read();
        for (int i = 0; i < allyCount; i++) {
            allies.add(FileUtil.readUUID(reader));
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
