package to.us.suncloud.myapplication;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A simple class to keep track of a Combatant
public class Combatant implements Serializable {
    // Enforce uniqueness by name (for enemies, should ALWAYS be different, e.g. "Zombie 1", "Zombie 2"...), although the UUID is used for actual unique identification
    private static final String INIT_NAME = "New Combatant";
    public static final int DOES_NOT_APPEAR = -2; // If, upon a search for highestOrdinalInstance among a list of Combatants, the given base name does not appear
    public static final int NO_ORDINAL = -1; // If, upon a search for highestOrdinalInstance among a list of Combatants, the given base name does appear, but it has no ordinal (i.e. "Zombie" exists, but not "Zombie 1")

    private Faction faction = Faction.Party;
    private String name; // Name will be initialized on construction
    private int iconIndex = 0; // Initialize with a blank icon
    private int speedFactor = 0;
    private int roll = 0;
    private int totalInitiative = 0;
    private boolean isSelected = false; // Is the Combatant selected or checked off?
    private boolean isVisible = true; // Is this Combatant visible in the initiative order?
    private UUID id = UUID.randomUUID();

    // Create a regex Pattern for finding the base name of a Combatant
    private static final Pattern ordinalChecker = Pattern.compile("^(.*?)(?:\\W*(\\d++)|$)"); // A pattern that matches the Combatant name into the first group, and the Combatant's ordinal number (if it exists) into the second group

    // Constructors
    public Combatant(AllFactionCombatantLists listOfAllCombatants) {
        // Require all CombatantLists to enforce uniqueness across all lists

        // Once we have found a unique name, set it as this Combatant's name
        setName(generateUniqueName(listOfAllCombatants));
    }

    public Combatant(ArrayList<String> listOfAllCombatantNames) {
        // Require all CombatantLists to enforce uniqueness across all lists

        // Once we have found a unique name, set it as this Combatant's name
        setName(generateUniqueName(listOfAllCombatantNames));
    }

    public Combatant(Combatant c) {
        // Copy constructor (used for cloning) - make an EXACT clone of this Combatant (careful about Combatant uniqueness!)
        faction = c.getFaction();
        name = c.getName();
        iconIndex = c.getIconIndex();
        speedFactor = c.getModifier();
        roll = c.getRoll();
        totalInitiative = c.getTotalInitiative();
        id = c.getId();
        isSelected = c.isSelected();
        isVisible = c.isVisible();
    }

    //
    // Simple Getters
    //
    public UUID getId() {
        return id;
    }

    public int getRoll() {
        return roll;
    }

    public int getTotalInitiative() {
        return totalInitiative;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public void setIconIndex(int iconIndex) {
        this.iconIndex = iconIndex;
    }

    public int getIconIndex() {
        return iconIndex;
    }

    public String getName() {
        return name;
    }

    //
    // Advanced Getters
    //
    public String getBaseName() {
        // Get the name of this Combatant without any number at the end
        Matcher match = ordinalChecker.matcher(name);
        if (match.matches()) {
            return match.group(1); // Get the first matched group (that isn't the full match), which corresponds to the Combatant's base name (without any ordinals)
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
            Log.e("Combatant", "Could not calculate ordinal of name " + name);
            return NO_ORDINAL;
        }
    }

    public Combatant getRaw() {
        // Useful for quickly getting a "sanitized" version of the Combatant (clears the roll/total initiative, gets rid of the name ordinal, if it exists, clears isSelected)
        Combatant rawCombatant = clone();

        // Set a few values for the new Combatant
        rawCombatant.clearRoll();
        rawCombatant.setName(getBaseName());
        rawCombatant.genUUID();
        rawCombatant.setSelected(false);
        return rawCombatant;
    }

    //
    // Simple Setters
    //
    public void setName(String name) {
        // Note: Be careful when using this function, name exclusivity must be enforced elsewhere (it will not be checked here)
        this.name = name;
    }

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

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    private void calcTotalInitiative() {
        // Recalculate the total initiative
        this.totalInitiative = speedFactor + this.roll;
    }

    public void setNameOrdinal(int ordinal) {
        // Set the ordinal number of the name
        setName(getBaseName() + " " + ordinal);
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
            boolean nameEqual = getName().equals(((Combatant) obj).getName());
            boolean facEqual = getFaction() == ((Combatant) obj).getFaction();
            boolean iconEqual = getIconIndex() == ((Combatant) obj).getIconIndex();
            boolean speedEqual = getModifier() == ((Combatant) obj).getModifier();
            boolean rollEqual = getRoll() == ((Combatant) obj).getRoll();
            boolean totalEqual = getTotalInitiative() == ((Combatant) obj).getTotalInitiative();
            boolean idEqual = getId() == ((Combatant) obj).getId();
            boolean selectedEqual = isSelected == ((Combatant) obj).isSelected();

            isEqual = nameEqual && facEqual &&iconEqual && speedEqual && rollEqual && totalEqual && idEqual && selectedEqual;
        }

        return isEqual;
    }

    public boolean rawEquals(@Nullable Object obj) {
        // Check if the Combatants are the same, for data saving purposes
        boolean isEqual = false;
        if (obj instanceof Combatant) {
            boolean nameEqual = getName().equals(((Combatant) obj).getName());
            boolean facEqual = getFaction() == ((Combatant) obj).getFaction();
            boolean iconEqual = getIconIndex() == ((Combatant) obj).getIconIndex();

            isEqual = nameEqual && facEqual && iconEqual;
        }

        return isEqual;
    }

    public boolean displayEquals(@Nullable Object obj) {
        // Check if the Combatants are the same, for RecyclerView viewing purpose
        boolean isEqual = false;
        if (obj instanceof Combatant) {
            boolean selectedEqual = isSelected == ((Combatant) obj).isSelected();

            isEqual = rawEquals(obj) && selectedEqual;
        }

        return isEqual;
    }

    public void displayCopy(Combatant c) {
        // Copy the display values from the incoming Combatant (NOT selection)
        setName(c.getName());
        setFaction(c.getFaction());
        setIconIndex(c.getIconIndex());
        setModifier(c.getModifier());
    }

    public static String factionToString(Faction faction) {
        switch (faction) {
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

    public void genUUID() {
        // Generate a new UUID for this Combatant
        id = UUID.randomUUID();
    }

    public static boolean isNameUnique(String nameToTest, ArrayList<FactionCombatantList> listOfAllCombatants) {
        // First, generate an ArrayList of Strings of all combatants
        ArrayList<String> allCombatantNames = new ArrayList<>();
        for (int fclIndex = 0; fclIndex < listOfAllCombatants.size(); fclIndex++) {
            // For each faction, add the Combatant names list to allCombatantNames
            allCombatantNames.addAll(listOfAllCombatants.get(fclIndex).getCombatantNamesList());
        }

        // Then, see if the supplied name is unique to the supplied list
        return !allCombatantNames.contains(nameToTest);
    }

    public static String generateUniqueName(AllFactionCombatantLists listOfAllCombatants) {
//        return generateUniqueName(listOfAllCombatants.getCombatantNamesList());
        int highestOrdinalInstance = listOfAllCombatants.getHighestOrdinalInstance(INIT_NAME);
        switch (highestOrdinalInstance) {
            case DOES_NOT_APPEAR:
                // No other Combatant appears with this name
                return INIT_NAME;
            case NO_ORDINAL:
                // There is a Combatant with this name, but no other ordinal (i.e. "Zombie" appears, but not "Zombie 2").
                // NOTE: When adding a Combatant with this new name, we will likely want to also rename the old Combatant to give it an ordinal (i.e. "Zombie" becomes "Zombie 1")
                return INIT_NAME + "  2";
            default:
                // There is a Combatant with this name already, so add one to the highest ordinal number
                return INIT_NAME + "" + (highestOrdinalInstance + 1);
        }
    }

    public static String generateUniqueName(ArrayList<String> listOfAllCombatantNames) {
        // Try making a name unique to this list
        boolean isUnique = false;
        int curSuffix = 2;
        String currentNameSelection = INIT_NAME;

        while (!isUnique) {
            isUnique = !listOfAllCombatantNames.contains(currentNameSelection); // See if the Combatant name list contains this name

            if (!isUnique) {
                // Try to make the name unique
                currentNameSelection = INIT_NAME + " " + curSuffix;
                curSuffix++; // Iterate curSuffix in case we need to try to make the name unique again
            }
        }

        return currentNameSelection;
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

    enum Faction {
        Party,
        Enemy,
        Neutral
    }
}
