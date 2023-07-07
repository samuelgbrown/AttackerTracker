package to.us.suncloud.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

// A bare-bones abstract class that can represent either a Combatant or a Group of combatants.
abstract public class Fightable implements Serializable {
    private Faction faction = Faction.Party;
    private boolean isSelected = false; // Is the Combatant selected or checked off?
    private UUID id = UUID.randomUUID();

    public static final int DOES_NOT_APPEAR = -2; // If, upon a search for highestOrdinalInstance among a list of Fightables, the given base name does not appear
    public static final int NO_ORDINAL = -1; // If, upon a search for highestOrdinalInstance among a list of Fightables, the given base name does appear, but it has no ordinal (i.e. "Zombie" exists, but not "Zombie 1")

    abstract public Fightable clone();
    abstract public Fightable cloneUnique();
    abstract public Fightable getRaw();
    abstract public ArrayList<Combatant> convertToCombatants();

    abstract public String getName();
    abstract public String getBaseName();
    abstract public int getOrdinal();

    public Faction getFaction() { return faction;}
    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public boolean isSelected() {
        return isSelected;
    }
    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    abstract boolean isVisible();
    abstract void setVisible(boolean isVisible);

    abstract void displayCopy(Fightable f);
    abstract void setNameOrdinal(int newOrdinal);

    abstract boolean displayEquals(@Nullable Object obj);

    public UUID getId() {
        return id;
    }
    protected void setID(UUID newID) {id = newID;}
    enum Faction {
        Group,
        Party,
        Enemy,
        Neutral
    }
}
