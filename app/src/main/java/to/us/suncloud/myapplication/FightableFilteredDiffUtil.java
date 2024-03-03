package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

public class FightableFilteredDiffUtil extends DiffUtil.Callback {
    public static final String DIFF_NAME = "Name";
    public static final String DIFF_FACTION = "Faction";
    public static final String DIFF_SELECTED = "Selected";
    public static final String DIFF_ICON = "iCON";
    public static final String DIFF_MULTISELECT = "Multiselect";
    public static final String DIFF_NUM_PARTY = "NumParty";
    public static final String DIFF_NUM_NEUTRAL = "NumNeutral";
    public static final String DIFF_NUM_ENEMY = "NumEnemy";

    public enum MultiSelectVisibilityChange {
        NO_CHANGE,
        START_MULTISELECT,
        END_MULTISELECT
    }

    AllFactionFightableLists oldList;
    AllFactionFightableLists newList;
    MultiSelectVisibilityChange visibilityChange;

    FightableFilteredDiffUtil(AllFactionFightableLists oldList, AllFactionFightableLists newList, MultiSelectVisibilityChange visChange) {
        this.oldList = oldList;
        this.newList = newList;
        visibilityChange = visChange;
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
        // First, see if these positions are Fightables or banners
        int oldFightableInd = oldList.posToFightableInd(oldItemPosition);
        int newFightableInd = newList.posToFightableInd(newItemPosition);
        if ((2* oldFightableInd + 1)*(2* newFightableInd + 1) > 0 ) {
            if (oldFightableInd >= 0) {
                // Both items are Fightables
                // Compare the UUID of the Fightables
                return oldList.get(oldFightableInd).getId().equals(newList.get(newFightableInd).getId());
            } else {
                // Both items are banners
                return oldFightableInd == newFightableInd;
            }
        } else {
            // The items at each position represent different types (one is a banner and the other is a Fightable
            return false;
        }
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Check if ALL of the values are the same

        // First, see if these positions are Fightable or banners
        int oldFightableInd = oldList.posToFightableInd(oldItemPosition);
        int newFightableInd = newList.posToFightableInd(newItemPosition);

        if ((2* oldFightableInd + 1)*(2* newFightableInd + 1) > 0 ) {
            if (oldFightableInd >= 0) {
                // Both items are Fightable
                if (visibilityChange != MultiSelectVisibilityChange.NO_CHANGE) {
                    // If the multi-select visibility is changing, contents are different
                    return false;
                } else {
                    // Make sure both Fightable displays are identical, and that the isSelected status is the same
                    return oldList.get(oldFightableInd).displayEquals(newList.get(newFightableInd));
                }
            } else {
                // Both items are banners
                return oldFightableInd == newFightableInd;
            }
        } else {
            // The items at each position represent different types (one is a banner and the other is a Fightable
            return false;
        }
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        // For this list, we only care about name, faction, icon, and isSelected status
        int oldFightableInd = oldList.posToFightableInd(oldItemPosition);
        int newFightableInd = newList.posToFightableInd(newItemPosition);

        Fightable oldFightable = oldList.get(oldFightableInd);
        Fightable newFightable = newList.get(newFightableInd);

        // Go through each field that we care about, and record any differences
        Bundle diffs = new Bundle();
        if (!oldFightable.getName().equals(newFightable.getName())) {
            diffs.putString(DIFF_NAME, newFightable.getName());
        }
        if (!oldFightable.getFaction().equals(newFightable.getFaction())) {
            diffs.putSerializable(DIFF_FACTION, newFightable.getFaction());
        }
        if (oldFightable.isSelected() != newFightable.isSelected()) {
            diffs.putBoolean(DIFF_SELECTED, newFightable.isSelected());
        }
        if ((oldFightable instanceof Combatant) && (newFightable instanceof  Combatant))
        {
            Combatant oldCombatant = (Combatant) oldFightable;
            Combatant newCombatant = (Combatant) newFightable;
            if (!(oldCombatant.getIconIndex() == newCombatant.getIconIndex())) {
                diffs.putInt(DIFF_ICON, newCombatant.getIconIndex());
            }
        }
        if ((oldFightable instanceof CombatantGroup) && (newFightable instanceof CombatantGroup))
        {
            CombatantGroup oldCombatantGroup = (CombatantGroup) oldFightable;
            CombatantGroup newCombatantGroup = (CombatantGroup) newFightable;
            if ( oldCombatantGroup.getTotalCombatantsInFaction(Fightable.Faction.Party) !=
                    newCombatantGroup.getTotalCombatantsInFaction(Fightable.Faction.Party))
            {
                diffs.putInt(DIFF_NUM_PARTY,
                        newCombatantGroup.getTotalCombatantsInFaction(Fightable.Faction.Party));
            }
            if ( oldCombatantGroup.getTotalCombatantsInFaction(Fightable.Faction.Neutral) !=
                    newCombatantGroup.getTotalCombatantsInFaction(Fightable.Faction.Neutral))
            {
                diffs.putInt(DIFF_NUM_NEUTRAL,
                        newCombatantGroup.getTotalCombatantsInFaction(Fightable.Faction.Neutral));
            }
            if ( oldCombatantGroup.getTotalCombatantsInFaction(Fightable.Faction.Enemy) !=
                    newCombatantGroup.getTotalCombatantsInFaction(Fightable.Faction.Enemy))
            {
                diffs.putInt(DIFF_NUM_ENEMY,
                        newCombatantGroup.getTotalCombatantsInFaction(Fightable.Faction.Enemy));
            }
        }
        if ( visibilityChange != MultiSelectVisibilityChange.NO_CHANGE )
        {
            diffs.putSerializable(DIFF_MULTISELECT, visibilityChange);
        }

        if (diffs.size() == 0) {
            return null;
        }

        return diffs;
    }
}
