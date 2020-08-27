package to.us.suncloud.myapplication;

import java.io.Serializable;
import java.util.ArrayList;

public class AllFactionCombatantLists implements Serializable {
    ArrayList<FactionCombatantList> allFactionLists;

    public ArrayList<FactionCombatantList> getAllFactionLists() {
        return allFactionLists;
    }

    AllFactionCombatantLists(ArrayList<FactionCombatantList> allFactionLists) {
        // Used as a shallow copy constructor
        // This method assumes that the inputted faction list has either 0 or 1 FactionCombatantList for each faction listed in Combatant.Faction (i.e. uniqueness)
        this.allFactionLists = allFactionLists;
    }

    AllFactionCombatantLists() {
        allFactionLists = new ArrayList<>();
    }

    AllFactionCombatantLists(AllFactionCombatantLists c) {
        // Perform a deep copy of c
        allFactionLists = new ArrayList<>(c.getAllFactionLists().size());

        for (int i = 0; i < c.getAllFactionLists().size(); i++) {
            allFactionLists.add(c.getAllFactionLists().get(i).clone()); // Create a clone of this FactionCombatantList, and save it to this list
        }

    }

    public void addFactionCombatantList(FactionCombatantList listToAdd) {
        // Add a new faction combatant list, if the faction isn't already in this list
        if (!containsFaction(listToAdd.faction())) {
            allFactionLists.add(listToAdd);
        }

        // TODO LATER: Do a check for name uniqueness...?  Not sure how to deal with it if something goes wrong, though...perhaps just log error and deal with the code error later?
    }

    public void addCombatant(Combatant newCombatant) {
        // If the Faction Lists contain a Combatant with this Combatant's name, then we must make the new combatant's name unique.
        // First, check what the largest existing ordinal is for this Combatant's base name

        // TODO LATER: Doing these checks could be a setting? Something like "Smart naming"?  Perhaps another setting could be if we even care about name uniqueness at all!
        int highestExistingOrdinal = getHighestOrdinalInstance(newCombatant);
        switch (highestExistingOrdinal) {
            case Combatant.DOES_NOT_APPEAR:
                // If the Combatant does not appear, then we do not need to make any modifications, regardless of what ordinal newCombatant has. Cool!
                break;
            case Combatant.NO_ORDINAL:
                // If this Combatant's base name DOES appear, but with no ordinal, then that Combatant's name must be changed
                // If newCombatant has an ordinal that's smaller than 2, then this Combatant's name must be changed to 1) preserve uniqueness, and 2) make it so that "Zombie" and "Zombie 1" don't both appear in the list (because that's weird)
                getCombatant(newCombatant.getName()).setNameOrdinal(1); // Find the existing Combatant with this Combatant's name, and set its ordinal to 1
                if (newCombatant.getOrdinal() < 2) {
                    newCombatant.setNameOrdinal(2); // Set the new Combatant's ordinal to 2
                    // If the new Combatant's ordinal is 2 or greater, then...well...it's not bothering anyone, I guess...
                }
                break;
            default:
                // If the Combatant's base name DOES appear in the list already, and it has an ordinal, then simply modify this combatant's ordinal (if needed) to be at least one higher than the current highest ordinal.
                newCombatant.setNameOrdinal(Math.max(highestExistingOrdinal + 1, newCombatant.getOrdinal()));
        }

        // Now, the Combatant and the list are *guaranteed* to unique to each other, and ready to have the Combatant added

        // Find the Faction that matches this Combatant, and add it
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (newCombatant.getFaction() == allFactionLists.get(i).faction()) {
                allFactionLists.get(i).add(newCombatant); // Upon adding, the list will automatically be sorted
                return;
            }
        }

        // If we got here, then no faction list exists for this combatant.  Therefore, we should make one
        FactionCombatantList newList = new FactionCombatantList(newCombatant.getFaction());
        newList.add(newCombatant);
        allFactionLists.add(newList);
    }

    public boolean containsName(String name) {
        boolean contains = false;
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (allFactionLists.get(i).containsName(name)) {
                contains = true;
                break;
            }
        }

        return contains;
    }


    public int getHighestOrdinalInstance(Combatant combatantToCheck) {
        return getHighestOrdinalInstance(combatantToCheck.getBaseName());
    }

    public int getHighestOrdinalInstance(String combatantBaseName) {
        // Get the highest ordinal instance of the baseName among all of the Faction lists
        int highestOrdinal = Combatant.DOES_NOT_APPEAR;
        for (int i = 0; i < allFactionLists.size(); i++) {
            // Go through each Faction, and get the highest ordinal instance of this base name
            highestOrdinal = Math.max(highestOrdinal, allFactionLists.get(i).getHighestOrdinalInstance(combatantBaseName));
        }

        return highestOrdinal;
    }

    public void removeAll(AllFactionCombatantLists combatantListToRemove) {
        // Remove all combatants present in the inputted AllFactionCombatantLists
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            // For each faction in this list...
            // Get the Combatants associated with this Faction
            FactionCombatantList thisFactionCombatantsToRemove = combatantListToRemove.getFactionList(allFactionLists.get(fac).faction());

            // Remove all of these Combatants
            allFactionLists.get(fac).removeAll(thisFactionCombatantsToRemove);
        }
    }

    public void addAll(AllFactionCombatantLists combatantListToAdd) {
        // Add all combatants present in the inputted AllFactionCombatantLists
        for (int fac = 0; fac < allFactionLists.size(); fac++) {
            // For each faction in this list...
            // Get the Combatants associated with this Faction
            FactionCombatantList thisFactionCombatantsToAdd = combatantListToAdd.getFactionList(allFactionLists.get(fac).faction());

            // Remove all of these Combatants
            allFactionLists.get(fac).addAll(thisFactionCombatantsToAdd);
        }
    }

    public FactionCombatantList getFactionList(Combatant.Faction faction) {
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (allFactionLists.get(i).faction() == faction) {
                return allFactionLists.get(i);
            }
        }

        // If we got here, then none of the faction lists match the desired faction, and we need to return an empty one.
        FactionCombatantList newList = new FactionCombatantList(faction);
        allFactionLists.add(newList);
        return newList;
    }

    public Combatant getCombatant(String name) {
        // Return the Combatant that has the inputted name (there should only ever be one, so we'll only return the first we get).  If no such name appears in the list, return a null
        for (int i = 0; i < allFactionLists.size(); i++) {
            Combatant thisCombatant = allFactionLists.get(i).get(name);
            if (thisCombatant != null) {
                return thisCombatant;
            }
        }

        // If we get here, then no such Combatant exists
        return null;
    }

    public boolean containsFaction(Combatant.Faction factionToCheck) {
        // This method will check if the provided
        boolean containsFaction = false;
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (allFactionLists.get(i).faction() == factionToCheck) {
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

    public boolean isEmpty() {
        for (int i = 0; i < allFactionLists.size(); i++) {
            if (!allFactionLists.get(i).isEmpty()) {
                // If any faction's list is not empty, then return false
                return false;
            }
        }

        // If we get here, then all of the faction lists are empty, and we should return true
        return true;
    }

    public AllFactionCombatantLists clone() {
        return new AllFactionCombatantLists(this);
    } // Deep copy
    public AllFactionCombatantLists shallowCopy() {return new AllFactionCombatantLists(getAllFactionLists());} // Shallow copy

}
