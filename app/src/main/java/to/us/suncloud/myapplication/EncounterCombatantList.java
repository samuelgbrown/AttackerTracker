package to.us.suncloud.myapplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

// This Combatant list is used for the encounter.  It keeps a list of Combatants that are not organized/separated (unlike the AllFactionsCombatantList) that can be reorganized easily.
// It will also perform all functions relevant to calculating initiative
public class EncounterCombatantList {
    private ArrayList<Combatant> combatantArrayList;
    private ArrayList<Integer> duplicateInitiatives; // Keep track of any initiative values that are duplicated across multiple Combatants
    private SortMethod currentSortMethod = SortMethod.ALPHABETICALLY_BY_FACTION;

    private EncounterCombatantList() {
        combatantArrayList = new ArrayList<>();
    }

    public EncounterCombatantList(ArrayList<Combatant> combatantArrayList) {
        this.combatantArrayList = new ArrayList<>(combatantArrayList.size()); // Shallow copy! (TODO CHECK: Check other copies, such as in adapter, that the memory etc lists are deep copies)
        for (int i = 0; i < combatantArrayList.size(); i++) {
            this.combatantArrayList.add(combatantArrayList.get(i).clone()); // Create a clone of the referenced Combatant, and save it
        }

        doSorting();
        updateDuplicateInitiatives();
    }

    public EncounterCombatantList(AllFactionCombatantLists factionList) {
        AllFactionCombatantLists clonedList = factionList.clone(); // First, make a cloned version of the list, so we don't affect any of the "real" copies of the Combatants
        for (int i = 0; i < clonedList.getAllFactionLists().size(); i++) {
            // For each faction, add all of the Combatants to this object's ArrayList
            combatantArrayList.addAll(clonedList.getAllFactionLists().get(i).getCombatantArrayList());
        }

        doSorting();
        updateDuplicateInitiatives();
    }

    public EncounterCombatantList(EncounterCombatantList c) {
        // Perform a deep copy of the incoming EncounterCombatantList
        combatantArrayList = new ArrayList<>(c.size());
        for (int i = 0; i < c.size(); i++) {
            combatantArrayList.add(c.get(i).clone()); // Create a clone of the Combatant, and save it to this object's ArrayList
        }

        currentSortMethod = c.getCurrentSortMethod(); // List should already be in a sorted state
        duplicateInitiatives = c.getDuplicateInitiatives(); // List should already be in a prepared state
    }

    private ArrayList<Combatant> getCombatantArrayList() {
        return combatantArrayList;
    }


    public SortMethod getCurrentSortMethod() {
        return currentSortMethod;
    }

    public ArrayList<Integer> getDuplicateInitiatives() {
        return duplicateInitiatives;
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
                Collections.sort(combatantArrayList, new CombatantSorter.SortByInitiative()); // Sort the combatantArrayList alphabetically
                return;
            case ALPHABETICALLY_BY_FACTION:
                Collections.sort(combatantArrayList, new CombatantSorter.SortAlphabeticallyByFaction()); // Sort the combatantArrayList alphabetically by faction
        }
    }

    public void rollInitiative() {
        // Roll initiative for each Combatant! (Don't worry about sorting for now, it will be called explicitly
        for (Combatant combatant : combatantArrayList) {
            combatant.rollInitiative();
        }

        // Update the duplicateInitiatives list
        updateDuplicateInitiatives();
    }

    private void updateDuplicateInitiatives() {
        // Update the duplicateInitiative array
        HashSet<Integer> existingInitiativeValues = new HashSet<>(); // Keep track of which values have already been found as initiatives in the Combatant List
        duplicateInitiatives = new ArrayList<>();
        for (Combatant c : combatantArrayList) {
            // For each Combatant, get its initiative
            int thisInit = c.getTotalInitiative();

            if (!existingInitiativeValues.contains(thisInit)) {
                // If this Combatant's initiative does NOT exist in the HashSet, then add it
                existingInitiativeValues.add(thisInit);
            } else {
                // If this Combatant's initiative DOES exist in the HashSet (another Combatant has this initiative as well), then add it to the duplicateInitiative ArrayList (if it does not exist in there already)
                if (!duplicateInitiatives.contains(thisInit)) {
                    duplicateInitiatives.add(thisInit);
                }
            }
        }

        // We now have an ArrayList<Integer> duplicateInitiatives that contains each initiative that is used by more than one Combatant
        Collections.sort(duplicateInitiatives); // Now sort the list, and we have a finished duplicateInitiatives ArrayList
    }

    public boolean isDuplicate(int i) {
        return duplicateInitiatives.contains(combatantArrayList.get(i).getTotalInitiative()); // If the list contains this Combatant's initiative, then it is a duplicate
    }

    public int getDuplicateColor(int i) {
        return duplicateInitiatives.indexOf(combatantArrayList.get(i).getTotalInitiative()) % 2; // Get the index of the Combatant's total initiative in the sorted duplicateInitiatives array, modulo 2 (so that the color alternates with increasing initiative....cuz it looks nice)
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
            Collections.sort(sortedList, new CombatantSorter.SortByInitiative());

            // Finally, find the index in the initiative sorted list of the selected Combatant
            return sortedList.indexOf(c);
        }
    }

    public enum SortMethod {
        INITIATIVE,
        ALPHABETICALLY_BY_FACTION
    }
}
