package net.fawnoculus.warclaims.claims.invade;

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

import java.util.HashMap;
import java.util.Map;

public class InvasionInstance {
    public long tickProgress;
    public long requiredTicksOnline;
    public long requiredTicksOffline;
    public Map<ClaimKey, Integer> invadingChunks;
    public boolean requiresClientSync = false;
    public boolean isNew = true;
    boolean defenderOnline = false;
    private FactionInstance attackingFaction = null;
    private FactionInstance defendingFaction = null;

    public InvasionInstance() {
        this.tickProgress = 20 * 5;
        this.requiredTicksOnline = 20 * 60;
        this.requiredTicksOffline = 20 * 60 * 5;
        this.invadingChunks = new HashMap<>();
    }

    public InvasionInstance(long tickProgress, long requiredTicksOnline, long requiredTicksOffline, HashMap<ClaimKey, Integer> invadingChunks) {
        this.isNew = false;
        this.tickProgress = tickProgress;
        this.requiredTicksOnline = requiredTicksOnline;
        this.requiredTicksOffline = requiredTicksOffline;
        this.invadingChunks = invadingChunks;
    }

    public static InvasionInstance fromJson(JsonObject json) throws RuntimeException {
        long tickProgress = json.get("tickProgress").getAsLong();
        long requiredTicksOnline = json.get("requiredTicksOnline").getAsLong();
        long requiredTicksOffline = json.get("requiredTicksOffline").getAsLong();

        JsonObject chunks = json.get("invadingChunks").getAsJsonObject();
        HashMap<ClaimKey, Integer> invadingChunks = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : chunks.entrySet()) {
            invadingChunks.put(ClaimKey.fromString(entry.getKey()), entry.getValue().getAsInt());
        }

        return new InvasionInstance(tickProgress, requiredTicksOnline, requiredTicksOffline, invadingChunks);
    }

    public static long getTicksOnline(int level) {
        switch (level) {
            case 0:
                return 60 * 20;
            case 1:
                return 2 * 60 * 20;
            case 2:
                return 4 * 60 * 20;
            case 3:
                return 6 * 60 * 20;
            case 4:
                return 10 * 60 * 20;
            case 5:
                return 15 * 60 * 20;
            default:
                return 0;
        }
    }

    public static long getTicksOffline(int level) {
        switch (level) {
            case 0:
            case 1:
            case 2:
                return 5 * 60 * 20;
            case 3:
                return 10 * 60 * 20;
            case 4:
                return 30 * 60 * 20;
            case 5:
                return 60 * 60 * 20;
            default:
                return 0;
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("tickProgress", this.tickProgress);
        json.addProperty("requiredTicksOnline", this.requiredTicksOnline);
        json.addProperty("requiredTicksOffline", this.requiredTicksOffline);

        JsonObject chunks = new JsonObject();
        for (Map.Entry<ClaimKey, Integer> entry : this.invadingChunks.entrySet()) {
            chunks.addProperty(entry.getKey().toString(), entry.getValue());
        }
        json.add("invadingChunks", chunks);

        return json;
    }

    public void writeClientInstance(ByteBuf buf) {
        buf.writeLong(this.tickProgress);
        buf.writeLong(this.requiredTicks());
    }

    public boolean onTick(MinecraftServer server, InvasionKey key) {
        this.isNew = false;
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
            if (attackingFaction.isMember(playerMP) && invadingChunks.containsKey(new ClaimKey(playerMP.dimension, playerMP.chunkCoordX, playerMP.chunkCoordZ))) {
                attackerInChunk = true;
            }
            if (defendingFaction.isMember(playerMP)) {
                newDefenderOnline = true;
                if (invadingChunks.containsKey(new ClaimKey(playerMP.dimension, playerMP.chunkCoordX, playerMP.chunkCoordZ))) {
                    defenderInChunk = true;
                }
            }
        }

        if (this.defenderOnline != newDefenderOnline) {
            if (newDefenderOnline) {
                this.tickProgress = (long) ((double) this.tickProgress * ((double) this.requiredTicksOnline / (double) requiredTicksOffline));
            } else {
                this.tickProgress = (long) ((double) this.tickProgress * ((double) this.requiredTicksOffline / (double) requiredTicksOnline));
            }

            this.defenderOnline = newDefenderOnline;
            this.requiresClientSync = true;
        }

        if (attackerInChunk) {
            tickProgress++;
            this.requiresClientSync = true;
        }

        if (defenderInChunk) {
            tickProgress--;
            this.requiresClientSync = true;
        }

        if (tickProgress < 0) {
            return true;
        }

        if (tickProgress >= requiredTicks()) {
            for (ClaimKey claimKey : this.invadingChunks.keySet()) {
                ClaimInstance claim = ClaimManager.getClaim(claimKey);
                if (claim != null && claim.level == 5) {
                    ClaimManager.transformClaims(previousClaim -> {
                        if (key.defendingFaction.equals(previousClaim.factionId)) {
                            return new ClaimInstance(key.attackingFaction, 0);
                        }
                        return previousClaim;
                    });
                    return true;
                }

                ClaimManager.setClaim(claimKey, new ClaimInstance(key.attackingFaction, 0));
            }
            return true;
        }

        return false;
    }

    public long requiredTicks() {
        if (this.defenderOnline) {
            return this.requiredTicksOnline;
        } else {
            return this.requiredTicksOffline;
        }
    }

    public boolean isEmpty() {
        return this.invadingChunks.isEmpty();
    }

    public void addChunk(ClaimKey claimKey, int level) {
        this.requiredTicksOnline += getTicksOnline(level);
        this.requiredTicksOffline += getTicksOffline(level);
        this.invadingChunks.put(claimKey, level);
    }

    public void removeChunk(ClaimKey claimKey) {
        int level = this.invadingChunks.getOrDefault(claimKey, 0);
        this.requiredTicksOnline -= getTicksOnline(level);
        this.requiredTicksOffline -= getTicksOffline(level);
        this.invadingChunks.remove(claimKey);
    }
}
