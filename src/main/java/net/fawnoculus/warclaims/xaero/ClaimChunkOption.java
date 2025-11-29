package net.fawnoculus.warclaims.xaero;

import net.fawnoculus.warclaims.networking.WarClaimsNetworking;
import net.fawnoculus.warclaims.networking.messages.TryClaimMessage;
import net.minecraft.client.gui.GuiScreen;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

public class ClaimChunkOption extends RightClickOption {
    private final int startX;
    private final int endX;
    private final int startZ;
    private final int endZ;

    public ClaimChunkOption(int index, IRightClickableElement target, int startX, int endX, int startZ, int endZ) {
        super(startX == endX && startZ == endZ ? "Try Claim" : "Try Claim Selected", index, target);

        this.startX = startX;
        this.endX = endX;
        this.startZ = startZ;
        this.endZ = endZ;
    }

    @Override
    public void onAction(GuiScreen guiScreen) {
        WarClaimsNetworking.WRAPPER.sendToServer(new TryClaimMessage(startX, endX, startZ, endZ));
    }
}
