package net.fawnoculus.warclaims.claims.invade;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.fawnoculus.warclaims.claims.ClaimKey;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.fawnoculus.warclaims.networking.messages.InvasionSyncMessage;
import net.fawnoculus.warclaims.utils.JsonUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;

public class InvasionManager {
    private static final Map<InvasionKey, InvasionInstance> INVASIONS = new HashMap<>();
    private static final Map<ClaimKey, InvasionKey> INVASIONS_BY_POS = new HashMap<>();
    private static final Map<InvasionKey, InvasionInstance> INVASIONS_TICK_CHANGES = new HashMap<>();
    private static final Map<ClaimKey, InvasionKey> INVASIONS_BY_POS_TICK_CHANGES = new HashMap<>();

    public static @Nullable InvasionKey fromPos(int dimension, int chunkX, int chunkZ) {
        return fromPos(new ClaimKey(dimension, chunkX, chunkZ));
    }

    public static @Nullable InvasionKey fromPos(ClaimKey claimKey) {
        return INVASIONS_BY_POS.get(claimKey);
    }

    public static @Nullable InvasionInstance getInvasion(InvasionKey key) {
        return INVASIONS.get(key);
    }

    public static void addInvasion(int dimension, int chunkX, int chunkZ, ClaimInstance claim, UUID attackingFaction) {
        addInvasion(new ClaimKey(dimension, chunkX, chunkZ), claim, attackingFaction);
    }

    public static void addInvasion(ClaimKey claimKey, ClaimInstance claim, UUID attackingFaction) {
        InvasionKey key = new InvasionKey(attackingFaction, claim.factionId);
        setInvasionPos(claimKey, key);

        InvasionInstance invasion = getInvasion(key);
        if (invasion == null) {
            invasion = new InvasionInstance();
            setInvasion(key, invasion);
        }

        invasion.addChunk(claimKey, claim.level);
    }

    public static void setInvasion(InvasionKey key, InvasionInstance invasion) {
        INVASIONS.put(key, invasion);
        INVASIONS_TICK_CHANGES.put(key, invasion);
    }

    public static void removeInvasion(InvasionKey key) {
        INVASIONS.remove(key);
        INVASIONS_TICK_CHANGES.put(key, null);

        removeInvasionPosIf(entry -> key.equals(entry.getValue()));
    }

    public static void setInvasionPos(ClaimKey claimKey, InvasionKey invasionKey) {
        INVASIONS_BY_POS.put(claimKey, invasionKey);
        INVASIONS_BY_POS_TICK_CHANGES.put(claimKey, invasionKey);
    }

    public static void removeInvasionPos(ClaimKey key) {
        updateInvasionPosRemoved(key);
        INVASIONS_BY_POS.remove(key);
        INVASIONS_BY_POS_TICK_CHANGES.put(key, null);
    }

    public static void updateInvasionPosRemoved(ClaimKey key) {
        InvasionKey invasionKey = INVASIONS_BY_POS.get(key);
        if (invasionKey == null) {
            return;
        }

        InvasionInstance invasion = INVASIONS.get(invasionKey);
        if (invasion == null) {
            return;
        }

        invasion.removeChunk(key);
        if (invasion.isEmpty()) {
            INVASIONS.remove(invasionKey);
            INVASIONS_TICK_CHANGES.put(invasionKey, null);
        }
    }

    public static void removeInvasionIf(Predicate<Map.Entry<InvasionKey, InvasionInstance>> predicate) {
        ArrayList<InvasionKey> toRemove = new ArrayList<>();

        for (Map.Entry<InvasionKey, InvasionInstance> entry : INVASIONS.entrySet()) {
            if (predicate.test(entry)) {
                toRemove.add(entry.getKey());
            }
        }

        for (InvasionKey key : toRemove) {
            removeInvasion(key);
        }
    }

    public static void removeInvasionPosIf(Predicate<Map.Entry<ClaimKey, InvasionKey>> predicate) {
        ArrayList<ClaimKey> toRemove = new ArrayList<>();

        for (Map.Entry<ClaimKey, InvasionKey> entry : INVASIONS_BY_POS.entrySet()) {
            if (predicate.test(entry)) {
                toRemove.add(entry.getKey());
            }
        }

        for (ClaimKey key : toRemove) {
            removeInvasionPos(key);
        }
    }

    /**
     * Takes the Items required to invade a Chunk out of the players inventory
     *
     * @return true if the player had enough Resources, false if they didn't
     */
    public static boolean takeRequiredItems(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.getItem() != Items.GOLD_INGOT) {
                continue;
            }
            if (stack.getCount() >= 1) {
                stack.shrink(1);
                return true;
            }
        }

        return false;
    }

    public static void onTick(MinecraftServer server) {
        List<InvasionKey> toRemove = new ArrayList<>();

        for (Map.Entry<InvasionKey, InvasionInstance> entry : INVASIONS.entrySet()) {
            boolean shouldRemove = entry.getValue().onTick(server, entry.getKey());

            if (shouldRemove) {
                toRemove.add(entry.getKey());
                continue;
            }

            if (entry.getValue().requiresClientSync) {
                INVASIONS_TICK_CHANGES.put(entry.getKey(), entry.getValue());
                entry.getValue().requiresClientSync = false;
            }
        }

        for (InvasionKey invasionKey : toRemove) {
            removeInvasion(invasionKey);
        }

        if (!INVASIONS_TICK_CHANGES.isEmpty() || !INVASIONS_BY_POS_TICK_CHANGES.isEmpty()) {
            for (Map.Entry<InvasionKey, InvasionInstance> entry : INVASIONS_TICK_CHANGES.entrySet()) {
                if (entry.getValue() != null && entry.getValue().ticksSinceCreation != 1) {
                    continue;
                }

                FactionInstance attackingFaction = FactionManager.getFaction(entry.getKey().attackingFaction);
                FactionInstance defendingFaction = FactionManager.getFaction(entry.getKey().defendingFaction);
                if (attackingFaction == null || defendingFaction == null) {
                    continue;
                }

                sendMessages(server, defendingFaction, attackingFaction);
            }

            WarClaimsNetworking.WRAPPER.sendToAll(new InvasionSyncMessage(INVASIONS_TICK_CHANGES, INVASIONS_BY_POS_TICK_CHANGES));
        }
    }

    private static void sendMessages(MinecraftServer server, FactionInstance defendingFaction, FactionInstance attackingFaction) {
        EntityPlayerMP owner = server.getPlayerList().getPlayerByUUID(defendingFaction.owner);
        if (owner != null) {
            sendMessage(owner, defendingFaction, attackingFaction);
        }

        for (UUID officerUuid : defendingFaction.officers) {
            EntityPlayerMP officer = server.getPlayerList().getPlayerByUUID(officerUuid);
            if (officer != null) {
                sendMessage(officer, defendingFaction, attackingFaction);
            }
        }

        for (UUID memberUuid : defendingFaction.members) {
            EntityPlayerMP member = server.getPlayerList().getPlayerByUUID(memberUuid);
            if (member != null) {
                sendMessage(member, defendingFaction, attackingFaction);
            }
        }
    }

    private static void sendMessage(EntityPlayerMP playerMP, FactionInstance defendingFaction, FactionInstance attackingFaction) {
        playerMP.sendMessage(new TextComponentString(String.format(
                "Faction '%1$s' you are a member of is being invaded by '%2$s'!",
                defendingFaction.name, attackingFaction.name
        )));
    }

    public static void clear() {
        INVASIONS.clear();
        INVASIONS_TICK_CHANGES.clear();
        INVASIONS_BY_POS_TICK_CHANGES.clear();
    }

    public static void onPlayerJoin(EntityPlayerMP playerMP) {
        WarClaimsNetworking.WRAPPER.sendTo(new InvasionSyncMessage(INVASIONS, INVASIONS_BY_POS), playerMP);

        for (InvasionKey key : INVASIONS.keySet()) {
            FactionInstance attackingFaction = FactionManager.getFaction(key.attackingFaction);
            FactionInstance defendingFaction = FactionManager.getFaction(key.defendingFaction);
            if (attackingFaction != null && defendingFaction != null && defendingFaction.isMember(playerMP)) {
                sendMessage(playerMP, defendingFaction, attackingFaction);
            }
        }
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

            try {
                for (Map.Entry<String, JsonElement> entry : json.get("invasions").getAsJsonObject().entrySet()) {
                    try {
                        setInvasion(InvasionKey.fromString(entry.getKey()), InvasionInstance.fromJson(entry.getValue().getAsJsonObject()));
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }

            try {
                for (Map.Entry<String, JsonElement> entry : json.get("chunks").getAsJsonObject().entrySet()) {
                    try {
                        setInvasionPos(ClaimKey.fromString(entry.getKey()), InvasionKey.fromString(entry.getValue().getAsString()));
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
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

            JsonObject invasions = new JsonObject();
            for (Map.Entry<InvasionKey, InvasionInstance> entry : INVASIONS.entrySet()) {
                invasions.add(entry.getKey().toString(), entry.getValue().toJson());
            }
            json.add("invasions", invasions);

            JsonObject chunks = new JsonObject();
            for (Map.Entry<ClaimKey, InvasionKey> entry : INVASIONS_BY_POS.entrySet()) {
                chunks.addProperty(entry.getKey().toString(), entry.getValue().toString());
            }
            json.add("chunks", chunks);

            JsonUtil.toWriter(writer, json);
        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to save Invasions: {}", e.getMessage());
        }
    }
}
