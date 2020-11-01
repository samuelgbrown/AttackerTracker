package to.us.suncloud.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;

public class PrefsHelper {
    // No constructor because I don't want to risk not clearing a Context and causing a memory leak

    public static int getTheme(Context context) {
        // Get the current theme as set in Settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useDark = prefs.getBoolean(context.getString(R.string.key_dark_mode), false);

        // Depending on the value of the boolean preference, return either the light theme or the dark theme
        return useDark ? R.style.Dark_Theme : R.style.Light_Theme;
    }

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
            ArrayList<String> modTypes = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.mod_vals))); // Get all values as an ArrayList<String>
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

    public static CombatantSorter.tieBreaker getTieBreaker(Context context) {
        // Get the tie-breaker, in terms of the enum tieBreaker
        // First get the preferences, and find the value that corresponds to the String we want
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String tieBreakerEntry = prefs.getString(context.getString(R.string.key_tie_breaker), context.getString(R.string.tie_entry_modifier));

        // Based on the value of the saved String, return a different tieBreaker, indicating what happens if there is a tie in initiative between two or more Combatants
        if (tieBreakerEntry != null) {
            switch (tieBreakerEntry) {
                case "mod":
                    return CombatantSorter.tieBreaker.Modifier;
                case "alpha":
                    return CombatantSorter.tieBreaker.AlphaByFaction;
                case "rand":
                    return CombatantSorter.tieBreaker.Random;
                default:
                    return CombatantSorter.tieBreaker.Modifier;
            }
        } else {
            return CombatantSorter.tieBreaker.Modifier;
        }
    }

    public static boolean getReRollInit(Context context) {
        // Get whether or not initiative gets rerolled each round
        // Get the preferences, and find the value that corresponds to the boolean we want
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.key_re_roll), true);
    }

    public static boolean doingEndOfRound(Context context) {
        // Determine whether or not we do a pause after the end of each round
        // Get the preferences, and find the value that corresponds to the boolean we want
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.key_end_of_round), false);
    }

    public static boolean doingIndividualInitiative(Context context) {
        // Determine whether or not we Initiative is rolled individually, or by Faction
        // Get the preferences, and find the value that corresponds to the boolean we want
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.key_individual_initiative), true);
    }

    public static boolean doingInitButtonAnim(Context context) {
        // Determine whether or not we are doing the Roll Initiative button animation
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.key_button_anim), true);
    }

    public static AdLocation getAdLocation(Context context) {
        // Get the ad location, in terms of the enum AdLocation
        // First get the preferences, and find the value that corresponds to the String we want
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String adLocationEntry = prefs.getString(context.getString(R.string.key_ad_location), context.getString(R.string.ad_loc_entry_bottom_below));

        // Based on the value of the saved String, return a different tieBreaker, indicating what happens if there is a tie in initiative between two or more Combatants
        if (adLocationEntry != null) {
            switch (adLocationEntry) {
                case "top":
                    return AdLocation.Top;
                case "bottom_below":
                    return AdLocation.Bottom_Below;
                case "bottom_above":
                    return AdLocation.Bottom_Above;
                default:
                    return AdLocation.Bottom_Below;
            }
        } else {
            return AdLocation.Bottom_Below;
        }
    }

    public enum AdLocation {
        Bottom_Below,
        Bottom_Above,
        Top
    }
}
