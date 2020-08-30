package to.us.suncloud.myapplication;

import androidx.recyclerview.widget.DiffUtil;

public class CombatantFilteredDiffUtil extends DiffUtil.Callback {
    AllFactionCombatantLists oldList;
    AllFactionCombatantLists newList;

    CombatantFilteredDiffUtil(AllFactionCombatantLists oldList, AllFactionCombatantLists newList) {
        // TODO CHECK: Does this really need the filter text...?  If not, then can basically use same CombatantFilteredDiffUtil as Encounter Activity...
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
                // Make sure both Combatant displays are identical
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
}
