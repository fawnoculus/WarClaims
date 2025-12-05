package net.fawnoculus.warclaims.claims;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.realmsclient.util.Pair;
import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.fawnoculus.warclaims.networking.messages.ClaimSyncMessage;
import net.fawnoculus.warclaims.utils.JsonUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

public class ClaimManager {
    private static final HashMap<Integer, HashMap<ChunkPos, ClaimInstance>> CLAIMS = new HashMap<>();
    public static ClaimSyncMessage currentTickUpdates = new ClaimSyncMessage();

    public static @Nullable ClaimInstance getClaim(int dimension, int chunkX, int chunkZ) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS.get(dimension);
        if (dimensionClaims == null) {
            return null;
        }
        return dimensionClaims.get(new ChunkPos(chunkX, chunkZ));
    }

    public static @Nullable FactionInstance getFaction(int dimension, int chunkX, int chunkZ) {
        ClaimInstance claim = getClaim(dimension, chunkX, chunkZ);
        if (claim == null) {
            return null;
        }

        return FactionManager.getFaction(claim.factionId);
    }

    public static void claim(int dimension, int chunkX, int chunkZ, UUID factionId, int level) {
        setClaim(dimension, chunkX, chunkZ, new ClaimInstance(factionId, level));
    }

    public static void setClaim(int dimension, int chunkX, int chunkZ, ClaimInstance claim) {
        setClaim(dimension, new ChunkPos(chunkX, chunkZ), claim);
    }

    public static void setClaim(int dimension, ChunkPos pos, ClaimInstance claim) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS
                .computeIfAbsent(dimension, ignored -> new HashMap<>());
        dimensionClaims.put(pos, claim);
        currentTickUpdates.setClaim(dimension, pos, claim);
    }

    public static void unclaim(int dimension, int chunkX, int chunkZ) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS
                .computeIfAbsent(dimension, ignored -> new HashMap<>());

        dimensionClaims.remove(new ChunkPos(chunkX, chunkZ));
        currentTickUpdates.setClaim(dimension, chunkX, chunkZ, null);
    }

    public static void unclaim(int dimension, ChunkPos pos) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS
                .computeIfAbsent(dimension, ignored -> new HashMap<>());

        dimensionClaims.remove(pos);
        currentTickUpdates.setClaim(dimension, pos, null);
    }

    public static void removeClaimIf(Predicate<ClaimInstance> predicate) {
        ArrayList<Pair<Integer, ChunkPos>> toRemove = new ArrayList<>();

        for (Integer dimension : CLAIMS.keySet()) {
            HashMap<ChunkPos, ClaimInstance> DimensionClaims = CLAIMS.get(dimension);

            for (ChunkPos pos : DimensionClaims.keySet()) {
                ClaimInstance claim = DimensionClaims.get(pos);

                if (predicate.test(claim)) {
                    toRemove.add(Pair.of(dimension, pos));
                }
            }
        }

        for (Pair<Integer, ChunkPos> pair : toRemove) {
            unclaim(pair.first(), pair.second());
        }
    }

    /**
     * Takes the Items required to claim a Chunk at the specified level out of the players inventory
     * @return true if the player had enough Resources, false if they didn't
     */
    public static boolean takeRequiredItems(EntityPlayer player, int level) {
        Pair<Item, Integer> pair = getRequiredItem(level);
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.getItem() != pair.first()) {
                continue;
            }
            if (stack.getCount() >= pair.second()) {
                stack.shrink(pair.second());
                stack.isEmpty();
                return true;
            }

        }

        return false;
    }

    public static Pair<Item, Integer> getRequiredItem(int level) {
        switch (level) {
            case 0: return Pair.of(Items.IRON_INGOT, 1);
            case 1: return Pair.of(Items.IRON_INGOT, 4);
            case 2: return Pair.of(Items.GOLD_INGOT, 1);
            case 3: return Pair.of(Items.GOLD_INGOT, 4);
            case 4: return Pair.of(Items.DIAMOND, 4);
            case 5: return Pair.of(Items.DIAMOND, 32);
            default: return Pair.of(Items.AIR, 0);
        }
    }

    public static void onTick() {
        if (!currentTickUpdates.isEmpty()) {
            WarClaimsNetworking.WRAPPER.sendToAll(currentTickUpdates);
            currentTickUpdates = new ClaimSyncMessage();
        }
    }

    public static void clear() {
        CLAIMS.clear();
        currentTickUpdates = new ClaimSyncMessage();
    }

    public static void onPlayerJoin(EntityPlayerMP playerMP) {
        WarClaimsNetworking.WRAPPER.sendTo(new ClaimSyncMessage(CLAIMS), playerMP);
    }

    public static void loadFromFile(String worldPath) {
        CLAIMS.clear();
        File file = new File(worldPath + File.separator + "data" + File.separator + "warclaims" + File.separator + "claims.json");
        if (!file.exists()) {
            return;
        }

        try (Reader reader = new FileReader(file)) {
            JsonObject json = JsonUtil.fromReader(reader, JsonObject.class);

            if (!WarClaims.isCorrectFileVersion(json.get(WarClaims.FILE_VERSION_NAME))) {
                WarClaims.LOGGER.warn("Trying to load Claims of different or unknown File Version, things may not go well!");
                WarClaims.LOGGER.info("Trying Making backup of Claims, just in case");
                try {
                    Files.copy(file.toPath(), file.toPath().resolveSibling("claims.json.bak"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException exception) {
                    WarClaims.LOGGER.warn("Failed to make Claims backup", exception);
                    WarClaims.LOGGER.info("(Load Claims) We are just gonna continue and pretend everything is fine, surely nothing bad will happen right?");
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
                    ClaimInstance claim;
                    try {
                        pos = JsonUtil.toChunkPos(dimensionEntry.getKey());
                        claim = ClaimInstance.fromJson(dimensionEntry.getValue().getAsJsonObject());
                    } catch (Throwable ignored) {
                        continue;
                    }

                    setClaim(dimension, pos, claim);
                }
            }

        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to load Claims: {}", e.getMessage());
        }
    }

    public static void saveToFile(String worldPath) {
        File file = new File(worldPath + File.separator + "data" + File.separator + "warclaims" + File.separator + "claims.json");
        try {
            if (!file.getParentFile().exists()) {
                boolean ignored = file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                boolean ignored = file.delete();
            }
            boolean ignored = file.createNewFile();
        } catch (IOException e) {
            WarClaims.LOGGER.warn("Failed to create new Claim file: {}", e.getMessage());
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            JsonObject json = new JsonObject();
            json.add(WarClaims.FILE_VERSION_NAME, new JsonPrimitive(WarClaims.FILE_VERSION));

            for (Integer dimension : CLAIMS.keySet()) {
                HashMap<ChunkPos, ClaimInstance> DimensionClaims = CLAIMS.get(dimension);
                JsonObject dimensionJson = new JsonObject();

                for (ChunkPos pos : DimensionClaims.keySet()) {
                    ClaimInstance claim = DimensionClaims.get(pos);
                    dimensionJson.add(JsonUtil.fromChunkPos(pos), ClaimInstance.toJson(claim));
                }

                json.add(dimension.toString(), dimensionJson);
            }

            JsonUtil.toWriter(writer, json);
        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to save Claims: {}", e.getMessage());
        }
    }
}
