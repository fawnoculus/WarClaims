package net.fawnoculus.warclaims.mixin;

import net.fawnoculus.warclaims.claims.ClientClaimManager;
import net.fawnoculus.warclaims.claims.faction.ClientFactionManager;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class NetHandlerPlayClientMixin {
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    public void afterInitializeRegistry(ITextComponent reason, CallbackInfo ci) {
        ClientClaimManager.clear();
        ClientFactionManager.clear();
    }
}
