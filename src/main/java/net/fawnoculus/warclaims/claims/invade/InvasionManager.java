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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class InvasionManager {
    private static final HashMap<Integer, HashMap<ChunkPos, InvasionInstance>> INVASIONS = new HashMap<>();
    public static InvasionSyncMessage currentTickUpdates = new InvasionSyncMessage();

    public static @Nullable InvasionInstance getClaim(int dimension, int chunkX, int chunkZ) {
        HashMap<ChunkPos, InvasionInstance> dimensionInvasions = INVASIONS.get(dimension);
        if (dimensionInvasions == null) {
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

    public static void invade(int dimension, int chunkX, int chunkZ, UUID factionId) {
        setInvasion(dimension, chunkX, chunkZ, new InvasionInstance(factionId));
    }

    public static void setInvasion(int dimension, int chunkX, int chunkZ, InvasionInstance invasion) {
        setInvasion(dimension, new ChunkPos(chunkX, chunkZ), invasion);
    }

    public static void setInvasion(int dimension, ChunkPos pos, InvasionInstance invasion) {
        HashMap<ChunkPos, InvasionInstance> dimensionInvasions = INVASIONS
                .computeIfAbsent(dimension, ignored -> new HashMap<>());
        dimensionInvasions.put(pos, invasion);
        currentTickUpdates.setInvasion(dimension, pos, invasion);
    }

    public static void unInvade(int dimension, int chunkX, int chunkZ) {
        unInvade(dimension, new ChunkPos(chunkX, chunkZ));
    }

    public static void unInvade(int dimension, ChunkPos pos) {
        HashMap<ChunkPos, InvasionInstance> dimensionInvasions = INVASIONS
                .computeIfAbsent(dimension, ignored -> new HashMap<>());

        dimensionInvasions.remove(pos);
        currentTickUpdates.setInvasion(dimension, pos, null);
    }

    public static void removeInvasionIf(Predicate<InvasionInstance> predicate) {
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

        for (Pair<Integer, ChunkPos> pair : toRemove) {
            unInvade(pair.first(), pair.second());
        }
    }

    /**
     * Takes the Items required to invade a Chunk out of the players inventory
     * @return true if the player had enough Resources, false if they didn't
     */
    public static boolean takeRequiredItems(EntityPlayer player) {
        // TODO
        return true;
    }

    public static long getInvasionTime(int level, boolean isOffline) {
        if (isOffline) {
            switch (level) {
                case 0:
                case 1:
                case 2: return 5 * 60 * 20;
                case 3: return 10 * 60 * 20;
                case 4: return 30 * 60 * 20;
                case 5: return 60 * 60 * 20;
            }
        }

        switch (level) {
            case 0: return 60 * 20;
            case 1: return 2 * 60 * 20;
            case 2: return 4 * 60 * 20;
            case 3: return 6 * 60 * 20;
            case 4: return 10 * 60 * 20;
            case 5: return 15 * 60 * 20;
        }

        return 0;
    }

    public static void onTick() {
        ArrayList<Pair<Integer, ChunkPos>> toRemove = new ArrayList<>();

        for (Map.Entry<Integer, HashMap<ChunkPos, InvasionInstance>> entry : INVASIONS.entrySet()) {
            for (Map.Entry<ChunkPos, InvasionInstance> dimensionEntry : entry.getValue().entrySet()) {
                InvasionInstance updatedInvasion = dimensionEntry.getValue().onTick(entry.getKey(), dimensionEntry.getKey());

                if (updatedInvasion == null) {
                    toRemove.add(Pair.of(entry.getKey(), dimensionEntry.getKey()));
                    continue;
                }

                if (dimensionEntry.getValue().isDifferent(updatedInvasion)) {
                    dimensionEntry.setValue(updatedInvasion);
                    currentTickUpdates.setInvasion(entry.getKey(), dimensionEntry.getKey(), updatedInvasion);
                }
            }
        }

        toRemove.forEach(pair -> unInvade(pair.first(), pair.second()));

        if (!currentTickUpdates.isEmpty()) {
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
                } catch (IOException exception) {
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
                } catch (NumberFormatException ignored) {
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
                    } catch (Throwable ignored) {
                        continue;
                    }

                    setInvasion(dimension, pos, invasion);
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
            if (file.exists()) {
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
