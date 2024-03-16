package to.us.suncloud.myapplication;

import android.content.Context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

// This Combatant list is used for the encounter.  It keeps a list of Combatants that are not organized/separated (unlike the AllFactionsCombatantList) that can be reorganized easily.
// It will also perform all functions relevant to calculating initiative
public class EncounterCombatantList implements Serializable {
    private static final int ALL_COMBATANTS_CHECKED = -3; // A state where all Combatants are checked off
    private ArrayList<Combatant> combatantArrayList = new ArrayList<>();
    private ArrayList<Integer> duplicateInitiatives; // Keep track of any initiative values that are duplicated across multiple Combatants
    private ArrayList<HashMap<UUID, Integer>> diceRollList = new ArrayList<>(); // Keep track of all dice rolls from previous rounds
    private ArrayList<HashMap<UUID, Integer>> modifierList = new ArrayList<>(); // Keep track of all modifier values from each round
    private int lastRecordedRoundNumber = 1; // The last round number that was set using setRoundNumber - used to store the values of all modifiers for each round
    private SortMethod currentSortMethod = SortMethod.INITIATIVE;
    private InitPrefs prefs; // The preferences Object that dictates how the combat cycle should flow
    private boolean haveHadPrepPhase; // Has the preparation phase been completed yet?  Used to determine if all Combatants are checked because we are preparing now, or because we are in the end-of-round actions phase
    private final HashSet<UUID> combatantsToRemove = new HashSet<>(); // Used only when Combatants are removed when an Encounter is resumed.  This Set saves the Combatants that must be removed in the upcoming round (used to synchronized visibility and the diceMap)
    private final HashSet<UUID> combatantsToAdd = new HashSet<>(); // Used only when Combatants are added when an Encounter is resumed.  This Set saves the Combatants that must be added in the upcoming round (used to synchronized visibility and the diceMap)

    private static final Random rand = new Random(); // A random number generator (static, so that each Combatant uses the same one, and it does not just use the system clock as a first time seed each time
    private long encounterRandomSeed = rand.nextLong(); // A random number, used for random tie breaking, that stays consistent for this entire Encounter

    public EncounterCombatantList(Context context) {
        combatantArrayList = new ArrayList<>();
        setPrefs(context);
    }

    private EncounterCombatantList(ArrayList<Combatant> combatantArrayList, InitPrefs newPrefs) {
        // NOTE: This is the only constructor that does NOT include safety features (sorting, initializing,etc).  combatantArrayList is assumed to have come from an otherwise already initialized EncounterCombatantList
        // Set up the preferences)
        prefs = newPrefs;

        // Copy over the new Combatant List
        for (int i = 0; i < combatantArrayList.size(); i++) {
            this.combatantArrayList.add(combatantArrayList.get(i).clone()); // Create a clone of the referenced Combatant, and save it
        }

        // Initialize the duplicateInitiatives list
        updateDuplicateInitiatives();
    }

    public EncounterCombatantList(AllFactionFightableLists factionList, Context context) {
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
        diceRollList = c.getDiceRollList(); // TO_DO Could stand to be a deep copy instead of shallow...
        modifierList = c.getModifierList();
        lastRecordedRoundNumber = c.getLastRecordedRoundNumber();
        prefs = c.getPrefs();
        haveHadPrepPhase = c.haveHadPrepPhase();
        encounterRandomSeed = c.encounterRandomSeed;
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
        int oldDiceSize;
        if (prefs != null) {
            oldDiceSize = prefs.getDiceSize();
        } else {
            oldDiceSize = newPrefs.getDiceSize();
        }

        // Save the new Preferences
        prefs = newPrefs;

        // If the dice size is different now than it was before, then restart combat
        if (oldDiceSize != newPrefs.getDiceSize()) {
            restartCombat();
        }
    }

    public void restartCombat() {
        // Completely reset all meta-data related to Combat, and start from the beginning (just don't touch the actual Combatants themselves, aside from moving us into the prep-phase [checking them all off]...)
        diceRollList = new ArrayList<>();
        modifierList = new ArrayList<>();
        lastRecordedRoundNumber = 1;
        encounterRandomSeed = rand.nextInt(); // Re-calculate the random seed used for tie-breaking
        initializeCombatants();
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

    public boolean contains(Combatant c) {
        // Checks if the given Combatant is in this list
        // Note: Check is by UUID!  Not by exact copy!
        for (int i = 0; i < combatantArrayList.size(); i++) {
            if (combatantArrayList.get(i).getId().equals(c.getId())) {
                return true; // Found the Combatant
            }
        }

        return false; // Could not find this Combatant
    }

    public void setCombatantArrayList(AllFactionFightableLists factionList) {
        AllFactionFightableLists clonedList = factionList.clone(); // First, make a cloned version of the list, so we don't affect any of the "real" copies of the Combatants
        combatantArrayList = new ArrayList<>(); // Clear the current list of Combatants
        for (int i = 0; i < clonedList.getAllFactionLists().size(); i++) {
            // For each faction, add all of the Combatants to this object's ArrayList
            combatantArrayList.addAll(clonedList.getAllFactionLists().get(i).getCombatantArrayList());
        }

        initializeCombatants();
        doSorting();
//        updateDuplicateInitiatives();
    }

    public boolean haveHadPrepPhase() {
        return haveHadPrepPhase;
    }

    public void setHaveHadPrepPhase(boolean haveHadPrepPhase) {
        this.haveHadPrepPhase = haveHadPrepPhase;
    }

    public void updateCombatants(AllFactionFightableLists factionList) {
        // Update this Combatant list according to the incoming list
        //      We should add any new Combatants, remove any that do not appear, and ensure that the meta-data for all matching Combatants is up to date
        // First, figure out what we should initialize the new Combatants to (I really hate this, there HAS to be a better way...I think isSelected may just need to become an enum
        int curPhase = calcActiveCombatant();
        final boolean initSelect = (curPhase == EncounterCombatantRecyclerAdapter.PREP_PHASE || curPhase == EncounterCombatantRecyclerAdapter.END_OF_ROUND_ACTIONS); // If we are in the preparation phase, then initialize selected to true (to stay in this phase).  If we are not in the preparatory phase (we are in the middle of a combat round), initialize selected to false, because the Combatant has not gone yet (god, I hate this so much...)

        // Assumed to be unique by ID, because the names may have changed (i.e. adding a new second copy of a given Combatant, changing the first's ordinal to 1)
        AllFactionFightableLists clonedList = factionList.clone(); // First, make a cloned version of the list, so we don't affect any of the "real" copies of the Combatants
        ArrayList<FactionFightableList> factionLists = clonedList.getAllFactionLists();

        // Go through the incoming Combatant list, Faction by Faction
        HashSet<UUID> combatantsInThisEncounter = new HashSet<>();
        for (int facInd = 0; facInd < factionLists.size(); facInd++) {
            // For each faction, add any Combatants that are not already in this list
            FactionFightableList thisFactionList = factionLists.get(facInd);
            for (Combatant cNew : thisFactionList.getCombatantArrayList()) {
                // For each Combatant in the faction list
                if (idExists(cNew.getId())) {
                    // If Combatant cNew already exists in this EncounterCombatantList, just update the display values (Name, Faction, Icon, Modifier) in case anything has changed (even if we end up removing it from combat, we still want display to be saved)
                    Combatant existingCombatant = combatantArrayList.get(indByID(cNew.getId())); // A reference to the existing Combatant in this list that matches the one we are checking now from the Faction list
                    existingCombatant.displayCopy(cNew);

                    if (cNew.isVisible() && !existingCombatant.isVisible()) {
                        // Update the visibility if this Combatant is not visible in the Encounter
                        existingCombatant.setVisible(true);
                        existingCombatant.setSelected(initSelect);
                    }
                } else {
                    // If the Combatant does not already exist in the Encounter list (it's new), we only care about it if it's *visible* in the Faction list
                    if (cNew.isVisible()) {
                        // Add and initialize it
                        Combatant cToAdd = cNew.clone();
                        cToAdd.setSelected(initSelect); // Initialize the selection according to where we are in the combat cycle
                        combatantArrayList.add(cToAdd); // Add the new Combatant

                        // Make sure the Combatant is added to the diceRollMap in the next round (slightly redundant, but useful because the isVisible member isn't a reliable indicator of whether or not the Combatant was in the encounter in the last round)
                        combatantsToAdd.add(cNew.getId());
                    }
                }

                if (cNew.isVisible()) {
                    // Regardless of how it was processed, ensure that, if the Combatant was visible, it is checked off as existing in the new list
                    combatantsInThisEncounter.add(cNew.getId());
                }
            }
        }

        // Finally, go through the current list of Combatants, find any that did not appear in the new AllFactionFightableLists (or are not visible), and make them invisible from this new round on
        for (Combatant c : combatantArrayList) {
            if (!combatantsInThisEncounter.contains(c.getId())) {
                // If this Combatant shouldn't be in the current encounter, remove it
                combatantsToRemove.add(c.getId()); // Add this Combatant to the removal Set, to indicate that it must be removed from the diceRollMap the next time the round is set
            }
        }

        // OLD: Finally, go through the current list of Combatants and remove any that did not appear in the new AllFactionFightableLists
//        Iterator<Combatant> iterator = combatantArrayList.iterator();
//        while (iterator.hasNext()) {
//            Combatant c = iterator.next(); // Get the Combatant to test
//            if (!combatantsToKeep.contains(c.getId())) {
//                // If we are not keeping this Combatant, remove it from the combatantArrayList
//                iterator.remove();
//            }
//        }

        // Finally, perform any post-processing (sorting, etc)
        doSorting();

//        updateDuplicateInitiatives();
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

    private int indByID(UUID id) {
        int returnInd = -1;
        for (int i = 0; i < combatantArrayList.size(); i++) {
            Combatant c = combatantArrayList.get(i);
            // Go through each Combatant in this EncounterCombatantList, and find the index of the Combatant with this UUID
            if (id.equals(c.getId())) {
                returnInd = i;
                break;
            }
        }

        // Return the index
        return returnInd;
    }

    public void initializeCombatants() {
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
        // Calculate the currently active Combatant in the list *in initiative order*
        //      The first Combatant in the final group of unchecked Combatants
        //          XOOXXOOOOOXX
        //               ^ <- currently active Combatant
        ArrayList<Combatant> sortedCombatants; // Get a list of Combatants sorted by Initiative
        if (currentSortMethod != SortMethod.INITIATIVE) {
            // If the current Combatant List is not sorted by initiative, do so
            sortedCombatants = (ArrayList<Combatant>) combatantArrayList.clone(); // Create a (shallow) copy of the Combatants List
            Collections.sort(sortedCombatants, new CombatantSorter.SortByInitiative(prefs.getSortOrder(), prefs.getTieBreaker(), getTiebreakRand())); // Sort the new list by Initiative
        } else {
            sortedCombatants = combatantArrayList;
        }

        // Go through each Combatant, and checked whether or not it has been checked off
        // Note - Special cases: If no Combatants have been checked off, then the first is active.  If all Combatants have been checked off, then we are in the prep phase
        boolean foundUnSelected = false;
        for (int combInd = (sortedCombatants.size() - 1); combInd >= 0; combInd--) {
            // Go through the sorted Combatant list in reverse initiative order
            Combatant c = sortedCombatants.get(combInd);
            if (!c.isVisible()) {
                // If this Combatant is not visible, then ignore it
                continue;
            }

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
            haveHadPrepPhase = true; // Record the fact that we have finished the prep phase
            return 0;
        } else {
            // If all of the Combatants are selected, then we are either in the end-of-round phase or the prep-phase
            if (prefs.doingEndOfRound() && haveHadPrepPhase) {
                // If we already had a prep-phase this round, then all the Combatants must be checked because we have an end-of-round actions phase
                return EncounterCombatantRecyclerAdapter.END_OF_ROUND_ACTIONS;
            } else {
                // All Combatants are checked because we are preparing for this round of combat
                return EncounterCombatantRecyclerAdapter.PREP_PHASE;
            }
        }
    }

    public ArrayList<Integer> getDuplicateInitiatives() {
        return duplicateInitiatives;
    }

    public ArrayList<HashMap<UUID, Integer>> getDiceRollList() {
        return diceRollList;
    }

    public ArrayList<HashMap<UUID, Integer>> getModifierList() {
        return modifierList;
    }

    public int getLastRecordedRoundNumber() {
        return lastRecordedRoundNumber;
    }

    private long getTiebreakRand() {
        long seedAdd = lastRecordedRoundNumber;// << 40;
        long tiebreakRand = seedAdd + encounterRandomSeed; // Left bit-shift the recorded round number by 40 bits, because the random seed used by Random() only uses the first 48 bits
        return tiebreakRand;
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
                Collections.sort(combatantArrayList, new CombatantSorter.SortByInitiative(prefs.getSortOrder(), prefs.getTieBreaker(), getTiebreakRand())); // Sort the combatantArrayList alphabetically
                return;
            case ALPHABETICALLY_BY_FACTION:
                Collections.sort(combatantArrayList, new CombatantSorter.SortAlphabeticallyByFaction()); // Sort the combatantArrayList alphabetically by faction
        }

        // After sorting the Combatant, update our list of duplicate initiatives
        updateDuplicateInitiatives();
    }

    public void setRoundNumber(int roundNumber) {
        // Modifier cases:
        //  1: final Combatant to prep phase (go forwards)
        //      cur = last + 1
        //      Store current mods under last (special: if last = 0, no save)
        //      Retrieve cur, if it exists
        //  2: prep phase to final Combatant (go backwards)
        //      cur = last - 1
        //      Store current mods under last
        //      Retrieve cur
        //  3: same round (same)
        //      cur = last
        //      Don't store any
        //      Don't retrieve any

        // Save the current dice roll and modifier maps, so we can retrieve them later
        HashMap<UUID, Integer> modifierMap = new HashMap<>();
        HashMap<UUID, Integer> diceRollMap = new HashMap<>(); // The dice roll may have changed if the manual roll mode was used
        for (Combatant c : combatantArrayList) {
            // For each visible Combatant, get the Combatant's roll and modifier, and save it to the corresponding map, keyed to the Combatant's ID
            if (c.isVisible() && !combatantsToRemove.contains(c.getId()) && !combatantsToAdd.contains(c.getId())) {
                // If this Combatant is visible and not about to be removed
                modifierMap.put(c.getId(), c.getModifier());
                diceRollMap.put(c.getId(), c.getRoll());
            }
        }

        // Save old values if needed
        if (lastRecordedRoundNumber != roundNumber) {
            // Add enough empty HashMaps to modifierList such that the modifierList has a HashMap at position lastRecordedRoundNumber, so ArrayList<>#set() can be used
            while (modifierList.size() < lastRecordedRoundNumber) {
                modifierList.add(new HashMap<UUID, Integer>());
            }
            while (diceRollList.size() < lastRecordedRoundNumber) {
                diceRollList.add(new HashMap<UUID, Integer>());
            }

            // Save the diceRollMap and modifierMap if we are changing rounds
            modifierList.set(lastRecordedRoundNumber - 1, modifierMap);
            diceRollList.set(lastRecordedRoundNumber - 1, diceRollMap);

            // Next get the modifierMap for this round, if needed (otherwise, use the current modifierMap, as seen on screen, that is currently saved in modifierMap)
            if (roundNumber <= modifierList.size()) {
                // If the current round number has been done before, then retrieve the saved modifier map
                modifierMap = modifierList.get(roundNumber - 1);
            }
        }

        // After saving the previous values, get the diceRollMap for this round
        if (prefs.isReRollInit()) {
            // If initiative is being re-rolled every round, then make sure we have the correct roll map for this round
            if (roundNumber > diceRollList.size()) {
                // This is a new round, so generate new rolls
                diceRollMap = rollAllInitiative();

                diceRollList.add(diceRollMap); // Add a new HashMap for this round, because it is brand new
            } else {
                if (lastRecordedRoundNumber != roundNumber) {
                    // If this is an "old" round (one that we already roll initiative for), then just retrieve the old values that we rolled already
                    diceRollMap = diceRollList.get(roundNumber - 1); // roundNumber is a user-facing variable, so it starts at 1 (ick)
                }
                // If this is the same round, then we really shouldn't set a new roll map (the user may have gone to settings or something
                // Use the diceMap that we just generated, based on the current view
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

        // See if we need to add or remove any Combatants from this round
        if (!(combatantsToRemove.isEmpty() && combatantsToAdd.isEmpty())) {
            // Go through each Combatant and see if we need to add or remove it
            for (Combatant c : combatantArrayList) {
                UUID thisID = c.getId(); // This Combatant's UUID
                if (combatantsToRemove.contains(thisID)) {
                    // If this Combatant is to be removed

                    // Remove it from the diceRollList and modifierList for this round (they will be set invisible later in this function)
                    diceRollMap.remove(thisID);
                }

//                if (combatantsToAdd.contains(thisID) && !diceRollMap.containsKey(thisID)) {
                if (combatantsToAdd.contains(thisID)) {
                    // If this Combatant is to be added, and it doesn't already appear in the dice map

                    // Add it to the diceRoll List for this round, along with an initiative roll
                    diceRollMap.put(thisID, getInitiativeRoll());
                }
            }

            // Now that we've dealt with all of the Combatants, clear the Sets
            combatantsToRemove.clear();
            combatantsToAdd.clear();
        }

        // Save this round number, so we know what we last saved the modifier list
        lastRecordedRoundNumber = roundNumber;

        // Now that we have the dice rolls/modifiers that we are going to use, assign them to each Combatant
        for (Combatant combatant : combatantArrayList) {
            // For each Combatant, retrieve the roll, and set it to the Combatant

            if (diceRollMap.containsKey(combatant.getId())) {
                // If they are visible and they have a roll, then set it
                combatant.setRoll(diceRollMap.get(combatant.getId()));
                combatant.setVisible(true); // Ensure that the Combatant is visible if it has a roll
            } else {
                // If they are invisible, or they do not have a roll recorded for this round, then set them invisible
                combatant.setVisible(false); // If the Combatant does not have a roll this round, make them invisible
            }

            // For each Combatant, retrieve the modifier, and set it to the Combatant
            if (modifierMap.containsKey(combatant.getId())) {
                combatant.setModifier(modifierMap.get(combatant.getId()));
            } else {
                // If the modifierMap does not contain this key (this Combatant did not have a modifier associated with it), then get the Combatant's current modifier (its default modifier), and add it to the map
                modifierMap.put(combatant.getId(), combatant.getModifier()); // Will be saved in modifierList
            }
        }

        // Finally, since we are in a new round, note that the end-of-round actions are not completed (if the user wants to use this feature)
        haveHadPrepPhase = false;

        // Update the duplicateInitiatives list
        doSorting(); // Sort the list now that the rolls/modifiers have likely changed
//        updateDuplicateInitiatives(); // See if any Combatants have duplicate initiatives, so we can display an indicator on screen
    }

    public void reRollCombatant(HashSet<UUID> setToReRoll) {
        boolean didReRoll = false;
        for (Combatant c : combatantArrayList) {
            // Check if each Combatant is in the set
            if (setToReRoll.contains(c.getId())) {
                c.setRoll(getInitiativeRoll());
                didReRoll = true;
            }
        }

        if (didReRoll) {
            doSorting();
//            updateDuplicateInitiatives();
        }
    }

    private HashMap<UUID, Integer> rollAllInitiative() {
        // Generate an initiative roll for each visible Combatant, assign the rolls to a HashMap keyed by each Combatant's UUID, and return it

        // Create a new Map
        HashMap<UUID, Integer> newDiceMap = new HashMap<>();
        HashMap<Fightable.Faction, Integer> factionDiceMap = new HashMap<>(); // Create a new HashMap to keep track of rolls by Faction (used iff the Preference key_individual_initiative is set to "false" [meaning that each Faction should get its own initiative roll])

        // For each visible Combatant, roll initiative
        for (Combatant c : combatantArrayList) {
            if (c.isVisible()) {
                int thisInitRoll;
                if (prefs.doingIndividualInitiative()) {
                    // If each Combatant is to get their own initiative roll...
                    // Generate a new initiative roll for this Combatant
                    thisInitRoll = getInitiativeRoll();
                } else {
                    // If each Faction gets their own initiative roll...
                    // Check if we have rolled an initiative value for this Faction yet
                    if (!factionDiceMap.containsKey(c.getFaction())) {
                        // If the factionDiceMap does not have a roll for this Faction yet, create a new one
                        factionDiceMap.put(c.getFaction(), getInitiativeRoll());
                    }

                    // Now that we definitely have an initiative roll for this Faction, assign it to the Combatant in the newDiceMap
                    thisInitRoll = factionDiceMap.get(c.getFaction());
                }

                // Store the new roll for each Combatant into the newDiceMap, keyed by the Combatant's ID
                newDiceMap.put(c.getId(), thisInitRoll);
            }
        }

        // Return the Map
        return newDiceMap;
    }

    public int getInitiativeRoll() {
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

    public int visibleSize() {
        // Get the number of visible Combatants
        int returnSize = 0;
        for (Combatant c : combatantArrayList) {
            returnSize += c.isVisible() ? 1 : 0;
        }

        return returnSize;
    }

    public boolean isLastVisibleCombatant(int position) {
        // Starting at the given position, see if there are any more visible combatants
        if (position == combatantArrayList.size() - 1) {
            // If this is the last Combatant, then it is the last visible Combatant
            return true;
        } else {
            for (int i = position + 1; i < combatantArrayList.size(); i++) {
                if (combatantArrayList.get(i).isVisible()) {
                    // If there is another visible Combatant, then we aren't last
                    return false;
                }
            }

            // If we got here, then we're the last visible Combatant
            return true;
        }
    }

    public int getLastVisibleCombatant() {
        // Get the index of the last visible Combatant
        for (int i = combatantArrayList.size() - 1; i >= 0; i--) {
            if (combatantArrayList.get(i).isVisible()) {
                return i;
            }
        }

        // Uh oh
        return 0;
    }

    public Combatant get(int i) {
        return combatantArrayList.get(i);
    }

    public EncounterCombatantList getVisibleSublist() {
        // Return a new EncounterCombatantList with only the visible Combatants
        ArrayList<Combatant> visibleCombatantsList = new ArrayList<>();
        for (Combatant c : combatantArrayList) {
            if (c.isVisible()) {
                visibleCombatantsList.add(c); // Add every visible Combatant to the list
            }
        }

        return new EncounterCombatantList(visibleCombatantsList, prefs); // A bit slow, but better than any alternative I've come up with...
    }

    public EncounterCombatantList clone() {
        return new EncounterCombatantList(this);
    }

    public int getInitiativeIndexOf(int indexInCurrentList) {
        // A method to find the initiative index in combatantArrayList of a Combatant, regardless of the actual sorted state of the list
        if (currentSortMethod == SortMethod.INITIATIVE) {
            // If the array is already sorted by initiative, then just return the input index
            return indexInCurrentList;
        } else {
            // The combatantArrayList is currently sorted alphabetically_by_faction, so we're going to need to sort the list, then get the index of the Combatant
            Combatant c = combatantArrayList.get(indexInCurrentList); // Get the Combatant at the indicated point in the current lis
            ArrayList<Combatant> sortedList = new ArrayList<>(combatantArrayList); // Create a SHALLOW copy of the list (the Combatants themselves are not cloned, only their references are copied)

            // Do initiative sorting on the sortedList
            Collections.sort(sortedList, new CombatantSorter.SortByInitiative(prefs.getSortOrder(), prefs.getTieBreaker(), getTiebreakRand()));

            // Finally, find the index in the initiative sorted list of the selected Combatant
            return sortedList.indexOf(c);  // Ignores effects of invisible Combatants, as they are sorted at the end of the list
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
            Collections.sort(sortedList, new CombatantSorter.SortByInitiative(prefs.getSortOrder(), prefs.getTieBreaker(), getTiebreakRand()));
            Combatant c = sortedList.get(indexInInitiativeOrder);

            return combatantArrayList.indexOf(c); // Ignores effects of invisible Combatants, as they are sorted at the end of the list
        }
    }

    public boolean isEmpty() {
        return combatantArrayList.isEmpty();
    }

    public boolean isVisiblyEmpty() {
        return visibleSize() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncounterCombatantList that = (EncounterCombatantList) o;
        return lastRecordedRoundNumber == that.lastRecordedRoundNumber &&
                haveHadPrepPhase == that.haveHadPrepPhase &&
                encounterRandomSeed == that.encounterRandomSeed &&
                Objects.equals(combatantArrayList, that.combatantArrayList) &&
                Objects.equals(duplicateInitiatives, that.duplicateInitiatives) &&
                Objects.equals(diceRollList, that.diceRollList) &&
                Objects.equals(modifierList, that.modifierList) &&
                currentSortMethod == that.currentSortMethod &&
                Objects.equals(prefs, that.prefs) &&
                Objects.equals(combatantsToRemove, that.combatantsToRemove) &&
                Objects.equals(combatantsToAdd, that.combatantsToAdd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(combatantArrayList, duplicateInitiatives, diceRollList, modifierList,
                lastRecordedRoundNumber, currentSortMethod, prefs, haveHadPrepPhase,
                combatantsToRemove, combatantsToAdd, encounterRandomSeed);
    }

    public enum SortMethod {
        INITIATIVE,
        ALPHABETICALLY_BY_FACTION
    }

    static class InitPrefs implements Serializable {
        private final CombatantSorter.sortOrder sortOrder;
        private final boolean reRollInit;
        private final int diceSize;
        private final boolean endOfRound;
        private final boolean individualInitiative;
        private final CombatantSorter.tieBreaker tieBreaker;

        InitPrefs(Context context) {
            // Using the InitPrefsHelper, populate this object
            sortOrder = PrefsHelper.getSortOrder(context);
            reRollInit = PrefsHelper.getReRollInit(context);
            diceSize = PrefsHelper.getDiceSize(context);
            endOfRound = PrefsHelper.doingEndOfRound(context);
            individualInitiative = PrefsHelper.doingIndividualInitiative(context);
            tieBreaker = PrefsHelper.getTieBreaker(context);
        }

        public CombatantSorter.sortOrder getSortOrder() {
            return sortOrder;
        }

        public CombatantSorter.tieBreaker getTieBreaker() {
            return tieBreaker;
        }

        public boolean isReRollInit() {
            return reRollInit;
        }

        public int getDiceSize() {
            return diceSize;
        }

        public boolean doingEndOfRound() {
            return endOfRound;
        }

        public boolean doingIndividualInitiative() {
            return individualInitiative;
        }
    }
}
