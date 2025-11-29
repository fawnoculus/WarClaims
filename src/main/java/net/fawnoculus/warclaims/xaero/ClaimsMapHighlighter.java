package net.fawnoculus.warclaims.xaero;

import net.fawnoculus.warclaims.claims.ClientClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.utils.ColorUtil;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import xaero.map.highlight.ChunkHighlighter;

import java.util.List;

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
        return 2;
    }

    @Override
    public boolean regionHasHighlights(int dimension, int regionX, int regionZ) {
        return true;
    }

    @Override
    public boolean chunkIsHighlit(int dimension, int chunkX, int chunkZ) {
        return ClientClaimManager.get(dimension, chunkX, chunkZ) != null;
    }

    @Override
    protected int[] getColors(int dimension, int chunkX, int chunkZ) {
        FactionInstance team = ClientClaimManager.getTeam(dimension, chunkX, chunkZ);
        if (!this.chunkIsHighlit(dimension, chunkX, chunkZ)  || team == null) {
            return null;
        } else {
            int sideColor = ColorUtil.withAlpha(255, team.color);
            int middleColor = ColorUtil.withAlpha(60, sideColor);

            boolean northSame = team == ClientClaimManager.getTeam(dimension, chunkX, chunkZ - 1);
            boolean eastSame = team == ClientClaimManager.getTeam(dimension, chunkX + 1, chunkZ);
            boolean southSame = team == ClientClaimManager.getTeam(dimension, chunkX, chunkZ + 1);
            boolean westSame = team == ClientClaimManager.getTeam(dimension, chunkX - 1, chunkZ);

            this.resultStore[MIDDLE] = middleColor;
            this.resultStore[NORTH] = northSame ? middleColor : sideColor;
            this.resultStore[EAST] = eastSame ? middleColor : sideColor;
            this.resultStore[SOUTH] = southSame ? middleColor : sideColor;
            this.resultStore[WEST] = westSame ? middleColor : sideColor;
            return this.resultStore;
        }
    }

    @Override
    public ITextComponent getChunkHighlightSubtleTooltip(int dimension, int chunkX, int chunkZ) {
        FactionInstance team = ClientClaimManager.getTeam(dimension, chunkX, chunkZ);
        if (team != null) {
            return new TextComponentString(team.name);
        }
        return null;
    }

    @Override
    public ITextComponent getChunkHighlightBluntTooltip(int dimension, int chunkX, int chunkZ) {
        FactionInstance team = ClientClaimManager.getTeam(dimension, chunkX, chunkZ);
        if (team != null) {
            return new TextComponentString(team.name);
        }
        return null;
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<ITextComponent> list, int dimension, int blockX, int blockZ, int width) {
        FactionInstance team = ClientClaimManager.getTeam(dimension, blockX >> 4, blockZ >> 4);
        if (team != null) {
            list.add(new TextComponentString(team.name));
        }
    }
}
