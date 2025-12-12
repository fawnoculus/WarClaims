package net.fawnoculus.warclaims.xaero;

import net.fawnoculus.warclaims.WarClaimsConfig;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

public class UnclaimChunkOption extends RightClickOption {
    private final int startX;
    private final int endX;
    private final int startZ;
    private final int endZ;

    public UnclaimChunkOption(int index, IRightClickableElement target, int startX, int endX, int startZ, int endZ) {
        super(makeName(startX, endX, startZ, endZ), index, target);

        this.startX = startX;
        this.endX = endX;
        this.startZ = startZ;
        this.endZ = endZ;
    }

    private static String makeName(int startX, int endX, int startZ, int endZ) {
        if (isMultiChunk(startX, endX, startZ, endZ)) {
            return "Unclaim Selected";
        }
        return "Unclaim Chunk";
    }

    private static boolean isMultiChunk(int startX, int endX, int startZ, int endZ) {
        return startX != endX || startZ != endZ;
    }

    private boolean isMultiChunk() {
        return isMultiChunk(this.startX, this.endX, this.startZ, this.endZ);
    }

    @Override
    public void onAction(GuiScreen guiScreen) {
        if (!this.isMultiChunk()) {
            guiScreen.mc.displayGuiScreen(new GuiChat(
                    String.format(WarClaimsConfig.unclaimSingleCommandFormat, this.startX, this.startZ)
            ));
            return;
        }
        guiScreen.mc.displayGuiScreen(new GuiChat(
                String.format(WarClaimsConfig.unclaimSelectionCommandFormat, this.startX, this.startZ, this.endX, this.endZ)
        ));
    }
}
