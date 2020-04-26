package to.us.suncloud.myapplication;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;

public class CombatantDiffUtil extends DiffUtil.Callback {
    ArrayList<Combatant> oldList;
    ArrayList<Combatant> newList;

    CombatantDiffUtil(ArrayList<Combatant> oldList, ArrayList<Combatant> newList) {
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
        // Combatants are unique by name...ideally...
        return oldList.get(oldItemPosition).getName().equals(newList.get(newItemPosition).getName());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Check if ALL of the values are the same
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
