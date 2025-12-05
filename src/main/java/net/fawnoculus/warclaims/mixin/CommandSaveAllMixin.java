package net.fawnoculus.warclaims.mixin;

import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.claims.invade.InvasionManager;
import net.fawnoculus.warclaims.mixin.accesor.MinecraftServerAccessor;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandSaveAll;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(CommandSaveAll.class)
public class CommandSaveAllMixin {

    @Inject(method = "execute", at = @At("HEAD"))
    private void onExecute(MinecraftServer server, ICommandSender sender, String[] args, CallbackInfo ci) {
        String worldPath = ((MinecraftServerAccessor) server).WarClaims$getAnvilFile().getAbsolutePath() + File.separatorChar + server.getFolderName();
        ClaimManager.saveToFile(worldPath);
        InvasionManager.saveToFile(worldPath);
        FactionManager.saveToFile(worldPath);
    }
}
