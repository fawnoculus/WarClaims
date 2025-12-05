package net.fawnoculus.warclaims.networking;

import net.fawnoculus.warclaims.Tags;
import net.fawnoculus.warclaims.networking.handlers.ClaimSyncMessageHandler;
import net.fawnoculus.warclaims.networking.handlers.FactionSyncMessageHandler;
import net.fawnoculus.warclaims.networking.handlers.InvasionSyncMessageHandler;
import net.fawnoculus.warclaims.networking.messages.ClaimSyncMessage;
import net.fawnoculus.warclaims.networking.messages.FactionSyncMessage;
import net.fawnoculus.warclaims.networking.messages.InvasionSyncMessage;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class WarClaimsNetworking {
    public static final SimpleNetworkWrapper WRAPPER = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    @SuppressWarnings("UnusedAssignment")
    public static void initialize() {
        int id = 0;
        // Server -> Client
        WRAPPER.registerMessage(new ClaimSyncMessageHandler(), ClaimSyncMessage.class, id++, Side.CLIENT);
        WRAPPER.registerMessage(new InvasionSyncMessageHandler(), InvasionSyncMessage.class, id++, Side.CLIENT);
        WRAPPER.registerMessage(new FactionSyncMessageHandler(), FactionSyncMessage.class, id++, Side.CLIENT);
    }
}
