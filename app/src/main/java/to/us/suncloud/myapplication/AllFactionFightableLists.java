package to.us.suncloud.myapplication;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class AllFactionFightableLists implements Serializable {
    ArrayList<FactionFightableList> allFactionLists = new ArrayList<>();

    AllFactionFightableLists(ArrayList<FactionFightableList> allFactionLists) {
        // Used as a shallow copy constructor
        // This method assumes that the inputted faction list has either 0 or 1 FactionFightableList for each faction listed in Fightable.Faction (i.e. uniqueness)
        this.allFactionLists = allFactionLists;
        initFactionLists();
    }

    AllFactionFightableLists() {
        initFactionLists();
    }

    AllFactionFightableLists(EncounterCombatantList encounterList) {
        addAll((ArrayList<Fightable>) ((ArrayList<?>) encounterList.getCombatantArrayList()), true, true); // Add all elements in this list, with force set to true (so no modifications occur)

        initFactionLists();
    }

    AllFactionFightableLists(AllFactionFightableLists c) {
        // Perform a deep copy of c
        allFactionLists = new ArrayList<>(c.getAllFactionLists().size());

        for (int i = 0; i < c.getAllFactionLists().size(); i++) { // List should already be sorted
            allFactionLists.add(c.getAllFactionLists().get(i).clone()); // Create a clone of this FactionFightableList, and save it to this list
        }

        initFactionLists();
    }

    private void initFactionLists() {
        // Initialize the Faction Lists
        // Ensure that there is a FactionFightableList for each Faction
        Fightable.Faction[] allFacs = Fightable.Faction.values();
        for (Fightable.Faction fac : allFacs) {
            if (!containsFaction(fac)) {
                // If this Faction is not represented in the list, then add an empty one
                allFactionLists.add(new FactionFightableList(fac));
            }
        }

        // Sort all of the Faction lists
        sort();
    }

    public ArrayList<FactionFightableList> getAllFactionLists() {
        return allFactionLists;
    }


//    public void addFactionFightableList(FactionFightableList listToAdd) {
//        // Add a new faction Fightable list, if the faction isn't already in this list
//        if (!containsFaction(listToAdd.faction())) {
//            allFactionLists.add(listToAdd);
//        }
//
//        // Sort the List
//        sort();
//    }

    public boolean addFightable(Fightable newFightable) {
        // The standard addFightable call, assumes that the newFightable is a completely new addition to the Fightable list
        return addFightable(newFightable, false, false);
    }

    public boolean addFightable(Fightable newFightable, boolean newFightableIsModifiedExistingFightable) {
        return addFightable(newFightable, newFightableIsModifiedExistingFightable, false);
    }

    public boolean addFightable(Fightable newFightable, boolean newFightableIsModifiedExistingFightable, boolean force) {
        // If the Faction Lists contain a Fightable with this Fightable's name, then we must make the new Fightable's name unique.
        // First, check what the largest existing ordinal is for this Fightable's base name
        // TO_DO LATER: Doing these checks could be a setting? Something like "Smart naming"?  Perhaps another setting could be if we even care about name uniqueness at all!
        // KNOWN BUG: Known minor bug: If two version of a Fightable are added, one without ordinal, and the non-ordinal Fightable is deleted (saved), and THEN the user adds a new version and decides to copy (not resurrect), then subsequent added copies also bring up the resurrect/copy dialog.  The horror.
        // Note on force:  In first half of function, "force" refers to forcing the Fightable to be added despite there being an old deleted version of it.  In second half, force refers to forcing the new Fightable's name to be unchanged.
        int highestExistingOrdinal = getHighestOrdinalInstance(newFightable);
        if (highestExistingOrdinal != Fightable.DOES_NOT_APPEAR) {
            // If the Fightable's name does appear, first check if there is an exact match to a Fightable that is invisible
            if (containsName(newFightable.getName())) {
                // If the list contains an exact match for this Fightable, see if that Fightable is invisible
                Fightable existingFightable = getFightable(newFightable.getName());
                if (!(existingFightable == null) && !existingFightable.isVisible()) {
                    // If the matching Fightable is invisible, then make it visible, and update the Fightable with the new Fightable's values
                    //  This will likely only occur if a Fightable is removed and then reenters combat
                    if (force) {
                        if (newFightableIsModifiedExistingFightable) {
                            // If this is an existing Fightable, then just modify the existing one
                            existingFightable.setVisible(true);
                            existingFightable.displayCopy(newFightable);
                            sortAllLists(); // After setting the Fightable to be visible, sort the lists (sorting depends partially on visibility, and the list must be in order to display properly)
                            return true; // We don't need to add the Fightable anymore, we've already "added" it.  Return success
                        } else {
                            force = false; // We've passed the check for matching the name of a visible Fightable.  Turn off "force" because, after this line, "force" refers to us wanting to force no name change for this Fightable (which is not what we want)
                            // Continue, and rename this and the existing Fightable according to proper ordinal
                        }

                        // If this is meant to be a brand new Fightable, then continue on to the next block, to modify both this and the existing Fightable as needed to make sure their names are distinct
                    } else {
                        return false; // Do nothing, ask for user input
                    }
                }
            }

            // If force is set, then add the Fightable without modification (we are probably getting it back from the Encounter or after a name modification, so no renaming should occur)
            // If there is no exact match to an invisible Fightable, then the names must be changed somehow...
            if (highestExistingOrdinal == Fightable.NO_ORDINAL) {
                if (!force) {
                    // If this Fightable's base name DOES appear, but with no ordinal, AND we aren't forcing then that Fightable's name must be changed
                    // If newFightable has an ordinal that's smaller than 2, then this Fightable's name must be changed to 1) preserve uniqueness, and 2) make it so that "Zombie" and "Zombie 1" don't both appear in the list (because that's weird)
                    getFightable(newFightable.getBaseName()).setNameOrdinal(1); // Find the existing Fightable with this Fightable's name, and set its ordinal to 1
                    if (newFightable.getOrdinal() < 2) {
                        newFightable.setNameOrdinal(2); // Set the new Fightable's ordinal to 2
                        // If the new Fightable's ordinal is 2 or greater, then...well...it's not bothering anyone, I guess...
                    }
                }
            } else {
                // If the Fightable's base name DOES appear in the list already with an ordinal, and the new Fightable's ordinal is already being used...

                if (newFightableIsModifiedExistingFightable) {
                    // If this new Fightable is the result of a modification of an existing Fightable...
                    if (containsName(newFightable.getName())) {
                        // If the list contains an EXACT match to this name, which is visible, then calmly explain to the user that they're an idiot
                        return false;
                    }

                    // If the list does not contain this name, then...sure, why not.  User can do whatever they want, and we don't need to modify anything
                } else {
                    // ...then simply modify this Fightable's ordinal (if needed) to be at least one higher than the current highest ordinal
                    newFightable.setNameOrdinal(Math.max(highestExistingOrdinal + 1, newFightable.getOrdinal())); // If the new Fightable's ordinal is not already being used, then it was probably already a part of the List (a Fightable being re-added after being modified), so don't mess with it!
                }
            }
        }

        // Now, the Fightable and the list are *guaranteed* to unique to each other, and ready to have the Fightable added
        getFactionList(newFightable.getFaction()).add(newFightable); // If the faction list does not exist yet, getFactionList will create it

        return true;  // Return success
    }

    public ArrayList<ArrayList<Integer>> getIndicesThatMatch(String text) {
        // Return a List of Lists of Integers that indicate which Fightables in allFactionLists contain the filter text
        ArrayList<ArrayList<Integer>> indices = new ArrayList<>();
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            indices.add(allFactionLists.get(fac).getIndicesThatMatch(text));
        }

        return indices;
    }

    public AllFactionFightableLists subList(ArrayList<ArrayList<Integer>> indices) {
        // Return a sub-list with only the Fightables indexed in indices
        ArrayList<FactionFightableList> factionList = new ArrayList<>();
        for (int i = 0; i < allFactionLists.size(); i++) {
            factionList.add(allFactionLists.get(i).subList(indices.get(i)));
        }

        return new AllFactionFightableLists(factionList);
    }

    public AllFactionFightableLists subListVisible(ArrayList<ArrayList<Integer>> indices) {
        // Return a sub-list with only the Fightables indexed in indices, where the indices are relative only to visible Fightables
        ArrayList<FactionFightableList> factionList = new ArrayList<>();
        for (int i = 0; i < allFactionLists.size(); i++) {
            factionList.add(allFactionLists.get(i).subListVisible(indices.get(i)));
        }

        return new AllFactionFightableLists(factionList);
    }

    public Fightable get(int fightableInd) {
        // Get the Fightable at the indicated index
        int originalFightableInd = fightableInd;
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            if (fightableInd < allFactionLists.get(fac).size()) {
                return allFactionLists.get(fac).get(fightableInd); // Get this index in allFactionLists
            }

            fightableInd -= allFactionLists.get(fac).size(); // Subtract the size of the faction list, to check the next Faction
        }

        throw new IndexOutOfBoundsException("Index " + originalFightableInd + ", Size " + size()); // The Fightable index is out of bounds
    }

    public Fightable getFightable(int fightableInd) {
        return get(fightableInd);
    }

    public Fightable getFromVisible(int desiredFightableInd, ArrayList<ArrayList<Integer>> filteredIndices) {
        // Get a Fightable using the ind, selected only from visible Fightables
        int curLoc = 0;
        for (int facInd = 0; facInd < filteredIndices.size(); facInd++) {
            // For each Faction...

            // Initialize our counting variables
            int filterIndInd = 0;
            int visInd = 0;

            // Go through each Fightable
            for (int fightableInd = 0; fightableInd < allFactionLists.get(facInd).size(); fightableInd++) {
                // For each Fightable in this Faction...

                if (allFactionLists.get(facInd).get(fightableInd).isVisible()) {
                    // We have found the visInd'th visible Fightable

                    if (visInd == filteredIndices.get(facInd).get(filterIndInd)) {
                        // We have found the filteredIndices.get(facInd).get(filterIndInd)'th visible Fightable

                        if (curLoc == desiredFightableInd) {
                            // If this is the Fightable that we want, then return it
                            return allFactionLists.get(facInd).get(fightableInd);
                        } else {
                            // Record that we've traversed one visible, filtered Fightable
                            curLoc++;
                        }

                        // Finalize
                        filterIndInd++; // We have encountered one visible Fightable that was in the filter list

                        if (filterIndInd >= filteredIndices.get(facInd).size()) {
                            // If we have exhausted all of the visible Fightables in this Faction that are within the filter, then don't bother looking through the rest
                            break;
                        }
                    }
                    visInd++; // We have encountered one visible Fightable
                }
            }
        }

        throw new IndexOutOfBoundsException("Index " + desiredFightableInd + ", Size " + size()); // The Fightable index is out of bounds
    }

    public Fightable getFightableFromVisible(int desiredFightableInd, ArrayList<ArrayList<Integer>> filteredIndices) {
        return getFromVisible(desiredFightableInd, filteredIndices);
    }

    int posToFightableInd(int position) {
        // Convert an adapter position to an index in fightableList_Master (adapter position will include banners)
        int viewsRemaining = position; // Keep track of how many Views have been traversed
        int returnPosition = 0; // Keep track of how many FightableViews have been traversed

        for (int facInd = 0; facInd < allFactionLists.size(); facInd++) {
            // Go through each Faction list
            if (!allFactionLists.get(facInd).isVisibleEmpty()) {
                // If there are any Fightables in this List

                if (viewsRemaining == 0) {
                    // This is a banner, now figure out which one it is by doing a look-ahead to the next non-empty list
                    for (int lookAheadFacInd = facInd; lookAheadFacInd < allFactionLists.size(); lookAheadFacInd++) {
                        // Find the next faction that is not empty (it may be this one)
                        if (!allFactionLists.get(lookAheadFacInd).isVisibleEmpty()) {
                            return -(lookAheadFacInd + 1); // Indicate which faction's banner this is
                        }
                    }
                } else {
                    viewsRemaining--; // We have traversed one View (the banner)
                }

                if (viewsRemaining < allFactionLists.get(facInd).visibleSize()) {
                    // The position is in this array,
                    returnPosition += viewsRemaining;
                    return returnPosition;
                } else {
                    // The position is beyond this array, so traverse all Views in this Faction
                    viewsRemaining -= allFactionLists.get(facInd).visibleSize();
                    returnPosition += allFactionLists.get(facInd).visibleSize();
                }
            }
        }
        throw new IndexOutOfBoundsException("Index " + position); // The Fightable index is out of bounds
    }


    public boolean containsName(String name) {
        boolean contains = false;
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (allFactionLists.get(i).containsName(name)) {
                contains = true;
                break;
            }
        }

        return contains;
    }

    public int size() {
        // Get the number of Fightables
        int returnSize = 0;
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            returnSize += allFactionLists.get(fac).size();
        }

        return returnSize;
    }

    public int visibleSize() {
        // Get the number of visible Fightables
        int returnSize = 0;
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            returnSize += allFactionLists.get(fac).visibleSize();
        }

        return returnSize;
    }

    public int sizeWithBanners() {
        int returnSize = 0;
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            if (!allFactionLists.get(fac).isVisibleEmpty()) {
//                returnSize += allFactionLists.get(fac).size() + 1;
                returnSize += allFactionLists.get(fac).visibleSize() + 1; // If this Faction has visible Fightables in it, then count them up and add one for the banner
            }
        }

        return returnSize;
    }

    public int getHighestOrdinalInstance(Fightable fightableToCheck) {
        return getHighestOrdinalInstance(fightableToCheck.getBaseName());
    }

    public int getHighestOrdinalInstance(String fightableBaseName) {
        // Get the highest ordinal instance of the baseName among all of the Faction lists
        int highestOrdinal = Fightable.DOES_NOT_APPEAR;
        for (int i = 0; i < allFactionLists.size(); i++) {
            // Go through each Faction, and get the highest ordinal instance of this base name
            highestOrdinal = Math.max(highestOrdinal, allFactionLists.get(i).getHighestOrdinalInstance(fightableBaseName));
        }

        return highestOrdinal;
    }

    public void removeAll(AllFactionFightableLists fightableListToRemove) {
        // Remove all Fightables present in the inputted AllFactionFightableLists
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            // For each faction in this list...
            // Get the Fightables associated with this Faction
            FactionFightableList thisFactionFightablesToRemove = fightableListToRemove.getFactionList(allFactionLists.get(fac).faction());

            // Remove all of these Fightables
            allFactionLists.get(fac).removeAll(thisFactionFightablesToRemove);
        }
    }

    public void remove(Fightable fightableToRemove) {
        if (getFactionList(fightableToRemove.getFaction()).containsName(fightableToRemove)) {
            // If the associated FactionFightableList has this Fightable, remove it
            getFactionList(fightableToRemove.getFaction()).remove(fightableToRemove);
        }
    }

    public void addAll(AllFactionFightableLists fightableListToAdd) {
        // Add all fightables present in the inputted AllFactionFightableLists
        for (int fac = 0; fac < fightableListToAdd.getAllFactionLists().size(); fac++) {
            // For each faction in the new list...
            // Get the Fightables associated with this Faction
            FactionFightableList thisFactionFightablesToAdd = fightableListToAdd.getAllFactionLists().get(fac);

            // Add all of these Fightables
            getFactionList(thisFactionFightablesToAdd.faction()).addAll(thisFactionFightablesToAdd); // If the FactionFightable list doesn't exist yet, then the getFactionList() method will create it
        }
    }

    public void addAll(ArrayList<Fightable> fightableListToAdd) {
        addAll(fightableListToAdd, false);
    }


    public void addAll(ArrayList<Fightable> fightableListToAdd, boolean newFightableIsModifiedExistingFightable) {
        addAll(fightableListToAdd, newFightableIsModifiedExistingFightable, false);
    }

    public void addAll(ArrayList<Fightable> fightableListToAdd, boolean newFightableIsModifiedExistingFightable, boolean force) {
        // Add all fightables in this list
        for (int i = 0; i < fightableListToAdd.size(); i++) {
//            addFightable(fightableListToAdd.get(i)); // (OLD: shallow copy)Renaming may occur if there was some kind of management error
            addFightable(fightableListToAdd.get(i).clone(), newFightableIsModifiedExistingFightable, force); // Renaming may occur if there was some kind of management error
        }
    }

    public FactionFightableList getFactionList(Fightable.Faction faction) {
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (allFactionLists.get(i).faction() == faction) {
                return allFactionLists.get(i);
            }
        }

        // If we got here, then none of the faction lists match the desired faction, and we need to return an empty one.
        FactionFightableList newList = new FactionFightableList(faction);
        allFactionLists.add(newList);

        // Sort the List
        sort();

        return newList;
    }

    public Fightable getFightable(String name) {
        // Return the Fightable that has the inputted name (there should only ever be one, so we'll only return the first we get).  If no such name appears in the list, return a null
        for (int i = 0; i < allFactionLists.size(); i++) {
            Fightable thisFightable = allFactionLists.get(i).get(name);
            if (thisFightable != null) {
                return thisFightable;
            }
        }

        // If we get here, then no such Fightable exists
        return null;
    }

    public boolean containsFaction(Fightable.Faction factionToCheck) {
        // This method will check if the provided faction is in the list
        boolean containsFaction = false;
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (allFactionLists.get(i).faction() == factionToCheck) {
                containsFaction = true;
                break;
            }
        }

        return containsFaction;
    }

    public ArrayList<String> getFightableNamesList() {
        ArrayList<String> allFightableNames = new ArrayList<>();
        for (int cIndex = 0; cIndex < allFactionLists.size(); cIndex++) {
            // For each faction Fightable list, add all Fightable names to allFightableNames
            allFightableNames.addAll(allFactionLists.get(cIndex).getFightableNamesList());
        }

        return allFightableNames;
    }

    public boolean isEmpty() {
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (!(allFactionLists.get(i).size() == 0)) {
                // If any faction's list is not empty, then return false
                return false;
            }
        }

        // If we get here, then all of the faction lists are empty, and we should return true
        return true;
    }

    public boolean isVisibleEmpty() {
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (!allFactionLists.get(i).isVisibleEmpty()) {
                // If any faction's list is not visibly empty, then return false
                return false;
            }
        }

        // If we get here, then all of the faction lists are visibly empty, and we should return true
        return true;
    }

    public AllFactionFightableLists clone() {
        return new AllFactionFightableLists(this);
    } // Deep copy

    public AllFactionFightableLists shallowCopy() {
        return new AllFactionFightableLists(getAllFactionLists());
    } // Shallow copy

    public void sort() {
        // Sort the Faction lists according to the order we would like to see them on screen
        // If the order should be changed, change the order of the constants defined in the enum Fightable.Faction
        Collections.sort(allFactionLists, new FightableSorter.SortFactionList());
    }

    public void sortAllLists() {
        // Sort all of the Faction lists by Alphabetical order
        for (FactionFightableList list : allFactionLists) {
            list.sort();
        }
    }

    public AllFactionFightableLists getRawCopy() {
        // Get a copy of this List that contains "Raw" version of all Fightables (only Name, Faction, and Icon can be non-default)
        AllFactionFightableLists newList = new AllFactionFightableLists();
        for (int i = 0; i < size(); i++) {
            newList.addFightable(get(i).getRaw());
        }

        return newList;
    }

    public boolean rawEquals(@Nullable Object obj) {
        // Check if the Fightable lists are the same, for data saving purposes
        boolean isEqual = false;
        if (obj instanceof AllFactionFightableLists) {
            AllFactionFightableLists objRaw = ((AllFactionFightableLists) obj).getRawCopy();
            AllFactionFightableLists thisRaw = getRawCopy();
            return thisRaw.equals(objRaw);
        }

        return isEqual;
    }
}
