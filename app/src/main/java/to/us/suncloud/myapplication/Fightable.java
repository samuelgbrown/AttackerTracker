package to.us.suncloud.myapplication;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A bare-bones abstract class that can represent either a Combatant or a Group of combatants.
abstract public class Fightable implements Serializable {
    private static final String TAG = "Fightable";

    private Faction faction = Faction.Party;
    private boolean isSelected = false; // Is the Fightable selected or checked off?
    private UUID id = UUID.randomUUID();
    private String name; // Name will be initialized on construction

    // Enforce uniqueness by name (for enemies, should ALWAYS be different, e.g. "Zombie 1", "Zombie 2"...), although the UUID is used for actual unique identification
    private static final String INIT_NAME = "New Object";
    // Regex Pattern for finding the base name of a Fightable
    private static final Pattern ordinalChecker = Pattern.compile("^(.*?)(?:\\s+(\\d++)$|$)"); // A pattern that matches the Fightable name into the first group, and the Fightable's ordinal number (if it exists) into the second group

    public static final int DOES_NOT_APPEAR = -1; // If, upon a search for highestOrdinalInstance among a list of Fightables, the given base name does not appear
    public static final int NO_ORDINAL = 0; // If, upon a search for highestOrdinalInstance among a list of Fightables, the given base name does appear, but it has no ordinal (i.e. "Zombie" exists, but not "Zombie 1")

    abstract public Fightable clone();
    abstract public ArrayList<Combatant> convertToCombatants(AllFactionFightableLists referenceList);

    public Faction getFaction() { return faction;}
    public void setFaction(Faction faction) {
        this.faction = faction;
    }
    public static String factionToString(Faction faction) {
        switch (faction) {
            case Group  :
                return "Group";
            case Party:
                return "Party";
            case Enemy:
                return "Enemy";
            case Neutral:
                return "Neutral";
            default:
                return "";
        }
    }

    public boolean isSelected() {
        return isSelected;
    }
    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    abstract boolean isVisible();
    abstract void setVisible(boolean isVisible);

    //
    // Constructors
    //
    public Fightable( ) {
        setName(INIT_NAME);
    }
    public Fightable(AllFactionFightableLists listOfAllFightables) {
        // Require all FightableLists to enforce uniqueness across all lists

        // Once we have found a unique name, set it as this Fightable's name
        setName(generateUniqueName(listOfAllFightables, this instanceof Combatant));
    }

    public Fightable(ArrayList<String> listOfAllFightableNames) {
        // Require all FightableLists to enforce uniqueness across all lists

        // Once we have found a unique name, set it as this Fightable's name
        setName(generateUniqueName(listOfAllFightableNames));
    }

    public Fightable(Fightable f) {
        // Copy constructor (used for cloning) - make an EXACT clone of this Fightable (careful about Fightable uniqueness!)
        setName(f.getName());
        setFaction(f.getFaction());
        setID(f.getId());
        setSelected(f.isSelected());
    }

    //
    // Name Handling
    //
    public String getName() {return name;}

    public void setName(String name) {
        // Note: Be careful when using this function, name exclusivity must be enforced elsewhere (it will not be checked here)
        this.name = name;
    }

    public String getBaseName() {
        return getBaseName(name);
    }

    static public String getBaseName( String this_name ) {
        // Get the name of this Fightable without any number at the end
        Matcher match = ordinalChecker.matcher(this_name);
        if (match.matches()) {
            if ( match.group( 2 ) != null ) {
                // If we got an ordinal, run this function again, trimming off the ordinal (in case there are multiple string fragments that might be considered ordinals)
                return getBaseName( match.group( 1 ) );
            } else {
                return match.group(1); // Get the first matched group (that isn't the full match), which corresponds to the Fightables' base name (without any ordinals)
            }
        } else {
            return "";
        }
    }

    public int getOrdinal() {
        Matcher match = ordinalChecker.matcher(name);
        if (match.matches()) {
            if (match.group(2) != null) {
                return Integer.parseInt(match.group(2));
            } else {
                // If there is no ordinal
                return NO_ORDINAL;
            }
        } else {
            // If no match could be found, return a -1 and log an error
            Log.e("Fightables", "Could not calculate ordinal of name " + name);
            return NO_ORDINAL;
        }
    }

    public void setNameOrdinal(int ordinal) {
        if ( ordinal != NO_ORDINAL ) {
            // Set the ordinal number of the name
            setName(getBaseName() + " " + ordinal);
        } else {
            // Remove any ordinals in the name
            setName(getBaseName());
        }
    }

    public static boolean isNameUnique(String nameToTest, ArrayList<FactionFightableList> listOfAllFightables) {
        // First, generate an ArrayList of Strings of all Fightables
        ArrayList<String> allFightableNames = new ArrayList<>();
        for (int fclIndex = 0; fclIndex < listOfAllFightables.size(); fclIndex++) {
            // For each faction, add the Fightables names list to allFightableNames
            allFightableNames.addAll(listOfAllFightables.get(fclIndex).getFightableNamesList());
        }

        // Then, see if the supplied name is unique to the supplied list
        return !allFightableNames.contains(nameToTest);
    }

    public static String generateUniqueName(ArrayList<String> listOfAllFightableNames) {
        // Try making a name unique to this list
        boolean isUnique = false;
        int curSuffix = 2;
        String currentNameSelection = INIT_NAME;

        while (!isUnique) {
            isUnique = !listOfAllFightableNames.contains(currentNameSelection); // See if the Fightable name list contains this name

            if (!isUnique) {
                // Try to make the name unique
                currentNameSelection = INIT_NAME + " " + curSuffix;
                curSuffix++; // Iterate curSuffix in case we need to try to make the name unique again
            }
        }

        return currentNameSelection;
    }

    public static String generateUniqueName(AllFactionFightableLists listOfAllFightables, boolean isCombatant) {
        // Default name to "New Fightable"
        return generateUniqueName(listOfAllFightables, isCombatant, INIT_NAME);
    }

    public static String generateUniqueName(AllFactionFightableLists listOfAllFightables, boolean isCombatant, String initName) {
        int highestOrdinalInstance = listOfAllFightables.getHighestOrdinalInstance(INIT_NAME, isCombatant);
        switch (highestOrdinalInstance) {
            case DOES_NOT_APPEAR:
                // No other Fightable appears with this name
                return initName;
            case NO_ORDINAL:
                // There is a Fightable with this name, but no other ordinal (i.e. "Zombie" appears, but not "Zombie 2").
                // NOTE: When adding a Fightable with this new name, we will likely want to also rename the old Fightable to give it an ordinal (i.e. "Zombie" becomes "Zombie 1")
                highestOrdinalInstance = 1; // Fall through to the default case, with ordinal "2"
            default:
                // There is a Fightable with this name already, so add one to the highest ordinal number
                return initName + " " + (highestOrdinalInstance + 1);
        }
    }

    public Fightable getRaw( ) {
        // Useful for quickly getting a "sanitized" version of the Fightable (clears the roll/total initiative, if it exists, clears isSelected)
        Fightable rawFightable = getRawFightable();
        getRaw_Child(rawFightable);
        return rawFightable;
    }
    abstract protected Fightable getRaw_Child(Fightable rawFightable); // Method to set values in Fightable f if getting raw version of object of child type
    private Fightable getRawFightable() {
        // Useful for quickly getting a "sanitized" version of theFightable (gets rid of selection status)
        Fightable rawFightable = this.clone();
        rawFightable.setSelected(false);

        return rawFightable;
    }

    public void displayCopy( Fightable f ) {
        displayCopyFightable(f);
        displayCopy_Child(f);
    }
    abstract protected void displayCopy_Child(Fightable f); // Copy display values from the f to the child object
    private void displayCopyFightable(Fightable f) {
        // Copy the display values from the incoming Fightable (NOT selection)
        setName(f.getName());
        setFaction(f.getFaction());
    }

    public boolean displayEquals(@Nullable Object obj ) {
        // Used for checking when the RecyclerView needs to update
        boolean FightableIsEqual = displayEqualsFightable(obj);
        boolean childIsEqual = displayEquals_Child(obj);

        return FightableIsEqual && childIsEqual;
    }
    abstract protected boolean displayEquals_Child(@Nullable Object obj); // Check if obj and the child object have the same display values
    private boolean displayEqualsFightable(@Nullable Object obj) {
        boolean isEqual = false;
        if ( obj instanceof Fightable ) {
            boolean facEqual = getFaction() == ((Fightable) obj).getFaction();
            boolean nameEqual = getName().equals(((Fightable) obj).getName());
            boolean selectedEqual = isSelected() == ((Fightable) obj).isSelected();
            isEqual = facEqual && nameEqual && selectedEqual;
        }
        return isEqual;
    }

    public Fightable cloneUnique() {
        Fightable uniqueClonedFightable = cloneUniqueFightable();
        cloneUnique_Child(uniqueClonedFightable);
        return uniqueClonedFightable;
    }
    abstract protected Fightable cloneUnique_Child( Fightable f ); // Method to set values in Fightable f if cloning object of child type
    private Fightable cloneUniqueFightable() {
        Fightable newFightable = clone();
        newFightable.genUUID();
        newFightable.setSelected(false);

        return newFightable;
    }


    // Combatant Handling
    static public ArrayList<Combatant> digestAllFightablesToCombatants( ArrayList<Fightable> fightablesList, AllFactionFightableLists referenceList ) {
        ArrayList<Combatant> returnList = new ArrayList<>();
        for ( Fightable fightable : fightablesList ) {
            returnList.addAll(fightable.convertToCombatants(referenceList));
        }

        return returnList;
    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (obj instanceof Fightable) {
            boolean idEqual = getId().equals(((Fightable) obj).getId());

            isEqual = displayEquals(obj) && idEqual;
        }

        return isEqual;
    }

    public UUID getId() {
        return id;
    }
    protected void setID(UUID newID) {id = newID;}
    protected void genUUID() {
        // Generate a new UUID for this Fightable
        setID(UUID.randomUUID());
    }
    enum Faction {
        // New values MUST be added to the end, AND pick up numbering where it left off
        Group(0), // MUST start at 0
        Party(1), // Each value MUST increment by 1
        Enemy(2),
        Neutral(3);

        static final Faction[] FactionVals = values();
        private final int val;
        Faction(int val) {this.val = val;}
        public int getVal() {return val;}
        public static Faction fromInt(int val) {return FactionVals[val];}
    }

    // For JSON conversions
    private static final String FACTION_KEY = "FACTION";
    private static final String IS_SELECTED_KEY = "IS_SELECTED";
    private static final String ID_KEY = "ID";
    private static final String NAME_KEY = "NAME";
    public static final String FIGHTABLE_TYPE = "TYPE";

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(FACTION_KEY, faction != null ? ( faction.getVal() ) : JSONObject.NULL);
            jsonObject.put(IS_SELECTED_KEY, isSelected);
            jsonObject.put(ID_KEY, id != null ? id.toString() : JSONObject.NULL);
            jsonObject.put(NAME_KEY, name != null ? name : JSONObject.NULL);

            toJSON_Child(jsonObject);
        } catch (JSONException e) {
            Log.e(TAG,e.toString());
        }

        return jsonObject;
    }

    public void fromJSON( JSONObject jsonObject ) {
        try {
            if (!jsonObject.isNull(FACTION_KEY)) {
                faction = Faction.fromInt(jsonObject.getInt(FACTION_KEY));
            }
            isSelected = jsonObject.getBoolean(IS_SELECTED_KEY);
            if (!jsonObject.isNull(ID_KEY)) {
                id = UUID.fromString(jsonObject.getString(ID_KEY));
            }
            if (!jsonObject.isNull(NAME_KEY)) {
                name = jsonObject.getString(NAME_KEY);
            }

            fromJSON_Child( jsonObject );
        } catch (JSONException e) {
            Log.e(TAG,e.toString());
        }
    }

    static public Fightable createFromJSON( JSONObject jsonObject ) {
        Fightable returnFightable = null;
        try {
            if (!jsonObject.isNull(FIGHTABLE_TYPE)) {
                switch (jsonObject.getInt(FIGHTABLE_TYPE)) {
                    case Combatant.THIS_FIGHTABLE_TYPE:
                        returnFightable = new Combatant(jsonObject);
                        break;
                    case CombatantGroup.THIS_FIGHTABLE_TYPE:
                        returnFightable = new CombatantGroup(jsonObject);
                        break;
                    default:
                        Log.e(TAG, "ERROR: Unknown Fightable type!  Cannot expand from JSON.");
                }
            } else {
                Log.e(TAG, "ERROR: JSON does not contain Fightable type!  Cannot expand from JSON.");
            }
        } catch ( JSONException e ) {
            Log.e(TAG,e.toString());
        }

        return returnFightable;
    }

    abstract protected void fromJSON_Child( JSONObject jsonObject );
    abstract protected void toJSON_Child( JSONObject jsonObject );
}
