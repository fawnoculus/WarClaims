package net.fawnoculus.warclaims.claims.invade;

import java.util.Objects;
import java.util.UUID;

public class InvasionKey {
    public final UUID attackingFaction;
    public final UUID defendingFaction;

    public InvasionKey(UUID attackingFaction, UUID defendingFaction) {
        this.attackingFaction = attackingFaction;
        this.defendingFaction = defendingFaction;
    }

    public static InvasionKey fromString(String string) throws IllegalArgumentException, IndexOutOfBoundsException {
        String[] split = string.split(":");
        UUID attackingFaction = UUID.fromString(split[0]);
        UUID defendingFation = UUID.fromString(split[1]);
        return new InvasionKey(attackingFaction, defendingFation);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof InvasionKey)) return false;
        InvasionKey that = (InvasionKey) o;
        return Objects.equals(this.attackingFaction, that.attackingFaction) && Objects.equals(this.defendingFaction, that.defendingFaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackingFaction, defendingFaction);
    }

    @Override
    public String toString() {
        return attackingFaction.toString() + ":" + defendingFaction.toString();
    }
}
