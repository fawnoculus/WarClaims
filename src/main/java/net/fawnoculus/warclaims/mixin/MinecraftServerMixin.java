package net.fawnoculus.warclaims.mixin;

import com.mojang.authlib.GameProfile;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Shadow
    @Final
    public Profiler profiler;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci){
        this.profiler.startSection("ClaimManager");
        ClaimManager.onTick();
        this.profiler.endSection();
    }
}
