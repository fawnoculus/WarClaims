package net.fawnoculus.warclaims.networking;

import net.fawnoculus.warclaims.Tags;
import net.fawnoculus.warclaims.networking.handlers.ClaimUpdateMessageHandler;
import net.fawnoculus.warclaims.networking.messages.ClaimUpdateMessage;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class WarClaimsNetworking {
    public static final SimpleNetworkWrapper WRAPPER = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    @SuppressWarnings("UnusedAssignment")
    public static void initialize() {
        int id = 0;
        WRAPPER.registerMessage(new ClaimUpdateMessageHandler(), ClaimUpdateMessage.class, id++, Side.CLIENT);
    }
}
