package to.us.suncloud.myapplication;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class CombatantGroup extends Fightable {
    private ArrayList<CombatantGroupData> combatantList; // List of Combatants that are part of this Group

    CombatantGroup( ) {
        // Initialize list to empty
        combatantList = new ArrayList<>();
    }

    public CombatantGroup(CombatantGroup combatantGroup) {
        combatantList = combatantGroup.getCombatantList();
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
        // Return true if all new Combatants are unique - false if one Combatant already exists
        boolean allCombatantsUnique = true;
        verifyGroupAgainstList(referenceList);

        // TODO: Finish - Remember all checks and behaviors!  Covered in AddToGroupRVA, as well as giant todo list
        ArrayList<Fightable> selectedFightables = referenceList.getSelected();
        ArrayList<Combatant> selectedCombatants = digestAllFightablesToCombatants(selectedFightables, referenceList);
        for (Combatant combatant: selectedCombatants) {
            // Add each Combatant to the Group!
            CombatantGroupData thisCombatantData = new CombatantGroupData(combatant);
            if (combatantList.contains(thisCombatantData)) {
                // If this Combatant already exists in this Group, do not add it and raise a flag
                allCombatantsUnique = false;
            } else {
                combatantList.add(thisCombatantData);
            }
        }

        return allCombatantsUnique;
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
    }

    // TODO: Fill out all abstract methods!  Are they all needed...?
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
    public String getName() {
        return null;
    }

    @Override
    public String getBaseName() {
        return null;
    }

    @Override
    public int getOrdinal() {
        return 0;
    }

    @Override
    boolean isVisible() {
        return false;
    }

    @Override
    void setVisible(boolean isVisible) {

    }

    @Override
    void displayCopy(Fightable f) {

    }

    @Override
    void setNameOrdinal(int newOrdinal) {

    }

    @Override
    boolean displayEquals(@Nullable Object obj) {
        return false;
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
