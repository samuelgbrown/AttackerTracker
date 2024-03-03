package to.us.suncloud.myapplication;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

public class AllEncounterSaveData implements Serializable {
    private static final String ENCOUNTER_SAVE_FILE = "encounterSaveFile";
    static private AllEncounterSaveData lastSavedEncounterData;

    AllFactionFightableLists savedCombatantLists;
    int savedRoundNumber;
    int savedMaxRoundNumber;
    EncounterCombatantList savedCurEncounterListData;

    AllEncounterSaveData(EncounterDataHolder dataHolder) {
        // Store all data to be saved
        savedCombatantLists = dataHolder.getSavedCombatantLists().getRawCopy();
        savedRoundNumber = dataHolder.getSavedRoundNumber();
        savedMaxRoundNumber = dataHolder.getSavedMaxRoundNumber();
        savedCurEncounterListData = dataHolder.getSavedCurEncounterListData();
    }

    AllEncounterSaveData(AllEncounterSaveData original) {
        savedRoundNumber = original.savedRoundNumber;
        savedMaxRoundNumber = original.savedMaxRoundNumber;
        savedCombatantLists = null;
        savedCurEncounterListData = null;

        if (original.savedCombatantLists != null ) {
            savedCombatantLists = original.savedCombatantLists.clone();
        }
        if (original.savedCurEncounterListData != null ) {
            savedCurEncounterListData = original.savedCurEncounterListData.clone();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllEncounterSaveData that = (AllEncounterSaveData) o;
        return savedRoundNumber == that.savedRoundNumber &&
                savedMaxRoundNumber == that.savedMaxRoundNumber &&
                Objects.equals(savedCombatantLists, that.savedCombatantLists) &&
                Objects.equals(savedCurEncounterListData, that.savedCurEncounterListData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(savedCombatantLists, savedRoundNumber,
                savedMaxRoundNumber, savedCurEncounterListData);
    }

    static public void saveEncounterData( EncounterDataHolder dataHolder ) {
        // Save the Combatant data that we have now
        AllEncounterSaveData currentData = new AllEncounterSaveData(dataHolder);
        if (!currentData.equals(lastSavedEncounterData)) {
            // If the test list is not equal to the list of Combatants from the file, that means that some Combatants were added (or possibly removed...?), so we should save the new list
            LocalPersistence.writeObjectToFile(dataHolder.getContext(), currentData, ENCOUNTER_SAVE_FILE);
        }

        // Now keep track of the most recently saved batch of Combatants
        lastSavedEncounterData = currentData.clone();
    }

    public AllEncounterSaveData clone() {
        return new AllEncounterSaveData(this);
    }

    static public AllEncounterSaveData readEncounterData(@NonNull EncounterDataHolder dataHolder ) {
        return (AllEncounterSaveData) LocalPersistence.readObjectFromFile(dataHolder.getContext(), ENCOUNTER_SAVE_FILE);
    }

    static public void removeEncounterData( ) {
        File encounterFile = new File(ENCOUNTER_SAVE_FILE);
        encounterFile.delete();
    }

    interface EncounterDataHolder {
        AllFactionFightableLists getSavedCombatantLists();
        int getSavedRoundNumber();
        int getSavedMaxRoundNumber();
        EncounterCombatantList getSavedCurEncounterListData();
        Context getContext();
    }
}
