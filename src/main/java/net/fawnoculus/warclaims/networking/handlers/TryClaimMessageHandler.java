package net.fawnoculus.warclaims.networking.handlers;

import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.networking.messages.TryClaimMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class TryClaimMessageHandler implements IMessageHandler<TryClaimMessage, IMessage> {

    @Override
    public IMessage onMessage(TryClaimMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;

        UUID selectedTeam = FactionManager.getSelectedFaction(player);
        if (selectedTeam == null) {
            return null;
        }

        for (int x = message.startX; x < message.endX; x++) {
            for (int z = message.startZ; z < message.endZ; z++) {
                ClaimManager.claim(player.dimension, x, z, selectedTeam, 5);
            }
        }

        return null;
    }
}
