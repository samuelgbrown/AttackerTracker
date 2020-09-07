package to.us.suncloud.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;

public class InitPrefsHelper {
    // No constructor because I don't want to risk not clearing a Context and causing a memory leak

    public static String getModString(Context context) {
        // Get the corresponding string of the mod type to use by examining the user's preferences
        // First get the preferences, and find the value that corresponds to the String we want
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String modTypeEntry = prefs.getString(context.getString(R.string.key_mod_used), context.getString(R.string.mod_entry_speed));

        // Using this string from preferences as a key, get the full String of the modifier name
        Resources res = context.getResources();
        String modType = context.getString(R.string.mod_entry_speed); // Initialize to modifier
        ArrayList<String> entryVals = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.mod_entry_vals))); // Get all entry values as an ArrayList<String>
        int modTypeEntryResourceInd = entryVals.indexOf(modTypeEntry);
        if (modTypeEntryResourceInd != -1) {
            // If this is a valid index in the entryVals ArrayList, use its index location to find the actual preference we are interested in
            ArrayList<String> modTypes = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.mod_entry))); // Get all values as an ArrayList<String>
            if (modTypeEntryResourceInd < modTypes.size()) {
                // If the entry value is in the actual mod types list (the arrays weren't improperly formatted in the xml)
                modType = modTypes.get(modTypeEntryResourceInd);
            }
        }

        // Return the modifier type
        return modType;
    }

    public static int getDiceSize(Context context) {
        // Get the size of the dice by examining the user's preferences
        // First get the preferences, and find the value that corresponds to the String we want
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String diceSize = prefs.getString(context.getString(R.string.key_dice_size), "20"); // Get the dice size as a String
        if (diceSize != null) {
            return Integer.parseInt(diceSize); // The value has already been error-checked, so we should be fine here
        } else {
            return 20;
        }
    }

    public static CombatantSorter.sortOrder getSortOrder(Context context) {
        // Get the sort order, in terms of the enum sortOrder
        // First get the preferences, and find the value that corresponds to the String we want
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sortOrderEntry = prefs.getString(context.getString(R.string.key_sort_order), context.getString(R.string.sort_entry_low_first));

        // Based on the value of the saved String, return a different sortOrder, indicating the sorting direction of the Combatants
        if (sortOrderEntry != null) {
            switch (sortOrderEntry) {
                case "low":
                    return CombatantSorter.sortOrder.LowToHigh;
                case "high":
                    return CombatantSorter.sortOrder.HighToLow;
                default:
                    return CombatantSorter.sortOrder.LowToHigh;
            }
        } else {
            return CombatantSorter.sortOrder.LowToHigh;
        }
    }

    public static boolean getReRollInit(Context context) {
        // Get whether or not initiative gets rerolled each round
        // Get the preferences, and find the value that corresponds to the boolean we want
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.key_re_roll), true);
    }
}
