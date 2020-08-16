package to.us.suncloud.myapplication;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;

// A simple class to keep track of a combatant
public class Combatant implements Serializable {
    // TODO: Enforce uniqueness by name (for enemies, should ALWAYS be different, e.g. "Zombie 1", "Zombie 2"...)
    static final String INIT_NAME = "New Combatant";
    private Faction faction = Faction.Party;
    private String name;
    private int speedFactor = 0;
    private int roll = 0;
    private int totalInitiative = 0;

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
        Enemy,
        Neutral
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
        return generateUniqueName(listOfAllCombatants.getCombatantNamesList());
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
}
