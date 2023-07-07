package to.us.suncloud.myapplication;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

// Simple wrapper for a list of combatants or groups that also holds faction information
public class FactionFightableList implements Serializable {
    // TODO GROUP UPDATE: Change this below ArrayList to contain a new class, one that will be a parent to Fightables (Edit: I think I meant Combatants?) and Groups
    //      Check this class does with Fightables that this parent class must be able to do
    private ArrayList<Fightable> fightableArrayList;
    private Fightable.Faction thisFaction;

    FactionFightableList(ArrayList<Fightable> fightableArrayList, Fightable.Faction thisFaction) {
        // Used for shallow copy
        this.thisFaction = thisFaction;
        this.fightableArrayList = fightableArrayList;

        sort();
    }

    FactionFightableList(Fightable.Faction thisFaction) {
        this.thisFaction = thisFaction;
        fightableArrayList = new ArrayList<>();

        sort();
    }

    FactionFightableList(FactionFightableList c) {
        // Perform a deep copy of the FactionFightableList
        this.fightableArrayList = new ArrayList<>(c.size());
        for (int i = 0; i < c.size(); i++) {
            fightableArrayList.add(c.get(i).clone()); // For each Fightable in c, make a clone to place in this array list
        }
        this.thisFaction = c.faction();
        c.sort(); // Ensure that the fightableArrayList is sorted (which is should be already...)
    }

    // ArrayList<> interface methods
    public Fightable get(int index) {
        return fightableArrayList.get(index);
    }

    public Fightable get(String name) {
        // Return the Fightable that has the inputted name (there should only ever be one, so we'll only return the first we get).  If no such name appears in the list, return a null
        for (int i = 0; i < fightableArrayList.size(); i++) {
            if (fightableArrayList.get(i).getName().equals(name)) {
                return fightableArrayList.get(i);
            }
        }

        // If we get here, then no such Fightable exists
        return null;
    }

    public void add(Fightable newFightable) {
        fightableArrayList.add(newFightable);
        sort();
    }

    public void add(int i, Fightable newFightable) {
        fightableArrayList.add(i, newFightable);
        sort();
    }

    public void remove(Fightable fightableToRemove) {
        fightableArrayList.remove(fightableToRemove);
    }

    public FactionFightableList subList(ArrayList<Integer> subListIndices) {
        ArrayList<Fightable> subList = new ArrayList<>();
        for (int i = 0; i < subListIndices.size(); i++) {
            subList.add(fightableArrayList.get(subListIndices.get(i))); // Go through each member of subListIndices and get the Fightable at that index
        }

        // Return a new FactionFightableList for this subList
        return new FactionFightableList(subList, faction());
    }

    public FactionFightableList subListVisible(ArrayList<Integer> subListVisibleIndices) {
        ArrayList<Fightable> subList = new ArrayList<>();
        for (int subListVisIndInd = 0; subListVisIndInd < subListVisibleIndices.size(); subListVisIndInd++) {
            int thisFightableVisInd = subListVisibleIndices.get(subListVisIndInd);
            // For each Fightable that we want (in subListVisibleIndices), go through the entire fightableArrayList to see which are visible
            int visInd = 0;
            for (int arrayInd = 0; arrayInd < fightableArrayList.size(); arrayInd++) {
                // Go through the fightableArrayList, and count the number of visible Fightables we see
                if (fightableArrayList.get(arrayInd).isVisible()) {
                    // If we find a visible Fightable...
                    if (visInd == thisFightableVisInd) {
                        // If this is the index of visible Fightables that we want
                        subList.add(fightableArrayList.get(arrayInd)); // Get the Fightable at this index

                        break; // Go on to the next Fightable in subListVisibleIndices
                    } else {
                        // Otherwise, iterate the number of visible Fightables we've seen and continue
                        visInd++;
                    }
                }
            }
        }

        // Return a new FactionFightableList for this subList
        return new FactionFightableList(subList, faction());
    }

    public void remove(int fightableIndToRemove) {
        fightableArrayList.remove(fightableIndToRemove);
    }

    public void removeAll(FactionFightableList fightablesToRemove) {
        // Remove all fightables present in the inputted FactionFightableList
        fightableArrayList.removeAll(fightablesToRemove.getFightableArrayList());
    }

    public void addAll(FactionFightableList fightablesToAdd) {
        // Add all fightables present in the inputted FactionFightableList
        fightableArrayList.addAll(fightablesToAdd.getFightableArrayList());
    }

    public boolean containsName(Fightable fightableToCheck) {
        return containsName(fightableToCheck.getName());
    }

    public boolean containsName(String name) {
        // Does this List contain another Fightable with the same name?
        boolean contains = false;
        for (int i = 0; i < fightableArrayList.size(); i++) {
            // Doing comparison manually, because Collection.contains(Object o) uses the equals(Object o) method, which includes more than just the name for Fightables
            if (fightableArrayList.get(i).getName().equals(name)) {
                contains = true;
                break;
            }
        }

        return contains;
    }

    public boolean containsBaseName(Fightable fightableToCheck) {
        return containsBaseName(fightableToCheck.getBaseName());
    }

    public boolean containsBaseName(String baseName) {
        // Does this List contain another Fightable with the same base name (i.e. with no number at the end)?
        boolean contains = false;
        for (int i = 0; i < fightableArrayList.size(); i++) {
            // Doing comparison manually, because Collection.contains(Object o) uses the equals(Object o) method, which includes more than just the name for Fightables
            if (fightableArrayList.get(i).getBaseName().equals(baseName)) {
                contains = true;
                break;
            }
        }

        return contains;
    }

    public int getHighestOrdinalInstance(Fightable fightableToCheck) {
        return getHighestOrdinalInstance(fightableToCheck.getBaseName());
    }

    public int getHighestOrdinalInstance(String fightableBaseNameToCheck) {
        // What is the highest ordinal instance of this Fightable's base name in this list (i.e. for new Fightable "Zombie 3", what is the largest X for which there is a Fightable named "Zombie X" that appears in this list?
        // If no other Fightable with this base name exists, function will return a -1
        int highestOrdinal = Fightable.DOES_NOT_APPEAR;

        for (int i = 0; i < fightableArrayList.size(); i++) {
            // Doing comparison manually, because Collection.contains(Object o) uses the equals(Object o) method, which includes more than just the name for Fightables
            if (fightableArrayList.get(i).getBaseName().equals(fightableBaseNameToCheck)) {
                // Get the new highest ordinal value, between the current highest and this Fightable
                highestOrdinal = Math.max(highestOrdinal, fightableArrayList.get(i).getOrdinal());
            }
        }

        return highestOrdinal;
    }

    public int indexOf(Fightable fightable) {
        return fightableArrayList.indexOf(fightable);
    }

    public ArrayList<Fightable> getFightableArrayList() {
        return fightableArrayList;
    }

    public ArrayList<Combatant> getCombatantArrayList() {
        // Get only the Combatants in the array list
        ArrayList<Combatant> combatantArrayList = new ArrayList<>();
        for (Fightable f : fightableArrayList) {
            if (f instanceof Combatant) {
                combatantArrayList.add((Combatant) f);
            }
        }

        return combatantArrayList;
    }

    public void setFightableArrayList(ArrayList<Fightable> fightableArrayList) {
        this.fightableArrayList = fightableArrayList;
        sort();
    }

    public void sort() {
        // Sort the fightableArrayList alphabetically
        Collections.sort(fightableArrayList, new FightableSorter.SortAlphabeticallyByFaction());
    }

    public ArrayList<String> getFightableNamesList() {
        ArrayList<String> allFightableNames = new ArrayList<>();
        for (int cIndex = 0; cIndex < fightableArrayList.size(); cIndex++) {
            // For each Fightable, add the Fightable's name to allFightableNames
            allFightableNames.add(fightableArrayList.get(cIndex).getName());
        }

        return allFightableNames;
    }

    public Fightable.Faction faction() {
        return thisFaction;
    }

    public void setThisFaction(Fightable.Faction thisFaction) {
        this.thisFaction = thisFaction;
    }

    public boolean isEmpty() {
        return fightableArrayList.isEmpty();
    }

    public boolean isVisibleEmpty() {
        return visibleSize() == 0;
    }

    public int size() {
        return fightableArrayList.size();
    }

    public int visibleSize() {
        // Count the number of visible Fightables in the fightableArrayList
        int returnSize = 0;
        for (Fightable c : fightableArrayList) {
            returnSize += c.isVisible() ? 1 : 0;
        }

        return returnSize;
    }

    public FactionFightableList clone() {
        return new FactionFightableList(this);
    }

    public FactionFightableList shallowCopy() {
        return new FactionFightableList(getFightableArrayList(), faction());
    }

    public ArrayList<Integer> getIndicesThatMatch(String text) {
        // Get indices in this List of Fightables whose name contains this text
        ArrayList<Integer> indices = new ArrayList<>();
        int visInd = 0;
        for (int i = 0; i < fightableArrayList.size(); i++) {
            Fightable c = fightableArrayList.get(i);
            if (c.isVisible()) {
                if (text.isEmpty() || c.getName().toLowerCase().contains(text)) {
                    // If the Fightable is visible and it matches the text String
                    indices.add(visInd); // Add the index (relative to the indices of visible Fightables)
                }
                visInd++; // Regardless, we have traversed one visible Fightable
            }
        }

        return indices;
    }


}
