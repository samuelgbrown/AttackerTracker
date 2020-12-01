package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat implements PurchaseHandler.purchaseHandlerInterface {
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
    Preference creditsPref; // A button to open up the credits
    CheckBoxPreference buttonAnimPref; // A preference for the animation of the Roll Initiative button

    // Ads
    String AD_TAG = "ADS";
    String REMOVE_ADS_SKU = "attacker_tracker.remove.ads";
    //    String REMOVE_ADS_SKU = "android.test.purchased";
    //    DropDownPreference adLocPref; // Ads location preference
    Preference purchasePref; // A button to purchase the entitlement to remove ads
    private final boolean adCatIsAttached = true; // Is the ads category currently attached
    PurchaseHandler purchaseHandler;
//    SkuDetails removeAdsSKUDetails; // The SKU details of the in-app purchase option (retrieved from the asynchronous call to the billing client)
//    int billConnectionTries = 0;
//    final static int BILL_MAX_TRIES = 5;

//    PurchasesUpdatedListener purchaseListener = new PurchasesUpdatedListener() { // A listener that receives updates on purchasing
//        // Handle brand new purchases
//        @Override
//        public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
//            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
//                for (Purchase purchase : purchases) {
//                    purchase.getPurchaseToken();
//                    handlePurchase(purchase);
//                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
//                        // If the user just bought this, thank them!
//                        Toast.makeText(getContext(), "Thank you for supporting the app! Enjoy!", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
//            } else {
//                // Handle any other error codes.
//            }
//        }
//    };

//    BillingClient client;

    boolean isMidCombat = false; // Is the user currently mid-combat?

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);
        PreferenceManager manager = getPreferenceManager();

        // Start a connection with the billing client
        // Make PurchaseHandler
//        client = BillingClient.newBuilder(getContext())
//                .setListener(purchaseListener)
//                .enablePendingPurchases()
//                .build(); // Build the client through which we will make purchases

//        client.startConnection(new BillingClientStateListener() {
//            @Override
//            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
//                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                    // The BillingClient is ready, query available purchases (damn well better be only one...)
//
//                    List<String> skuList = new ArrayList<>();
//                    skuList.add(REMOVE_ADS_SKU);
//                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
//                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
//                    client.querySkuDetailsAsync(params.build(),
//                            new SkuDetailsResponseListener() {
//                                @Override
//                                public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
//                                    // Process the result
//                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                                        if (skuDetailsList != null && skuDetailsList.size() > 0) {
//                                            removeAdsSKUDetails = skuDetailsList.get(0); // Save the first (and hopefully only) SKUDetails object
//                                            purchasePref.setEnabled(true); // We got info from the Play store, so we can allow the purchase flow to begin
//                                        }
//                                    } else {
//                                        Log.w(AD_TAG, "Could not retrieve SKU details: " + billingResult.getDebugMessage());
//                                    }
//
//                                    // Now that we have SKU details, update the GUI
//                                    updateAdVis();
//                                }
//                            });
//                }
//            }
//
//            @Override
//            public void onBillingServiceDisconnected() {
//                // ...Uh-oh
//                billConnectionTries++;
//                if (billConnectionTries < BILL_MAX_TRIES) {
//                    // Try connecting again, up to 5 times
//                    client.startConnection(this);
//                } else {
//                    Log.e(AD_TAG, "Could not connect to Billing Service.");
////                        Toast.makeText(getContext(), "Could not connect to billing service", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

        // Ads Preferences
        // Set up the in-app purchasing interaction, in case it's needed
        List<String> allSKUs = new ArrayList<>();
        allSKUs.add(REMOVE_ADS_SKU);
        purchaseHandler = new PurchaseHandler(this, allSKUs);
        purchasePref = manager.findPreference(getString(R.string.key_purchase));
        purchasePref.setEnabled(false); // Disable the ad removal purchase until we have retrieved SKU info from the Play Store

        purchasePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                purchaseHandler.startPurchase(REMOVE_ADS_SKU);
                return false;
            }
        });

        // Set the visibility of the ad preference category, depending on on whether we THINK the user has bought the remove_ad feature
        setAdCategoryVisible(!purchaseHandler.wasPurchased(REMOVE_ADS_SKU));

        // TO_DO : Add colorblind mode! Blue and yellow?  Orange?  Google it!
        //  Android already has a built-in colorblind mode

        Bundle args = getArguments();

        if (args != null) {
            if (args.containsKey(IS_MID_COMBAT)) {
                isMidCombat = args.getBoolean(IS_MID_COMBAT);
            }
        }

        // TODO START HERE: Developer options (only appears in debug version)
        if (BuildConfig.DEBUG) {
            PreferenceCategory devOptionsCat = new PreferenceCategory(getContext());
            devOptionsCat.setTitle("Developer Options");
            Preference restoreAds = new Preference(getContext());
            restoreAds.setTitle("Restore Ads");
            restoreAds.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // TODO: Restore ads (if possible)
                    if (purchaseHandler.wasPurchased(REMOVE_ADS_SKU)) {
                        Purchase.PurchasesResult purchaseResult = purchaseHandler.getQueriedPurchases();
                        if (purchaseResult != null) {
                            int responseCode = purchaseResult.getResponseCode();
                            if (responseCode == BillingClient.BillingResponseCode.OK) {
                                List<Purchase> pList = purchaseResult.getPurchasesList();
                                boolean gotSKU = false;
                                for (Purchase p : pList) {
                                    if (p.getSku().equals(REMOVE_ADS_SKU)) {
                                        // Got the Purchase associated with the remove ads product
                                        // Submit a request to consume this item, so it goes away
                                        purchaseHandler.consumePurchase(p); // If successful, will send a toast via listener
                                        gotSKU = true;
                                    }
                                }

                                if (!gotSKU) {
                                    Toast.makeText(getContext(), "Cannot restore Ads, could not find purchase associated with " + REMOVE_ADS_SKU + ".", Toast.LENGTH_SHORT).show();
                                    Log.d(AD_TAG, "Cannot restore Ads, could not find purchase associated with " + REMOVE_ADS_SKU + ".");
                                }
                            } else {
                                Toast.makeText(getContext(), "Cannot restore Ads, billing client response was " + responseCode, Toast.LENGTH_SHORT).show();
                                Log.d(AD_TAG, "Cannot restore Ads, billing client response was " + responseCode);
                            }
                        } else {
                            Toast.makeText(getContext(), "Cannot restore Ads, billing client not connected.", Toast.LENGTH_SHORT).show();
                            Log.d(AD_TAG, "Cannot restore Ads, billing client not connected.");
                        }
                    } else {
                        Toast.makeText(getContext(), "Cannot restore Ads, item " + REMOVE_ADS_SKU + " was not purchased.", Toast.LENGTH_SHORT).show();
                        Log.d(AD_TAG, "Cannot restore Ads, item " + REMOVE_ADS_SKU + " was not purchased.");
                    }
                    return false;
                }
            });

            // Add the preferences (category first)
            manager.getPreferenceScreen().addPreference(devOptionsCat);
            devOptionsCat.addPreference(restoreAds);
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

//        adLocPref = manager.findPreference(getString(R.string.key_ad_location));

        // Display Preferences
        darkModePref = manager.findPreference(getString(R.string.key_dark_mode));
        buttonAnimPref = manager.findPreference(getString(R.string.key_button_anim));

        // Info Preferences
        creditsPref = manager.findPreference(getString(R.string.key_credits));

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

        buttonAnimPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!(boolean) newValue) {
                    // If the user turned off the roll initiative button animation, CHIDE THEM
                    Toast.makeText(getContext(), R.string.button_anim_chide, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        creditsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager fm = getChildFragmentManager();
                CreditsFragment.newInstance().show(fm, "CreditsDialog");
                return false; // Never have anything actually CHANGE...
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check to see if any purchases have changed status
        purchaseHandler.queryPurchases();
//        updateAdVis();
    }

//    private void updateAdVis() {
//        // Update the ad visibility on this screen based on whether or not the user purchased the no-ads item
//        // Check the purchase status
//        if (client != null) {
//            Purchase.PurchasesResult result = client.queryPurchases(BillingClient.SkuType.INAPP);
//            int responseCode = result.getResponseCode();
//            if (responseCode == BillingClient.BillingResponseCode.OK) {
//                // We now have a list of purchases
//                List<Purchase> pList = result.getPurchasesList();
//                for (Purchase p : pList) {
//                    handlePurchase(p); // Go through each one and handle them as needed
//                }
//            } else {
//                Log.w(AD_TAG, "Could not query purchases, got result: " + result.getBillingResult().getDebugMessage());
//            }
//        }
//    }

    private void setAdCategoryVisible(boolean isVisible) {
        // TODO CHECK THIS!!!
        try {
            PreferenceCategory adCat = findPreference(getString(R.string.key_ads_category)); // TODO: Getting Fragment not attached to context error
            adCat.setVisible(isVisible);
        } catch (Exception e) {
            // Do nothing...the visibility being wrong can't really hurt us, because the purchase flow won't start if the remove-ads entitlement has been bought already
        }

//        PreferenceScreen screen = findPreference(getString(R.string.key_screen));
//        if (isVisible) {
//            if (!adCatIsAttached) {
//                // If the category is not attached, but it should be visible...
//                PreferenceCategory adCat = findPreference(getString(R.string.key_ads_category));
//                if (adCat != null) {
//                    screen.addPreference(adCat); // Attach the category
//                }
//            }
//        } else {
//            if (adCatIsAttached) {
//                // If the category is attached, but it should be invisible...
//                if (screen != null) {
//                    screen.removePreferenceRecursively(getString(R.string.key_ads_category)); // Remove the Ads category, because it no longer applies to them!
//                }
//            }
//        }
//
//        // Update the stored value of this category's visibility
//        adCatIsAttached = isVisible;
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

        if (newPresetValue.equals(getString(R.string.rpg_ver_entry_dnd1))) {
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

        if (newPresetValue.equals(getString(R.string.rpg_ver_entry_adnd1))) {
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
            ArrayList<String> presetVals = new ArrayList<>(Arrays.asList(getContext().getResources().getStringArray(R.array.rpg_ver_entry_vals)));
            if (presetVals.contains(newPresetValue)) {
                presetPref.setValueIndex(presetVals.indexOf(newPresetValue));
            }
        }


        return true;
    }

    private void setPresetToCustom() {
        // Set the preset list preference to the "Custom" position, to indicate that something has been changed and we (may be) no longer in a preset
        ArrayList<String> allPresetStrings = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.rpg_ver_entry_vals)));
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

    public void handlePurchases(HashSet<String> purchases) {
        // Go through all purchase objects returned by the client, and handle the current payment status on each
        // Display Ad settings, depending on whether or not the user has bought the remove-ads entitlement
        setAdCategoryVisible(!purchases.contains(REMOVE_ADS_SKU));

        if (!purchases.contains(REMOVE_ADS_SKU)) {
            // If the user has not bought the remove_ads entitlement, and we have gotten here, it means that they are now able to buy it if they would like
            purchasePref.setEnabled(purchaseHandler.isPurchaseFlowReady(REMOVE_ADS_SKU));
        }
    }
}
