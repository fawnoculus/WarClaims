package net.fawnoculus.warclaims.networking.handlers;

import net.fawnoculus.warclaims.claims.invade.ClientInvasionManager;
import net.fawnoculus.warclaims.networking.messages.InvasionSyncMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class InvasionSyncMessageHandler implements IMessageHandler<InvasionSyncMessage, IMessage> {

    @Override
    public IMessage onMessage(InvasionSyncMessage message, MessageContext ctx) {
        ClientInvasionManager.update(message);
        return null;
    }
}
