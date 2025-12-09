package net.fawnoculus.warclaims.claims.invade;

import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.ClaimKey;
import net.fawnoculus.warclaims.claims.faction.ClientFactionManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.networking.messages.InvasionSyncMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.ChunkPos;
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
import java.util.Map;

public class ClientInvasionManager {
    private static final Map<InvasionKey, ClientInvasionInstance> INVASIONS = new HashMap<>();
    private static final Map<ClaimKey, InvasionKey> INVASIONS_BY_POS = new HashMap<>();

    public static @Nullable InvasionKey fromPos(int dimension, int chunkX, int chunkZ) {
        return INVASIONS_BY_POS.get(new ClaimKey(dimension, chunkX, chunkZ));
    }

    public static @Nullable ClientInvasionInstance getInvasion(InvasionKey key) {
        return INVASIONS.get(key);
    }

    public static @Nullable ClientInvasionInstance get(int dimension, int chunkX, int chunkZ) {
        InvasionKey invasionKey = fromPos(dimension, chunkX, chunkZ);
        if (invasionKey == null) {
            return null;
        }
        return getInvasion(invasionKey);
    }

    private static void setInvasion(InvasionKey key, ClientInvasionInstance invasion) {
        INVASIONS.put(key, invasion);
    }

    private static void setInvasionPos(ClaimKey claimKey, InvasionKey invasionKey) {
        INVASIONS_BY_POS.put(claimKey, invasionKey);
    }

    private static void removeInvasion(InvasionKey key) {
        INVASIONS.remove(key);
    }

    private static void removeInvasionPos(ClaimKey claimKey) {
        INVASIONS_BY_POS.remove(claimKey);
    }

    public static @Nullable FactionInstance getInvadingTeam(int dimension, int chunkX, int chunkZ) {
        InvasionKey key = fromPos(dimension, chunkX, chunkZ);
        if (key == null) {
            return null;
        }

        return ClientFactionManager.get(key.attackingFaction);
    }

    public static void clear() {
        INVASIONS.clear();
    }

    public static void update(InvasionSyncMessage message) {
        int currentDimension = Integer.MIN_VALUE;

        try {
            currentDimension = Minecraft.getMinecraft().world.provider.getDimension();
        } catch (Throwable ignored) {
        }

        for (Map.Entry<InvasionKey, ClientInvasionInstance> entry : message.getClientInvasions().entrySet()) {
            if (entry.getValue() == null) {
                removeInvasion(entry.getKey());
                continue;
            }
            setInvasion(entry.getKey(), entry.getValue());
        }

        HashSet<ChunkPos> regionsToUpdate = new HashSet<>();
        for (Map.Entry<ClaimKey, InvasionKey> entry : message.getInvasionsByPos().entrySet()) {
            if (entry.getValue() == null) {
                removeInvasionPos(entry.getKey());
                continue;
            }

            setInvasionPos(entry.getKey(), entry.getValue());

            if (entry.getKey().dimension == currentDimension) {
                regionsToUpdate.add(chunkToRegion(entry.getKey().pos));
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
