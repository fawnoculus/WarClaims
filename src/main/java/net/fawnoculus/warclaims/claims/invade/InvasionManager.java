package net.fawnoculus.warclaims.claims.invade;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.realmsclient.util.Pair;
import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.fawnoculus.warclaims.networking.messages.InvasionSyncMessage;
import net.fawnoculus.warclaims.utils.JsonUtil;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;

public class InvasionManager {
    private static final HashMap<Integer, HashMap<ChunkPos, InvasionInstance>> INVASIONS = new HashMap<>();
    public static InvasionSyncMessage currentTickUpdates = new InvasionSyncMessage();

    public static @Nullable InvasionInstance getClaim(int dimension, int chunkX, int chunkZ) {
        HashMap<ChunkPos, InvasionInstance> dimensionInvasions = INVASIONS.get(dimension);
        if(dimensionInvasions == null) {
            return null;
        }
        return dimensionInvasions.get(new ChunkPos(chunkX, chunkZ));
    }

    public static @Nullable FactionInstance getFaction(int dimension, int chunkX, int chunkZ) {
        InvasionInstance invasion = getClaim(dimension, chunkX, chunkZ);
        if (invasion == null) {
            return null;
        }

        return FactionManager.getFaction(invasion.factionId);
    }

    public static void invasion(int dimension, int chunkX, int chunkZ, UUID factionId, int level) {
        setClaim(dimension, chunkX, chunkZ, new InvasionInstance(factionId, level));
    }

    public static void setClaim(int dimension, int chunkX, int chunkZ, InvasionInstance invasion) {
        setClaim(dimension, new ChunkPos(chunkX, chunkZ), invasion);
    }

    public static void setClaim(int dimension, ChunkPos pos, InvasionInstance invasion) {
        HashMap<ChunkPos, InvasionInstance> dimensionInvasions = INVASIONS
                .computeIfAbsent(dimension, ignored -> new HashMap<>());
        dimensionInvasions.put(pos, invasion);
        currentTickUpdates.setClaim(dimension, pos, invasion);
    }

    public static void unclaim(int dimension, int chunkX, int chunkZ) {
        HashMap<ChunkPos, InvasionInstance> dimensionInvasions = INVASIONS
                .computeIfAbsent(dimension, ignored -> new HashMap<>());

        dimensionInvasions.remove(new ChunkPos(chunkX, chunkZ));
        currentTickUpdates.setClaim(dimension, chunkX, chunkZ, null);
    }

    public static void removeClaimIf(Predicate<InvasionInstance> predicate) {
        ArrayList<Pair<Integer, ChunkPos>> toRemove = new ArrayList<>();

        for (Integer dimension : INVASIONS.keySet()) {
            HashMap<ChunkPos, InvasionInstance> DimensionClaims = INVASIONS.get(dimension);

            for (ChunkPos pos : DimensionClaims.keySet()) {
                InvasionInstance invasion = DimensionClaims.get(pos);

                if (predicate.test(invasion)) {
                    toRemove.add(Pair.of(dimension, pos));
                }
            }
        }

        toRemove.forEach(pair -> unclaim(pair.first(), pair.second().x, pair.second().z));
    }

    public static void onTick() {
        if(!currentTickUpdates.isEmpty()) {
            WarClaimsNetworking.WRAPPER.sendToAll(currentTickUpdates);
            currentTickUpdates = new InvasionSyncMessage();
        }
    }

    public static void clear() {
        INVASIONS.clear();
        currentTickUpdates = new InvasionSyncMessage();
    }

    public static void onPlayerJoin(EntityPlayerMP playerMP) {
        WarClaimsNetworking.WRAPPER.sendTo(new InvasionSyncMessage(INVASIONS), playerMP);
    }

    public static void loadFromFile(String worldPath) {
        INVASIONS.clear();
        File file = new File(worldPath + File.separator + "data" + File.separator + "warclaims" + File.separator + "invasions.json");
        if (!file.exists()) {
            return;
        }

        try (Reader reader = new FileReader(file)) {
            JsonObject json = JsonUtil.fromReader(reader, JsonObject.class);

            if (!WarClaims.isCorrectFileVersion(json.get(WarClaims.FILE_VERSION_NAME))) {
                WarClaims.LOGGER.warn("Trying to load Invasions of different or unknown File Version, things may not go well!");
                WarClaims.LOGGER.info("Trying Making backup of Invasions, just in case");
                try {
                    Files.copy(file.toPath(), file.toPath().resolveSibling("invasions.json.bak"), StandardCopyOption.REPLACE_EXISTING);
                }catch (IOException exception) {
                    WarClaims.LOGGER.warn("Failed to make Invasions backup", exception);
                    WarClaims.LOGGER.info("(Load Invasions) We are just gonna continue and pretend everything is fine, surely nothing bad will happen right?");
                }
            }

            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                int dimension;
                try {
                    dimension = Integer.parseInt(entry.getKey());
                }catch (NumberFormatException ignored) {
                    continue;
                }

                JsonObject dimensionJson = entry.getValue().getAsJsonObject();


                for (Map.Entry<String, JsonElement> dimensionEntry : dimensionJson.entrySet()) {
                    if (!dimensionEntry.getValue().isJsonObject()) {
                        continue;
                    }

                    ChunkPos pos;
                    InvasionInstance invasion;
                    try {
                        pos = JsonUtil.toChunkPos(dimensionEntry.getKey());
                        invasion = InvasionInstance.fromJson(dimensionEntry.getValue().getAsJsonObject());
                    }catch (Throwable ignored) {
                        continue;
                    }

                    setClaim(dimension, pos, invasion);
                }
            }

        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to load Invasions: {}", e.getMessage());
        }
    }

    public static void saveToFile(String worldPath) {
        File file = new File(worldPath + File.separator + "data" + File.separator + "warclaims" + File.separator + "invasions.json");
        try {
            if (!file.getParentFile().exists()) {
                boolean ignored = file.getParentFile().mkdirs();
            }
            if(file.exists()) {
                boolean ignored = file.delete();
            }
            boolean ignored = file.createNewFile();
        } catch (IOException e) {
            WarClaims.LOGGER.warn("Failed to create new Invasions file: {}", e.getMessage());
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            JsonObject json = new JsonObject();
            json.add(WarClaims.FILE_VERSION_NAME, new JsonPrimitive(WarClaims.FILE_VERSION));

            for (Integer dimension : INVASIONS.keySet()) {
                HashMap<ChunkPos, InvasionInstance> DimensionClaims = INVASIONS.get(dimension);
                JsonObject dimensionJson = new JsonObject();

                for (ChunkPos pos : DimensionClaims.keySet()) {
                    InvasionInstance invasion = DimensionClaims.get(pos);
                    dimensionJson.add(JsonUtil.fromChunkPos(pos), InvasionInstance.toJson(invasion));
                }

                json.add(dimension.toString(), dimensionJson);
            }

            JsonUtil.toWriter(writer, json);
        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to save Invasions: {}", e.getMessage());
        }
    }
}
