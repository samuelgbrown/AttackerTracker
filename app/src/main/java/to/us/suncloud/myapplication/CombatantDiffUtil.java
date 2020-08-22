package to.us.suncloud.myapplication;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;

// Used only for the Encounter Activity
public class CombatantDiffUtil extends DiffUtil.Callback {
    EncounterCombatantList oldList;
    EncounterCombatantList newList;

    CombatantDiffUtil(EncounterCombatantList oldList, EncounterCombatantList newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Compare the UUID of the Combatants
        return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Check if ALL of the values are the same
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
