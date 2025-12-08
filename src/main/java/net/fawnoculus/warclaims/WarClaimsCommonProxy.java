package net.fawnoculus.warclaims;

import net.fawnoculus.warclaims.claims.ClaimEventHandler;
import net.fawnoculus.warclaims.commands.*;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class WarClaimsCommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        WarClaimsConfig.synchronizeConfiguration(event.getSuggestedConfigurationFile());
    }

    public void init(FMLInitializationEvent ignored) {
        WarClaimsNetworking.initialize();
        MinecraftForge.EVENT_BUS.register(ClaimEventHandler.class);
    }

    public void postInit(FMLPostInitializationEvent ignored) {
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ClaimSingleCommand());
        event.registerServerCommand(new ClaimSelectionCommand());
        event.registerServerCommand(new UnclaimSingleCommand());
        event.registerServerCommand(new UnclaimSelectionCommand());
        event.registerServerCommand(new InvadeSingleCommand());
        event.registerServerCommand(new InvadeSelectionCommand());
        event.registerServerCommand(new ForceClaimCommand());
        event.registerServerCommand(new FactionCommand());
        event.registerServerCommand(new CurrentFactionCommand());
    }
}
