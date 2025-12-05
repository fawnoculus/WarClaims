package net.fawnoculus.warclaims.claims;

import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.faction.ClientFactionManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.networking.messages.ClaimSyncMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.highlight.DimensionHighlighterHandler;
import xaero.common.minimap.write.MinimapWriter;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.region.MapRegion;
import xaero.minimap.XaeroMinimapStandaloneSession;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Predicate;

@SideOnly(Side.CLIENT)
public class ClientClaimManager {
    private static final HashMap<Integer, HashMap<ChunkPos, ClaimInstance>> CLAIMS = new HashMap<>();

    public static @Nullable ClaimInstance get(int dimension, int chunkX, int chunkZ) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS.get(dimension);
        if (dimensionClaims == null) {
            return null;
        }
        return dimensionClaims.get(new ChunkPos(chunkX, chunkZ));
    }

    private static void setClaim(int dimension, ChunkPos pos, ClaimInstance claim) {
        HashMap<ChunkPos, ClaimInstance> dimensionClaims = CLAIMS.get(dimension);
        if (dimensionClaims == null) {
            dimensionClaims = new HashMap<>();
        }

        if (claim == null) {
            dimensionClaims.remove(pos);
        } else {
            dimensionClaims.put(pos, claim);
        }

        CLAIMS.put(dimension, dimensionClaims);
    }

    public static @Nullable FactionInstance getTeam(int dimension, int chunkX, int chunkZ) {
        ClaimInstance claim = get(dimension, chunkX, chunkZ);
        if (claim != null) {
            return ClientFactionManager.get(claim.factionId);
        }
        return null;
    }

    public static void clear() {
        CLAIMS.clear();
    }

    public static void update(ClaimSyncMessage message) {
        int currentDimension = 0;

        try {
            currentDimension = Minecraft.getMinecraft().world.provider.getDimension();
        } catch (Throwable ignored) {
        }

        HashSet<ChunkPos> regionsToUpdate = new HashSet<>();

        for (Integer dimensionId : message.getMap().keySet()) {
            HashMap<ChunkPos, ClaimInstance> dimensionClaims = message.getMap().get(dimensionId);

            for (ChunkPos pos : dimensionClaims.keySet()) {
                setClaim(dimensionId, pos, dimensionClaims.get(pos));

                if (dimensionId == currentDimension) {
                    regionsToUpdate.add(chunkToRegion(pos));
                }
            }
        }

        for (ChunkPos regionPos : regionsToUpdate) {
            updateMapRegions(regionPos);
        }
    }

    public static void updateClaimIf(Predicate<ClaimInstance> predicate) {
        int currentDimension = 0;

        try {
            currentDimension = Minecraft.getMinecraft().world.provider.getDimension();
        } catch (Throwable ignored) {
        }

        HashSet<ChunkPos> regionsToUpdate = new HashSet<>();

        for (Integer dimension : CLAIMS.keySet()) {
            if (dimension != currentDimension) {
                continue;
            }
            HashMap<ChunkPos, ClaimInstance> DimensionClaims = CLAIMS.get(dimension);

            for (ChunkPos pos : DimensionClaims.keySet()) {
                ClaimInstance claim = DimensionClaims.get(pos);

                if (predicate.test(claim)) {
                    regionsToUpdate.add(chunkToRegion(pos));
                }
            }
        }

        for (ChunkPos regionPos : regionsToUpdate) {
            updateMapRegions(regionPos);
        }
    }

    private static ChunkPos chunkToRegion(ChunkPos pos) {
        return new ChunkPos(chunkToRegion(pos.x), chunkToRegion(pos.z));
    }

    private static int chunkToRegion(int chunk) {
        return chunk >> 5;
    }

    private static void updateMapRegions(ChunkPos region) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            try {
                @SuppressWarnings("deprecation")
                XaeroMinimapSession session = XaeroMinimapStandaloneSession.getCurrentSession();
                if (session == null) {
                    return;
                }
                MinimapProcessor processor = session.getMinimapProcessor();
                if (processor == null) {
                    return;
                }
                MinimapWriter writer = processor.getMinimapWriter();
                if (writer == null) {
                    return;
                }
                DimensionHighlighterHandler handler = writer.getDimensionHighlightHandler();
                if (handler == null) {
                    return;
                }
                handler.requestRefresh(region.x, region.z);
            } catch (Throwable exception) {
                WarClaims.LOGGER.warn("Exception occurred while refreshing Xaero's Mini-Map Cache: ", exception);
            }
        });

        Minecraft.getMinecraft().addScheduledTask(() -> {
            try {
                WorldMapSession session = WorldMapSession.getCurrentSession();
                if (session == null) {
                    return;
                }
                MapProcessor processor = session.getMapProcessor();
                if (processor == null) {
                    return;
                }
                MapRegion mapRegion = processor.getLeafMapRegion(Integer.MAX_VALUE, region.x, region.z, false);
                if (mapRegion == null) {
                    return;
                }
                mapRegion.requestRefresh(processor, true);
            } catch (Throwable exception) {
                WarClaims.LOGGER.warn("Exception occurred while refreshing Xaero's World-Map Cache: ", exception);
            }
        });
    }
}
