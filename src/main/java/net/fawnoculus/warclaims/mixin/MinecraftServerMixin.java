package net.fawnoculus.warclaims.mixin;

import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.claims.invade.InvasionManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Shadow
    @Final
    public Profiler profiler;

    @Shadow
    @Final
    private File anvilFile;

    @Shadow
    private String folderName;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        this.profiler.startSection("[WarClaims] ClaimManager");
        ClaimManager.onTick();
        this.profiler.endSection();

        this.profiler.startSection("[WarClaims] InvasionManager");
        InvasionManager.onTick((MinecraftServer) (Object) this);
        this.profiler.endSection();

        this.profiler.startSection("[WarClaims] TeamManager");
        FactionManager.onTick();
        this.profiler.endSection();
    }

    @Inject(method = "initialWorldChunkLoad", at = @At("TAIL"))
    private void onLoadWorlds(CallbackInfo ci) {
        String worldPath = this.anvilFile.getAbsolutePath() + File.separatorChar + this.folderName;
        ClaimManager.loadFromFile(worldPath);
        InvasionManager.loadFromFile(worldPath);
        FactionManager.loadFromFile(worldPath);
    }

    @Inject(method = "saveAllWorlds", at = @At("TAIL"))
    private void onSave(CallbackInfo ci) {
        String worldPath = this.anvilFile.getAbsolutePath() + File.separatorChar + this.folderName;
        ClaimManager.saveToFile(worldPath);
        InvasionManager.saveToFile(worldPath);
        FactionManager.saveToFile(worldPath);
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void onStop(CallbackInfo ci) {
        String worldPath = this.anvilFile.getAbsolutePath() + File.separatorChar + this.folderName;
        ClaimManager.saveToFile(worldPath);
        InvasionManager.saveToFile(worldPath);
        FactionManager.saveToFile(worldPath);

        ClaimManager.clear();
        InvasionManager.clear();
        FactionManager.clear();
    }
}
