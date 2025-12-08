package net.fawnoculus.warclaims.claims.invade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.fawnoculus.warclaims.claims.ClaimKey;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;

public class InvasionInstance {
    public long tickProgress;
    public long requiredTicksOnline;
    public long requiredTicksOffline;
    public Set<ClaimKey> invadingChunks;
    boolean defenderOnline = false;
    public boolean requiresClientSync = false;
    private FactionInstance attackingFaction = null;
    private FactionInstance defendingFaction = null;

    public InvasionInstance() {
        this.tickProgress = 0;
        this.requiredTicksOnline = 0;
        this.requiredTicksOffline = 0;
        this.invadingChunks = new HashSet<>();
    }

    public InvasionInstance(long tickProgress, long requiredTicksOnline, long requiredTicksOffline, HashSet<ClaimKey> invadingChunks) {
        this.tickProgress = tickProgress;
        this.requiredTicksOnline = requiredTicksOnline;
        this.requiredTicksOffline = requiredTicksOffline;
        this.invadingChunks = invadingChunks;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("tickProgress", this.tickProgress);
        json.addProperty("requiredTicksOnline", this.requiredTicksOnline);
        json.addProperty("requiredTicksOffline", this.requiredTicksOffline);

        JsonArray array = new JsonArray();
        for (ClaimKey claimKey : this.invadingChunks) {
            array.add(claimKey.toString());
        }
        json.add("invadingChunks", array);

        return json;
    }

    public static InvasionInstance fromJson(JsonObject json) throws RuntimeException {
        long tickProgress = json.get("tickProgress").getAsLong();
        long requiredTicksOnline = json.get("requiredTicksOnline").getAsLong();
        long requiredTicksOffline = json.get("requiredTicksOffline").getAsLong();

        JsonArray array = json.get("invadingChunks").getAsJsonArray();
        HashSet<ClaimKey> invadingChunks = new HashSet<>();
        for (JsonElement element : array) {
            invadingChunks.add(ClaimKey.fromString(element.getAsString()));
        }

        return new InvasionInstance(tickProgress, requiredTicksOnline, requiredTicksOffline, invadingChunks);
    }

    public void writeClientInstance(ByteBuf buf) {
        buf.writeLong(this.tickProgress);
    }

    public boolean onTick(MinecraftServer server, InvasionKey key) {
        if (attackingFaction == null) {
            attackingFaction = FactionManager.getFaction(key.attackingFaction);
            if (attackingFaction == null) {
                return true;
            }
        }

        if (defendingFaction == null) {
            defendingFaction = FactionManager.getFaction(key.defendingFaction);
            if (defendingFaction == null) {
                return true;
            }
        }

        boolean attackerInChunk = false;
        boolean defenderInChunk = false;
        boolean newDefenderOnline = false;

        for (EntityPlayerMP playerMP : server.getPlayerList().getPlayers()) {
            if (attackingFaction.isMember(playerMP) && invadingChunks.contains(new ClaimKey(playerMP.dimension, playerMP.chunkCoordX, playerMP.chunkCoordZ))) {
                attackerInChunk = true;
            }
            if (defendingFaction.isMember(playerMP)) {
                newDefenderOnline = true;
                if (invadingChunks.contains(new ClaimKey(playerMP.dimension, playerMP.chunkCoordX, playerMP.chunkCoordZ))) {
                    defenderInChunk = true;
                }
            }
        }

        this.defenderOnline = newDefenderOnline;

        if (attackerInChunk) {
            tickProgress++;
        }

        if (defenderInChunk) {
            tickProgress--;
        }

        if (tickProgress >= requiredTicks()) {
            for (ClaimKey claimKey : this.invadingChunks) {
                ClaimManager.setClaim(claimKey, new ClaimInstance(key.attackingFaction, 0));
            }
            return true;
        }

        return false;
    }

    public long requiredTicks() {
        if (this.defenderOnline) {
            return  this.requiredTicksOnline;
        } else {
            return  this.requiredTicksOffline;
        }
    }

    public void addChunk(ClaimKey claimKey, int level) {
        this.requiredTicksOnline += getTicksOnline(level);
        this.requiredTicksOffline += getTicksOffline(level);
        this.invadingChunks.add(claimKey);
    }

    public void removeChunk(ClaimKey claimKey, int level) {
        this.requiredTicksOnline -= getTicksOnline(level);
        this.requiredTicksOffline -= getTicksOffline(level);
        this.invadingChunks.remove(claimKey);
    }

    public static long getTicksOnline(int level) {
        switch (level) {
            case 0: return 60 * 20;
            case 1: return 2 * 60 * 20;
            case 2: return 4 * 60 * 20;
            case 3: return 6 * 60 * 20;
            case 4: return 10 * 60 * 20;
            case 5: return 15 * 60 * 20;
            default: return 0;
        }
    }

    public static long getTicksOffline(int level) {
        switch (level) {
            case 0:
            case 1:
            case 2: return 5 * 60 * 20;
            case 3: return 10 * 60 * 20;
            case 4: return 30 * 60 * 20;
            case 5: return 60 * 60 * 20;
            default: return 0;
        }
    }
}
