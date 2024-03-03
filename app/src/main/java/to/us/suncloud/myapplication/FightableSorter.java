package to.us.suncloud.myapplication;

import java.util.Comparator;
import java.util.Random;

public class FightableSorter {
    // Sorting Comparator to sort the Fightables first by Faction, then alphabetically
    static public class SortAlphabeticallyByFaction implements Comparator<Fightable> {
        private int factionToInt(Fightable.Faction f) {
            switch (f) {
                case Group:
                    return 0;
                case Party:
                    return 1;
                case Enemy:
                    return 2;
                case Neutral:
                    return 3;
                default:
                    return 10;
            }
        }

        @Override
        public int compare(Fightable fightable, Fightable t1) {
            boolean cVis = fightable.isVisible();
            boolean tVis = t1.isVisible();
            if (cVis ^ tVis) {
                // If one of the Fightables is visible, while the other is not
                return (cVis ? 0 : 1) - (tVis ? 0 : 1); // Send the invisible one to the end of the list
            }

            int fC = factionToInt(fightable.getFaction());
            int fT1 = factionToInt(t1.getFaction());
            if (fC != fT1) {
                // If the Factions are different, then just make sure they are sorted Party < Enemy < Neutral
                return fC - fT1;
            } else {
                // If the Factions are the same, sort alphabetically
                String main1 = fightable.getName().replaceAll("\\d", "");
                String main2 = t1.getName().replaceAll("\\d", "");

                if (main1.equalsIgnoreCase(main2)) {
                    String num1 = fightable.getName().replaceAll("\\D", "");
                    String num2 = t1.getName().replaceAll("\\D", "");

                    return (num1.isEmpty() ? 0 : Integer.parseInt(num1)) - (num2.isEmpty() ? 0 : Integer.parseInt(num2));
                } else {
                    return main1.compareToIgnoreCase(main2);
                }
            }
        }
    }

    static public class SortFactionList implements Comparator<FactionFightableList> {
        @Override
        public int compare(FactionFightableList o1, FactionFightableList o2) {
            return o1.faction().compareTo(o2.faction()); // Sort according to the factions
        }
    }
}
