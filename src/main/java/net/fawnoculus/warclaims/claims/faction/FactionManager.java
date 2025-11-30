package net.fawnoculus.warclaims.claims.faction;

import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.fawnoculus.warclaims.networking.messages.FactionSyncMessage;
import net.fawnoculus.warclaims.utils.FileUtil;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;
import java.io.*;
import java.util.HashMap;
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
        currentTickUpdates.setTeam(factionId, team);
        FACTION_BY_NAME.put(team.name, factionId);
    }

    public static void removeFaction(UUID factionId) {
        FactionInstance team = FACTIONS.get(factionId);
        if (team != null) {
            FACTION_BY_NAME.remove(team.name);
        }
        FACTIONS.remove(factionId);

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
        File file = new File(worldPath  + File.separator + "data" + File.separator + "warclaims" + File.separator + "factions.bin");
        if (!file.exists()) {
            return;
        }

        try (Reader reader = new FileReader(file)) {
            int factionsSize = reader.read();
            for (int i = 0; i < factionsSize; i++) {
                UUID factionId = FileUtil.readUUID(reader);
                FactionInstance faction = FactionInstance.fromReader(reader);

                setFaction(factionId, faction);
            }

        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to load Factions: {}", e.getMessage());
            return;
        }

        // Load Faction Selections
        SELECTED_FACTION.clear();
        File selectedFactions = new File(worldPath  + File.separator + "data" + File.separator + "warclaims" + File.separator + "selected-factions.bin");
        if (!selectedFactions.exists()) {
            return;
        }

        try (Reader reader = new FileReader(selectedFactions)) {
            int selectedFactionSize = reader.read();
            for (int i = 0; i < selectedFactionSize; i++) {
                UUID playerID = FileUtil.readUUID(reader);
                UUID factionId = FileUtil.readUUID(reader);

                SELECTED_FACTION.put(playerID, factionId);
            }

        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to load Selected Factions: {}", e.getMessage());
        }
    }

    public static void saveToFile(String worldPath) {
        // Save Factions
        File file = new File(worldPath  + File.separator + "data" + File.separator + "warclaims" + File.separator + "factions.bin");
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
            writer.write(FACTIONS.size());
            for (UUID factionId : FACTIONS.keySet()) {
                FileUtil.writeUUID(writer, factionId);

                FactionInstance faction = FACTIONS.get(factionId);
                FactionInstance.toWriter(writer, faction);
            }

        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to save Factions: {}", e.getMessage());
            return;
        }

        // Save Faction Selections
        File selectedFactions = new File(worldPath  + File.separator + "data" + File.separator + "warclaims" + File.separator + "selected-factions.bin");
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
            writer.write(SELECTED_FACTION.size());
            for (UUID playerID : SELECTED_FACTION.keySet()) {
                UUID selectedTeam = SELECTED_FACTION.get(playerID);
                FileUtil.writeUUID(writer, playerID);
                FileUtil.writeUUID(writer, selectedTeam);
            }

        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to save Selected Factions: {}", e.getMessage());
        }
    }
}
