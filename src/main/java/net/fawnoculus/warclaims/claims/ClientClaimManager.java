package net.fawnoculus.warclaims.claims;

import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.faction.ClientFactionManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.networking.messages.ClaimSyncMessage;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;

@SideOnly(Side.CLIENT)
public class ClientClaimManager {
    private static final HashMap<Integer, HashMap<ChunkPos, ClaimInstance>> CLAIMS = new HashMap<>();

    public static @Nullable ClaimInstance get(int dimension, int chunkX, int chunkZ) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS.get(dimension);
        if(dimensionClaims == null) {
            return null;
        }
        return dimensionClaims.get(new ChunkPos(chunkX, chunkZ));
    }

    private static void setClaim(int dimension, ChunkPos pos, ClaimInstance claim) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS.get(dimension);
        if(dimensionClaims == null) {
            dimensionClaims = new HashMap<>();
        }

        if (claim == null) {
            dimensionClaims.remove(pos);
        } else {
            dimensionClaims.put(pos, claim);
        }

        // TODO: how tf do we remove Chunks from the Xaero Map Cache sp that the marker applies????

        WarClaims.LOGGER.info("A: {}, {}, {}", dimension, pos, claim);

        CLAIMS.put(dimension, dimensionClaims);
    }

    public static @Nullable FactionInstance getTeam(int dimension, int chunkX, int chunkZ) {
        ClaimInstance claim = get(dimension, chunkX, chunkZ);
        if (claim != null) {
            return ClientFactionManager.get(claim.factionId);
        }
        return null;
    }

    public static void update(ClaimSyncMessage message) {
        for (Integer dimensionId : message.getMap().keySet()) {
            HashMap<ChunkPos, ClaimInstance> dimensionClaims = message.getMap().get(dimensionId);

            for (ChunkPos pos : dimensionClaims.keySet()) {
                setClaim(dimensionId, pos, dimensionClaims.get(pos));
            }
        }
    }

    public static void clear() {
        CLAIMS.clear();
    }
}
