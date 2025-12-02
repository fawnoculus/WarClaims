package net.fawnoculus.warclaims.claims;

import com.mojang.realmsclient.util.Pair;
import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.fawnoculus.warclaims.networking.messages.ClaimSyncMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Predicate;

public class ClaimManager {
    private static final HashMap<Integer, HashMap<ChunkPos, ClaimInstance>> CLAIMS = new HashMap<>();
    public static ClaimSyncMessage currentTickUpdates = new ClaimSyncMessage();

    public static @Nullable ClaimInstance getClaim(int dimension, int chunkX, int chunkZ) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS.get(dimension);
        if(dimensionClaims == null) {
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
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS.get(dimension);
        if(dimensionClaims == null) {
            dimensionClaims = new HashMap<>();
        }
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);

        dimensionClaims.put(pos, claim);
        currentTickUpdates.setClaim(dimension, chunkX, chunkZ, claim);

        CLAIMS.put(dimension, dimensionClaims);
    }

    public static void unclaim(int dimension, int chunkX, int chunkZ) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS.get(dimension);
        if(dimensionClaims == null) {
            dimensionClaims = new HashMap<>();
        }

        dimensionClaims.remove(new ChunkPos(chunkX, chunkZ));
        currentTickUpdates.setClaim(dimension, chunkX, chunkZ, null);

        CLAIMS.put(dimension, dimensionClaims);
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

        toRemove.forEach(pair -> unclaim(pair.first(), pair.second().x, pair.second().z));
    }

    public static void onTick() {
        if(!currentTickUpdates.isEmpty()) {
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
        File file = new File(worldPath + File.separator + "data" + File.separator + "warclaims" + File.separator + "claims.bin");
        if (!file.exists()) {
            return;
        }

        try (Reader reader = new FileReader(file)) {
            char[] fileVersion = new char[WarClaims.FILE_VERSION.length];
            int ignored = reader.read(fileVersion);
            if (!Arrays.equals(fileVersion, WarClaims.FILE_VERSION)) {
                throw new RuntimeException("Incorrect File Version");
            }

            int claimsSize = reader.read();
            for (int i = 0; i < claimsSize; i++) {
                int dimensionId = reader.read();

                int dimensionClaimSize = reader.read();
                for (int j = 0; j < dimensionClaimSize; j++) {
                    int chunkX = reader.read();
                    int chunkZ = reader.read();

                    ClaimInstance claim = ClaimInstance.fromReader(reader);
                    setClaim(dimensionId, chunkX, chunkZ, claim);
                }
            }

        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to load Claims: {}", e.getMessage());
        }
    }

    public static void saveToFile(String worldPath) {
        File file = new File(worldPath + File.separator + "data" + File.separator + "warclaims" + File.separator + "claims.bin");
        try {
            if (!file.getParentFile().exists()) {
                boolean ignored = file.getParentFile().mkdirs();
            }
            if(file.exists()) {
                boolean ignored = file.delete();
            }
            boolean ignored = file.createNewFile();
        } catch (IOException e) {
            WarClaims.LOGGER.warn("Failed to create new Claim file: {}", e.getMessage());
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(WarClaims.FILE_VERSION);
            writer.write(CLAIMS.size());
            for (Integer dimension : CLAIMS.keySet()) {
                writer.write(dimension);

                HashMap<ChunkPos, ClaimInstance> DimensionClaims = CLAIMS.get(dimension);
                writer.write(DimensionClaims.size());
                for (ChunkPos pos : DimensionClaims.keySet()) {
                    writer.write(pos.x);
                    writer.write(pos.z);

                    ClaimInstance claim = DimensionClaims.get(pos);
                    ClaimInstance.toWriter(writer, claim);
                }
            }

        } catch (Throwable e) {
            WarClaims.LOGGER.warn("Failed to save Claims: {}", e.getMessage());
        }
    }
}
