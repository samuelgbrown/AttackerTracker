package to.us.suncloud.myapplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

// This Combatant list is used for the encounter.  It keeps a list of Combatants that are not organized/separated (unlike the AllFactionsCombatantList) that can be reorganized easily.
// It will also perform all functions relevant to calculating initiative
public class EncounterCombatantList {
    private ArrayList<Combatant> combatantArrayList;
    private SortMethod currentSortMethod = SortMethod.ALPHABETICALLY_BY_FACTION;

    private EncounterCombatantList() {
        combatantArrayList = new ArrayList<>();
    }

    public EncounterCombatantList(ArrayList<Combatant> combatantArrayList) {
        this.combatantArrayList = new ArrayList<>(combatantArrayList.size()); // Shallow copy! (TODO CHECK: Check other copies, such as in adapter, that the memory etc lists are deep copies)
        for (int i = 0;i < combatantArrayList.size();i++) {
            this.combatantArrayList.add(combatantArrayList.get(i).clone()); // Create a clone of the referenced Combatant, and save it
        }

        doSorting();
    }

    public EncounterCombatantList(AllFactionCombatantLists factionList) {
        AllFactionCombatantLists clonedList = factionList.clone(); // First, make a cloned version of the list, so we don't affect any of the "real" copies of the Combatants
        for (int i = 0;i <clonedList.getAllFactionLists().size();i++) {
            // For each faction, add all of the Combatants to this object's ArrayList
            combatantArrayList.addAll(clonedList.getAllFactionLists().get(i).getCombatantArrayList());
        }

        doSorting();
    }

    public EncounterCombatantList (EncounterCombatantList c) {
        // Perform a deep copy of the incoming EncounterCombatantList
        combatantArrayList = new ArrayList<>(c.size());
        for (int i = 0;i < c.size();i++) {
            combatantArrayList.add(c.get(i).clone()); // Create a clone of the Combatant, and save it to this object's ArrayList
        }

        currentSortMethod = c.getCurrentSortMethod(); // List should already be in a sorted state
    }

    private ArrayList<Combatant> getCombatantArrayList() {
        return combatantArrayList;
    }


    public SortMethod getCurrentSortMethod() {
        return currentSortMethod;
    }

    public void sort(SortMethod sortMethod) {
        // Sort according to some rule
        currentSortMethod = sortMethod;
        doSorting();
    }

    public void reSort() {
        // Sort the list again, using the current sort method
        doSorting();
    }

    private void doSorting() {
        // Depending on the current sorting style (currentSortMethod), sort the contents of combatantArrayList
        switch (currentSortMethod) {
            case INITIATIVE:
                Collections.sort(combatantArrayList, new SortByInitiative()); // Sort the combatantArrayList alphabetically
                return;
            case ALPHABETICALLY_BY_FACTION:
                Collections.sort(combatantArrayList, new SortAlphabeticallyByFaction()); // Sort the combatantArrayList alphabetically by faction
        }
    }

    public void rollInitiative() {
        // Roll initiative for each Combatant! (Don't worry about sorting for now, it will be called explicitly
        for (Combatant combatant : combatantArrayList) {
            combatant.rollInitiative();
        }

        // TODO: Add an "update duplicate initiative array" function, which will be called here, and whenever the roll or speed factors are updated [rolls and speed factors should only be updated through this object].  This will populate an ArrayList of Integers that have any Total Initiative values that are duplicated.  The total initiative value's location in the duplicate initiative array will denote its color (or perhaps the pos % 2 will denote its color?)
    }

    public int size() {
        return combatantArrayList.size();
    }

    public Combatant get(int i) {
        return combatantArrayList.get(i);
    }

    public EncounterCombatantList clone() {
        return new EncounterCombatantList(this);
    }

    public int getInitiativeIndexOf(int indexInCurrentList) {
        // A method to find the index in combatantArrayList of a Combatant when the list is sorted by initiative, regardless of the actual sorted state of the list
        if (currentSortMethod == SortMethod.INITIATIVE) {
            // If the array is already sorted by initiative, then just return the input index
            return indexInCurrentList;
        } else {
            // The combatantArrayList is currently sorted alphabetically_by_faction, so we're going to need to sort the list, then get the index of the Combatant
            Combatant c = combatantArrayList.get(indexInCurrentList); // Get the Combatant at the indicated point in the current lis
            ArrayList<Combatant> sortedList = new ArrayList<>(combatantArrayList); // Create a SHALLOW copy of the list (the Combatants themselves are not cloned, only their references are copied)

            // Do initiative sorting on the sortedList
            Collections.sort(sortedList, new SortByInitiative());

            // Finally, find the index in the initiative sorted list of the selected Combatant
            return sortedList.indexOf(c);
        }
    }

    public enum SortMethod {
        INITIATIVE,
        ALPHABETICALLY_BY_FACTION
    }

    static private class SortByInitiative implements Comparator<Combatant> {
        @Override
        public int compare(Combatant combatant, Combatant t1) {
            int cTI = combatant.getTotalInitiative();
            int t1TI = t1.getTotalInitiative();
            // TODO: This comparison method can be changed according to which version we're using by using a method (perhaps set the method in the Constructor of this object?
            if (cTI != t1TI) {
                // If the total initiatives are different, then it's a simple sort
                return cTI - t1TI;
            } else {
                //  If the total initiatives are the same, then sort according to the SortAlphabeticallyByFaction class
                return new SortAlphabeticallyByFaction().compare(combatant, t1);
            }
        }
    }

    static private class SortAlphabeticallyByFaction implements Comparator<Combatant> {
        static private int factionToInt(Combatant.Faction f) {
            switch (f) {
                case Party:
                    return 0;
                case Enemy:
                    return 1;
                case Neutral:
                    return 2;
                default:
                    return 10;
            }
        }

        @Override
        public int compare(Combatant combatant, Combatant t1) {
            int fC = factionToInt(combatant.getFaction());
            int fT1 = factionToInt(t1.getFaction());
            if (fC != fT1) {
                // If the Factions are different, then just make sure they are sorted Party < Enemy < Neutral
                return fC - fT1;
            } else {
                // If the Factions are the same, sort alphabetically
                return combatant.getName().compareToIgnoreCase(t1.getName());
            }
        }


    }
}
