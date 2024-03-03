package to.us.suncloud.myapplication;

import java.util.Comparator;

public class CombatantGroupDataSorter {
    // Sorting Comparator to sort the Fightables first by Faction, then alphabetically
    public static class SortCombatantData implements Comparator<CombatantGroup.CombatantGroupData> {
        AllFactionFightableLists referenceList;
        SortCombatantData(AllFactionFightableLists referenceList) {
            this.referenceList = referenceList;
        }

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
        public int compare(CombatantGroup.CombatantGroupData data, CombatantGroup.CombatantGroupData d1) {
            int fC = factionToInt(data.mFaction);
            int fT1 = factionToInt(d1.mFaction);
            if (fC != fT1) {
                // If the Factions are different, then just make sure they are sorted Party < Enemy < Neutral
                return fC - fT1;
            } else {
                // If the Factions are the same, sort alphabetically
                String s1 = referenceList.getFightableWithID(data).getName();
                String s2 = referenceList.getFightableWithID(d1).getName();

                // First, compare only the base names
                String main1 = s1.replaceAll("\\d", "");
                String main2 = s2.replaceAll("\\d", "");

                if (main1.equalsIgnoreCase(main2)) {
                    // Compare the numbers
                    String num1 = s1.replaceAll("\\D", "");
                    String num2 = s2.replaceAll("\\D", "");

                    return (num1.isEmpty() ? 0 : Integer.parseInt(num1)) - (num2.isEmpty() ? 0 : Integer.parseInt(num2));
                } else {
                    return main1.compareToIgnoreCase(main2);
                }
            }
        }
    }
}