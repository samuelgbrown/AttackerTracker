package to.us.suncloud.myapplication;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class AllFactionCombatantLists implements Serializable {
    ArrayList<FactionCombatantList> allFactionLists = new ArrayList<>();

    AllFactionCombatantLists(ArrayList<FactionCombatantList> allFactionLists) {
        // Used as a shallow copy constructor
        // This method assumes that the inputted faction list has either 0 or 1 FactionCombatantList for each faction listed in Combatant.Faction (i.e. uniqueness)
        this.allFactionLists = allFactionLists;
        initFactionLists();
    }

    AllFactionCombatantLists() {
        initFactionLists();
    }

    AllFactionCombatantLists(EncounterCombatantList encounterList) {
        addAll(encounterList.getCombatantArrayList(), true, true); // Add all elements in this list, with force set to true (so no modifications occur)

        initFactionLists();
    }

    AllFactionCombatantLists(AllFactionCombatantLists c) {
        // Perform a deep copy of c
        allFactionLists = new ArrayList<>(c.getAllFactionLists().size());

        for (int i = 0; i < c.getAllFactionLists().size(); i++) { // List should already be sorted
            allFactionLists.add(c.getAllFactionLists().get(i).clone()); // Create a clone of this FactionCombatantList, and save it to this list
        }

        initFactionLists();
    }

    private void initFactionLists() {
        // Initialize the Faction Lists
        // Ensure that there is a FactionCombatantList for each Faction
        Combatant.Faction[] allFacs = Combatant.Faction.values();
        for (Combatant.Faction fac : allFacs) {
            if (!containsFaction(fac)) {
                // If this Faction is not represented in the list, then add an empty one
                allFactionLists.add(new FactionCombatantList(fac));
            }
        }

        // Sort all of the Faction lists
        sort();
    }

    public ArrayList<FactionCombatantList> getAllFactionLists() {
        return allFactionLists;
    }


//    public void addFactionCombatantList(FactionCombatantList listToAdd) {
//        // Add a new faction Combatant list, if the faction isn't already in this list
//        if (!containsFaction(listToAdd.faction())) {
//            allFactionLists.add(listToAdd);
//        }
//
//        // Sort the List
//        sort();
//    }

    public boolean addCombatant(Combatant newCombatant) {
        // The standard addCombatant call, assumes that the newCombatant is a completely new addition to the Combatant list
        return addCombatant(newCombatant, false, false);
    }

    public boolean addCombatant(Combatant newCombatant, boolean newCombatantIsModifiedExistingCombatant) {
        return addCombatant(newCombatant, newCombatantIsModifiedExistingCombatant, false);
    }

    public boolean addCombatant(Combatant newCombatant, boolean newCombatantIsModifiedExistingCombatant, boolean force) {
        // If the Faction Lists contain a Combatant with this Combatant's name, then we must make the new Combatant's name unique.
        // First, check what the largest existing ordinal is for this Combatant's base name
        // TO_DO LATER: Doing these checks could be a setting? Something like "Smart naming"?  Perhaps another setting could be if we even care about name uniqueness at all!
        // KNOWN BUG: Known minor bug: If two version of a Combatant are added, one without ordinal, and the non-ordinal Combatant is deleted (saved), and THEN the user adds a new version and decides to copy (not resurrect), then subsequent added copies also bring up the resurrect/copy dialog.  The horror.
        // TODO START HERE: Name bug! (Yay...) If there are 3 ordinal Combatants (1,2,3) that have been in the encounter, 2 is deleted, 3 is copied, and (The new) 4 is renamed to 2
        //  Expected behavior: Old version of 2 will be resurrected
        //  Actual behavior: A second 3 appears
        int highestExistingOrdinal = getHighestOrdinalInstance(newCombatant);
        if (highestExistingOrdinal != Combatant.DOES_NOT_APPEAR) {
            // If the Combatant's name does appear, first check if there is an exact match to a Combatant that is invisible
            if (containsName(newCombatant.getName())) {
                // If the list contains an exact match for this Combatant, see if that Combatant is invisible
                Combatant existingCombatant = getCombatant(newCombatant.getName());
                if (!(existingCombatant == null) && !existingCombatant.isVisible()) {
                    // If the matching Combatant is invisible, then make it visible, and update the Combatant with the new Combatant's values
                    //  This will likely only occur if a Combatant is removed and then reenters combat
                    if (force) {
                        if (newCombatantIsModifiedExistingCombatant) {
                            // If this is an existing Combatant, then just modify the existing one
                            existingCombatant.setVisible(true);
                            existingCombatant.displayCopy(newCombatant);
                            return true; // We don't need to add the Combatant anymore, we've already "added" it.  Return success
                        }

                        // If this is meant to be a brand new Combatant, then continue on to the next block, to modify both this and the existing Combatant as needed to make sure their names are distinct
                    } else {
                        return false; // Do nothing, ask for user input
                    }
                }
            }

            // If force is set, then add the Combatant without modification (we are probably getting it back from the Encounter, so no renaming should occur)
            // If there is no exact match to an invisible Combatant, then the names must be changed somehow...
            if (highestExistingOrdinal == Combatant.NO_ORDINAL) {
                if (!force) {
                    // If this Combatant's base name DOES appear, but with no ordinal, AND we aren't forcing then that Combatant's name must be changed
                    // If newCombatant has an ordinal that's smaller than 2, then this Combatant's name must be changed to 1) preserve uniqueness, and 2) make it so that "Zombie" and "Zombie 1" don't both appear in the list (because that's weird)
                    getCombatant(newCombatant.getBaseName()).setNameOrdinal(1); // Find the existing Combatant with this Combatant's name, and set its ordinal to 1
                    if (newCombatant.getOrdinal() < 2) {
                        newCombatant.setNameOrdinal(2); // Set the new Combatant's ordinal to 2
                        // If the new Combatant's ordinal is 2 or greater, then...well...it's not bothering anyone, I guess...
                    }
                }
            } else {
                // If the Combatant's base name DOES appear in the list already with an ordinal, and the new Combatant's ordinal is already being used...

                if (newCombatantIsModifiedExistingCombatant) {
                    // If this new Combatant is the result of a modification of an existing Combatant...
                    if (containsName(newCombatant.getName())) {
                        // If the list contains an EXACT match to this name, which is visible, then calmly explain to the user that they're an idiot
                        return false;
                    }

                    // If the list does not contain this name, then...sure, why not.  User can do whatever they want, and we don't need to modify anything
                } else {
                    // ...then simply modify this Combatant's ordinal (if needed) to be at least one higher than the current highest ordinal
                    newCombatant.setNameOrdinal(Math.max(highestExistingOrdinal + 1, newCombatant.getOrdinal())); // If the new Combatant's ordinal is not already being used, then it was probably already a part of the List (a Combatant being re-added after being modified), so don't mess with it!
                }
            }
        }

        // Now, the Combatant and the list are *guaranteed* to unique to each other, and ready to have the Combatant added
        getFactionList(newCombatant.getFaction()).add(newCombatant); // If the faction list does not exist yet, getFactionList will create it

        return true;  // Return success
    }

    public ArrayList<ArrayList<Integer>> getIndicesThatMatch(String text) {
        // Return a List of Lists of Integers that indicate which Combatants in allFactionLists contain the filter text
        ArrayList<ArrayList<Integer>> indices = new ArrayList<>();
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            indices.add(allFactionLists.get(fac).getIndicesThatMatch(text));
        }

        return indices;
    }

    public AllFactionCombatantLists subList(ArrayList<ArrayList<Integer>> indices) {
        // Return a sub-list with only the Combatants indexed in indices
        ArrayList<FactionCombatantList> factionList = new ArrayList<>();
        for (int i = 0; i < allFactionLists.size(); i++) {
            factionList.add(allFactionLists.get(i).subList(indices.get(i)));
        }

        return new AllFactionCombatantLists(factionList);
    }

    public AllFactionCombatantLists subListVisible(ArrayList<ArrayList<Integer>> indices) {
        // Return a sub-list with only the Combatants indexed in indices, where the indices are relative only to visible Combatants
        ArrayList<FactionCombatantList> factionList = new ArrayList<>();
        for (int i = 0; i < allFactionLists.size(); i++) {
            factionList.add(allFactionLists.get(i).subListVisible(indices.get(i)));
        }

        return new AllFactionCombatantLists(factionList);
    }

    public Combatant get(int combatantInd) {
        // Get the Combatant at the indicated index
        int originalCombatantInd = combatantInd;
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            if (combatantInd < allFactionLists.get(fac).size()) {
                return allFactionLists.get(fac).get(combatantInd); // Get this index in allFactionLists
            }

            combatantInd -= allFactionLists.get(fac).size(); // Subtract the size of the faction list, to check the next Faction
        }

        throw new IndexOutOfBoundsException("Index " + originalCombatantInd + ", Size " + size()); // The Combatant index is out of bounds
    }

    public Combatant getFromVisible(int desiredCombatantInd, ArrayList<ArrayList<Integer>> filteredIndices) {
        // Get a Combatant using the ind, selected only from visible Combatants
        int curLoc = 0;
        for (int facInd = 0; facInd < filteredIndices.size(); facInd++) {
            // For each Faction...

            // Initialize our counting variables
            int filterIndInd = 0;
            int visInd = 0;

            // Go through each Combatant
            for (int combatantInd = 0; combatantInd < allFactionLists.get(facInd).size(); combatantInd++) {
                // For each Combatant in this Faction...

                if (allFactionLists.get(facInd).get(combatantInd).isVisible()) {
                    // We have found the visInd'th visible Combatant

                    if (visInd == filteredIndices.get(facInd).get(filterIndInd)) {
                        // We have found the filteredIndices.get(facInd).get(filterIndInd)'th visible Combatant

                        if (curLoc == desiredCombatantInd) {
                            // If this is the Combatant that we want, then return it
                            return allFactionLists.get(facInd).get(combatantInd);
                        } else {
                            // Record that we've traversed one visible, filtered Combatant
                            curLoc++;
                        }

                        // Finalize
                        filterIndInd++; // We have encountered one visible Combatant that was in the filter list

                        if (filterIndInd >= filteredIndices.get(facInd).size()) {
                            // If we have exhausted all of the visible Combatants in this Faction that are within the filter, then don't bother looking through the rest
                            break;
                        }
                    }
                    visInd++; // We have encountered one visible Combatant
                }
            }
        }

        throw new IndexOutOfBoundsException("Index " + desiredCombatantInd + ", Size " + size()); // The Combatant index is out of bounds
    }

    int posToCombatantInd(int position) {
        // Convert an adapter position to an index in combatantList_Master (adapter position will include banners)
        int viewsRemaining = position; // Keep track of how many Views have been traversed
        int returnPosition = 0; // Keep track of how many CombatantViews have been traversed

        for (int facInd = 0; facInd < allFactionLists.size(); facInd++) {
            // Go through each Faction list
            if (!allFactionLists.get(facInd).isVisibleEmpty()) {
                // If there are any Combatants in this List

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
        throw new IndexOutOfBoundsException("Index " + position); // The Combatant index is out of bounds
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
        // Get the number of Combatants
        int returnSize = 0;
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            returnSize += allFactionLists.get(fac).size();
        }

        return returnSize;
    }

    public int visibleSize() {
        // Get the number of visible Combatants
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
                returnSize += allFactionLists.get(fac).visibleSize() + 1; // If this Faction has visible Combatants in it, then count them up and add one for the banner
            }
        }

        return returnSize;
    }

    public int getHighestOrdinalInstance(Combatant combatantToCheck) {
        return getHighestOrdinalInstance(combatantToCheck.getBaseName());
    }

    public int getHighestOrdinalInstance(String combatantBaseName) {
        // Get the highest ordinal instance of the baseName among all of the Faction lists
        int highestOrdinal = Combatant.DOES_NOT_APPEAR;
        for (int i = 0; i < allFactionLists.size(); i++) {
            // Go through each Faction, and get the highest ordinal instance of this base name
            highestOrdinal = Math.max(highestOrdinal, allFactionLists.get(i).getHighestOrdinalInstance(combatantBaseName));
        }

        return highestOrdinal;
    }

    public void removeAll(AllFactionCombatantLists combatantListToRemove) {
        // Remove all combatants present in the inputted AllFactionCombatantLists
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            // For each faction in this list...
            // Get the Combatants associated with this Faction
            FactionCombatantList thisFactionCombatantsToRemove = combatantListToRemove.getFactionList(allFactionLists.get(fac).faction());

            // Remove all of these Combatants
            allFactionLists.get(fac).removeAll(thisFactionCombatantsToRemove);
        }
    }

    public void remove(Combatant combatantToRemove) {
        if (getFactionList(combatantToRemove.getFaction()).containsName(combatantToRemove)) {
            // If the associated FactionCombatantList has this Combatant, remove it
            getFactionList(combatantToRemove.getFaction()).remove(combatantToRemove);
        }
    }

    public void addAll(AllFactionCombatantLists combatantListToAdd) {
        // Add all combatants present in the inputted AllFactionCombatantLists
        for (int fac = 0; fac < combatantListToAdd.getAllFactionLists().size(); fac++) {
            // For each faction in the new list...
            // Get the Combatants associated with this Faction
            FactionCombatantList thisFactionCombatantsToAdd = combatantListToAdd.getAllFactionLists().get(fac);

            // Add all of these Combatants
            getFactionList(thisFactionCombatantsToAdd.faction()).addAll(thisFactionCombatantsToAdd); // If the FactionCombatant list doesn't exist yet, then the getFactionList() method will create it
        }
    }

    public void addAll(ArrayList<Combatant> combatantListToAdd) {
        addAll(combatantListToAdd, false);
    }


    public void addAll(ArrayList<Combatant> combatantListToAdd, boolean newCombatantIsModifiedExistingCombatant) {
        addAll(combatantListToAdd, newCombatantIsModifiedExistingCombatant, false);
    }

    public void addAll(ArrayList<Combatant> combatantListToAdd, boolean newCombatantIsModifiedExistingCombatant, boolean force) {
        // Add all combatants in this list
        for (int i = 0; i < combatantListToAdd.size(); i++) {
//            addCombatant(combatantListToAdd.get(i)); // (OLD: shallow copy)Renaming may occur if there was some kind of management error
            addCombatant(combatantListToAdd.get(i).clone(), newCombatantIsModifiedExistingCombatant, force); // Renaming may occur if there was some kind of management error
        }
    }

    public FactionCombatantList getFactionList(Combatant.Faction faction) {
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (allFactionLists.get(i).faction() == faction) {
                return allFactionLists.get(i);
            }
        }

        // If we got here, then none of the faction lists match the desired faction, and we need to return an empty one.
        FactionCombatantList newList = new FactionCombatantList(faction);
        allFactionLists.add(newList);

        // Sort the List
        sort();

        return newList;
    }

    public Combatant getCombatant(String name) {
        // Return the Combatant that has the inputted name (there should only ever be one, so we'll only return the first we get).  If no such name appears in the list, return a null
        for (int i = 0; i < allFactionLists.size(); i++) {
            Combatant thisCombatant = allFactionLists.get(i).get(name);
            if (thisCombatant != null) {
                return thisCombatant;
            }
        }

        // If we get here, then no such Combatant exists
        return null;
    }

    public boolean containsFaction(Combatant.Faction factionToCheck) {
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

    public ArrayList<String> getCombatantNamesList() {
        ArrayList<String> allCombatantNames = new ArrayList<>();
        for (int cIndex = 0; cIndex < allFactionLists.size(); cIndex++) {
            // For each faction Combatant list, add all Combatant names to allCombatantNames
            allCombatantNames.addAll(allFactionLists.get(cIndex).getCombatantNamesList());
        }

        return allCombatantNames;
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

    public AllFactionCombatantLists clone() {
        return new AllFactionCombatantLists(this);
    } // Deep copy

    public AllFactionCombatantLists shallowCopy() {
        return new AllFactionCombatantLists(getAllFactionLists());
    } // Shallow copy

    public void sort() {
        // Sort the Faction lists according to the order we would like to see them on screen
        // If the order should be changed, change the order of the constants defined in the enum Combatant.Faction
        Collections.sort(allFactionLists, new CombatantSorter.SortFactionList());
    }

    public void sortAllLists() {
        // Sort all of the Faction lists by Alphabetical order
        for (FactionCombatantList list : allFactionLists) {
            list.sort();
        }
    }

    public AllFactionCombatantLists getRawCopy() {
        // Get a copy of this List that contains "Raw" version of all Combatants (only Name, Faction, and Icon can be non-default)
        AllFactionCombatantLists newList = new AllFactionCombatantLists();
        for (int i = 0; i < size(); i++) {
            newList.addCombatant(get(i).getRaw());
        }

        return newList;
    }

    public boolean rawEquals(@Nullable Object obj) {
        // Check if the Combatant lists are the same, for data saving purposes
        boolean isEqual = false;
        if (obj instanceof AllFactionCombatantLists) {
            AllFactionCombatantLists objRaw = ((AllFactionCombatantLists) obj).getRawCopy();
            AllFactionCombatantLists thisRaw = getRawCopy();
            return thisRaw.equals(objRaw);
        }

        return isEqual;
    }
}
