package to.us.suncloud.myapplication;

import android.content.Context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

// This Combatant list is used for the encounter.  It keeps a list of Combatants that are not organized/separated (unlike the AllFactionsCombatantList) that can be reorganized easily.
// It will also perform all functions relevant to calculating initiative
public class EncounterCombatantList implements Serializable {
    private ArrayList<Combatant> combatantArrayList = new ArrayList<>();
    private ArrayList<Integer> duplicateInitiatives; // Keep track of any initiative values that are duplicated across multiple Combatants
    private ArrayList<HashMap<UUID, Integer>> diceRollList = new ArrayList<>(); // Keep track of all dice rolls from previous rounds
    private ArrayList<HashMap<UUID, Integer>> modifierList = new ArrayList<>(); // Keep track of all modifier values from each round
    private int lastRecordedRoundNumber = 1; // The last round number that was set using setRoundNumber - used to store the values of all modifiers for each round
    private SortMethod currentSortMethod = SortMethod.INITIATIVE;
    private InitPrefs prefs; // The preferences Object that dictates how the combat cycle should flow

    private static Random rand = new Random(); // A random number generator (static, so that each Combatant uses the same one, and it does not just use the system clock as a first time seed each time

    public EncounterCombatantList(Context context) {
        combatantArrayList = new ArrayList<>();
        setPrefs(context);
    }

    public EncounterCombatantList(ArrayList<Combatant> combatantArrayList, Context context) {
        setPrefs(context);
        for (int i = 0; i < combatantArrayList.size(); i++) {
            this.combatantArrayList.add(combatantArrayList.get(i).clone()); // Create a clone of the referenced Combatant, and save it
        }

        initializeCombatants();
        doSorting();
        updateDuplicateInitiatives();
    }

    public EncounterCombatantList(AllFactionCombatantLists factionList, Context context) {
        setPrefs(context);
        setCombatantArrayList(factionList);
    }

    public EncounterCombatantList(EncounterCombatantList c) {
        // Perform a deep copy of the incoming EncounterCombatantList
        for (int i = 0; i < c.size(); i++) {
            combatantArrayList.add(c.get(i).clone()); // Create a clone of the Combatant, and save it to this object's ArrayList
        }

        currentSortMethod = c.getCurrentSortMethod(); // List should already be in a sorted state
        duplicateInitiatives = c.getDuplicateInitiatives(); // List should already be in a prepared state
        diceRollList = c.getDiceRollList();
        prefs = c.getPrefs();
    }

    public InitPrefs getPrefs() {
        return prefs;
    }

    public boolean isMidCombat() {
        // Are we in the middle of combat?  Determined simply by whether or not the diceRollList is empty
        return !diceRollList.isEmpty();
    }

    public void setPrefs(Context context) {
        // Create new preferences based on the current context
        InitPrefs newPrefs = new InitPrefs(context);

        // If the dice size is different now than it was before, then clear the dice roll list
        if (prefs != null) {
            if (prefs.getDiceSize() != newPrefs.getDiceSize()) {
                diceRollList = new ArrayList<>();
            }
        }

        // Save the new Preferences
        prefs = newPrefs;
    }

    public ArrayList<Combatant> getCombatantArrayList() {
        return combatantArrayList;
    }

//    public void setCombatantArrayList(ArrayList<Combatant> combatantArrayList) {
//        this.combatantArrayList = combatantArrayList;
//        initializeCombatants();
//        doSorting();
//        updateDuplicateInitiatives();
//    }

    public void setCombatantArrayList(AllFactionCombatantLists factionList) {
        AllFactionCombatantLists clonedList = factionList.clone(); // First, make a cloned version of the list, so we don't affect any of the "real" copies of the Combatants
        combatantArrayList = new ArrayList<>(); // Clear the current list of Combatants
        for (int i = 0; i < clonedList.getAllFactionLists().size(); i++) {
            // For each faction, add all of the Combatants to this object's ArrayList
            combatantArrayList.addAll(clonedList.getAllFactionLists().get(i).getCombatantArrayList());
        }

        initializeCombatants();
        doSorting();
        updateDuplicateInitiatives();
    }

    public void updateCombatants(AllFactionCombatantLists factionList) {
        // Add any new Combatants
        // First, figure out what we should initialize the new Combatants to (I really hate this, there HAS to be a better way...I think isSelected may just need to become an enum
        int curPhase = calcActiveCombatant();
        final boolean initSelect = (curPhase == EncounterCombatantRecyclerAdapter.PREP_PHASE); // If we are in the preparation phase, then initialize selected to true (to stay in this phase).  If we are not in the preparatory phase (we are in the middle of a combat round), initialize selected to false, because the Combatant has not gone yet (god, I hate this so much...)

        // Assumed to be unique by ID, because the names may have changed (i.e. adding a new second copy of a given Combatant, changing the first's ordinal to 1)
        AllFactionCombatantLists clonedList = factionList.clone(); // First, make a cloned version of the list, so we don't affect any of the "real" copies of the Combatants
        ArrayList<FactionCombatantList> factionLists = clonedList.getAllFactionLists();
        for (int facInd = 0; facInd < factionLists.size(); facInd++) {
            // For each faction, add any Combatants that are not already in this list
            FactionCombatantList thisFactionList = factionLists.get(facInd);
            for (Combatant cNew : thisFactionList.getCombatantArrayList()) {
                // For each Combatant in the faction list
                if (!idExists(cNew.getId())) {
                    // If cNew does not exist in this EncounterCombatantList, then add it, and initialize it
                    Combatant cToAdd = cNew.clone();
                    cToAdd.setSelected(initSelect); // Initialize the selection according to where we are in the combat cycle
                    combatantArrayList.add(cToAdd); // Add the new Combatant
                }
            }
        }

        // Finally, perform any post-processing (sorting, etc)
        doSorting();
        updateDuplicateInitiatives();
    }

    private boolean idExists(UUID id) {
        for (Combatant c : combatantArrayList) {
            // ...go through each Combatant in this EncounterCombatantList, and check if it exists
            if (id.equals(c.getId())) {
                return true; // The ID exists in the list!
            }
        }
        return false; // The ID does not exist the combatantArrayList
    }

    private void initializeCombatants() {
        // In a kind of annoying quirk of how the data is being stored, all Combatants being checked off is how the prep phase is indicated in calcActiveCombatant()
        // So, if we want to start off in the prep phase (and we do), we need to check off every Combatant.  Yay...
        for (Combatant c : combatantArrayList) {
            c.setSelected(true);
        }
    }

    public SortMethod getCurrentSortMethod() {
        return currentSortMethod;
    }

    public int calcActiveCombatant() {
        // Calculate the currently active Combatant in the list
        //      The first Combatant in the final group of unchecked Combatants
        //          XOOXXOOOOOXX
        //               ^ <- currently active Combatant
        ArrayList<Combatant> sortedCombatants; // Get a list of Combatants sorted by Initiative
        if (currentSortMethod != SortMethod.INITIATIVE) {
            // If the current Combatant List is not sorted by initiative, do so
            sortedCombatants = (ArrayList<Combatant>) combatantArrayList.clone(); // Create a (shallow) copy of the Combatants List
            Collections.sort(sortedCombatants, new CombatantSorter.SortByInitiative(prefs.getSortOrder())); // Sort the new list by Initiative
        } else {
            sortedCombatants = combatantArrayList;
        }

        // Go through each Combatant, and checked whether or not it has been checked off
        // Note - Special cases: If no Combatants have been checked off, then the first is active.  If all Combatants have been checked off, then we are in the prep phase
        boolean foundUnSelected = false;
        for (int combInd = (sortedCombatants.size() - 1); combInd >= 0; combInd--) {
            // Go through the sorted Combatant list in reverse initiative order
            Combatant c = sortedCombatants.get(combInd);
            if (!(foundUnSelected || c.isSelected())) {
                // We found at least one unselected Combatant (going in reverse order)
                foundUnSelected = true;
            }

            if (foundUnSelected && c.isSelected()) {
                // If we found the beginning of the final group of unselected Combatants, then the currently active Combatant is the one after this Combatant
                return combInd + 1;
            }

            // OLD
//            if (c.isSelected()) {
//                // We found the last selected Combatant
//                // Depending on where we are in the list, it can mean two things
//                if (combInd == (sortedCombatants.size() - 1)) {
//                    // The last Combatant has been checked off, so there actually isn't a currently active Combatant; we are in the prep-phase
//                    return EncounterCombatantRecyclerAdapter.PREP_PHASE;
//                } else {
//                    // One of the mid-initiative Combatants has been checked off, so the next in order is the currently active Combatant
//                    return combInd + 1;
//                }
//            }
        }

        // If we got here, then all of the Combatants are selected, and therefore we are in the preparation phase
        if (foundUnSelected) {
            // If all of the Combatants are unselected, then the first Combatant is currently active
            return 0;
        } else {
            // If all of the Combatants are selected, then we are in the prep-phase
            return EncounterCombatantRecyclerAdapter.PREP_PHASE;
        }

        // OLD
//        // If we got here, then none of the Combatants have been selected yet, so the first is our currently active Combatant
//        return 0;
    }

    public ArrayList<Integer> getDuplicateInitiatives() {
        return duplicateInitiatives;
    }

    public ArrayList<HashMap<UUID, Integer>> getDiceRollList() {
        return diceRollList;
    }

    public void sort(SortMethod sortMethod) {
        // Sort according to some rule
        if (currentSortMethod != sortMethod) {
            // If the requested sort method is different, then adjust
            currentSortMethod = sortMethod;
            doSorting();
        }
    }

    public void resort() {
        // Sort the list again, using the current sort method
        doSorting();
    }

    private void doSorting() {
        // Depending on the current sorting style (currentSortMethod), sort the contents of combatantArrayList
        switch (currentSortMethod) {
            case INITIATIVE:
                Collections.sort(combatantArrayList, new CombatantSorter.SortByInitiative(prefs.getSortOrder())); // Sort the combatantArrayList alphabetically
                return;
            case ALPHABETICALLY_BY_FACTION:
                Collections.sort(combatantArrayList, new CombatantSorter.SortAlphabeticallyByFaction()); // Sort the combatantArrayList alphabetically by faction
        }
    }

    public void setRoundNumber(int roundNumber) {
        // First, get the diceRollMap for this round
        HashMap<UUID, Integer> diceRollMap;
        if (prefs.isReRollInit()) {
            // If initiative is being re-rolled every round, then make sure we have the correct roll map for this round
            if (roundNumber > diceRollList.size()) {
                // This is a new round, so generate new rolls
                diceRollMap = rollAllInitiative();

                diceRollList.add(diceRollMap); // Add a new HashMap for this round, because it is brand new
            } else {
                // If this is an "old" round (one that we already roll initiative for), then just retrieve the old values that we rolled already
                diceRollMap = diceRollList.get(roundNumber - 1); // roundNumber is a user-facing variable, so it starts at 1 (ick)
            }
        } else {
            // If we are using the same initiative roll for each round, then roll once and just hang onto it in the first position of diceRollList
            if (diceRollList.isEmpty()) {
                // diceRollList is empty, so we need to roll initiative now!
                diceRollMap = rollAllInitiative();
                diceRollList.add(diceRollMap); // Add the new dice map to the empty list
            } else {
                // We can just grab the initiative we rolled previously
                diceRollMap = diceRollList.get(0);
            }
        }

        // TODO Cases:
        //  1: final combatant to prep phase (go forwards)
        //      cur = last + 1
        //      Store current mods under last (special: if last = 0, no save)
        //      Retrieve cur, if it exists
        //  2: prep phase to final combatant (go backwards)
        //      cur = last - 1
        //      Store current mods under last
        //      Retrieve cur
        //  3: same round (same)
        //      cur = last
        //      Don't store any
        //      Don't retrieve any

        // Then, save the current modifier map to the modifierList, so we can retrieve it later
        HashMap<UUID, Integer> modifierMap = new HashMap<>();
        for (Combatant c : combatantArrayList) {
            // For each Combatant, get the Combatant's modifier, and save it to modifierMap, keyed to the Combatant's ID
            modifierMap.put(c.getId(), c.getModifier());
        }

        // TODO START HERE: Test this!
        // Save the modifierMap if we are changing rounds
        if (lastRecordedRoundNumber != roundNumber) {
            while (modifierList.size() < lastRecordedRoundNumber) {
                modifierList.add(new HashMap<UUID, Integer>()); // Add enough empty HashMaps to modifierList such that the modifierList has a HashMap at position lastRecordedRoundNumber, so ArrayList<>#set() can be used
            }
            modifierList.set(lastRecordedRoundNumber - 1, modifierMap);

            // Next get the modifierMap for this round, if needed (otherwise, use the current modifierMap, as seen on screen, that is currently saved in modifierMap)
            if (roundNumber <= modifierList.size()) {
                // If the current round number has been done before, then retrieve the saved modifier map
                modifierMap = modifierList.get(roundNumber - 1);
            }

            // Save this round number, so we know what we last saved the modifier list
            lastRecordedRoundNumber = roundNumber;
        }


        // Now that we have the dice rolls/modifiers that we are going to use, assign them to each Combatant
        for (Combatant combatant : combatantArrayList) {
            // For each Combatant, retrieve the roll, and set it to the Combatant
            if (diceRollMap.containsKey(combatant.getId())) {
                combatant.setRoll(diceRollMap.get(combatant.getId()));
            } else {
                // If the diceRollMap does not contain this key (this Combatant did not have a roll associated with it), then generate a new roll for this Combatant, and add it to the map
                int newRoll = getInitiativeRoll();
                combatant.setRoll(newRoll);
                diceRollMap.put(combatant.getId(), newRoll); // Will be saved in diceRollList
            }

            // For each Combatant, retrieve the modifier, and set it to the Combatant
            if (modifierMap.containsKey(combatant.getId())) {
                combatant.setModifier(modifierMap.get(combatant.getId()));
            } else {
                // If the modifierMap does not contain this key (this Combatant did not have a modifier associated with it), then get the  Combatant's current modifier (its default modifier), and add it to the map
                modifierMap.put(combatant.getId(), combatant.getModifier()); // Will be saved in modifierList
            }
        }

        // Update the duplicateInitiatives list
        doSorting(); // Sort the list now that the rolls/modifiers have likely changed
        updateDuplicateInitiatives(); // See if any Combatants have duplicate initiatives, so we can display an indicator on screen
    }

    private HashMap<UUID, Integer> rollAllInitiative() {
        // Generate an initiative roll for each Combatant, assign the rolls to a HashMap keyed by each Combatant's UUID, and return it

        // Create a new Map
        HashMap<UUID, Integer> newDiceMap = new HashMap<>();

        // For each Combatant, roll initiative
        for (Combatant combatant : combatantArrayList) {
            // Store the new rolls for each Combatant into the diceRollList, keyed by the Combatant's ID
            newDiceMap.put(combatant.getId(), getInitiativeRoll());
        }

        // Return the Map
        return newDiceMap;
    }

    public int getInitiativeRoll() {
        // TODO: Adjust according to Settings
        // Find a pseudorandom number between 1 and 10, assign it to the roll member variable, and re-calculate the total initiative
        return rand.nextInt(prefs.getDiceSize()) + 1; // Calculate a random number between [0 10), and adjust it so it produces a roll between [1 10]
    }

    public void updateDuplicateInitiatives() {
        // Update the duplicateInitiative array
        HashSet<Integer> existingInitiativeValues = new HashSet<>(); // Keep track of which values have already been found as initiatives in the Combatant List
        duplicateInitiatives = new ArrayList<>();
        for (Combatant c : combatantArrayList) {
            // For each Combatant, get its initiative
            int thisInit = c.getTotalInitiative();

            if (!existingInitiativeValues.contains(thisInit)) {
                // If this Combatant's initiative does NOT exist in the HashSet, then add it
                existingInitiativeValues.add(thisInit);
            } else {
                // If this Combatant's initiative DOES exist in the HashSet (another Combatant has this initiative as well), then add it to the duplicateInitiative ArrayList (if it does not exist in there already)
                if (!duplicateInitiatives.contains(thisInit)) {
                    duplicateInitiatives.add(thisInit);
                }
            }
        }

        // We now have an ArrayList<Integer> duplicateInitiatives that contains each initiative that is used by more than one Combatant
        Collections.sort(duplicateInitiatives); // Now sort the list, and we have a finished duplicateInitiatives ArrayList
    }

    public boolean isDuplicate(int i) {
        return duplicateInitiatives.contains(combatantArrayList.get(i).getTotalInitiative()); // If the list contains this Combatant's initiative, then it is a duplicate
    }

    public int getDuplicateColor(int i) {
        if (duplicateInitiatives.contains(combatantArrayList.get(i).getTotalInitiative())) {
            return duplicateInitiatives.indexOf(combatantArrayList.get(i).getTotalInitiative()) % 2; // Get the index of the Combatant's total initiative in the sorted duplicateInitiatives array, modulo 2 (so that the color alternates with increasing initiative....cuz it looks nice)
        } else {
            // If this initiative is not the in the List, just return -1
            return -1;
        }
    }

    public int size() {
        return combatantArrayList.size();
    }

    public Combatant get(int i) {
        return combatantArrayList.get(i);
    }

    public EncounterCombatantList clone() {
        return new EncounterCombatantList(this);
    }

    public int getInitiativeIndexOf(int indexInCurrentList) {
        // TODO: May want to try and optimize this...?  Store the sorted list, and update it every time it changes? Will need to figure out how to know when roll/modifier is changed, though...
        // A method to find the initiative index in combatantArrayList of a Combatant, regardless of the actual sorted state of the list
        if (currentSortMethod == SortMethod.INITIATIVE) {
            // If the array is already sorted by initiative, then just return the input index
            return indexInCurrentList;
        } else {
            // The combatantArrayList is currently sorted alphabetically_by_faction, so we're going to need to sort the list, then get the index of the Combatant
            Combatant c = combatantArrayList.get(indexInCurrentList); // Get the Combatant at the indicated point in the current lis
            ArrayList<Combatant> sortedList = new ArrayList<>(combatantArrayList); // Create a SHALLOW copy of the list (the Combatants themselves are not cloned, only their references are copied)

            // Do initiative sorting on the sortedList
            Collections.sort(sortedList, new CombatantSorter.SortByInitiative(prefs.getSortOrder()));

            // Finally, find the index in the initiative sorted list of the selected Combatant
            return sortedList.indexOf(c);
        }
    }

    public int getViewIndexOf(int indexInInitiativeOrder) {
        // A method to find the current index in this list of a Combatant given its position in the initiative order
        if (currentSortMethod == SortMethod.INITIATIVE) {
            // If the array is already sorted by initiative, then just return the input index
            return indexInInitiativeOrder;
        } else {
            // The combatantArrayList is currently sorted alphabetically_by_faction, so we're going to need to sort the list
            ArrayList<Combatant> sortedList = new ArrayList<>(combatantArrayList); // Create a SHALLOW copy of the list (the Combatants themselves are not cloned, only their references are copied)

            // Do initiative sorting on the sortedList
            Collections.sort(sortedList, new CombatantSorter.SortByInitiative(prefs.getSortOrder()));
            Combatant c = sortedList.get(indexInInitiativeOrder);

            return combatantArrayList.indexOf(c);
        }
    }

    public boolean isEmpty() {
        return combatantArrayList.isEmpty();
    }

    public enum SortMethod {
        INITIATIVE,
        ALPHABETICALLY_BY_FACTION
    }

    class InitPrefs implements Serializable {
        private CombatantSorter.sortOrder sortOrder;
        private boolean reRollInit;
        private int diceSize;

        InitPrefs(Context context) {
            // Using the InitPrefsHelper, populate this object
            sortOrder = InitPrefsHelper.getSortOrder(context);
            reRollInit = InitPrefsHelper.getReRollInit(context);
            diceSize = InitPrefsHelper.getDiceSize(context);
        }

        public CombatantSorter.sortOrder getSortOrder() {
            return sortOrder;
        }

        public boolean isReRollInit() {
            return reRollInit;
        }

        public int getDiceSize() {
            return diceSize;
        }
    }
}
