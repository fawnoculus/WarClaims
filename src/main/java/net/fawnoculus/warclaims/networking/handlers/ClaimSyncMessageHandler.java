package net.fawnoculus.warclaims.networking.handlers;

import net.fawnoculus.warclaims.claims.ClientClaimManager;
import net.fawnoculus.warclaims.networking.messages.ClaimSyncMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClaimSyncMessageHandler implements IMessageHandler<ClaimSyncMessage, IMessage> {

    @Override
    public IMessage onMessage(ClaimSyncMessage message, MessageContext ctx) {
        ClientClaimManager.update(message);
        return null;
    }
}
