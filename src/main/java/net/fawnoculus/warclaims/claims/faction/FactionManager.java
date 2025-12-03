package net.fawnoculus.warclaims.claims.faction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.fawnoculus.warclaims.networking.messages.FactionSyncMessage;
import net.fawnoculus.warclaims.utils.JsonUtil;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FactionManager {
    private static final HashMap<UUID, FactionInstance> FACTIONS = new HashMap<>();
    private static final HashMap<String, UUID> FACTION_BY_NAME = new HashMap<>();
    private static final HashMap<UUID, UUID> SELECTED_FACTION = new HashMap<>();
    private static FactionSyncMessage currentTickUpdates = new FactionSyncMessage();

    public static UUID makeFaction(EntityPlayerMP owner, String name) {
        UUID uuid = UUID.randomUUID();
        setFaction(uuid, new FactionInstance(owner, name));
        return uuid;
    }

    public static void setFaction(UUID factionId, FactionInstance team) {
        FACTIONS.put(factionId, team);
        FACTION_BY_NAME.put(team.name, factionId);
        currentTickUpdates.setTeam(factionId, team);
    }

    public static void removeFaction(UUID factionId) {
        FactionInstance team = FACTIONS.get(factionId);
        if (team == null) {
            return;
        }

        FACTIONS.remove(factionId);
        FACTION_BY_NAME.remove(team.name);
        currentTickUpdates.setTeam(factionId, null);
        ClaimManager.removeClaimIf(claim -> factionId.equals(claim.factionId));
    }

    public static @Nullable FactionInstance getFaction(UUID factionId) {
        return FACTIONS.get(factionId);
    }

    public static Set<String> getFactionNames() {
        return FACTION_BY_NAME.keySet();
    }

    public static UUID getFactionFromName(String name) {
        return FACTION_BY_NAME.get(name);
    }

    public static void setSelectedFaction(EntityPlayerMP player, UUID factionId) {
        SELECTED_FACTION.put(player.getGameProfile().getId(), factionId);
    }

    public static void setSelectedFaction(UUID playerId, UUID factionId) {
        SELECTED_FACTION.put(playerId, factionId);
    }

    public static @Nullable UUID getSelectedFaction(EntityPlayerMP player) {
        return SELECTED_FACTION.get(player.getGameProfile().getId());
    }

    public static void onTick() {
        if(!currentTickUpdates.isEmpty()) {
            WarClaimsNetworking.WRAPPER.sendToAll(currentTickUpdates);
            currentTickUpdates = new FactionSyncMessage();
        }
    }

    public static void clear() {
        FACTIONS.clear();
        FACTION_BY_NAME.clear();
        SELECTED_FACTION.clear();
        currentTickUpdates = new FactionSyncMessage();
    }

    public static void onPlayerJoin(EntityPlayerMP playerMP) {
        WarClaimsNetworking.WRAPPER.sendTo(new FactionSyncMessage(FACTIONS), playerMP);
    }

    public static void loadFromFile(String worldPath) {
        // Load Factions
        FACTIONS.clear();
        FACTION_BY_NAME.clear();
        File file = new File(worldPath  + File.separator + "data" + File.separator + "warclaims" + File.separator + "factions.json");
        if (!file.exists()) {
            return;
        }

        try (Reader reader = new FileReader(file)) {
            JsonObject json = JsonUtil.fromReader(reader, JsonObject.class);

            if (!WarClaims.isCorrectFileVersion(json.get(WarClaims.FILE_VERSION_NAME))) {
                WarClaims.LOGGER.warn("Trying to load Faction of different or unknown File Version, things may not go well!");
                WarClaims.LOGGER.info("Trying Making backup of Faction, just in case");
                try {
                    Files.copy(file.toPath(), file.toPath().resolveSibling("factions.json.bak"), StandardCopyOption.REPLACE_EXISTING);
                }catch (IOException exception) {
                    WarClaims.LOGGER.warn("Failed to make Faction backup", exception);
                    WarClaims.LOGGER.info("(Load Factions) We are just gonna continue and pretend everything is fine, surely nothing bad will happen right?");
                }
            }

            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                UUID factionId;
                FactionInstance faction;
                try {
                    factionId = UUID.fromString(entry.getKey());
                    faction = FactionInstance.fromJson(entry.getValue().getAsJsonObject());
                }catch (Throwable ignored) {
                    continue;
                }

                setFaction(factionId, faction);
            }
        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to load Factions: {}", e.getMessage());
            return;
        }

        // Load Faction Selections
        SELECTED_FACTION.clear();
        File selectedFactions = new File(worldPath  + File.separator + "data" + File.separator + "warclaims" + File.separator + "selected-factions.json");
        if (!selectedFactions.exists()) {
            return;
        }

        try (Reader reader = new FileReader(selectedFactions)) {
            JsonObject json = JsonUtil.fromReader(reader, JsonObject.class);

            if (!WarClaims.isCorrectFileVersion(json.get(WarClaims.FILE_VERSION_NAME))) {
                WarClaims.LOGGER.warn("Trying to load Selected-Faction of different or unknown File Version, things may not go well!");
                WarClaims.LOGGER.info("Trying Making backup of Selected-Faction, just in case");
                try {
                    Files.copy(file.toPath(), file.toPath().resolveSibling("factions.json.bak"), StandardCopyOption.REPLACE_EXISTING);
                }catch (IOException exception) {
                    WarClaims.LOGGER.warn("Failed to make Selected-Faction backup", exception);
                    WarClaims.LOGGER.info("(Load Selected-Factions) We are just gonna continue and pretend everything is fine, surely nothing bad will happen right?");
                }
            }

            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                    continue;
                }

                UUID playerId;
                UUID factionId;
                try {
                    playerId = UUID.fromString(entry.getKey());
                    factionId = UUID.fromString(entry.getValue().getAsString());
                }catch (Throwable ignored) {
                    continue;
                }

                setSelectedFaction(playerId, factionId);
            }
        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to load Selected Factions: {}", e.getMessage());
        }
    }

    public static void saveToFile(String worldPath) {
        // Save Factions
        File file = new File(worldPath  + File.separator + "data" + File.separator + "warclaims" + File.separator + "factions.json");
        try {
            if (!file.getParentFile().exists()) {
                boolean ignored = file.getParentFile().mkdirs();
            }
            if(file.exists()) {
                boolean ignored = file.delete();
            }
            boolean ignored = file.createNewFile();
        } catch (IOException e) {
            WarClaims.LOGGER.warn("Failed to create new Factions file: {}", e.getMessage());
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            JsonObject json = new JsonObject();
            json.add(WarClaims.FILE_VERSION_NAME, new JsonPrimitive(WarClaims.FILE_VERSION));

            for (UUID factionID : FACTIONS.keySet()) {
                json.add(factionID.toString(), FactionInstance.toJson(FACTIONS.get(factionID)));
            }

            JsonUtil.toWriter(writer, json);
        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to save Factions: {}", e.getMessage());
            return;
        }

        // Save Faction Selections
        File selectedFactions = new File(worldPath  + File.separator + "data" + File.separator + "warclaims" + File.separator + "selected-factions.json");
        try {
            if (!selectedFactions.getParentFile().exists()) {
                boolean ignored = selectedFactions.getParentFile().mkdirs();
            }
            if(selectedFactions.exists()) {
                boolean ignored = selectedFactions.delete();
            }
            boolean ignored = selectedFactions.createNewFile();
        } catch (IOException e) {
            WarClaims.LOGGER.warn("Failed to create new Selected Factions file: {}", e.getMessage());
            return;
        }

        try (FileWriter writer = new FileWriter(selectedFactions)) {
            JsonObject json = new JsonObject();
            json.add(WarClaims.FILE_VERSION_NAME, new JsonPrimitive(WarClaims.FILE_VERSION));

            for (UUID playerID : SELECTED_FACTION.keySet()) {
                UUID selectedTeam = SELECTED_FACTION.get(playerID);
                json.add(playerID.toString(), new JsonPrimitive(selectedTeam.toString()));
            }

            JsonUtil.toWriter(writer, json);
        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to save Selected Factions: {}", e.getMessage());
        }
    }
}
