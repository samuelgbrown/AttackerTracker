package to.us.suncloud.myapplication;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class CombatantGroup extends Fightable {
    private ArrayList<CombatantGroupData> combatantList; // List of Combatants that are part of this Group

    // To speed up getTotalCombatantsInFaction/displayEquals calculations
    private final HashMap<Faction, Integer> membersInFaction = new HashMap<>();

    public CombatantGroup( AllFactionFightableLists listOfAllFightables ) {
        super(listOfAllFightables);
        setFaction(Faction.Group);

        // Initialize list to empty
        combatantList = new ArrayList<>();
        combatantListChanged();
    }

    public CombatantGroup(CombatantGroup combatantGroup) {
        super(combatantGroup);

        // Do a manual deep copy of the CombatantGroupData, because Java sucks I guess (or I suck, who knows)
        ArrayList<CombatantGroupData> newGroupData = new ArrayList<>(combatantGroup.getCombatantList().size());
        for (CombatantGroupData newData : combatantGroup.getCombatantList()) {
            newGroupData.add(new CombatantGroupData(newData));
        }
        setCombatantList(newGroupData);

        combatantListChanged();
    }

    private void setCombatantList(ArrayList<CombatantGroupData> newData) {
        // Used only for cloning, so no sorting required
        combatantList = newData;

        combatantListChanged();
    }

    public ArrayList<CombatantGroupData> getCombatantList() {
        return combatantList;
    }

    public boolean addSelected(AllFactionFightableLists referenceAFFL) {
        // Add all of the selected Fightables in this list to the Group.
        // Return false if all new Combatants are unique - true if any Combatants already exists in this Group
        boolean anyCombatantsDoubled = false; // Result - does this Combatant already exist?
        boolean anyCombatantsAdded = false; // If any Combatants are added to the CombatantList
        verifyGroupAgainstList(referenceAFFL);

        ArrayList<Fightable> selectedFightables = referenceAFFL.getSelected();
        ArrayList<Combatant> selectedCombatants = digestAllFightablesToCombatants(selectedFightables, referenceAFFL);
        for (Combatant combatant: selectedCombatants) {
            // Add each Combatant to the Group!
            CombatantGroupData thisCombatantData = new CombatantGroupData(combatant);
            if (combatantList.contains(thisCombatantData)) {
                // If this Combatant already exists in this Group, do not add it and raise a flag
                anyCombatantsDoubled = true;
            } else {
                combatantList.add(thisCombatantData);
                anyCombatantsAdded = true;
            }
        }

        if ( anyCombatantsAdded ) {
            combatantListChanged();
            sort(referenceAFFL);
        }

        return anyCombatantsDoubled;
    }

    public boolean removeSelectedCombatants( AllFactionFightableLists referenceList ) {
        boolean stillHasMembers = true; // After removal, does this group still have members?
        ArrayList<Fightable> selectedCombatants = referenceList.getSelected();
        for ( Fightable thisFightable : selectedCombatants ) {
            // Go through each selected Fightable
            if ( !removeCombatant(thisFightable.getId(), referenceList) ) {
                stillHasMembers = false;
                break;
            }
        }

        return stillHasMembers;
    }

    public boolean removeCombatant(UUID thisID, AllFactionFightableLists referenceAFFL) {
        verifyGroupAgainstList(referenceAFFL);
        boolean stillHasMembers = true; // After removal, does this group still have members?

        boolean anyCombatantsRemoved = false;
        for ( Iterator<CombatantGroupData> iter = combatantList.iterator(); iter.hasNext();) {
            // Go through combatantList, and remove thisCombatant (if it exists)
            CombatantGroupData thisData = iter.next();
            if ( thisID == thisData.mID ) {
                iter.remove();
                anyCombatantsRemoved = true;
                break;
            }
        }

        if ( combatantList.isEmpty() ) {
            stillHasMembers = false;
        } else {
            if (anyCombatantsRemoved) {
                combatantListChanged();
                sort(referenceAFFL);
            }
        }

        return stillHasMembers; // If this returns false, the group should be deleted
    }

    public boolean removeCombatant(int combatantIndex, AllFactionFightableLists referenceAFFL) {
        boolean stillHasMembers = true; // After removal, does this group still have members?
        verifyGroupAgainstList(referenceAFFL);
        combatantList.remove(combatantIndex);

        if ( combatantList.isEmpty() ) {
            stillHasMembers = false;
        } else {
            combatantListChanged();
            sort(referenceAFFL);
        }

        return stillHasMembers; // If this returns false, the group should be deleted
    }

    public boolean allCombatantsAreSelected( AllFactionFightableLists referenceList ) {
        boolean allSelected = true;
        for ( CombatantGroupData thisData : combatantList ) {
            Combatant thisCombatant = (Combatant) referenceList.getFightableWithID(thisData);
            if ( ( thisCombatant == null ) || ( !thisCombatant.isSelected() ) ) {
                // If this Combatant 1) doesn't exist, or 2) isn't selected, we can stop here
                allSelected = false;
            }
        }

        return allSelected;
    }

    public boolean selectedCombatantsHaveMultiples( AllFactionFightableLists referenceList ) {
        // Do any of the Combatants in this group that are selected have multiples greater than 1?
        boolean haveMultiples = false;
        for ( CombatantGroupData thisData : combatantList ) {
            Combatant thisCombatant = (Combatant) referenceList.getFightableWithID(thisData);
            if ( ( thisCombatant.isSelected() )  && ( thisData.mMultiples > 1 ) ) {
                haveMultiples = true;
            }
        }

        return haveMultiples;
    }

    public UUID getUUIDOfCombatant(int combatantInd ) {
        return combatantList.get(combatantInd).mID;
    }

    public int getNumMultiplesOfCombatant(int combatantInd ) {
        return combatantList.get(combatantInd).mMultiples;
    }

    public void setNumMultiplesOfCombatant(int combatantInd, int numMultiples ) {
        combatantList.get(combatantInd).mMultiples = numMultiples;
        combatantListChanged();
    }

    public CombatantGroupData getCombatantData(int combatantInd) {
        return getCombatantList().get(combatantInd);
    }

    public Combatant getCombatant(AllFactionFightableLists referenceAFFL, int combatantInd) {
        verifyGroupAgainstList(referenceAFFL);
        Combatant returnCombatant = null;

        if ( combatantInd < combatantList.size() ) {
            CombatantGroupData thisData = combatantList.get(combatantInd);
            Faction thisFaction = thisData.mFaction;
            UUID thisID = thisData.mID;
            returnCombatant = (Combatant) referenceAFFL.getFactionList(thisFaction).getFightableWithID(thisID);
        }
        return returnCombatant;
    }

    public void verifyGroupAgainstList(AllFactionFightableLists referenceList ) {
        boolean combatantDataModified = false;
        for (Iterator<CombatantGroupData> iterator = combatantList.iterator(); iterator.hasNext(); ) {
            CombatantGroupData thisCombatant = iterator.next();
            // Check if the Combatant is still in the combatantList, in the expected Faction
            if ( !referenceList.containsFightableWithID(thisCombatant.mID, thisCombatant.mFaction) ) {
                if ( referenceList.containsFightableWithID(thisCombatant.mID) ) {
                    // If the Faction has changed, update the combatantList
                    thisCombatant.mFaction = referenceList.getFightableWithID(thisCombatant.mID).getFaction();
                } else {
                    // Remove this Combatant is no longer in the passed AFFL, from it from this Group
                    iterator.remove();
                }

                combatantDataModified = true;
            }
        }

        if (combatantDataModified) {
            combatantListChanged();
            sort(referenceList);
        }
    }

    public int size( ) {
        return combatantList.size();
    }

    public int numTotalCombatants( ) {
        // Get the number of Combatants that this group contains (INCLUDING multiples)
        int thisTotalCombatants = 0;
        for ( Faction thisFaction : Faction.values() ) {
            // Go through every Faction, and sum the number of Combatants from each
            thisTotalCombatants += getTotalCombatantsInFaction(thisFaction);
        }

        return thisTotalCombatants;
    }

    public int getTotalCombatantsInFaction( Faction faction ) {
        // Get the number of Combatants that this Faction contains (INCLUDING multiples)
        int thisFactionCombatants = 0;
        Integer savedMembersInFaction = getSavedMembersInFaction(faction);
        if ( savedMembersInFaction == null ) {
            // Recalculate only if necessary
            for (CombatantGroupData thisData : combatantList) {
                if (thisData.mFaction == faction) {
                    thisFactionCombatants += thisData.mMultiples;
                }
            }

            // Now that we've calculated the value, save it for the next time we need it!
            setSavedMembersInFaction(faction, thisFactionCombatants);
        } else {
            thisFactionCombatants = savedMembersInFaction;
        }

        return thisFactionCombatants;
    }

    @Override
    public Fightable clone() {
        return new CombatantGroup(this);
    }

    @Override
    protected Fightable cloneUnique_Child( Fightable f ) {
        return f; // Nothing to make unique about the clone
    }
    @Override
    protected Fightable getRaw_Child(Fightable rawFightable) {
        return rawFightable;
    }

    @Override
    public ArrayList<Combatant> convertToCombatants(AllFactionFightableLists referenceAFFL) {
        verifyGroupAgainstList(referenceAFFL);

        ArrayList<Combatant> returnList = new ArrayList<>();
        for (CombatantGroupData combatantData : combatantList) {
            for (int multiplesInd = 0; multiplesInd < combatantData.mMultiples; multiplesInd++) {
                Combatant thisCombatant = (Combatant) referenceAFFL.getFightableWithID(combatantData);
                if ( thisCombatant != null ) {
                    returnList.add(thisCombatant);
                }
            }
        }

        return returnList;
    }

    @Override
    boolean isVisible() {
        return true;
    }

    @Override
    void setVisible(boolean isVisible) {
        // Do nothing
    }

    @Override
    protected void displayCopy_Child(Fightable f) {
        if ( f instanceof CombatantGroup ) {
            setCombatantList(((CombatantGroup) f).getCombatantList());
        }
    }

    @Override
    protected boolean displayEquals_Child(@Nullable Object obj) {
        boolean isEqual = false;
        if ( obj instanceof CombatantGroup ) {
            boolean allFactionCountsEqual = true;
            for (Faction f : Faction.values()) {
                if (getTotalCombatantsInFaction(f) != ((CombatantGroup) obj).getTotalCombatantsInFaction(f)) {
                    // If any faction has an unequal number of members, then this and obj cannot be display-equal
                    allFactionCountsEqual = false;
                    break;
                }
            }

            isEqual = allFactionCountsEqual;
        }
        return isEqual;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean isEqual = false;
        if (obj instanceof CombatantGroup) {
            boolean parentEqual = super.equals(obj);
            boolean combatantDataEqual = false;
            if ( getCombatantList().size() == ((CombatantGroup) obj).getCombatantList().size() ) {
                combatantDataEqual = true;
                for (int i = 0; i < combatantList.size(); i++) {
                    if (!getCombatantData(i).fullEquals(((CombatantGroup) obj).getCombatantData(i))) {
                        combatantDataEqual = false;
                        break;
                    }
                }
            }

            isEqual = parentEqual && combatantDataEqual;
        }

        return isEqual;
    }

    boolean combatantGroupDataEquals(Object obj, int objCombatantInd, int thisCombatantInd) {
        boolean isEqual = false;
        if ( obj instanceof CombatantGroup ) {
            isEqual = ((CombatantGroup) obj).getNumMultiplesOfCombatant(objCombatantInd) ==
                    getNumMultiplesOfCombatant(thisCombatantInd);
        }

        return isEqual;
    }

    public void removeAllCombatants() {
        combatantList.clear();
        combatantListChanged();
    }

    static public class CombatantGroupData implements Serializable {
        UUID mID; // UUID's of this Combatant
        Faction mFaction; // The faction of this Combatant (makes searches slightly faster)
        Integer mMultiples = 1; // The number of copies of this Combatant

        CombatantGroupData(Combatant combatant) {
            mID = combatant.getId();
            mFaction = combatant.getFaction();
        }

        CombatantGroupData(CombatantGroupData combatantGroupData) {
            mID = combatantGroupData.mID;
            mFaction = combatantGroupData.mFaction;
            mMultiples = combatantGroupData.mMultiples;
        }

        // For ArrayList<>.contains(...):
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CombatantGroupData that = (CombatantGroupData) o;
            return Objects.equals(mID, that.mID) && mFaction == that.mFaction;
        }

        public boolean fullEquals(Object o) {
            CombatantGroupData that = (CombatantGroupData) o;
            return ( Objects.equals(mID, that.mID) ) && ( mFaction == that.mFaction) &&
                    (Objects.equals(mMultiples, that.mMultiples));
        }

        @Override
        public int hashCode() {
            return Objects.hash(mID, mFaction);
        }
    }

    private void combatantListChanged( ) {
        membersInFaction.clear();
    }

    private void sort(AllFactionFightableLists referenceList) {
        // Sort the combatantList by faction, and then by UUID
        Collections.sort(combatantList, new CombatantGroupDataSorter.SortCombatantData(referenceList));
    }

    private Integer getSavedMembersInFaction( Faction f ) {
        // Returns the number of members in the Faction.
        // If the result is NULL, then the combatantList has changed since the last time this was
        // calculated, and therefore must be recalculated
        return membersInFaction.get(f);
    }

    private void setSavedMembersInFaction( Faction f, int numMembers ) {
        membersInFaction.put(f, numMembers);
    }
}
