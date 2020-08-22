package to.us.suncloud.myapplication;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;

public class CombatantFilteredDiffUtil extends DiffUtil.Callback {
    FactionCombatantList oldList;
    FactionCombatantList newList;
    String oldString;
    String newString;

    CombatantFilteredDiffUtil(FactionCombatantList oldList, String oldString, FactionCombatantList newList, String newString) {
        // TODO: Does this really need the filter text...?  If not, then can basically use same CombatantFilteredDiffUtil as Encounter Activity...
        this.oldList = oldList;
        this.newList = newList;
        this.oldString = oldString;
        this.newString = newString;
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
        // Item identity has no relation to the filter text
        return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Check if ALL of the values are the same (the filter text also needs to be the same)
        return oldString.equals(newString) && oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
