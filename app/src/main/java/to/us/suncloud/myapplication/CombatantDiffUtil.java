package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

// Used only for the Encounter Activity
public class CombatantDiffUtil extends DiffUtil.Callback {
    EncounterCombatantList oldList;
    EncounterCombatantList newList;
//    boolean enteredOrExitedPrephase; // Did we go into or leave pre-phase in this action?
    int curActiveCombatant; // The currently active Combatant
    int prevActiveCombatant; // The previously active Combatant

    CombatantDiffUtil(EncounterCombatantList oldList, EncounterCombatantList newList) {
        this.oldList = oldList;
        this.prevActiveCombatant = oldList.calcActiveCombatant();
        this.newList = newList;
        this.curActiveCombatant = newList.calcActiveCombatant();
//        this.enteredOrExitedPrephase =
//                curActiveCombatant == EncounterCombatantRecyclerAdapter.PREP_PHASE && prevActiveCombatant != EncounterCombatantRecyclerAdapter.PREP_PHASE
//                || prevActiveCombatant == EncounterCombatantRecyclerAdapter.PREP_PHASE && curActiveCombatant != EncounterCombatantRecyclerAdapter.PREP_PHASE;
    }

    @Override
    public int getOldListSize() {
        return oldList.visibleSize();
    }

    @Override
    public int getNewListSize() {
        return newList.visibleSize();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Compare the UUID of the Combatants
        boolean isSame = oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId()); // Separated out for debugging purposes
        return isSame;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Check if ALL of the values are the same
        boolean progressionStatus = EncounterCombatantRecyclerAdapter.getStatus(oldList, oldItemPosition, prevActiveCombatant) == EncounterCombatantRecyclerAdapter.getStatus(newList, newItemPosition, curActiveCombatant);
        boolean isDuplicate = EncounterCombatantRecyclerAdapter.getDuplicateColor(oldList, oldItemPosition, prevActiveCombatant) == EncounterCombatantRecyclerAdapter.getDuplicateColor(newList, newItemPosition, curActiveCombatant); // Get the current tab color, which is based on the initiative value AND the current phase in the combat cycle
        boolean initValues = oldList.get(oldItemPosition).getModifier() ==  newList.get(newItemPosition).getModifier() && oldList.get(oldItemPosition).getRoll() == newList.get(newItemPosition).getRoll();
//        boolean isChecked = !enteredOrExitedPrephase && EncounterCombatantRecyclerAdapter.isCheckedState(oldList, oldItemPosition, prevActiveCombatant) == EncounterCombatantRecyclerAdapter.isCheckedState(newList, newItemPosition, curActiveCombatant);
        boolean isChecked = EncounterCombatantRecyclerAdapter.isCheckedState(oldList, oldItemPosition, prevActiveCombatant) == EncounterCombatantRecyclerAdapter.isCheckedState(newList, newItemPosition, curActiveCombatant);

        return initValues && progressionStatus && isDuplicate && isChecked;
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        // Is the previous or current state set to the prepare phase?
        boolean progressionStatus = EncounterCombatantRecyclerAdapter.getStatus(oldList, oldItemPosition, prevActiveCombatant) == EncounterCombatantRecyclerAdapter.getStatus(newList, newItemPosition, curActiveCombatant);
        boolean isDuplicate = EncounterCombatantRecyclerAdapter.getDuplicateColor(oldList, oldItemPosition, prevActiveCombatant) == EncounterCombatantRecyclerAdapter.getDuplicateColor(newList, newItemPosition, curActiveCombatant); // Get the current tab color, which is based on the initiative value AND the current phase in the combat cycle
        boolean initValues = oldList.get(oldItemPosition).getModifier() ==  newList.get(newItemPosition).getModifier() && oldList.get(oldItemPosition).getRoll() == newList.get(newItemPosition).getRoll();
        boolean isChecked = EncounterCombatantRecyclerAdapter.isCheckedState(oldList, oldItemPosition, prevActiveCombatant) == EncounterCombatantRecyclerAdapter.isCheckedState(newList, newItemPosition, curActiveCombatant);

        // Go through each possible difference, and update the ViewHolder precisely based on what has changed
        Bundle diffs = new Bundle();
        if (!progressionStatus) {
            diffs.putSerializable("Progression", EncounterCombatantRecyclerAdapter.getStatus(newList, newItemPosition, curActiveCombatant));
        }
        if (!isDuplicate) {
            diffs.putInt("Duplicate", EncounterCombatantRecyclerAdapter.getDuplicateColor(newList, newItemPosition, curActiveCombatant));
        }
        if (!initValues) {
            diffs.putBoolean("InitValues", false); // ViewHolder will need to get values directly from combatantList
        }
        if (!isChecked) {
            diffs.putInt("Checked", EncounterCombatantRecyclerAdapter.isCheckedState(newList, newItemPosition, curActiveCombatant));
        }

        if (diffs.size() == 0) {
            diffs = null;
        }

        return diffs;
    }
}
