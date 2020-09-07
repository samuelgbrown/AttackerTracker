package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String IS_MID_COMBAT = "isMidCombat";

    DropDownPreference presetPref; // The preset list preference
    DropDownPreference sortPref; // The sort order preference
    DropDownPreference modPref; // The sort order preference
    EditTextPreference dicePref; // The dice size preference
    CheckBoxPreference reRollPref; // The re-roll preference

    boolean isMidCombat = false; // Is the user currently mid-combat?

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);
        PreferenceManager manager = getPreferenceManager();

        Bundle args = getArguments();

        if (args != null) {
            if (args.containsKey(IS_MID_COMBAT)) {
                isMidCombat = args.getBoolean(IS_MID_COMBAT);
            }
        }

        // Grab any preferences that we need
        presetPref = manager.findPreference(getString(R.string.key_preset));
        sortPref = manager.findPreference(getString(R.string.key_sort_order));
        dicePref = manager.findPreference(getString(R.string.key_dice_size));
        modPref = manager.findPreference(getString(R.string.key_mod_used));
        reRollPref = manager.findPreference(getString(R.string.key_re_roll));

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
                // TODO LATER: Set icon based on chosen entry?  May be useful visual feedback

                return true;
            }
        });

        reRollPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setPresetToCustom();

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
        int modValue = 0;
        int diceValue = 10;
        boolean reRollVal = false;

        // Get the current dice value, for comparison.  If this is changed mid-combat, then data will be lost
        int curDiceValue = Integer.parseInt(dicePref.getText());

        if (newPresetValue.equals(getString(R.string.rpg_ver_entry_dnd2))) {
            // The user just chose DnD 2
            newVals = true;

            sortValue = 0;
            modValue = 0;
            diceValue = 10;
            reRollVal = true;
        }

        if (newPresetValue.equals(getString(R.string.rpg_ver_entry_dnd3))) {
            // The user just chose DnD 3.5
            newVals = true;

            sortValue = 1;
            modValue = 1;
            diceValue = 20;
            reRollVal = false;
        }

        if (!newVals) {
            // If nothing changed, then just leave the method
            return false;
        }

        // Check if the user is sure they want to change anything
        if (!forceUpdate && isMidCombat && curDiceValue != diceValue) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.change_dice_title)
                    .setMessage(R.string.change_dice_message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Go through this method again, but force
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
        modPref.setValueIndex(modValue);
        dicePref.setText(String.valueOf(diceValue));
        reRollPref.setChecked(reRollVal);

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
