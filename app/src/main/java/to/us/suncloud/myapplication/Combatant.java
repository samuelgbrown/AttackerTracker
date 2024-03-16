package to.us.suncloud.myapplication;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

// A simple class to keep track of a Combatant
public class Combatant extends Fightable implements Serializable {
    public static final int THIS_FIGHTABLE_TYPE = 0; // Must be unique among all subclasses of Fightables!
    private static final String TAG = "Combatant";

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

    public Combatant( JSONObject jsonObject ) {
        fromJSON(jsonObject);
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

    // For JSON conversions
    private static final String ICON_INDEX_KEY = "ICON_INDEX";
    private static final String SPEED_FACTOR_KEY = "SPEED_FACTOR";
    private static final String ROLL_KEY = "ROLL";
    private static final String TOTAL_INITIATIVE_KEY = "TOTAL_INITIATIVE";
    private static final String IS_VISIBLE_KEY = "IS_VISIBLE";

    @Override
    protected void fromJSON_Child(JSONObject jsonObject) {
        try {
            iconIndex = jsonObject.getInt(ICON_INDEX_KEY);
            speedFactor = jsonObject.getInt(SPEED_FACTOR_KEY);
            roll = jsonObject.getInt(ROLL_KEY);
            totalInitiative = jsonObject.getInt(TOTAL_INITIATIVE_KEY);
            isVisible = jsonObject.getBoolean(IS_VISIBLE_KEY);
        } catch (JSONException e) {
            Log.e(TAG,e.toString());
        }
    }

    @Override
    protected void toJSON_Child(JSONObject jsonObject) {
        try {
            jsonObject.put(ICON_INDEX_KEY, iconIndex);
            jsonObject.put(SPEED_FACTOR_KEY, speedFactor);
            jsonObject.put(ROLL_KEY, roll);
            jsonObject.put(TOTAL_INITIATIVE_KEY, totalInitiative);
            jsonObject.put(IS_VISIBLE_KEY, isVisible);
            jsonObject.put(Fightable.FIGHTABLE_TYPE, THIS_FIGHTABLE_TYPE);
        } catch (JSONException e) {
            Log.e(TAG,e.toString());
        }
    }
}
