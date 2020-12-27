package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import static to.us.suncloud.myapplication.EncounterCombatantRecyclerAdapter.PREP_PHASE;
import static to.us.suncloud.myapplication.EncounterCombatantRecyclerAdapter.getDuplicateColor;
import static to.us.suncloud.myapplication.EncounterCombatantRecyclerAdapter.getStatus;
import static to.us.suncloud.myapplication.EncounterCombatantRecyclerAdapter.isCheckedState;
import static to.us.suncloud.myapplication.EncounterCombatantRecyclerAdapter.getCurrentDisplayedRolledInit;

// Used only for the Encounter Activity
public class CombatantDiffUtil extends DiffUtil.Callback {
    //    boolean enteredOrExitedPrephase; // Did we go into or leave pre-phase in this action?
    // Combat states
    EncounterCombatantRecyclerAdapter.CombatStateStruct prevState;
    EncounterCombatantRecyclerAdapter.CombatStateStruct curState;

    // Global state parameters
    int prevActiveCombatant; // The previously active Combatant
    int curActiveCombatant; // The currently active Combatant
    boolean preEditVisible; // Was the dice edit View previously visible?
    boolean curEditVisible; // Is the dice edit View currently visible?

    CombatantDiffUtil(EncounterCombatantRecyclerAdapter.CombatStateStruct oldState, EncounterCombatantRecyclerAdapter.CombatStateStruct newState) {
        // Save the overall states
        this.prevState = oldState;
        this.curState = newState;

        // Pre-calculated global values
        this.prevActiveCombatant = oldState.combatantList.calcActiveCombatant();
        this.curActiveCombatant = newState.combatantList.calcActiveCombatant();
        this.preEditVisible = oldState.diceCheatModeOn || (oldState.playerDefinedRolls && (prevActiveCombatant == PREP_PHASE));
        this.curEditVisible = newState.diceCheatModeOn || (newState.playerDefinedRolls && (curActiveCombatant == PREP_PHASE));
    }

    @Override
    public int getOldListSize() {
        return prevState.combatantList.visibleSize();
    }

    @Override
    public int getNewListSize() {
        return curState.combatantList.visibleSize();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Compare the UUID of the Combatants
        boolean isSame = prevState.combatantList.get(oldItemPosition).getId().equals(curState.combatantList.get(newItemPosition).getId()); // Separated out for debugging purposes
        return isSame;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Check if ALL of the values are the same
        boolean progressionStatus = getStatus(prevState.combatantList, oldItemPosition, prevActiveCombatant) == getStatus(curState.combatantList, newItemPosition, curActiveCombatant);
        boolean isDuplicate = getDuplicateColor(prevState.combatantList, oldItemPosition, prevActiveCombatant) == getDuplicateColor(curState.combatantList, newItemPosition, curActiveCombatant); // Get the current tab color, which is based on the initiative value AND the current phase in the combat cycle
//        boolean initValues = prevState.combatantList.get(oldItemPosition).getModifier() == curState.combatantList.get(newItemPosition).getModifier() && prevState.combatantList.get(oldItemPosition).getRoll() == curState.combatantList.get(newItemPosition).getRoll();
//        boolean isChecked = !enteredOrExitedPrephase && EncounterCombatantRecyclerAdapter.isCheckedState(oldState.combatantList, oldItemPosition, prevActiveCombatant) == EncounterCombatantRecyclerAdapter.isCheckedState(newState.combatantList, newItemPosition, curActiveCombatant);
        boolean isChecked = isCheckedState(prevState.combatantList, oldItemPosition, prevActiveCombatant) == isCheckedState(curState.combatantList, newItemPosition, curActiveCombatant);
        boolean initiativeValues = getCurrentDisplayedRolledInit(prevState, oldItemPosition, prevActiveCombatant) == getCurrentDisplayedRolledInit(curState, newItemPosition, curActiveCombatant);
        boolean isRollEditVisible = preEditVisible == curEditVisible;

//        return initValues && progressionStatus && isDuplicate && isChecked && initiativeValues && isRollEditVisible;
        return progressionStatus && isDuplicate && isChecked && initiativeValues && isRollEditVisible;
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        // Is the previous or current state set to the prepare phase?
        boolean progressionStatus = getStatus(prevState.combatantList, oldItemPosition, prevActiveCombatant) == getStatus(curState.combatantList, newItemPosition, curActiveCombatant);
        boolean isDuplicate = getDuplicateColor(prevState.combatantList, oldItemPosition, prevActiveCombatant) == getDuplicateColor(curState.combatantList, newItemPosition, curActiveCombatant); // Get the current tab color, which is based on the initiative value AND the current phase in the combat cycle
//        boolean initValues = prevState.combatantList.get(oldItemPosition).getModifier() == curState.combatantList.get(newItemPosition).getModifier() && prevState.combatantList.get(oldItemPosition).getRoll() == curState.combatantList.get(newItemPosition).getRoll();
        boolean isChecked = isCheckedState(prevState.combatantList, oldItemPosition, prevActiveCombatant) == isCheckedState(curState.combatantList, newItemPosition, curActiveCombatant);
        boolean initiativeValues = getCurrentDisplayedRolledInit(prevState, oldItemPosition, prevActiveCombatant) == getCurrentDisplayedRolledInit(curState, newItemPosition, curActiveCombatant);
        boolean isRollEditVisible = preEditVisible == curEditVisible;

        // Go through each possible difference, and update the ViewHolder precisely based on what has changed
        Bundle diffs = new Bundle();
        if (!progressionStatus) {
            diffs.putSerializable("Progression", getStatus(curState.combatantList, newItemPosition, curActiveCombatant));
        }
        if (!isDuplicate) {
            diffs.putInt("Duplicate", getDuplicateColor(curState.combatantList, newItemPosition, curActiveCombatant));
        }
//        if (!initValues) {
//            diffs.putBoolean("InitValues", false); // ViewHolder will need to get values directly from combatantList
//        }
        if (!isChecked) {
            diffs.putInt("Checked", isCheckedState(curState.combatantList, newItemPosition, curActiveCombatant));
        }
        if (!initiativeValues) {
            diffs.putIntArray("InitValues", getCurrentDisplayedRolledInit(curState, newItemPosition, curActiveCombatant));
        }
        if(!isRollEditVisible) {
            diffs.putBoolean(EncounterCombatantRecyclerAdapter.PAYLOAD_DICE_VIEW_VIS, curEditVisible);
        }

        if (diffs.size() == 0) {
            diffs = null;
        }

        return diffs;
    }
}
