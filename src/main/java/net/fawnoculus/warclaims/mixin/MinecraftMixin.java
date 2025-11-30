package net.fawnoculus.warclaims.mixin;

import net.fawnoculus.warclaims.claims.ClientClaimManager;
import net.fawnoculus.warclaims.claims.faction.ClientFactionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    public void onLoadWorld(WorldClient worldClientIn, String loadingMessage, CallbackInfo ci) {
        if (worldClientIn == null) {
            ClientClaimManager.clear();
            ClientFactionManager.clear();
        }
    }
}
