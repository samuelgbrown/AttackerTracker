package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;

public class CombatantFilteredDiffUtil extends DiffUtil.Callback {
    AllFactionCombatantLists oldList;
    AllFactionCombatantLists newList;

    CombatantFilteredDiffUtil(AllFactionCombatantLists oldList, AllFactionCombatantLists newList) {
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
        int oldCombatantInd = oldList.posToCombatantInd(oldItemPosition);
        int newCombatantInd = newList.posToCombatantInd(newItemPosition);
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
        int oldCombatantInd = oldList.posToCombatantInd(oldItemPosition);
        int newCombatantInd = newList.posToCombatantInd(newItemPosition);
        if ((2*oldCombatantInd + 1)*(2*newCombatantInd + 1) > 0 ) {
            if (oldCombatantInd >= 0) {
                // Both items are Combatants
                // Make sure both Combatant displays are identical, and that the isSelected status is the same
                boolean contentsSame = oldList.get(oldCombatantInd).displayEquals(newList.get(newCombatantInd));
                return contentsSame;
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
        int oldCombatantInd = oldList.posToCombatantInd(oldItemPosition);
        int newCombatantInd = newList.posToCombatantInd(newItemPosition);

        Combatant oldCombatant = oldList.get(oldCombatantInd);
        Combatant newCombatant = newList.get(newCombatantInd);

        // Go through each field that we care about, and record any differences
        Bundle diffs = new Bundle();
        if (!oldCombatant.getName().equals(newCombatant.getName())) {
            diffs.putString("Name", newCombatant.getName());
        }
        if (!oldCombatant.getFaction().equals(newCombatant.getFaction())) {
            diffs.putSerializable("Faction", newCombatant.getFaction());
        }
        if (!(oldCombatant.getIconIndex() == newCombatant.getIconIndex())) {
            diffs.putInt("Icon", newCombatant.getIconIndex());
        }
        if (oldCombatant.isSelected() != newCombatant.isSelected()) {
            diffs.putBoolean("Selected", newCombatant.isSelected());
        }
        if (diffs.size() == 0) {
            return null;
        }

        return diffs;
    }
}
