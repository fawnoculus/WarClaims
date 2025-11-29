package net.fawnoculus.warclaims.mixin.xaero;

import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.xaero.ClaimsMapHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.highlight.HighlighterRegistry;
import xaero.common.mods.WorldMapHighlighter;

@Mixin(HighlighterRegistry.class)
public class MiniMapHighlighterRegistryMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    public void afterInitializeRegistry(CallbackInfo ci) {
        HighlighterRegistry registry = (HighlighterRegistry) (Object) this;
        registry.register(new WorldMapHighlighter(new ClaimsMapHighlighter()));

        WarClaims.LOGGER.info("Injected Chunk Highlighters into Xaero's Minimap");
    }
}
