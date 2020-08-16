package to.us.suncloud.myapplication;

import java.io.Serializable;
import java.util.ArrayList;

// Simple shell for a list of combatants that also holds faction information
public class FactionCombatantList implements Serializable {
    private ArrayList<Combatant> combatantArrayList;
    private Combatant.Faction thisFaction;

    FactionCombatantList(ArrayList<Combatant> combatantArrayList, Combatant.Faction thisFaction) {
        this.thisFaction = thisFaction;
        this.combatantArrayList = combatantArrayList;
    }

    FactionCombatantList(Combatant.Faction thisFaction) {
        this.thisFaction = thisFaction;
        combatantArrayList = new ArrayList<>();
    }

    public ArrayList<Combatant> getCombatantArrayList() {
        return combatantArrayList;
    }

    public void setCombatantArrayList(ArrayList<Combatant> combatantArrayList) {
        this.combatantArrayList = combatantArrayList;
    }

    public ArrayList<String> getCombatantNamesList() {
        ArrayList<String> allCombatantNames = new ArrayList<>();
        for (int cIndex = 0; cIndex < combatantArrayList.size(); cIndex++) {
            // For each combatant, add the combatant's name to allCombatantNames
            allCombatantNames.add(combatantArrayList.get(cIndex).getName());
        }

        return allCombatantNames;
    }

    public void addCombatant(Combatant newCombatant) {
        combatantArrayList.add(newCombatant);
    }

    public void removeCombatant(Combatant combatant) {
        combatantArrayList.remove(combatant);
    }

    public Combatant.Faction getThisFaction() {
        return thisFaction;
    }

    public void setThisFaction(Combatant.Faction thisFaction) {
        this.thisFaction = thisFaction;
    }

    public int size() {
        return combatantArrayList.size();
    }
}
