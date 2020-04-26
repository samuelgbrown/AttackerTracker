package to.us.suncloud.myapplication;

import java.io.Serializable;
import java.util.ArrayList;

// Simple shell for a list of combatants that also holds faction information
public class CombatantList implements Serializable {
    private ArrayList<Combatant> combatantArrayList;
    private Combatant.Faction thisFaction;

    public ArrayList<Combatant> getCombatantArrayList() {
        return combatantArrayList;
    }

    public void setCombatantArrayList(ArrayList<Combatant> combatantArrayList) {
        this.combatantArrayList = combatantArrayList;
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
}
