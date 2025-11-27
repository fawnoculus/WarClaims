package net.fawnoculus.warclaims.mixin;

import com.mojang.authlib.GameProfile;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "createPlayerForUser", at = @At("RETURN"))
    private void onPlayerJoin(GameProfile profile, CallbackInfoReturnable<EntityPlayerMP> cir){
        ClaimManager.onPlayerJoin(cir.getReturnValue());
    }
}
