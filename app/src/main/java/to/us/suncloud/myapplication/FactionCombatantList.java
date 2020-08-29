package to.us.suncloud.myapplication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

// Simple wrapper for a list of combatants that also holds faction information
public class FactionCombatantList implements Serializable {
    private ArrayList<Combatant> combatantArrayList;
    private Combatant.Faction thisFaction;

    FactionCombatantList(ArrayList<Combatant> combatantArrayList, Combatant.Faction thisFaction) {
        // Used for shallow copy
        this.thisFaction = thisFaction;
        this.combatantArrayList = combatantArrayList;

        sort();
    }

    FactionCombatantList(Combatant.Faction thisFaction) {
        this.thisFaction = thisFaction;
        combatantArrayList = new ArrayList<>();

        sort();
    }

    FactionCombatantList(FactionCombatantList c) {
        // Perform a deep copy of the FactionCombatantList
        this.combatantArrayList = new ArrayList<>(c.size());
        for (int i = 0; i < c.size(); i++) {
            combatantArrayList.add(c.get(i).clone()); // For each Combatant in c, make a clone to place in this array list
        }
        this.thisFaction = c.faction();
        c.sort(); // Ensure that the combatantArrayList is sorted (which is should be already...)
    }

    // ArrayList<> interface methods
    public Combatant get(int index) {
        return combatantArrayList.get(index);
    }

    public Combatant get(String name) {
        // Return the Combatant that has the inputted name (there should only ever be one, so we'll only return the first we get).  If no such name appears in the list, return a null
        for (int i = 0; i < combatantArrayList.size(); i++) {
            if (combatantArrayList.get(i).getName().equals(name)) {
                return combatantArrayList.get(i);
            }
        }

        // If we get here, then no such Combatant exists
        return null;
    }

    public void add(Combatant newCombatant) {
        combatantArrayList.add(newCombatant);
        sort();
    }

    public void add(int i, Combatant newCombatant) {
        combatantArrayList.add(i, newCombatant);
        sort();
    }

    public void remove(Combatant combatantToRemove) {
        combatantArrayList.remove(combatantToRemove);
    }

    public FactionCombatantList subList(ArrayList<Integer> subListIndices) {
        ArrayList<Combatant> subList = new ArrayList<>();
        for (int i = 0; i < subListIndices.size(); i++) {
            subList.add(combatantArrayList.get(subListIndices.get(i))); // Go through each member of subListIndices and get the Combatant at that index
        }

        // Return a new FactionCombatantList for this subList
        return new FactionCombatantList(subList, faction());
    }

    public void remove(int combatantIndToRemove) {
        combatantArrayList.remove(combatantIndToRemove);
    }

    public void removeAll(FactionCombatantList combatantsToRemove) {
        // Remove all combatants present in the inputted FactionCombatantList
        combatantArrayList.removeAll(combatantsToRemove.getCombatantArrayList());
    }

    public void addAll(FactionCombatantList combatantsToAdd) {
        // Add all combatants present in the inputted FactionCombatantList
        combatantArrayList.addAll(combatantsToAdd.getCombatantArrayList());
    }

    public boolean containsName(Combatant combatantToCheck) {
        return containsName(combatantToCheck.getName());
    }

    public boolean containsName(String name) {
        // Does this List contain another Combatant with the same name?
        boolean contains = false;
        for (int i = 0; i < combatantArrayList.size(); i++) {
            // Doing comparison manually, because Collection.contains(Object o) uses the equals(Object o) method, which includes more than just the name for Combatants
            if (combatantArrayList.get(i).getName().equals(name)) {
                contains = true;
                break;
            }
        }

        return contains;
    }

    public boolean containsBaseName(Combatant combatantToCheck) {
        return containsBaseName(combatantToCheck.getBaseName());
    }

    public boolean containsBaseName(String baseName) {
        // Does this List contain another Combatant with the same base name (i.e. with no number at the end)?
        boolean contains = false;
        for (int i = 0; i < combatantArrayList.size(); i++) {
            // Doing comparison manually, because Collection.contains(Object o) uses the equals(Object o) method, which includes more than just the name for Combatants
            if (combatantArrayList.get(i).getBaseName().equals(baseName)) {
                contains = true;
                break;
            }
        }

        return contains;
    }

    public int getHighestOrdinalInstance(Combatant combatantToCheck) {
        return getHighestOrdinalInstance(combatantToCheck.getBaseName());
    }

    public int getHighestOrdinalInstance(String combatantBaseNameToCheck) {
        // What is the highest ordinal instance of this Combatant's base name in this list (i.e. for new Combatant "Zombie 3", what is the largest X for which there is a Combatant named "Zombie X" that appears in this list?
        // If no other Combatant with this base name exists, function will return a -1
        int highestOrdinal = Combatant.DOES_NOT_APPEAR;

        for (int i = 0; i < combatantArrayList.size(); i++) {
            // Doing comparison manually, because Collection.contains(Object o) uses the equals(Object o) method, which includes more than just the name for Combatants
            if (combatantArrayList.get(i).getBaseName().equals(combatantBaseNameToCheck)) {
                // Get the new highest ordinal value, between the current highest and this Combatant
                highestOrdinal = Math.max(highestOrdinal, combatantArrayList.get(i).getOrdinal());
            }
        }

        return highestOrdinal;
    }

    public int indexOf(Combatant combatant) {
        return combatantArrayList.indexOf(combatant);
    }

    public ArrayList<Combatant> getCombatantArrayList() {
        return combatantArrayList;
    }

    public void setCombatantArrayList(ArrayList<Combatant> combatantArrayList) {
        this.combatantArrayList = combatantArrayList;
        sort();
    }

    public void sort() {
        // Sort the CombatantArrayList alphabetically
        Collections.sort(combatantArrayList, new CombatantSorter.SortAlphabeticallyByFaction());
    }

    public ArrayList<String> getCombatantNamesList() {
        ArrayList<String> allCombatantNames = new ArrayList<>();
        for (int cIndex = 0; cIndex < combatantArrayList.size(); cIndex++) {
            // For each combatant, add the combatant's name to allCombatantNames
            allCombatantNames.add(combatantArrayList.get(cIndex).getName());
        }

        return allCombatantNames;
    }

    public Combatant.Faction faction() {
        return thisFaction;
    }

    public void setThisFaction(Combatant.Faction thisFaction) {
        this.thisFaction = thisFaction;
    }

    public boolean isEmpty() {
        return combatantArrayList.isEmpty();
    }

    public int size() {
        return combatantArrayList.size();
    }

    public FactionCombatantList clone() {
        return new FactionCombatantList(this);
    }

    public FactionCombatantList shallowCopy() {
        return new FactionCombatantList(getCombatantArrayList(), faction());
    }

    public ArrayList<Integer> getIndicesThatMatch(String text) {
        // Get indices in this List of Combatants whose name contains this text
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < combatantArrayList.size(); i++) {
            if (text.isEmpty() || combatantArrayList.get(i).getName().toLowerCase().contains(text)) {
                indices.add(i);
            }
        }

        return indices;
    }


}
