package net.fawnoculus.warclaims.claims;

import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClaimEventHandler {
    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent breakEvent) {
        int dimension = breakEvent.getWorld().provider.getDimension();
        ChunkPos pos = new ChunkPos(breakEvent.getPos());

        ClaimInstance claim = ClaimManager.getClaim(dimension, pos.x, pos.z);
        FactionInstance claimingFaction = ClaimManager.getFaction(dimension, pos.x, pos.z);

        if (claim == null || claim.level < 1 || claimingFaction == null) {
            return;
        }

        if (!(breakEvent.getPlayer() instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP playerMP = (EntityPlayerMP) breakEvent.getPlayer();
        if (!claimingFaction.isAlly(playerMP)) {
            playerMP.sendMessage(new TextComponentString("FUCK U"));
            breakEvent.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void placeBlock(BlockEvent.EntityPlaceEvent placeEvent) {
        int dimension = placeEvent.getWorld().provider.getDimension();
        ChunkPos pos = new ChunkPos(placeEvent.getPos());

        ClaimInstance claim = ClaimManager.getClaim(dimension, pos.x, pos.z);
        FactionInstance claimingFaction = ClaimManager.getFaction(dimension, pos.x, pos.z);

        if (claim == null || claim.level < 1 || claimingFaction == null) {
            return;
        }

        if (!(placeEvent.getEntity() instanceof EntityPlayerMP)) {
            placeEvent.setCanceled(true);
            return;
        }

        EntityPlayerMP playerMP = (EntityPlayerMP) placeEvent.getEntity();
        if (!claimingFaction.isAlly(playerMP)) {
            playerMP.sendMessage(new TextComponentString("NUH UH"));
            placeEvent.setCanceled(true);
        }
    }
}
