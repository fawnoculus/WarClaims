package net.fawnoculus.warclaims.claims;

import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.fawnoculus.warclaims.networking.messages.ClaimUpdateMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ClaimManager {
    private static final HashMap<ChunkPos, ClaimInstance> CLAIM_MAP = new HashMap<>();
    private static final List<EntityPlayerMP> PLAYERS_TO_UPDATE = new ArrayList<>();
    private static ClaimUpdateMessage currentTickUpdates = new ClaimUpdateMessage();

    public static @Nullable ClaimInstance fromChunk(ChunkPos chunkPos) {
        return CLAIM_MAP.get(chunkPos);
    }

    public static void claim(ChunkPos chunkPos, UUID teamId, int level) {
        ClaimInstance claimInstance = new ClaimInstance(teamId, chunkPos, level);
        CLAIM_MAP.put(chunkPos, claimInstance);
        currentTickUpdates.addClaim(claimInstance);
    }

    public static void onTick() {
        if(!PLAYERS_TO_UPDATE.isEmpty()) {
            ClaimUpdateMessage initialClaimSync = new ClaimUpdateMessage(CLAIM_MAP);
            for (EntityPlayerMP playerMP : PLAYERS_TO_UPDATE) {
                WarClaimsNetworking.WRAPPER.sendTo(initialClaimSync, playerMP);
            }
            PLAYERS_TO_UPDATE.clear();
        }
        if(!currentTickUpdates.isEmpty()) {
            WarClaimsNetworking.WRAPPER.sendToAll(currentTickUpdates);
            currentTickUpdates = new ClaimUpdateMessage();
        }
    }


    public static void onPlayerJoin(EntityPlayerMP playerMP) {
        WarClaims.LOGGER.info("SENDING TO on the next tick: {}", playerMP.getGameProfile().getName());
        PLAYERS_TO_UPDATE.add(playerMP);
    }

    public static void update(ClaimUpdateMessage message) {
        CLAIM_MAP.putAll(message.getFromChunk());
    }

    public static void clear() {
        CLAIM_MAP.clear();
    }
}
