package net.fawnoculus.warclaims.claims.invade;

import io.netty.buffer.ByteBuf;
import net.fawnoculus.warclaims.WarClaimsConfig;
import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.minecraft.util.text.ITextComponent;

public class ClientInvasionInstance {
    public final long requiredTicks;
    public final long passedTicks;

    public ClientInvasionInstance(long requiredTicks, long passedTicks) {
        this.requiredTicks = requiredTicks;
        this.passedTicks = passedTicks;
    }

    public static ClientInvasionInstance fromByteBuff(ByteBuf buf) {
        return new ClientInvasionInstance(buf.readLong(), buf.readLong());
    }

    public double getCompletion() {
        return (double) requiredTicks / (double) passedTicks;
    }

    public String getCompletionPercentage() {
        if (WarClaimsConfig.showTicksInInvasions) {
            return String.format("%1$06.2f%% (%2$d/%3$d)", this.getCompletion() * 100.0, passedTicks, requiredTicks);
        }
        return String.format("%1$06.2f%%", this.getCompletion() * 100.0);
    }

    public ITextComponent makeTooltip(ClaimInstance claim, FactionInstance team, FactionInstance invadingTeam) {
        return claim.makeTooltip(team).appendText(" vs " + invadingTeam.name + " " + this.getCompletionPercentage());
    }
}
