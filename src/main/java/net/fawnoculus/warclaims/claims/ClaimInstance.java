package net.fawnoculus.warclaims.claims;

import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.utils.FileUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.UUID;

public class ClaimInstance {
    public final UUID factionId;
    public final int level;

    public ClaimInstance(UUID factionId, int level) {
        this.factionId = factionId;
        this.level = level;
    }

    public static void toWriter(Writer writer, ClaimInstance claim) throws IOException {
        FileUtil.writeUUID(writer, claim.factionId);
        writer.write(claim.level);
    }

    public static ClaimInstance fromReader(Reader reader) throws IOException {
        UUID factionId = FileUtil.readUUID(reader);
        int level = reader.read();
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
