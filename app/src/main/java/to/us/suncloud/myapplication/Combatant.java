package to.us.suncloud.myapplication;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A simple class to keep track of a Combatant
public class Combatant extends Fightable implements Serializable {
    private int iconIndex = 0; // Initialize with a blank icon
    private int speedFactor = 0;
    private int roll = 0;
    private int totalInitiative = 0;
    private boolean isVisible = true; // Is this Combatant visible in the initiative order?

    // Constructors
    public Combatant(AllFactionFightableLists listOfAllCombatants) {
        super(listOfAllCombatants);
    }

    public Combatant(ArrayList<String> listOfAllCombatantNames) {
        super(listOfAllCombatantNames);
    }

    public Combatant(Combatant c) {
        // Copy constructor (used for cloning) - make an EXACT clone of this Combatant (careful about Combatant uniqueness!)
        super(c);
        iconIndex = c.getIconIndex();
        speedFactor = c.getModifier();
        roll = c.getRoll();
        totalInitiative = c.getTotalInitiative();
        setID(c.getId());
        setSelected(c.isSelected());
        isVisible = c.isVisible();
    }

    //
    // Simple Getters
    //
    public int getRoll() {
        return roll;
    }

    public int getTotalInitiative() {
        return totalInitiative;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public void setIconIndex(int iconIndex) {
        this.iconIndex = iconIndex;
    }

    public int getIconIndex() {
        return iconIndex;
    }

    //
    // Advanced Getters
    //
    public Combatant getRaw() {
        // Useful for quickly getting a "sanitized" version of the Combatant (clears the roll/total initiative, if it exists, clears isSelected)
        Combatant rawCombatant = (Combatant) getRawFightable();

        // Set a few values for the new Combatant
        rawCombatant.clearRoll();
        rawCombatant.setSelected(false);
        return rawCombatant;
    }

    //
    // Simple Setters
    //

    public int getModifier() {
        return speedFactor;
    }

    public void setModifier(int speedFactor) {
        this.speedFactor = speedFactor;
        calcTotalInitiative();
    }

    //
    // Advanced Setters
    //
    public void setRoll(int roll) {
        this.roll = roll;

        // Set the total initiative
        calcTotalInitiative();
    }

    private void calcTotalInitiative() {
        // Recalculate the total initiative
        this.totalInitiative = speedFactor + this.roll;
    }

    public void clearRoll() {
        // Clear the roll (also affects total initiative)
        setRoll(0);
    }

    public void clearVals() {
        // Clear all values
        clearRoll();
        setModifier(0);
    }

    //
    // Other functions
    //
    @Override
    public boolean equals(@Nullable Object obj) {
        boolean isEqual = false;
        if (obj instanceof Combatant) {
            boolean parentEqual = super.equals(obj);
            boolean iconEqual = getIconIndex() == ((Combatant) obj).getIconIndex();
            boolean speedEqual = getModifier() == ((Combatant) obj).getModifier();
            boolean rollEqual = getRoll() == ((Combatant) obj).getRoll();
            boolean totalEqual = getTotalInitiative() == ((Combatant) obj).getTotalInitiative();

            isEqual = parentEqual && iconEqual && speedEqual && rollEqual && totalEqual;
        }

        return isEqual;
    }

    public boolean rawEquals(@Nullable Object obj) {
        // Check if the Combatants are the same, for data saving purposes
        boolean isEqual = false;
        if (obj instanceof Combatant) {
            boolean parentEqual = super.equals(obj);
            boolean iconEqual = getIconIndex() == ((Combatant) obj).getIconIndex();

            isEqual = parentEqual && iconEqual;
        }

        return isEqual;
    }

    public boolean displayEquals(@Nullable Object obj) {
        // Check if the Combatants are the same, for RecyclerView viewing purpose
        boolean isEqual = false;
        if (obj instanceof Combatant) {
            boolean selectedEqual = isSelected() == ((Combatant) obj).isSelected();
            boolean iconEqual = getIconIndex() == ((Combatant) obj).getIconIndex();

            isEqual = displayEqualsFightable(obj) && iconEqual && selectedEqual;
        }

        return isEqual;
    }

    public void displayCopy(Fightable c) {
        // Copy the display values from the incoming Fightable (NOT selection)

        if (c instanceof Combatant) {
            displayCopyFightable((Fightable) c);
            setIconIndex(((Combatant) c).getIconIndex());
            setModifier(((Combatant) c).getModifier());
        }
    }

    public ArrayList<Combatant> convertToCombatants(AllFactionFightableLists referenceList) {
        ArrayList<Combatant> returnList = new ArrayList<>();
        returnList.add(this);
        return returnList;
    }


    public void genUUID() {
        // Generate a new UUID for this Combatant
        setID(UUID.randomUUID());
    }

    public Combatant clone() {
        return new Combatant(this);
    }

    public Combatant cloneUnique() {
        // Generate a Combatant with a unique ID and no roll/initiative/modifier
        Combatant newCombatant = clone();
        newCombatant.genUUID();
//        newCombatant.clearVals();
        newCombatant.clearRoll();
        newCombatant.setSelected(false);
        return newCombatant;
    }
}
