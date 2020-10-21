package to.us.suncloud.myapplication;

import java.util.Comparator;
import java.util.Random;

public class CombatantSorter {
    // Sorting Comparator to sort the Combatants by increasing Total Initiative
    static public class SortByInitiative implements Comparator<Combatant> {
        sortOrder sortOrder; // The sort order for initiative (low to high, or high to low)
        tieBreaker tieBreaker; // The type of initiative tie-breaker to use
        long randomSeed; // The random seed to use for the random value calculation (Note: Should be 1) different between rounds (dependent on round number), 2) consistent when revisiting to each round [i.e. scrolling through history] (dependent on round number), and 3) different between encounters)

        SortByInitiative(sortOrder sortOrder, tieBreaker tieBreaker, long randomSeed) {
            this.sortOrder = sortOrder;
            this.tieBreaker = tieBreaker;
            this.randomSeed = randomSeed;
        }

        @Override
        public int compare(Combatant combatant, Combatant t1) {
            // First check if one Combatant should be sent to the end because it is invisible
            boolean cVis = combatant.isVisible();
            boolean tVis = t1.isVisible();
            if (cVis ^ tVis) {
                // If one of the Combatants is visible, while the other is not
                return (cVis ? 0 : 1) - (tVis ? 0 : 1); // Send the invisible one to the end of the list
            }

            // I will calculate the low-to-high initiative, then reverse it at the end if needed
            int L2HInit;
            int cTI = combatant.getTotalInitiative();
            int tTI = t1.getTotalInitiative();

            if (cTI != tTI) {
                // If the total initiatives are different, then it's a simple sort
                L2HInit = cTI - tTI; // Make the low initiative Combatants appear first
            } else {
                //  If the total initiatives are the same, then sort according to the SortAlphabeticallyByFaction class
                //  D&D Original: Must still look into
                //  AD&D 1/2: Actions are simultaneous (either random, or could do the alphabetically-by-faction route here?)
                //  3e/3.5e: Combatant with higher Dex bonus goes first, otherwise random
                //  4e: Combatant with higher bonus (Dex+lvl+mods) goes first, otherwise random
                //  5e: Random (or player determined), but it's a house rule
                //  Note: If it's random, it must be CONSISTENTLY random...how?  I'm not sure...do a computation on the round number, perhaps?
                //  TODO NOTE: Have an alert to the player when this is changed that it won't delete data, but ties in previous rounds will be recalculated?

                switch (tieBreaker) {
                    case Modifier:
                        // Compare the initiative modifiers
                        int cM = combatant.getModifier();
                        int tM = t1.getModifier();
                        if (cM != tM) {
                            // If the modifiers are different, return the difference
                            L2HInit = cM - tM;
                        } else {
                            // If the modifiers are the same, randomly select one
                            L2HInit = getRandDiff(randomSeed, combatant, t1);
                        }

                        // If the initiative modifiers are also identical, then just randomly select the order
                        // Create a random number generator based on the random seed
                    case Random:
                        // Create a random number generator based on the random seed, as well as each Combatant's IDs
                        L2HInit = getRandDiff(randomSeed, combatant, t1);
                    default:
                        // Used if tieBreaker == alphaByFaction
                        // Simply use the alphabetically by faction ordering
                        L2HInit = new SortAlphabeticallyByFaction().compare(combatant, t1);
                }
            }

            switch (sortOrder) {
                case LowToHigh:
                    return L2HInit;
                case HighToLow:
                    return -L2HInit;
                default:
                    return L2HInit;
            }
        }

        private static int getRandDiff(long seed, Combatant c, Combatant t) {
            Random randC = new Random(seed + c.getId().getMostSignificantBits());
            Random randT = new Random(seed + t.getId().getMostSignificantBits());

            // Get unique, random values for each Combatant, and return the difference
            return randC.nextInt() - randT.nextInt();
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
            boolean cVis = combatant.isVisible();
            boolean tVis = t1.isVisible();
            if (cVis ^ tVis) {
                // If one of the Combatants is visible, while the other is not
                return (cVis ? 0 : 1) - (tVis ? 0 : 1); // Send the invisible one to the end of the list
            }

            int fC = factionToInt(combatant.getFaction());
            int fT1 = factionToInt(t1.getFaction());
            if (fC != fT1) {
                // If the Factions are different, then just make sure they are sorted Party < Enemy < Neutral
                return fC - fT1;
            } else {
                // If the Factions are the same, sort alphabetically
                String main1 = combatant.getName().replaceAll("\\d", "");
                String main2 = t1.getName().replaceAll("\\d", "");

                if (main1.equalsIgnoreCase(main2)) {
                    String num1 = combatant.getName().replaceAll("\\D", "");
                    String num2 = t1.getName().replaceAll("\\D", "");

                    return (num1.isEmpty() ? 0 : Integer.parseInt(num1)) - (num2.isEmpty() ? 0 : Integer.parseInt(num2));
                } else {
                    return main1.compareToIgnoreCase(main2);
                }
            }
        }
    }

    static public class SortFactionList implements Comparator<FactionCombatantList> {
        @Override
        public int compare(FactionCombatantList o1, FactionCombatantList o2) {
            return o1.faction().compareTo(o2.faction()); // Sort according to the factions
        }
    }

    // What directional order the initiative should be sorted
    enum sortOrder {
        LowToHigh,
        HighToLow
    }

    // What to do in the case of an initiative tie
    enum tieBreaker {
        AlphaByFaction, // Just use the SortAlphabeticallyByFaction
        Modifier, // Sort according to the modifier, then random
        Random // Sort (consistently!) randomly
    }
}
