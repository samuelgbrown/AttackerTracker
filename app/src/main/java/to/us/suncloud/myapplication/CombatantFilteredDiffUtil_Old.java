package to.us.suncloud.myapplication;

import androidx.recyclerview.widget.DiffUtil;

public class CombatantFilteredDiffUtil_Old extends DiffUtil.Callback {
    FactionFightableList oldList;
    FactionFightableList newList;
    String oldString;
    String newString;

    CombatantFilteredDiffUtil_Old(FactionFightableList oldList, String oldString, FactionFightableList newList, String newString) {
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
