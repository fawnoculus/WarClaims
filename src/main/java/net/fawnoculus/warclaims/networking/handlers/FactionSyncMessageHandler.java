package net.fawnoculus.warclaims.networking.handlers;

import net.fawnoculus.warclaims.networking.messages.FactionSyncMessage;
import net.fawnoculus.warclaims.claims.faction.ClientFactionManager;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FactionSyncMessageHandler implements IMessageHandler<FactionSyncMessage, IMessage> {

    @Override
    public IMessage onMessage(FactionSyncMessage message, MessageContext ctx) {
        ClientFactionManager.update(message);
        return null;
    }
}
