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
    public Combatant( ) {}
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
    public Fightable getRaw_Child(Fightable f) {
        if ( f instanceof Combatant ) {
            ((Combatant) f).clearRoll();
        }
        return f;
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

//    public boolean rawEquals(@Nullable Object obj) {
//        // Check if the Combatants are the same, for data saving purposes
//        boolean isEqual = false;
//        if (obj instanceof Combatant) {
//            boolean parentEqual = super.equals(obj);
//            boolean iconEqual = getIconIndex() == ((Combatant) obj).getIconIndex();
//
//            isEqual = parentEqual && iconEqual;
//        }
//
//        return isEqual;
//    }

    public boolean displayEquals_Child(@Nullable Object obj) {
        boolean isEqual = false;
        if (obj instanceof Combatant) {
            boolean selectedEqual = isSelected() == ((Combatant) obj).isSelected();
            boolean iconEqual = getIconIndex() == ((Combatant) obj).getIconIndex();

            isEqual = iconEqual && selectedEqual;
        }

        return isEqual;
    }

    public void displayCopy_Child(Fightable f) {
        if (f instanceof Combatant) {
            setIconIndex(((Combatant) f).getIconIndex());
            setModifier(((Combatant) f).getModifier());
        }
    }

    public ArrayList<Combatant> convertToCombatants(AllFactionFightableLists referenceList) {
        ArrayList<Combatant> returnList = new ArrayList<>();
        returnList.add(this);
        return returnList;
    }

    public Combatant clone() {
        return new Combatant(this);
    }

    public Fightable cloneUnique_Child( Fightable f ) {
        // Generate a Combatant with no roll
        if ( f instanceof Combatant ) {
            ((Combatant) f).clearRoll();
        }
        return f;
    }
}
