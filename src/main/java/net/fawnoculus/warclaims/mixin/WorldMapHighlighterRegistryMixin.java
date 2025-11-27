package net.fawnoculus.warclaims.mixin;

import net.fawnoculus.warclaims.WarClaims;
import net.fawnoculus.warclaims.xaero.ClaimsMapHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.highlight.HighlighterRegistry;

@Mixin(HighlighterRegistry.class)
public class WorldMapHighlighterRegistryMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    public void afterInitializeRegistry(CallbackInfo ci) {
        HighlighterRegistry registry = (HighlighterRegistry) (Object) this;
        registry.register(new ClaimsMapHighlighter());

        WarClaims.LOGGER.info("Injected Chunk Highlighters into Xaero's World Map");
    }
}
