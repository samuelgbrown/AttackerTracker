package to.us.suncloud.myapplication;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;

public class CombatantFilteredDiffUtil extends DiffUtil.Callback {
    ArrayList<Combatant> oldList;
    ArrayList<Combatant> newList;
    String oldString;
    String newString;

    CombatantFilteredDiffUtil(ArrayList<Combatant> oldList, String oldString, ArrayList<Combatant> newList, String newString) {
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
        // Combatants are unique by name...ideally...
        // Item identity has no relation to the filter text
        return oldList.get(oldItemPosition).getName().equals(newList.get(newItemPosition).getName());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Check if ALL of the values are the same (the filter text also needs to be the same)
        return oldString.equals(newString) && oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
