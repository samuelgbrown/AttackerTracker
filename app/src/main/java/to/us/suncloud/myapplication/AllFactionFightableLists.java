package to.us.suncloud.myapplication;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class AllFactionFightableLists implements Serializable {
    private static final String TAG = "AllFactionFightableLists";

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
        addAll( (ArrayList<Fightable>) ((ArrayList<?>) encounterList.getCombatantArrayList()) ); // Add all elements in this list, with force set to true (so no modifications occur)
    }

    AllFactionFightableLists(AllFactionFightableLists c) {
        // Perform a deep copy of c
        allFactionLists = new ArrayList<>(c.getAllFactionLists().size());

        for (int i = 0; i < c.getAllFactionLists().size(); i++) { // List should already be sorted
            allFactionLists.add(c.getAllFactionLists().get(i).clone()); // Create a clone of this FactionFightableList, and save it to this list
        }

        initFactionLists();
    }

    public AllFactionFightableLists(JSONObject jsonObject) {
        initFactionLists();
        fromJSON( jsonObject );
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

    public boolean
    addOrModifyFightable(Fightable newFightable ) {
        // Now, the Fightable and the list are *guaranteed* to unique to each other, and ready to have the Fightable added
        Fightable oldFightable = getFightableWithID( newFightable.getId() );
        boolean newFightableIsModifiedExistingFightable = oldFightable != null;
        if ( newFightableIsModifiedExistingFightable ) {
                // If modifying an existing Fightable, first remove the Fightable from the old list
                getFactionList(oldFightable.getFaction()).remove(oldFightable);
        }

        // Returns true if there are no name collisions
        boolean isSuccessful = getFactionList(newFightable.getFaction()).addFightable(newFightable); // If the faction list does not exist yet, getFactionList will create it

        if ( isSuccessful && newFightableIsModifiedExistingFightable ) {
            // Notify any CombatantGroups that this Fightable has changed
            verifyAllCombatantGroups();
        }

        return isSuccessful;
    }

    public void verifyAllCombatantGroups() {
        // Go through each CombatantGroup in this list, and notify it that a change has occurred
        // (can handle removals and faction changes, though it is slower than manually notifying of removals)
        Iterator<Fightable> i = getFactionList(Fightable.Faction.Group).getFightableArrayList().iterator();
        while ( i.hasNext() ) {
            Fightable fightableGroup = i.next();
            if (fightableGroup instanceof CombatantGroup) {
                ((CombatantGroup) fightableGroup).verifyGroupAgainstList(this);
                if ( ((CombatantGroup) fightableGroup).size() == 0 ) {
                    i.remove();
                }
            }
        }
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

                    if ( !filteredIndices.get(facInd).isEmpty() ) {
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
                    }
                    visInd++; // We have encountered one visible Fightable
                }
            }
        }

        throw new IndexOutOfBoundsException("Index " + desiredFightableInd + ", Size " + size()); // The Fightable index is out of bounds
    }

    public boolean isFightableAGroup(int desiredFightableInd, ArrayList<ArrayList<Integer>> filteredIndices) {
        boolean isCombatantGroup = false;

        // Selecting only from visible Fightables, see if this Fightable is a Group
        int curLoc = 0;
        int facInd = 0; // We are only doing the Group Faction for this purpose!
        FactionFightableList groupFactionList = getFactionList(Fightable.Faction.Group);

        // Initialize our counting variables
        int filterIndInd = 0;

        // Go through each Fightable
        for (int fightableInd = 0; fightableInd < groupFactionList.size(); fightableInd++) {
            // For each Fightable in this Faction...

                if ( !filteredIndices.get(facInd).isEmpty() ) {
                    if (fightableInd == filteredIndices.get(facInd).get(filterIndInd)) {
                        // We have found the filteredIndices.get(facInd).get(filterIndInd)'th Fightable

                        if (curLoc == desiredFightableInd) {
                            // We found the Combatant within this Faction, so it must be a group
                            isCombatantGroup = true;
                            break;
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
                }
        }


        return isCombatantGroup;
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

                int thisFacVisibleSize = allFactionLists.get(facInd).visibleSize();
                if (viewsRemaining < thisFacVisibleSize) {
                    // The position is in this array...
                    return returnPosition + viewsRemaining;
                } else {
                    // The position is beyond this array, so traverse all Views in this Faction
                    viewsRemaining -= thisFacVisibleSize;
                    returnPosition += thisFacVisibleSize;
                }
            }
        }
        throw new IndexOutOfBoundsException("Index " + position); // The Fightable index is out of bounds
    }

    public boolean containsFightableOfTypeWithName(String name, Fightable.Faction thisFaction) {
        boolean returnVal;
        if ( thisFaction == Fightable.Faction.Group ) {
            returnVal = containsGroupWithName(name);
        } else {
            returnVal = containsCombatantWithName(name);
        }

        return returnVal;
    }

    public boolean containsCombatantWithName(String name) {
        // Check ONLY non-Group faction lists
        boolean contains = false;
        for (int i = 0; i < allFactionLists.size(); i++) {
            FactionFightableList thisList = allFactionLists.get(i);
            if ( thisList.faction() != Fightable.Faction.Group ) {
                if (thisList.containsName(name)) {
                    contains = true;
                    break;
                }
            }
        }

        return contains;
    }

    public boolean containsCombatantWithBaseName(String name) {
        // Check ONLY non-Group faction lists
        boolean contains = false;
        for (int i = 0; i < allFactionLists.size(); i++) {
            FactionFightableList thisList = allFactionLists.get(i);
            if ( thisList.faction() != Fightable.Faction.Group ) {
                if (thisList.containsBaseName(name)) {
                    contains = true;
                    break;
                }
            }
        }

        return contains;
    }

    public boolean containsGroupWithName(String name) {
        // Check ONLY Group faction list
        return getFactionList(Fightable.Faction.Group).containsName(name);
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

    public int numFactionLists() {
        return allFactionLists.size();
    }

    public int getHighestOrdinalInstance(Fightable fightableToCheck) {
        return getHighestOrdinalInstance(fightableToCheck.getBaseName(),
                fightableToCheck instanceof Combatant, false);
    }


    public int getHighestVisibleOrdinalInstance(Fightable fightableToCheck) {
        return getHighestOrdinalInstance(fightableToCheck.getBaseName(),
                fightableToCheck instanceof Combatant, true);
    }

    public int getHighestOrdinalInstance(String fightableBaseName, boolean isCombatant) {
        return getHighestOrdinalInstance( fightableBaseName, isCombatant, false );
    }
    public int getHighestOrdinalInstance(String fightableBaseName, boolean isCombatant, boolean needVisible) {
        // Get the highest ordinal instance of the baseName among all of the Faction lists
        int highestOrdinal = Fightable.DOES_NOT_APPEAR;
        for (int i = 0; i < allFactionLists.size(); i++) {
            FactionFightableList thisFactionList = allFactionLists.get(i);
            // Enforce uniqueness between Combatants and between Groups, but not across these two categories
            if (( isCombatant && (thisFactionList.faction() != Fightable.Faction.Group) ) ||
                    ( !isCombatant && (thisFactionList.faction() == Fightable.Faction.Group) )) {
                // Go through each Faction, and get the highest ordinal instance of this base name
                if ( needVisible ) {
                    highestOrdinal = Math.max(highestOrdinal, thisFactionList.getHighestVisibleOrdinalInstance(fightableBaseName));
                } else {
                    highestOrdinal = Math.max(highestOrdinal, thisFactionList.getHighestOrdinalInstance(fightableBaseName));
                }
            }
        }

        return highestOrdinal;
    }

    public void remove(Fightable fightableToRemove) {
        if (getFactionList(fightableToRemove.getFaction()).containsName(fightableToRemove)) {
            // If the associated FactionFightableList has this Fightable, remove it
            FactionFightableList thisList = getFactionList(fightableToRemove.getFaction());
            thisList.remove(fightableToRemove);

            if ( fightableToRemove.getFaction() != Fightable.Faction.Group ) {
                // Groups cannot contain other groups
                for (Iterator<Fightable> i = getFactionList(Fightable.Faction.Group).getFightableArrayList().iterator(); i.hasNext(); ) {
                    Fightable groupFightable = i.next();
                    if (groupFightable instanceof CombatantGroup) {
                        // Remove fightableToRemove from the CombatantGroup groupFightable (if it exists in that group)
                        boolean groupStillHasMembers = ((CombatantGroup) groupFightable).removeCombatant(fightableToRemove.getId(), this);
                        if ( !groupStillHasMembers ) {
                            // Group is now empty and we shouldn't preserve it, so remove it
                            i.remove();
                        }
                    }
                }
            }
        }
    }

    public void addAll(AllFactionFightableLists fightableListToAdd) {
        // Add all fightables present in the inputted AllFactionFightableLists
        for (int fightableInd = 0; fightableInd < fightableListToAdd.size(); fightableInd++) {
            // Add all of these Fightables
            addOrModifyFightable(fightableListToAdd.get(fightableInd).clone());
        }
    }

    public void addAll(ArrayList<Fightable> fightableListToAdd) {
        // Add all fightables in this list
        for (int i = 0; i < fightableListToAdd.size(); i++) {
//            addFightable(fightableListToAdd.get(i)); // (OLD: shallow copy)Renaming may occur if there was some kind of management error
            addOrModifyFightable(fightableListToAdd.get(i).clone()); // Renaming may occur if there was some kind of management error
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

    public Fightable getFightableOfType(String name, Fightable.Faction faction) {
        // Return the Fightable that has the inputted name (there should only ever be one, so we'll only return the first we get).  If no such name appears in the list, return a null
        boolean isGroup = faction == Fightable.Faction.Group;
        for (int i = 0; i < allFactionLists.size(); i++) {
            if ( isGroup == (allFactionLists.get(i).faction() == Fightable.Faction.Group)) {
                // Only check in faction lists that match the "type" (Combatant or Group)
                Fightable thisFightable = allFactionLists.get(i).get(name);
                if (thisFightable != null) {
                    return thisFightable;
                }
            }
        }

        // If we get here, then no such Fightable exists
        return null;
    }

    public Fightable getFightableWithName(String name) {
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
            newList.addOrModifyFightable(get(i).getRaw());
        }

        return newList;
    }

    public boolean rawEquals(@Nullable Object obj) {
        // Check if the Fightable lists are the same, for data saving purposes
        boolean isEqual = false;
        if (obj instanceof AllFactionFightableLists) {
            AllFactionFightableLists objRaw = ((AllFactionFightableLists) obj).getRawCopy();
            AllFactionFightableLists thisRaw = getRawCopy();
            isEqual = thisRaw.equals(objRaw);
        }

        return isEqual;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllFactionFightableLists that = (AllFactionFightableLists) o;
        return Objects.equals(allFactionLists, that.allFactionLists);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allFactionLists);
    }

    public boolean containsFightableWithID(UUID fightableID) {
        boolean containsFightable = false;
        for ( FactionFightableList list : allFactionLists ) {
            Fightable.Faction thisFaction = list.faction();
            if ( containsFightableWithID(fightableID, thisFaction ) ) {
                containsFightable = true;
                break;
            }
        }

        return containsFightable;
    }

    public boolean containsFightableWithID(UUID fightableID, Fightable.Faction fightableFaction) {
        boolean containsFightable = false;
        FactionFightableList list = getFactionList(fightableFaction);
        if (list.containsFightableWithID(fightableID)) {
            containsFightable = true;
        }

        return containsFightable;
    }


    public Fightable getFightableWithID(UUID fightableID) {
        Fightable returnFightable = null;
        for ( FactionFightableList list : allFactionLists ) {
            returnFightable = list.getFightableWithID( fightableID );
            if ( returnFightable != null ) {
                break;
            }
        }

        return returnFightable;
    }

    public Fightable getFightableWithID(CombatantGroup.CombatantGroupData data) {
        return getFightableWithID(data.mID);
    }

    public boolean combatantIsInAGroup( Combatant combatant ) {
        boolean inGroup = false;
        CombatantGroup.CombatantGroupData testGroupData = new CombatantGroup.CombatantGroupData(combatant);
        for ( Fightable groupFightable : getFactionList(Fightable.Faction.Group).getFightableArrayList()) {
            if ( groupFightable instanceof CombatantGroup ) { // It damn well better be...
                if (((CombatantGroup) groupFightable).getCombatantList().contains(testGroupData)) {
                    inGroup = true;
                    break;
                }
            }
        }

        return inGroup;
    }

    public ArrayList<Fightable> getSelected( ) {
        ArrayList<Fightable> returnList = new ArrayList<>();
        for ( FactionFightableList list : allFactionLists ) {
            for ( int fightableInd = 0; fightableInd < list.size(); fightableInd++ ) {
                Fightable thisFightable = list.get(fightableInd);
                if ( thisFightable.isSelected() ) {
                    returnList.add(thisFightable);
                }
            }
        }

        return returnList;
    }

    public void clearSelected( ) {
        for ( FactionFightableList list : allFactionLists ) {
            for ( int fightabbleInd = 0; fightabbleInd < list.size(); fightabbleInd++ ) {
                list.get(fightabbleInd).setSelected(false);
            }
        }
    }

    public void clear() {
        // Clear each faction list
        for (FactionFightableList list: allFactionLists) {
            list.clear();
        }
    }

    // For JSON conversions
    private static final String FACTION_FIGHTABLE_LIST_KEY = "FIGHTABLE_LIST";
    // TODO START HERE: Add import/export buttons to the toolbar (should probably usually be hidden), and implement these!  Let the user choose where to import from / export to?

    protected void fromJSON( JSONObject jsonObject ) {
        if ( allFactionLists == null ) {
            allFactionLists = new ArrayList<>();
        } else {
            clear();
        }
        try {
            if (!jsonObject.isNull(FACTION_FIGHTABLE_LIST_KEY)) {
                JSONArray jsonArray = jsonObject.getJSONArray(FACTION_FIGHTABLE_LIST_KEY);
                ArrayList<Fightable> fightablesToAdd = new ArrayList<>();
                // Go through each Fightable list
                for (int i = 0; i < jsonArray.length(); i++) {
                    // Create the new list
                    FactionFightableList thisList = new FactionFightableList(jsonArray.getJSONObject(i));
                    for ( int fightableInd = 0; fightableInd < thisList.size(); fightableInd++ ) {
                        // Add all Fightables from this FactionFightableList to the Fightables list
                        fightablesToAdd.add(thisList.get(fightableInd));
                    }
                }

                // Add all of the new Fightables
                addAll(fightablesToAdd);
            }
        } catch (JSONException e) {
            Log.e(TAG,e.toString());
        }
    }

    protected JSONObject toJSON( ) {
        JSONObject jsonObject = new JSONObject();
        JSONArray fightableListJSON = new JSONArray();
        try {
            for (FactionFightableList list : allFactionLists) {
                fightableListJSON.put(list.toJSON());
            }
            jsonObject.put(FACTION_FIGHTABLE_LIST_KEY, fightableListJSON);
        } catch ( JSONException e ) {
            Log.e(TAG,e.toString());
        }

        return jsonObject;
    }
}
