package to.us.suncloud.myapplication;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class AllFactionCombatantLists implements Serializable {
    ArrayList<FactionCombatantList> allFactionLists = new ArrayList<>();

    public ArrayList<FactionCombatantList> getAllFactionLists() {
        return allFactionLists;
    }

    AllFactionCombatantLists(ArrayList<FactionCombatantList> allFactionLists) {
        // Used as a shallow copy constructor
        // This method assumes that the inputted faction list has either 0 or 1 FactionCombatantList for each faction listed in Combatant.Faction (i.e. uniqueness)
        this.allFactionLists = allFactionLists;
        sort();
    }

    AllFactionCombatantLists() {
        allFactionLists = new ArrayList<>();
    }

    AllFactionCombatantLists(EncounterCombatantList encounterList) {
        addAll(encounterList.getCombatantArrayList()); // Add all elements in this list
    }

    AllFactionCombatantLists(AllFactionCombatantLists c) {
        // Perform a deep copy of c
        allFactionLists = new ArrayList<>(c.getAllFactionLists().size());

        for (int i = 0; i < c.getAllFactionLists().size(); i++) { // List should already be sorted
            allFactionLists.add(c.getAllFactionLists().get(i).clone()); // Create a clone of this FactionCombatantList, and save it to this list
        }

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

    public void addCombatant(Combatant newCombatant) {
        // If the Faction Lists contain a Combatant with this Combatant's name, then we must make the new Combatant's name unique.
        // First, check what the largest existing ordinal is for this Combatant's base name

        // TODO LATER: Doing these checks could be a setting? Something like "Smart naming"?  Perhaps another setting could be if we even care about name uniqueness at all!
        int highestExistingOrdinal = getHighestOrdinalInstance(newCombatant);
        switch (highestExistingOrdinal) {
            case Combatant.DOES_NOT_APPEAR:
                // If the Combatant does not appear, then we do not need to make any modifications, regardless of what ordinal newCombatant has. Cool!
                break;
            case Combatant.NO_ORDINAL:
                // If this Combatant's base name DOES appear, but with no ordinal, then that Combatant's name must be changed
                // If newCombatant has an ordinal that's smaller than 2, then this Combatant's name must be changed to 1) preserve uniqueness, and 2) make it so that "Zombie" and "Zombie 1" don't both appear in the list (because that's weird)
                getCombatant(newCombatant.getBaseName()).setNameOrdinal(1); // Find the existing Combatant with this Combatant's name, and set its ordinal to 1
                if (newCombatant.getOrdinal() < 2) {
                    newCombatant.setNameOrdinal(2); // Set the new Combatant's ordinal to 2
                    // If the new Combatant's ordinal is 2 or greater, then...well...it's not bothering anyone, I guess...
                }
                break;
            default:
                // If the Combatant's base name DOES appear in the list already with an ordinal, and the new Combatant's ordinal is already being used, then simply modify this Combatant's ordinal (if needed) to be at least one higher than the current highest ordinal.
//                if (containsName(newCombatant.getName())) {
                newCombatant.setNameOrdinal(Math.max(highestExistingOrdinal + 1, newCombatant.getOrdinal())); // If the new Combatant's ordinal is not already being used, then it was probably already a part of the List (a Combatant being re-added after being modified), so don't mess with it!
//                }
        }

        // Now, the Combatant and the list are *guaranteed* to unique to each other, and ready to have the Combatant added
        getFactionList(newCombatant.getFaction()).add(newCombatant); // If the faction list does not exist yet, getFactionList will create it
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
        ArrayList<FactionCombatantList> factionList = new ArrayList<>();
        for (int i = 0; i < allFactionLists.size(); i++) {
            factionList.add(allFactionLists.get(i).subList(indices.get(i)));
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

    int posToCombatantInd(int position) {
        // TODO SOON: Fix this!  If only party member(s) and neutral are added, neutral label is called enemy
        // Convert an adapter position to an index in combatantList_Master (adapter position will include banners)
        int viewsRemaining = position; // Keep track of how many Views have been traversed
        int returnPosition = 0; // Keep track of how many CombatantViews have been traversed

        for (int facInd = 0; facInd < allFactionLists.size(); facInd++) {
            // Go through each Faction list
            if (!allFactionLists.get(facInd).isEmpty()) {
                // If there are any Combatants in this List

                if (viewsRemaining == 0) {
                    // This is a banner, now figure out which one it is by doing a look-ahead to the next non-empty list
                    for (int lookAheadFacInd = facInd; lookAheadFacInd < allFactionLists.size(); lookAheadFacInd++) {
                        // Find the next faction that is not empty (it may be this one)
                        if (!allFactionLists.get(lookAheadFacInd).isEmpty()) {
                            return -(lookAheadFacInd + 1); // Indicate which faction's banner this is
                        }
                    }
                } else {
                    viewsRemaining--; // We have traversed one View (the banner)
                }

                if (viewsRemaining < allFactionLists.get(facInd).size()) {
                    // The position is in this array,
                    returnPosition += viewsRemaining;
                    return returnPosition;
                } else {
                    // The position is beyond this array, so traverse all Views in this Faction
                    viewsRemaining -= allFactionLists.get(facInd).size();
                    returnPosition += allFactionLists.get(facInd).size();
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

    public int sizeWithBanners() {
        int returnSize = 0;
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            if (!allFactionLists.get(fac).isEmpty()) {
                returnSize += allFactionLists.get(fac).size() + 1;
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
        // Add all combatants in this list
        for (int i = 0; i < combatantListToAdd.size(); i++) {
            addCombatant(combatantListToAdd.get(i)); // Renaming may occur if there was some kind of management error
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
            if (!allFactionLists.get(i).isEmpty()) {
                // If any faction's list is not empty, then return false
                return false;
            }
        }

        // If we get here, then all of the faction lists are empty, and we should return true
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
