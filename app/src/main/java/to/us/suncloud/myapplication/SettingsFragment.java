package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.Arrays;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String IS_MID_COMBAT = "isMidCombat";

    DropDownPreference presetPref; // The preset list preference
    DropDownPreference sortPref; // The sort order preference
    DropDownPreference tiePref; // The tie breaker preference
    DropDownPreference modPref; // The sort order preference
    EditTextPreference dicePref; // The dice size preference
    CheckBoxPreference reRollPref; // The re-roll preference
    CheckBoxPreference endOfRoundPref; // The end-of-round actions preference
    CheckBoxPreference individualInitiativePref; // The group initiatives preference
    SwitchPreference darkModePref; // The dark mode preference

    boolean isMidCombat = false; // Is the user currently mid-combat?

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);
        PreferenceManager manager = getPreferenceManager();

        // TO_DO : Add colorblind mode! Blue and yellow?  Orange?  Google it!
        //  Android already has a built-in colorblind mode

        Bundle args = getArguments();

        if (args != null) {
            if (args.containsKey(IS_MID_COMBAT)) {
                isMidCombat = args.getBoolean(IS_MID_COMBAT);
            }
        }

        // Grab any Preferences that we need
        // Initiative Preferences 1 (set by preset spinner)
        presetPref = manager.findPreference(getString(R.string.key_preset));
        sortPref = manager.findPreference(getString(R.string.key_sort_order));
        tiePref = manager.findPreference(getString(R.string.key_tie_breaker));
        dicePref = manager.findPreference(getString(R.string.key_dice_size));
        modPref = manager.findPreference(getString(R.string.key_mod_used));

        // Initiative Preferences 2 (set by preset spinner, but does not set spinner to "Custom")
        endOfRoundPref = manager.findPreference(getString(R.string.key_end_of_round));
        individualInitiativePref = manager.findPreference(getString(R.string.key_individual_initiative));
        reRollPref = manager.findPreference(getString(R.string.key_re_roll));

        // Display Preferences
        darkModePref = manager.findPreference(getString(R.string.key_dark_mode));

        // Set up the preset functionality
        presetPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof String) {
                    return updateFromPreset((String) newValue);
                }
                return false;
            }
        });

        dicePref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                // Set up the edit text in the new preference window
                editText.setInputType(InputType.TYPE_CLASS_NUMBER); // Only show the number pad

                // If the user focuses on the text box, selected everything in it
                editText.setSelectAllOnFocus(true);
                editText.selectAll();
            }
        });

        dicePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof String) {
                    // We have a new dice size value, as a String
                    return setDiceSizeIfValid((String) newValue, Integer.parseInt(((EditTextPreference) preference).getText())); // Also pass in the current value, as an int
                }
                return false;
            }
        });

        sortPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setPresetToCustom();
                return true;
            }
        });

        modPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setPresetToCustom();
                return true;
            }
        });

//        reRollPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                setPresetToCustom();
//
//                return true;
//            }
//        });

        darkModePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // Whenever this preference is changed, reload the activity
                getActivity().finish();
                getActivity().overridePendingTransition(0, 0);
                startActivity(getActivity().getIntent());
                getActivity().overridePendingTransition(0, 0);
                return true;
            }
        });
    }

    private boolean updateFromPreset(String newPresetValue) {
        return updateFromPreset(newPresetValue, false);
    }

    private boolean updateFromPreset(final String newPresetValue, boolean forceUpdate) {
        // We got a new value for the preset
        boolean newVals = false;
        int sortValue = 0;
        int tieValue = 0;
        int modValue = 0;
        int diceValue = 10;
        boolean reRollVal = false;
        boolean endOfRoundVal = false;
        boolean individualInitiativeVal = false;

        // Get the current dice value, for comparison.  If this is changed mid-combat, then data will be lost
        int curDiceValue = Integer.parseInt(dicePref.getText());

        if (newPresetValue.equals(getString(R.string.rpg_ver_dnd1))) {
            // https://www.dandwiki.com/wiki/How_Combat_Works_(Basic_D%26D)
            // TODO SOON: Perhaps add a way to make a Combatant "lose" initiative (go last this round) because they are using a two-handed weapon, drawing a weapon, etc?
            //      Actions should also happen in order of: Movement, Missile combat, Spell casting, Hand-to-hand combat

            // The user just chose Original D&D
            newVals = true;

            sortValue = 1; // High goes first
            tieValue = 1; // Party First
            modValue = 1; // Initiative Modifier
            diceValue = 6;
            reRollVal = true;
            endOfRoundVal = false;
            individualInitiativeVal = false; // Faction roll
        }

        if (newPresetValue.equals(getString(R.string.rpg_ver_adnd1))) {
            // The user just chose AD&D 1
            newVals = true;

            sortValue = 1; // High goes first
            tieValue = 1; // Party First
            modValue = 0; // Speed Factor
            diceValue = 6;
            reRollVal = true;
            endOfRoundVal = false;
            individualInitiativeVal = false; // Faction roll
        }

        if (newPresetValue.equals(getString(R.string.rpg_ver_entry_adnd2))) {
            // The user just chose AD&D 2
            newVals = true;

            sortValue = 0; // Low goes first
            tieValue = 1; // Party First
            modValue = 0; // Speed Factor
            diceValue = 10;
            reRollVal = true;
            endOfRoundVal = true;
            individualInitiativeVal = false; // Faction roll
        }

        if (newPresetValue.equals(getString(R.string.rpg_ver_entry_dnd3))) {
            // The user just chose D&D 3/3.5
            newVals = true;

            sortValue = 1; // High goes first
            tieValue = 0; // Best Modifier
            modValue = 1; // Initiative Modifier
            diceValue = 20;
            reRollVal = false;
            endOfRoundVal = false;
            individualInitiativeVal = true; // Individual roll
        }

        if (newPresetValue.equals(getString((R.string.rpg_ver_entry_pf)))) {
            // The user just chose Pathfinder
            newVals = true;

            sortValue = 1; // High goes first
            tieValue = 0; // Best Modifier
            modValue = 1; // Initiative Modifier
            diceValue = 20;
            reRollVal = false;
            endOfRoundVal = false;
            individualInitiativeVal = true; // Individual roll
        }

        if (newPresetValue.equals(getString(R.string.rpg_ver_entry_dnd4))) {
            // The user just chose D&D 4
            newVals = true;

            sortValue = 1; // High goes first
            tieValue = 0; // Best Modifier
            modValue = 1; // Initiative Modifier
            diceValue = 20;
            reRollVal = false;
            endOfRoundVal = false;
            individualInitiativeVal = true; // Individual roll
        }

        if (newPresetValue.equals(getString(R.string.rpg_ver_entry_dnd5))) {
            // The user just chose D&D 5
            newVals = true;

            sortValue = 1; // High goes first
            tieValue = 2; // Random
            modValue = 1; // Initiative Modifier
            diceValue = 20;
            reRollVal = false;
            endOfRoundVal = false;
            individualInitiativeVal = true; // Individual roll
        }

        if (!newVals) {
            // If nothing changed, then just leave the method
            return false;
        }

        // TODO SOON: Include new option for initiative: random ORDER, not initiative value.  Should also allow simple manipulations of where a character is in the queue...somehow
        //  Could be implemented via the EncountercombatantList getInitiativeRoll() function, or in setRoundNumber()? Assign each combatant a number from 1-numCombatants.  Moving up or down N slots would just be adding N+.1 as a modifier (perhaps hidden?) "Losing initiative" would be subtracting some large number that is consistent between all Combatants that lose initiative

        // Check if the user is sure they want to change anything
        if (!forceUpdate && isMidCombat && curDiceValue != diceValue) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.change_dice_title)
                    .setMessage(R.string.change_dice_message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Go through this method again, but force
                            isMidCombat = false;
                            updateFromPreset(newPresetValue, true);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    })
                    .show();

            // Do not update anything
            // If the user wants to update the value, then this method will be run again with forceUpdate == true
            return false;
        }

        // Set the new preferences
        sortPref.setValueIndex(sortValue);
        tiePref.setValueIndex(tieValue);
        modPref.setValueIndex(modValue);
        dicePref.setText(String.valueOf(diceValue));
        reRollPref.setChecked(reRollVal);
        endOfRoundPref.setChecked(endOfRoundVal);
        individualInitiativePref.setChecked(individualInitiativeVal);

        if (forceUpdate) {
            // If this is a force update (the user just confirmed that they wanted to overwrite combat data), then set the value of the preset dropdown menu, because it wasn't set
            ArrayList<String> presetVals = new ArrayList<>(Arrays.asList(getContext().getResources().getStringArray(R.array.rpg_ver_entry)));
            if (presetVals.contains(newPresetValue)) {
                presetPref.setValueIndex(presetVals.indexOf(newPresetValue));
            }
        }


        return true;
    }

    private void setPresetToCustom() {
        // Set the preset list preference to the "Custom" position, to indicate that something has been changed and we (may be) no longer in a preset
        ArrayList<String> allPresetStrings = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.rpg_ver_entry)));
        int custEntryIndex = allPresetStrings.indexOf(getString(R.string.rpg_ver_entry_cust)); // Get the value of the custom String in R.array.rpg_ver_entry
        if (custEntryIndex != -1) {
            presetPref.setValueIndex(custEntryIndex);
        }
    }

    private boolean setDiceSizeIfValid(final String newDiceSizeString, int oldDiceSize) {
        // This method will be used any time the user confirms a text string in the roll view EditText
        int newDiceSize = 0; // Initial value never used, but at least the IDE won't yell at me...
        boolean newValIsValid = false;
        try {
            newDiceSize = Integer.parseInt(newDiceSizeString); // Get the value that was entered into the text box

            // If the new entered roll value is valid (greater than 0), then reject it
            newValIsValid = newDiceSize > 0;

        } catch (NumberFormatException e) {
//                newValIsValid = false;
        }

        // If the roll is NOT valid and it is new, then just revert the EditText and return
        if (!newValIsValid) {
//            dicePref.setText(String.valueOf(curDiceSize)); // Revert to the current roll value
            Toast.makeText(getContext(), "Dice size must be an integer greater than 0.", Toast.LENGTH_SHORT).show(); // Let the user know they're a dummy
            return false;
        }

        // Check if the user is sure they want to change anything
        if (isMidCombat && oldDiceSize != newDiceSize) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.change_dice_title)
                    .setMessage(R.string.change_dice_message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Directly set the value of dicePref
                            dicePref.setText(newDiceSizeString);
                            setPresetToCustom(); // Let the preset preference list know that we have changed something
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    })
                    .show();

            // Do not update anything
            // If the user wants to update the value, then this method will be run again with forceUpdate == true
            return false;
        }

        // Change the preset setting so the user knows that we have changed from a defined preset
        setPresetToCustom();
        return true;
    }
}
