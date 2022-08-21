package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

public class CombatantFilteredDiffUtil extends DiffUtil.Callback {
    AllFactionFightableLists oldList;
    AllFactionFightableLists newList;

    CombatantFilteredDiffUtil(AllFactionFightableLists oldList, AllFactionFightableLists newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.sizeWithBanners();
    }

    @Override
    public int getNewListSize() {
        return newList.sizeWithBanners();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // First, see if these positions are Combatants or banners
        int oldCombatantInd = oldList.posToFightableInd(oldItemPosition);
        int newCombatantInd = newList.posToFightableInd(newItemPosition);
        if ((2*oldCombatantInd + 1)*(2*newCombatantInd + 1) > 0 ) {
            if (oldCombatantInd >= 0) {
                // Both items are Combatants
                // Compare the UUID of the Combatants
                boolean areSame = oldList.get(oldCombatantInd).getId().equals(newList.get(newCombatantInd).getId());
                return areSame;
            } else {
                // Both items are banners
                return oldCombatantInd == newCombatantInd;
            }
        } else {
            // The items at each position represent different types (one is a banner and the other is a Combatant
            return false;
        }
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Check if ALL of the values are the same
        // First, see if these positions are Combatants or banners
        int oldCombatantInd = oldList.posToFightableInd(oldItemPosition);
        int newCombatantInd = newList.posToFightableInd(newItemPosition);
        if ((2*oldCombatantInd + 1)*(2*newCombatantInd + 1) > 0 ) {
            if (oldCombatantInd >= 0) {
                // Both items are Combatants
                // Make sure both Combatant displays are identical, and that the isSelected status is the same
                return oldList.get(oldCombatantInd).displayEquals(newList.get(newCombatantInd));
            } else {
                // Both items are banners
                return oldCombatantInd == newCombatantInd;
            }
        } else {
            // The items at each position represent different types (one is a banner and the other is a Combatant
            return false;
        }
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        // For this list, we only care about name, faction, icon, and isSelected status
        int oldCombatantInd = oldList.posToFightableInd(oldItemPosition);
        int newCombatantInd = newList.posToFightableInd(newItemPosition);

        Fightable oldFightable = oldList.get(oldCombatantInd);
        Fightable newFightable = newList.get(newCombatantInd);

        // Go through each field that we care about, and record any differences
        Bundle diffs = new Bundle();
        if (!oldFightable.getName().equals(newFightable.getName())) {
            diffs.putString("Name", newFightable.getName());
        }
        if (!oldFightable.getFaction().equals(newFightable.getFaction())) {
            diffs.putSerializable("Faction", newFightable.getFaction());
        }
        if (oldFightable.isSelected() != newFightable.isSelected()) {
            diffs.putBoolean("Selected", newFightable.isSelected());
        }
        if ((oldFightable instanceof Combatant) && (newFightable instanceof  Combatant))
        {
            Combatant oldCombatant = (Combatant) oldFightable;
            Combatant newCombatant = (Combatant) newFightable;
            if (!(oldCombatant.getIconIndex() == newCombatant.getIconIndex())) {
                diffs.putInt("Icon", newCombatant.getIconIndex());
            }
        }
        if (diffs.size() == 0) {
            return null;
        }

        return diffs;
    }
}
