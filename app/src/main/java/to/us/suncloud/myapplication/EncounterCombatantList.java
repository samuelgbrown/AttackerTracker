package to.us.suncloud.myapplication;

import java.util.ArrayList;

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
        // TODO: Sort according to some rule
        currentSortMethod = sortMethod;
        doSorting();
    }

    private void doSorting() {
        // TODO: Depending on the current sorting style (currentSortMethod), sort the contents of combatantArrayList
        // TODO: Use sortByInit(ArrayList<Combatant>) and sortByAlpha(ArrayList<Combatant>), or similar (so that getInitiativeIndex has access to initiative sorting to use on a different ArrayList)
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

    public int getInitiativeIndexOf(Combatant c) {
        // A method to find the index in combatantArrayList of a Combatant when the list is sorted by initiative, regardless of the actual sorted state of the list
        if (currentSortMethod == SortMethod.INITIATIVE) {
            // If the array is already sorted by initiative, then just get the index of the Combatant
            return combatantArrayList.indexOf(c);
        } else {
            // We're going to need to sort the list, then get the index of the Combatant
            ArrayList<Combatant> sortedList = new ArrayList<>(combatantArrayList); // Create a SHALLOW copy of the list (the Combatants themselves are not cloned, only their references are copied)

            // TODO: Do initiative sorting on the sortedList

            return sortedList.indexOf(c);
        }
    }


    public enum SortMethod {
        INITIATIVE,
        ALPHABETICALLY_BY_FACTION
    }
}
