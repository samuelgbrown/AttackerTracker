package to.us.suncloud.myapplication;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A bare-bones abstract class that can represent either a Combatant or a Group of combatants.
abstract public class Fightable implements Serializable {
    private Faction faction = Faction.Party;
    private boolean isSelected = false; // Is the Fightable selected or checked off?
    private UUID id = UUID.randomUUID();

    private String name; // Name will be initialized on construction
    // Enforce uniqueness by name (for enemies, should ALWAYS be different, e.g. "Zombie 1", "Zombie 2"...), although the UUID is used for actual unique identification
    private static final String INIT_NAME = "New Fightable";
    // Regex Pattern for finding the base name of a Fightable
    private static final Pattern ordinalChecker = Pattern.compile("^(.*?)(?:\\W*(\\d++)|$)"); // A pattern that matches the Fightable name into the first group, and the Fightable's ordinal number (if it exists) into the second group

    public static final int DOES_NOT_APPEAR = -2; // If, upon a search for highestOrdinalInstance among a list of Fightables, the given base name does not appear
    public static final int NO_ORDINAL = -1; // If, upon a search for highestOrdinalInstance among a list of Fightables, the given base name does appear, but it has no ordinal (i.e. "Zombie" exists, but not "Zombie 1")

    abstract public Fightable clone();
    abstract public Fightable cloneUnique();
    abstract public Fightable getRaw();
    abstract public ArrayList<Combatant> convertToCombatants(AllFactionFightableLists referenceList);

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

    abstract boolean displayEquals(@Nullable Object obj);

    //
    // Constructors
    //
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
        // Get the name of this Fightable without any number at the end
        Matcher match = ordinalChecker.matcher(name);
        if (match.matches()) {
            return match.group(1); // Get the first matched group (that isn't the full match), which corresponds to the Fightables's base name (without any ordinals)
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
        // Set the ordinal number of the name
        setName(getBaseName() + " " + ordinal);
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

    private static String generateUniqueName(AllFactionFightableLists listOfAllFightables, boolean isCombatant) {
        int highestOrdinalInstance = listOfAllFightables.getHighestOrdinalInstance(INIT_NAME, isCombatant);
        switch (highestOrdinalInstance) {
            case DOES_NOT_APPEAR:
                // No other Fightable appears with this name
                return INIT_NAME;
            case NO_ORDINAL:
                // There is a Fightable with this name, but no other ordinal (i.e. "Zombie" appears, but not "Zombie 2").
                // NOTE: When adding a Fightable with this new name, we will likely want to also rename the old Fightable to give it an ordinal (i.e. "Zombie" becomes "Zombie 1")
                return INIT_NAME + "  2";
            default:
                // There is a Fightable with this name already, so add one to the highest ordinal number
                return INIT_NAME + " " + (highestOrdinalInstance + 1);
        }
    }

    private static String generateUniqueName(ArrayList<String> listOfAllFightableNames) {
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

    protected Fightable getRawFightable() {
        // Useful for quickly getting a "sanitized" version of theFightable (gets rid of the name ordinal)
        Fightable rawFightable = this.clone();
        rawFightable.setName(getBaseName());

        return rawFightable;
    }

    protected void displayCopyFightable(Fightable f) {
        // Copy the display values from the incoming Fightable (NOT selection)
        setName(f.getName());
        setFaction(f.getFaction());
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
            boolean nameEqual = getName().equals(((Fightable) obj).getName());
            boolean facEqual = getFaction() == ((Fightable) obj).getFaction();
            boolean idEqual = getId() == ((Fightable) obj).getId();
            boolean selectedEqual = isSelected() == ((Fightable) obj).isSelected();

            isEqual = nameEqual && facEqual && idEqual && selectedEqual;
        }

        return isEqual;
    }

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
