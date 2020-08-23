package to.us.suncloud.myapplication;

import android.service.autofill.FieldClassification;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A simple class to keep track of a combatant
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
    private UUID id = UUID.randomUUID();

    private static Random rand = new Random(); // A random number generator (static, so that each Combatant uses the same one, and it does not just use the system clock as a first time seed each time

    // Create a regex Pattern for finding the base name of a Combatant
    private static Pattern ordinalChecker = Pattern.compile("^(.*?)(?:\\W*(\\d++)|$)"); // A pattern that matches the Combatant name into the first group, and the Combatant's ordinal number (if it exists) into the second group

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
        // Copy constructor (used for cloning)
        faction = c.getFaction();
        name = c.getName();
        iconIndex = c.getIconIndex();
        speedFactor = c.getSpeedFactor();
        roll = c.getRoll();
        totalInitiative = c.getTotalInitiative();
        id = c.getId();
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
        // Useful for quickly getting a "sanitized" version of the combatant (clears the roll/total initiative, gets rid of the name ordinal, if it exists)
        Combatant rawCombatant = clone();

        rawCombatant.clearRoll();
        setName(getBaseName());
        return rawCombatant;
    }

    //
    // Simple Setters
    //
    public void setName(String name) {
        // Note: Be careful when using this function, name exclusivity must be enforced elsewhere (it will not be checked here)
        this.name = name;
    }

    public int getSpeedFactor() {
        return speedFactor;
    }

    public void setSpeedFactor(int speedFactor) {
        this.speedFactor = speedFactor;
    }

    //
    // Advanced Setters
    //
    public void setRoll(int roll) {
        this.roll = roll;

        // Set the total initiative
        calcTotalInitiative();
    }

    public void rollInitiative() {
        // Find a pseudorandom number between 1 and 20, assign it to the roll member variable, and re-calculate the total initiative
        setRoll(rand.nextInt(20) + 1); // Calculate a random number between [0 20), and adjust it so it produces a roll between [1 20]
    }

    private void calcTotalInitiative() {
        // Recalculate the total initiative
        this.totalInitiative = speedFactor + this.roll;

        // TODO: This calculation can be changed according to a setting?
    }

    public void setNameOrdinal(int ordinal) {
        // Set the ordinal number of the name
        setName(getBaseName() + " " + String.valueOf(ordinal));
    }

    public void clearRoll() {
        setRoll(0);
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
            boolean speedEqual = getSpeedFactor() == ((Combatant) obj).getSpeedFactor();
            boolean rollEqual = getRoll() == ((Combatant) obj).getRoll();
            boolean totalEqual = getTotalInitiative() == ((Combatant) obj).getTotalInitiative();
            boolean idEqual = getId() == ((Combatant) obj).getId();

            isEqual = nameEqual && facEqual &&iconEqual && speedEqual && rollEqual && totalEqual && idEqual;
        }

        return isEqual;
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

    public static boolean isNameUnique(String nameToTest, ArrayList<FactionCombatantList> listOfAllCombatants) {
        // First, generate an ArrayList of Strings of all combatants
        ArrayList<String> allCombatantNames = new ArrayList<>();
        for (int fclIndex = 0; fclIndex < listOfAllCombatants.size(); fclIndex++) {
            // For each faction, add the combatant names list to allCombatantNames
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
                return INIT_NAME + "" + String.valueOf(highestOrdinalInstance + 1);
        }
    }

    public static String generateUniqueName(ArrayList<String> listOfAllCombatantNames) {
        // Try making a name unique to this list
        boolean isUnique = false;
        int curSuffix = 2;
        String currentNameSelection = String.valueOf(INIT_NAME);

        while (!isUnique) {
            isUnique = !listOfAllCombatantNames.contains(currentNameSelection); // See if the combatant name list contains this name

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

    enum Faction {
        Party,
        Enemy,
        Neutral
    }
}
