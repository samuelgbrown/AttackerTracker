package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

public class CombatantsInGroupDiffUtil extends DiffUtil.Callback {
    public static final String DIFF_NAME = "Name";
    public static final String DIFF_FACTION = "Faction";
    public static final String DIFF_SELECTED = "Selected";
    public static final String DIFF_ICON = "iCON";
    public static final String DIFF_MULTISELECT = "Multiselect";
    public static final String DIFF_MULTIPLES = "Multiples";

    public enum MultiSelectVisibilityChange {
        NO_CHANGE,
        START_MULTISELECT,
        END_MULTISELECT
    }

    AllFactionFightableLists oldRefList;
    AllFactionFightableLists newRefList;
    CombatantGroup oldGroup;
    CombatantGroup newGroup;
    MultiSelectVisibilityChange visibilityChange;

    CombatantsInGroupDiffUtil(CombatantGroup oldGroup, CombatantGroup newGroup, AllFactionFightableLists oldRefList, AllFactionFightableLists newRefList, MultiSelectVisibilityChange visChange) {
        this.oldGroup  = oldGroup;
        this.newGroup = newGroup;
        this.oldRefList = oldRefList;
        this.newRefList = newRefList;
        visibilityChange = visChange;
    }

    @Override
    public int getOldListSize() {
        return oldGroup.size();
    }

    @Override
    public int getNewListSize() {
        return newGroup.size();
    }

    private Combatant getOldCombatant(int oldItemPosition ) {
        return oldGroup.getCombatant(oldRefList, oldItemPosition);
    }

    private Combatant getNewCombatant(int newItemPosition ) {
        return newGroup.getCombatant(newRefList, newItemPosition);
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Compare the UUID of the Combatants
        return oldGroup.getUUIDOfCombatant(oldItemPosition).equals(newGroup.getUUIDOfCombatant(newItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Check if ALL of the values are the same

        // First, see if these positions are Combatants or banners
        Combatant oldCombatant = getOldCombatant(oldItemPosition);
        Combatant newCombatant = getNewCombatant(newItemPosition);

        // Both items are Combatants
        if (visibilityChange != MultiSelectVisibilityChange.NO_CHANGE) {
            // If the multi-select visibility is changing, contents are different
            return false;
        } else {
            // Make sure both Combatant displays are identical, and that the isSelected status is the same
            boolean combatantsEqual = oldCombatant.displayEquals(newCombatant); // Raw info on the Combatants themselves
            boolean groupInfoEqual = oldGroup.combatantGroupDataEquals(newGroup, newItemPosition, oldItemPosition); // Meta-data held by the group
            return combatantsEqual && groupInfoEqual;
        }
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Combatant oldCombatant = getOldCombatant(oldItemPosition);
        Combatant newCombatant = getNewCombatant(newItemPosition);

        // Go through each field that we care about, and record any differences
        Bundle diffs = new Bundle();

        // Check for things that we expect may change
        if (oldCombatant.isSelected() != newCombatant.isSelected()) {
            diffs.putBoolean(DIFF_SELECTED, newCombatant.isSelected());
        }
        if ( visibilityChange != MultiSelectVisibilityChange.NO_CHANGE )
        {
            diffs.putSerializable(DIFF_MULTISELECT, visibilityChange);
        }
        if ( !oldGroup.combatantGroupDataEquals(newGroup, newItemPosition, oldItemPosition) ) {
            diffs.putInt(DIFF_MULTIPLES, newGroup.getNumMultiplesOfCombatant(newItemPosition));
        }

        // NOTE: NONE of these should be changing, but we'll watch for them just in case...
        if (!oldCombatant.getName().equals(newCombatant.getName())) {
            diffs.putString(DIFF_NAME, newCombatant.getName());
        }
        if (!oldCombatant.getFaction().equals(newCombatant.getFaction())) {
            diffs.putSerializable(DIFF_FACTION, newCombatant.getFaction());
        }
        if (!(oldCombatant.getIconIndex() == newCombatant.getIconIndex())) {
            diffs.putInt(DIFF_ICON, newCombatant.getIconIndex());
        }

        if (diffs.size() == 0) {
            return null;
        }

        return diffs;
    }
}
