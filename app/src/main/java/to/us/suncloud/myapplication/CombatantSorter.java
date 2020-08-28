package to.us.suncloud.myapplication;

import java.util.Comparator;

import to.us.suncloud.myapplication.Combatant;

public class CombatantSorter {
    // Sorting Comparator to sort the Combatants by increasing Total Initiative
    static public class SortByInitiative implements Comparator<Combatant> {
        @Override
        public int compare(Combatant combatant, Combatant t1) {
            int cTI = combatant.getTotalInitiative();
            int t1TI = t1.getTotalInitiative();
            // TODO: This comparison method can be changed according to which version we're using by using a method (perhaps set the method in the Constructor of this object?
            if (cTI != t1TI) {
                // If the total initiatives are different, then it's a simple sort
                return cTI - t1TI;
            } else {
                //  If the total initiatives are the same, then sort according to the SortAlphabeticallyByFaction class
                return new SortAlphabeticallyByFaction().compare(combatant, t1);
            }
        }
    }

    // Sorting Comparator to sort the Combatants first by Faction, then alphabetically
    static public class SortAlphabeticallyByFaction implements Comparator<Combatant> {
        private int factionToInt(Combatant.Faction f) {
            switch (f) {
                case Party:
                    return 0;
                case Enemy:
                    return 1;
                case Neutral:
                    return 2;
                default:
                    return 10;
            }
        }

        @Override
        public int compare(Combatant combatant, Combatant t1) {
            int fC = factionToInt(combatant.getFaction());
            int fT1 = factionToInt(t1.getFaction());
            if (fC != fT1) {
                // If the Factions are different, then just make sure they are sorted Party < Enemy < Neutral
                return fC - fT1;
            } else {
                // If the Factions are the same, sort alphabetically
                return combatant.getName().compareToIgnoreCase(t1.getName());
            }
        }
    }

    static public class SortFactionList implements Comparator<FactionCombatantList> {
        @Override
        public int compare(FactionCombatantList o1, FactionCombatantList o2) {
            return o1.faction().compareTo(o2.faction()); // Sort according to the factions
        }
    }
}
