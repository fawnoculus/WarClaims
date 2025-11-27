package net.fawnoculus.warclaims.networking.handlers;

import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.networking.messages.ClaimUpdateMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClaimUpdateMessageHandler implements IMessageHandler<ClaimUpdateMessage, IMessage> {

    @Override
    public IMessage onMessage(ClaimUpdateMessage message, MessageContext ctx) {
        ClaimManager.update(message);
        WarClaims.LOGGER.info("RECIVED: {}", message);
        return null;
    }
}
