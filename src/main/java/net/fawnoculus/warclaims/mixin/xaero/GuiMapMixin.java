package net.fawnoculus.warclaims.mixin.xaero;

import net.fawnoculus.warclaims.xaero.ClaimChunkOption;
import net.fawnoculus.warclaims.xaero.InvadeChunkOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

@Mixin(GuiMap.class)
public class GuiMapMixin {

    @Shadow(remap = false)
    private MapTileSelection mapTileSelection;

    @Inject(method = "getRightClickOptions", at = @At("RETURN"), remap = false)
    private void addOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        int startX = this.mapTileSelection.getStartX();
        int endX = this.mapTileSelection.getEndX();
        int startZ = this.mapTileSelection.getStartZ();
        int endZ = this.mapTileSelection.getEndZ();

        ArrayList<RightClickOption> options = cir.getReturnValue();
        options.add(new ClaimChunkOption(options.size(), (IRightClickableElement) this, startX, endX, startZ, endZ));
        options.add(new InvadeChunkOption(options.size(), (IRightClickableElement) this, startX, endX, startZ, endZ));
    }
}
