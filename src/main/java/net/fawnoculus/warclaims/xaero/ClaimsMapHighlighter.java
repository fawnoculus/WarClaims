package net.fawnoculus.warclaims.xaero;

import net.fawnoculus.warclaims.utils.ColorUtil;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import xaero.map.highlight.ChunkHighlighter;

import java.util.List;
import java.util.Objects;

public class ClaimsMapHighlighter extends ChunkHighlighter {
    private static final int MIDDLE = 0;
    private static final int NORTH = 1;
    private static final int EAST = 2;
    private static final int SOUTH = 3;
    private static final int WEST = 4;

    public ClaimsMapHighlighter() {
        super(true);
    }

    @Override
    public int calculateRegionHash(int dimension, int regionX, int regionZ) {
        return Objects.hash(dimension, regionX, regionZ);
    }

    @Override
    public boolean regionHasHighlights(int dimension, int regionX, int regionZ) {
        return true;
    }

    @Override
    public boolean chunkIsHighlit(int dimension, int chunkX, int chunkZ) {
        return  (chunkX + chunkZ) % 2 == 0;
    }

    @Override
    protected int[] getColors(int dimension, int chunkX, int chunkZ) {
        if (!this.chunkIsHighlit(dimension, chunkX, chunkZ)) {
            return null;
        } else {
            int sideColor = ColorUtil.fromRGB(175, 0, 200);
            int middleColor = ColorUtil.withAlpha(60, sideColor);

            this.resultStore[MIDDLE] = middleColor;
            this.resultStore[NORTH] = sideColor;
            this.resultStore[EAST] = sideColor;
            this.resultStore[SOUTH] = sideColor;
            this.resultStore[WEST] = sideColor;
            return this.resultStore;
        }
    }

    @Override
    public ITextComponent getChunkHighlightSubtleTooltip(int dimension, int chunkX, int chunkZ) {
        return new TextComponentString("subtle!");
    }

    @Override
    public ITextComponent getChunkHighlightBluntTooltip(int dimension, int chunkX, int chunkZ) {
        return new TextComponentString("blunt!");
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<ITextComponent> list, int dimension, int chunkX, int chunkZ, int dimension3) {
        list.add(new TextComponentString("minimap tooltip!"));
    }
}
