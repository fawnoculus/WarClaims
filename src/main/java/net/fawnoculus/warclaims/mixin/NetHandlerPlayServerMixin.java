package net.fawnoculus.warclaims.mixin;

import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public class NetHandlerPlayServerMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onPlayerJoin(MinecraftServer server, NetworkManager networkManagerIn, EntityPlayerMP playerIn, CallbackInfo ci){
        ClaimManager.onPlayerJoin(playerIn);
        FactionManager.onPlayerJoin(playerIn);
    }
}
