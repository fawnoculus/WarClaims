package net.fawnoculus.warclaims;

import net.fawnoculus.warclaims.claims.ClaimEventHandler;
import net.fawnoculus.warclaims.commands.ClaimCommand;
import net.fawnoculus.warclaims.commands.CurrentFactionCommand;
import net.fawnoculus.warclaims.commands.ForceClaimCommand;
import net.fawnoculus.warclaims.commands.FactionCommand;
import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class WarClaimsCommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        WarClaimsNetworking.initialize();
        MinecraftForge.EVENT_BUS.register(ClaimEventHandler.class);
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ClaimCommand());
        event.registerServerCommand(new ForceClaimCommand());
        event.registerServerCommand(new FactionCommand());
        event.registerServerCommand(new CurrentFactionCommand());
    }
}
