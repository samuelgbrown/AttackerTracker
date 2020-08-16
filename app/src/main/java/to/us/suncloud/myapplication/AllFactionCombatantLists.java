package to.us.suncloud.myapplication;

import java.util.ArrayList;

public class AllFactionCombatantLists {
    ArrayList<FactionCombatantList> allFactionLists;

    public ArrayList<FactionCombatantList> getAllFactionLists() {
        return allFactionLists;
    }

    AllFactionCombatantLists(ArrayList<FactionCombatantList> allFactionLists) {
        // This method assumes that the inputted faction list has either 0 or 1 FactionCombatantList for each faction listed in Combatant.Faction
        this.allFactionLists = allFactionLists;
    }

    public void addFactionCombatantList(FactionCombatantList listToAdd) {
        // Add a new faction combatant list, if the faction isn't already in this list
        if (!containsFaction(listToAdd.getThisFaction())) {
            allFactionLists.add(listToAdd);
        }
    }

    public FactionCombatantList getFactionList(Combatant.Faction faction) {
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (allFactionLists.get(i).getThisFaction() == faction) {
                return allFactionLists.get(i);
            }
        }

        // If we got here, then none of the faction lists match the desired faction, and we need to return an empty one.
        return new FactionCombatantList(faction);
    }

    public boolean containsFaction(Combatant.Faction factionToCheck) {
        // This method will check if the provided
        boolean containsFaction = false;
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (allFactionLists.get(i).getThisFaction() == factionToCheck) {
                containsFaction = true;
                break;
            }
        }

        return containsFaction;
    }

    public ArrayList<String> getCombatantNamesList() {
        ArrayList<String> allCombatantNames = new ArrayList<>();
        for (int cIndex = 0; cIndex < allFactionLists.size(); cIndex++) {
            // For each faction combatant list, add all combatant names to allCombatantNames
            allCombatantNames.addAll(allFactionLists.get(cIndex).getCombatantNamesList());
        }

        return allCombatantNames;
    }

}
