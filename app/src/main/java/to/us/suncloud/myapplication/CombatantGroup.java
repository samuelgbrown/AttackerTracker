package to.us.suncloud.myapplication;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class CombatantGroup extends Fightable {
    private ArrayList<CombatantGroupData> combatantList; // List of Combatants that are part of this Group

    public CombatantGroup( AllFactionFightableLists listOfAllFightables ) {
        super(listOfAllFightables);

        // Initialize list to empty
        combatantList = new ArrayList<>();
    }

    public CombatantGroup(CombatantGroup combatantGroup) {
        super(combatantGroup);
        setCombatantList(new ArrayList<>(combatantGroup.getCombatantList())); // Clone combatantList into the new CombatantGroup
    }

    private void setCombatantList(ArrayList<CombatantGroupData> newData) {
        // Used only for cloning
        combatantList = newData;
    }

    public ArrayList<CombatantGroupData> getCombatantList() {
        return combatantList;
    }

    public boolean addSelected(AllFactionFightableLists referenceList ) {
        // Add all of the selected Fightables in this list to the Group.
        // Return false if all new Combatants are unique - true if any Combatants already exists in this Group
        boolean anyCombatantsDoubled = false;
        verifyGroupAgainstList(referenceList);

        // TODO: Finish - Remember all checks and behaviors!  Covered in AddToGroupRVA, as well as giant todo list
        ArrayList<Fightable> selectedFightables = referenceList.getSelected();
        ArrayList<Combatant> selectedCombatants = digestAllFightablesToCombatants(selectedFightables, referenceList);
        for (Combatant combatant: selectedCombatants) {
            // Add each Combatant to the Group!
            CombatantGroupData thisCombatantData = new CombatantGroupData(combatant);
            if (combatantList.contains(thisCombatantData)) {
                // If this Combatant already exists in this Group, do not add it and raise a flag
                anyCombatantsDoubled = true;
            } else {
                combatantList.add(thisCombatantData);
            }
        }

        return anyCombatantsDoubled;
    }

    public void removeSelectedCombatants( AllFactionFightableLists referenceList ) {
        ArrayList<Fightable> selectedCombatants = referenceList.getSelected();
        for ( Fightable thisFightable : selectedCombatants ) {
            // Go through each selected Combatant (ignoring Groups)
            if ( thisFightable instanceof Combatant ) {
                removeCombatant((Combatant) thisFightable);
            } // Will ONLY remove Combatants, not digested CombatantGroups
        }
    }

    public void removeCombatant(Combatant combatant) {
        for ( Iterator<CombatantGroupData> iter = combatantList.iterator(); iter.hasNext();) {
            // Go through combatantList, and remove thisCombatant (if it exists)
            CombatantGroupData thisData = iter.next();
            if ( combatant.getId() == thisData.mID ) {
                iter.remove();
                break;
            }
        }
    }

    public boolean allCombatantsAreSelected( AllFactionFightableLists referenceList ) {
        boolean allSelected = true;
        for ( CombatantGroupData thisData : combatantList ) {
            Combatant thisCombatant = referenceList.getCombatantWithID(thisData.mID, thisData.mFaction);
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
            Combatant thisCombatant = referenceList.getCombatantWithID(thisData.mID, thisData.mFaction);
            if ( ( thisCombatant.isSelected() )  && ( thisData.mMultiples > 1 ) ) {
                haveMultiples = true;
            }
        }

        return haveMultiples;
    }

    public void removeCombatant(int combatantIndex) {
        combatantList.remove(combatantIndex);
    }

    public UUID getUUIDOfCombatant(int combatantInd ) {
        return combatantList.get(combatantInd).mID;
    }

    public int getNumMultiplesOfCombatant(int combatantInd ) {
        return combatantList.get(combatantInd).mMultiples;
    }

    public void setNumMultiplesOfCombatant(int combatantInd, int numMultiples ) {
        combatantList.get(combatantInd).mMultiples = numMultiples;
    }

    public Combatant getCombatant(AllFactionFightableLists referenceAFFL, int combatantInd) {
        CombatantGroupData thisData = combatantList.get(combatantInd);
        Faction thisFaction = thisData.mFaction;
        UUID thisID = thisData.mID;
        return referenceAFFL.getFactionList(thisFaction).getCombatantWithID(thisID);
    }

    private void verifyGroupAgainstList(AllFactionFightableLists referenceList ) {
        for (Iterator<CombatantGroupData> iterator = combatantList.iterator(); iterator.hasNext(); ) {
            CombatantGroupData thisCombatant = iterator.next();
            if ( !referenceList.containsCombatantWithID(thisCombatant.mID, thisCombatant.mFaction) ) {
                // Remove this Combatant is no longer in the passed AFFL, from it from this Group
                iterator.remove();
            }
        }
    }

    private void combatantDeleted( UUID combatantID ) {
        // TODO: Use whenever the AFFL that owns this Group deletes a Combatant
        // Remove the combatant with the given ID from this Group, if it exists
        for (Iterator<CombatantGroupData> iterator = combatantList.iterator(); iterator.hasNext(); ) {
            if (iterator.next().mID == combatantID) {
                iterator.remove();
            }
        }

        // TODO: Implement sorting Combatants (will either need referenceAFFL each time, or to store each Combatant's name in combatantList, which will lead to desynchronizations
        // TODO: Sort Combatants
    }

    public int getTotalCombatantsInFaction( Faction faction ) {
        int runningSum = 0;
        for ( CombatantGroupData combatantData : combatantList ) {
            if ( combatantData.mFaction == faction) {
                runningSum++;
            }
        }

        return runningSum;
    }

    public int size( ) {
        return combatantList.size();
    }

    public int numTotalCombatants( ) {
        // Get the number of Combatants that this group contains (INCLUDING multiples)
        int runningSum = 0;
        for ( CombatantGroupData thisData : combatantList ) {
            runningSum += thisData.mMultiples;
        }

        return runningSum;
    }

    @Override
    public Fightable clone() {
        return new CombatantGroup(this);
    }

    @Override
    public Fightable cloneUnique() {
        return clone();
    }

    @Override
    public Fightable getRaw() {
        return this;
    }

    @Override
    public ArrayList<Combatant> convertToCombatants(AllFactionFightableLists referenceList) {
        ArrayList<Combatant> returnList = new ArrayList<>();
        for (CombatantGroupData combatantData : combatantList) {
            for (int multiplesInd = 0; multiplesInd < combatantData.mMultiples; multiplesInd++) {
                Combatant thisCombatant = referenceList.getCombatantWithID(combatantData.mID, combatantData.mFaction);
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
    void displayCopy(Fightable f) {
        if ( f instanceof CombatantGroup ) {
            displayCopyFightable(f);
            setCombatantList(((CombatantGroup) f).getCombatantList());
        }
    }

    @Override
    boolean displayEquals(@Nullable Object obj) {
        return displayEqualsFightable(obj);
    }

    boolean combatantGroupDataEquals(Object obj, int objCombatantInd, int thisCombatantInd) {
        boolean isEqual = false;
        if ( obj instanceof CombatantGroup ) {
            isEqual = ((CombatantGroup) obj).getNumMultiplesOfCombatant(objCombatantInd) ==
                    getNumMultiplesOfCombatant(thisCombatantInd);
        }

        return isEqual;
    }

    static private class CombatantGroupData {
        UUID mID; // UUID's of this Combatant
        Faction mFaction; // The faction of this Combatant (makes searches slightly faster)
        Integer mMultiples = 1; // The number of copies of this Combatant

        CombatantGroupData(Combatant combatant) {
            mID = combatant.getId();
            mFaction = combatant.getFaction();
        }

        // For ArrayList<>.contains(...):
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CombatantGroupData that = (CombatantGroupData) o;
            return Objects.equals(mID, that.mID) && mFaction == that.mFaction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mID, mFaction);
        }
    }
}
