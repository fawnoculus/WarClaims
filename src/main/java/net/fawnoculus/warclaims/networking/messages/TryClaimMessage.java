package net.fawnoculus.warclaims.networking.messages;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class TryClaimMessage implements IMessage {
    public int startX;
    public int endX;
    public int startZ;
    public int endZ;

    public TryClaimMessage(){
    }

    public TryClaimMessage(int startX, int endX, int startZ, int endZ) {
        this.startX = startX;
        this.endX = endX;
        this.startZ = startZ;
        this.endZ = endZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.startX = buf.readInt();
        this.endX = buf.readInt();
        this.startZ = buf.readInt();
        this.endZ = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.startX);
        buf.writeInt(this.endX);
        buf.writeInt(this.startZ);
        buf.writeInt(this.endZ);
    }
}
