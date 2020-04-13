package to.us.suncloud.myapplication;

import androidx.annotation.Nullable;

// A simple class to keep track of a combatant
public class Combatant {
    // TODO: Enforce uniqueness by name (for enemies, should ALWAYS be different, e.g. "Zombie 1", "Zombie 2"...)
    private Faction faction;
    private String name;
    private int speedFactor;
    private int roll;
    private int totalInitiative;

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSpeedFactor() {
        return speedFactor;
    }

    public void setSpeedFactor(int speedFactor) {
        this.speedFactor = speedFactor;
    }

    public int getRoll() {
        return roll;
    }

    public void setRoll(int roll) {
        this.roll = roll;

        // Set the total initiative
        this.totalInitiative = speedFactor + this.roll;
    }

    public int getTotalInitiative() {
        return totalInitiative;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean isEqual = false;
        if (obj instanceof Combatant) {
            boolean nameEqual = getName().equals(((Combatant) obj).getName());
            boolean facEqual = getFaction() == ((Combatant) obj).getFaction();
            boolean speedEqual = getSpeedFactor() == ((Combatant) obj).getSpeedFactor();
            boolean rollEqual = getRoll() == ((Combatant) obj).getRoll();

            isEqual = nameEqual && facEqual && speedEqual && rollEqual;
        }

        return isEqual;
    }

    enum Faction {
        Party,
        Enemy
    }
}
