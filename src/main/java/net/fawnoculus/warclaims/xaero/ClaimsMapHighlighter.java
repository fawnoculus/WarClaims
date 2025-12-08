package net.fawnoculus.warclaims.xaero;

import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.fawnoculus.warclaims.claims.ClientClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.invade.ClientInvasionInstance;
import net.fawnoculus.warclaims.claims.invade.ClientInvasionManager;
import net.fawnoculus.warclaims.claims.invade.InvasionInstance;
import net.minecraft.util.text.ITextComponent;
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
        return ClientClaimManager.getFaction(dimension, chunkX, chunkZ) != null;
    }

    @Override
    protected int[] getColors(int dimension, int chunkX, int chunkZ) {
        FactionInstance faction = ClientClaimManager.getFaction(dimension, chunkX, chunkZ);
        if (faction == null) {
            return null;
        }

        FactionInstance invadingFaction = ClientInvasionManager.getInvadingTeam(dimension, chunkX, chunkZ);

        FactionInstance northFaction = ClientClaimManager.getFaction(dimension, chunkX, chunkZ - 1);
        FactionInstance northInvadingFaction = ClientInvasionManager.getInvadingTeam(dimension, chunkX, chunkZ - 1);
        boolean northIsSame = faction == northFaction && invadingFaction == northInvadingFaction;

        FactionInstance eastFaction = ClientClaimManager.getFaction(dimension, chunkX + 1, chunkZ);
        FactionInstance eastInvadingFaction = ClientInvasionManager.getInvadingTeam(dimension, chunkX + 1, chunkZ);
        boolean eastIsSame = faction == eastFaction && invadingFaction == eastInvadingFaction;

        FactionInstance southFaction = ClientClaimManager.getFaction(dimension, chunkX, chunkZ + 1);
        FactionInstance southInvadingFaction = ClientInvasionManager.getInvadingTeam(dimension, chunkX, chunkZ + 1);
        boolean southIsSame = faction == southFaction && invadingFaction == southInvadingFaction;

        FactionInstance westFaction = ClientClaimManager.getFaction(dimension, chunkX - 1, chunkZ);
        FactionInstance westInvadingFaction = ClientInvasionManager.getInvadingTeam(dimension, chunkX - 1, chunkZ);
        boolean westIsSame = faction == westFaction && invadingFaction == westInvadingFaction;

        this.resultStore[MIDDLE] = faction.makeColor(invadingFaction, true);
        this.resultStore[NORTH] = faction.makeColor(invadingFaction, northIsSame);
        this.resultStore[EAST] = faction.makeColor(invadingFaction, eastIsSame);
        this.resultStore[SOUTH] = faction.makeColor(invadingFaction, southIsSame);
        this.resultStore[WEST] = faction.makeColor(invadingFaction, westIsSame);
        return this.resultStore;
    }

    @Override
    public ITextComponent getChunkHighlightSubtleTooltip(int dimension, int chunkX, int chunkZ) {
        return makeChunkTooltip(dimension, chunkX, chunkZ);
    }

    @Override
    public ITextComponent getChunkHighlightBluntTooltip(int dimension, int chunkX, int chunkZ) {
        return makeChunkTooltip(dimension, chunkX, chunkZ);
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<ITextComponent> list, int dimension, int blockX, int blockZ, int width) {
        ITextComponent tooltip = makeChunkTooltip(dimension, blockX >> 4, blockZ >> 4);
        if (tooltip != null) {
            list.add(tooltip);
        }
    }

    private static ITextComponent makeChunkTooltip(int dimension, int chunkX, int chunkZ) {
        ClaimInstance claim = ClientClaimManager.get(dimension, chunkX, chunkZ);
        FactionInstance team = ClientClaimManager.getFaction(dimension, chunkX, chunkZ);

        if (claim != null && team != null) {

            ClientInvasionInstance invasion = ClientInvasionManager.get(dimension, chunkX, chunkZ);
            FactionInstance invadingTeam = ClientInvasionManager.getInvadingTeam(dimension, chunkX, chunkZ);
            if (invasion != null && invadingTeam != null) {
                return invasion.makeTooltip(claim, team, invadingTeam);
            }

            return claim.makeTooltip(team);
        }

        return null;
    }
}
