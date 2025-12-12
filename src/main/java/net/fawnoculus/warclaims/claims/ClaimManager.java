package net.fawnoculus.warclaims.claims;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.fawnoculus.warclaims.utils.Pair;
import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.claims.invade.InvasionManager;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.fawnoculus.warclaims.networking.messages.ClaimSyncMessage;
import net.fawnoculus.warclaims.utils.JsonUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class ClaimManager {
    private static final Map<ClaimKey, ClaimInstance> CLAIMS = new HashMap<>();
    private static final Map<ClaimKey, ClaimInstance> CLAIMS_TICK_CHANGES = new HashMap<>();

    public static @Nullable ClaimInstance getClaim(int dimension, int chunkX, int chunkZ) {
        return CLAIMS.get(new ClaimKey(dimension, chunkX, chunkZ));
    }

    public static @Nullable ClaimInstance getClaim(ClaimKey claimKey) {
        return CLAIMS.get(claimKey);
    }

    public static @Nullable UUID getFactionId(int dimension, int chunkX, int chunkZ) {
        ClaimInstance claim = getClaim(dimension, chunkX, chunkZ);
        if (claim == null) {
            return null;
        }

        return claim.factionId;
    }

    public static @Nullable FactionInstance getFaction(int dimension, int chunkX, int chunkZ) {
        ClaimInstance claim = getClaim(dimension, chunkX, chunkZ);
        if (claim == null) {
            return null;
        }

        return FactionManager.getFaction(claim.factionId);
    }

    public static void claim(int dimension, int chunkX, int chunkZ, UUID factionId, int level) {
        setClaim(new ClaimKey(dimension, chunkX, chunkZ), new ClaimInstance(factionId, level));
    }

    public static void setClaim(ClaimKey key, ClaimInstance claim) {
        CLAIMS.put(key, claim);
        CLAIMS_TICK_CHANGES.put(key, claim);
    }

    public static void unclaim(int dimension, int chunkX, int chunkZ) {
        removeClaim(new ClaimKey(dimension, chunkX, chunkZ));
    }

    public static void removeClaim(ClaimKey key) {
        InvasionManager.removeInvasionPos(key);
        CLAIMS.remove(key);
        CLAIMS_TICK_CHANGES.put(key, null);
    }

    public static void removeClaimIf(Predicate<ClaimInstance> predicate) {
        List<ClaimKey> toRemove = new ArrayList<>();

        for (Map.Entry<ClaimKey, ClaimInstance> entry : CLAIMS.entrySet()) {
            if (predicate.test(entry.getValue())) {
                toRemove.add(entry.getKey());
            }
        }

        for (ClaimKey key : toRemove) {
            removeClaim(key);
        }
    }

    public static void transformClaims(Function<ClaimInstance, ClaimInstance> claimTransformer) {
        for (Map.Entry<ClaimKey, ClaimInstance> entry : CLAIMS.entrySet()) {
            ClaimInstance previousClaim = entry.getValue();
            ClaimInstance newClaim = claimTransformer.apply(previousClaim);
            if (previousClaim != newClaim) {
                setClaim(entry.getKey(), newClaim);
            }
        }
    }

    /**
     * Takes the Items required to claim a Chunk at the specified level out of the players inventory
     *
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
                return true;
            }

        }

        return false;
    }

    public static Pair<Item, Integer> getRequiredItem(int level) {
        switch (level) {
            case 0:
                return Pair.of(Items.IRON_INGOT, 1);
            case 1:
                return Pair.of(Items.IRON_INGOT, 4);
            case 2:
                return Pair.of(Items.GOLD_INGOT, 1);
            case 3:
                return Pair.of(Items.GOLD_INGOT, 4);
            case 4:
                return Pair.of(Items.DIAMOND, 4);
            case 5:
                return Pair.of(Items.DIAMOND, 32);
            default:
                return Pair.of(Items.AIR, 0);
        }
    }

    public static void onTick() {
        if (!CLAIMS_TICK_CHANGES.isEmpty()) {
            WarClaimsNetworking.WRAPPER.sendToAll(new ClaimSyncMessage(CLAIMS_TICK_CHANGES));
            CLAIMS_TICK_CHANGES.clear();
        }
    }

    public static void clear() {
        CLAIMS.clear();
        CLAIMS_TICK_CHANGES.clear();
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

        try (FileReader reader = new FileReader(file)) {
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

                ClaimKey key;
                try {
                    key = ClaimKey.fromString(entry.getKey());
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                    continue;
                }

                ClaimInstance claim;
                try {
                    claim = ClaimInstance.fromJson(entry.getValue().getAsJsonObject());
                } catch (RuntimeException ignored) {
                    continue;
                }
                setClaim(key, claim);
            }

        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to load Claims: {}", e.getMessage());
        }
    }

    public static void saveToFile(String worldPath) {
        if (CLAIMS.isEmpty()) {
            return;
        }

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

            for (Map.Entry<ClaimKey, ClaimInstance> entry : CLAIMS.entrySet()) {
                json.add(entry.getKey().toString(), entry.getValue().toJson());
            }

            JsonUtil.toWriter(writer, json);
        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to save Claims: {}", e.getMessage());
        }
    }
}
