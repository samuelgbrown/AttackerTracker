package to.us.suncloud.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ConfigureCombatantListActivity extends AppCompatActivity implements ListCombatantRecyclerAdapter.MasterCombatantKeeper, ViewSavedCombatantsFragment.ReceiveAddedCombatant, PurchaseHandler.purchaseHandlerInterface {

    public static final String COMBATANT_LIST = "combatantListList"; // ID for inputs to the activity
    public static final String ROUND_NUMBER = "roundNumber"; // ID for inputs to the activity
    public static final String MAX_ROUND_ROLLED = "maxRoundRolled"; // ID for inputs to the activity
    //    public static final String ACTIVE_COMBATANT_NUMBER = "activeCombatantNumber"; // ID for inputs to the activity
    public static final String ENCOUNTER_DATA = "encounterData"; // ID for the encounter list data (dice rolls, etc) sorted as a EncounterCombatantList (really just for savedStateInstance, we transfer the Combatant list data with the Encounter Activity using a single, finalized EncounterCombatantList)

    public static final int COMBATANT_LIST_CODE = 0; // The Code to represent requesting a Combatant List back from the Encounter Activity, used in startActivityForResult()

    int curTheme; // The current theme of this Activity

    // Parameters related to display ads
    ConstraintLayout adContainer;
    AdView adView;
    PrefsHelper.AdLocation curAdLoc;
    String REMOVE_ADS_SKU = "attacker_tracker.remove.ads";
    //    String REMOVE_ADS_SKU = "android.test.purchased";
    PurchaseHandler purchaseHandler;

    TextView mainButton; // The Main button at the bottom, to transfer over to the Encounter Activity
    View noCombatantMessage; // Text message to show the use when there are no Combatants
    RecyclerView combatantListView; // RecyclerView that holds the Combatant List
    ListCombatantRecyclerAdapter adapter; // Adapter that holds the combatantList

    // Stored values for the Encounter Activity
    AllFactionCombatantLists combatantLists = new AllFactionCombatantLists();
    int roundNumber = 1; // The current round number of the Encounter
    int maxRoundNumber = 0; // The max round number that has been rolled of the Encounter
    //    int curActiveCombatant = EncounterCombatantRecyclerAdapter.PREP_PHASE; // The currently active Combatant in the Encounter
    EncounterCombatantList curEncounterListData = null; // Keep track of any historical meta-data related to the encounter (dice rolls, any other things I was silly enough to try and keep consistent even though only like 2% of users will use this feature...)

//    HashMap<Combatant.Faction, FactionFragmentInfo> factionFragmentMap = new HashMap<>();

//    AllFactionCombatantLists combatantLists = new AllFactionCombatantLists();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the theme
        curTheme = PrefsHelper.getTheme(getApplicationContext());
        setTheme(curTheme);
        setContentView(R.layout.activity_main);

        // Initialize Ads (early because it involves a server request)
        List<String> allSKUs = new ArrayList<>();
        allSKUs.add(REMOVE_ADS_SKU);
        purchaseHandler = new PurchaseHandler(this, allSKUs);

        // Initialize the ad retrieval process (just in case)
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        // Get the combatantList and any other initialization parameters
        if (savedInstanceState != null) {
            // Get the Combatant lists
            if (savedInstanceState.containsKey(COMBATANT_LIST)) {
                combatantLists = (AllFactionCombatantLists) savedInstanceState.getSerializable(COMBATANT_LIST);
            }

            // Get the current and max round numbers
            if (savedInstanceState.containsKey(ROUND_NUMBER)) {
                roundNumber = savedInstanceState.getInt(ROUND_NUMBER, 1);
            }

            // Get the current round number
            if (savedInstanceState.containsKey(MAX_ROUND_ROLLED)) {
                maxRoundNumber = savedInstanceState.getInt(MAX_ROUND_ROLLED, 0);
            }

            // Get the meta-data for the current encounter
            if (savedInstanceState.containsKey(ENCOUNTER_DATA)) {
                curEncounterListData = (EncounterCombatantList) savedInstanceState.getSerializable(ENCOUNTER_DATA);
            }
        }

        // Store all of the Views we will need
        mainButton = findViewById(R.id.finish_button);
        Toolbar toolbar = findViewById(R.id.configure_toolbar);
        noCombatantMessage = findViewById(R.id.configure_combatant_empty);
        combatantListView = findViewById(R.id.configure_combatant_list);

//        // Ready Ads for display
//        prepareAd();

        // Set up the Views
        // Main Button
        updateMainButton();

        mainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the Encounter!  WOOO!!
                Intent encounterIntent = new Intent(ConfigureCombatantListActivity.this, EncounterActivity.class);

                // Create an EncounterCombatantList to send to the Encounter (done here so we can send along the meta-data included in the EncounterCombatantList, so the state stays consistent)
                EncounterCombatantList combatantListToSend;
                if (curEncounterListData != null) {
                    // Update the EncounterCombatantList
                    combatantListToSend = curEncounterListData; // Set the meta-data (dice rolls, etc) to be the same as before

                    // Add the actual list of Combatants to the EncounterCombatantList
                    combatantListToSend.updateCombatants(adapter.getCombatantList()); // Do not initialize the Combatants, but add the new ones to the list
                } else {
                    // Create a new EncounterCombatantList (and "initialize" the Combatants - setting them all to isSelected so we start in the preparation phase of combat)
                    combatantListToSend = new EncounterCombatantList(adapter.getCombatantList(), getContext());
                }

                encounterIntent.putExtra(COMBATANT_LIST, combatantListToSend);
                encounterIntent.putExtra(ROUND_NUMBER, roundNumber);
                encounterIntent.putExtra(MAX_ROUND_ROLLED, maxRoundNumber);
                startActivityForResult(encounterIntent, COMBATANT_LIST_CODE);
            }
        });

        // Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.configure_title);

        // Create a ListCombatantRecyclerAdapter, and assign it to the RecyclerView
        ListCombatantRecyclerAdapter.LCRAFlags flags = new ListCombatantRecyclerAdapter.LCRAFlags(); // Create flags structure (does this look worse?  It may look worse...I just wanted it to be clean!!!)
        flags.adapterCanCopy = true;
        flags.adapterCanModify = true;
        adapter = new ListCombatantRecyclerAdapter(this, combatantLists, flags);
        combatantListView.setAdapter(adapter);
        combatantListView.setHasFixedSize(true);
        combatantListView.addItemDecoration(new BannerDecoration(getApplicationContext()));
        combatantListView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    @Override
    public void handlePurchases(HashSet<String> purchases) {
        displayAd(!purchases.contains(REMOVE_ADS_SKU)); // If the user purchased the remove_ads entitlement, do not display ads
    }

    public void displayAd(boolean isVisible) {
        // Display an ad if required, or remove it
        if (isVisible) {
            setupAd(); // Put up the ad
        } else {
            removeAd(); // Remove the ads from the screen (if the screen is displayed right now)
        }
    }

    private void setupAd() {
        // Set up the banner ad on screen (if required, otherwise just remove it)

        // Ensure that an ad is being displayed
        // Get the ad's current location
        PrefsHelper.AdLocation adLoc = PrefsHelper.getAdLocation(getApplicationContext());

        // If this is a new location (or it's the first time we're loading an ad since we created this Activity
        if (curAdLoc == null || adLoc != curAdLoc) {
            // If we need to place an ad in the new container

            // Set the old ad container back to standby
            removeAd();

            // Retrieve the new ad container, and make it visible
            adContainer = getAdContainer(adLoc);
            adContainer.setVisibility(View.VISIBLE);

            // Add the ad to the container
            if (adView == null) {
                // If no AdView has been created yet, create one
                adView = new AdView(this);
                adView.setAdSize(AdSize.BANNER);

                adView.setAdUnitId(getString(R.string.encounter_ad_id));

                // Request and load an ad
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
                adView.setId(View.generateViewId());
            }
            adContainer.addView(adView, 1);

            // Ensure that the adView is in the center of the container
            ConstraintSet set = new ConstraintSet();
            set.clone(adContainer);
            set.connect(adView.getId(), ConstraintSet.TOP, adContainer.getId(), ConstraintSet.TOP, 0);
            set.connect(adView.getId(), ConstraintSet.BOTTOM, adContainer.getId(), ConstraintSet.BOTTOM, 0);
            set.connect(adView.getId(), ConstraintSet.START, adContainer.getId(), ConstraintSet.START, 0);
            set.connect(adView.getId(), ConstraintSet.END, adContainer.getId(), ConstraintSet.END, 0);
            set.applyTo(adContainer);
            adContainer.bringChildToFront(adView);
        }

        // Store the new ad's location
        curAdLoc = adLoc;
//        } else {
//            // No ad should be displayed, so remove any that are
//            removeAd();
//        }
    }

    private void removeAd() {
        // Ensure that no ad is being displayed on screen
        if (adContainer != null) {
            // If there is already an ad on screen
            if (adView != null) {
                adContainer.removeView(adView); // Remove the adView, to be moved to a new container
            }

            // Make the old container invisible
            adContainer.setVisibility(View.GONE);
        }

        curAdLoc = null; // The ad does not have a location any more
    }

    private ConstraintLayout getAdContainer(PrefsHelper.AdLocation adLoc) {
        // Give an AdLocation, get the corresponding ConstraintLayout container in the layout
        switch (adLoc) {
            case Top:
                return findViewById(R.id.configure_ad_container_top);
            case Bottom_Above:
                return findViewById(R.id.configure_ad_container_bottom_above);
            case Bottom_Below:
                return findViewById(R.id.configure_ad_container_bottom_below);
        }
        return findViewById(R.id.encounter_ad_container_bottom_below); // Default
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save a few useful things (don't let Google see this, they'll get pissed off at how much data I'm saving in outState...whoops...)
        outState.putInt(ROUND_NUMBER, roundNumber);
        outState.putInt(MAX_ROUND_ROLLED, maxRoundNumber);
        outState.putSerializable(COMBATANT_LIST, combatantLists);
        outState.putSerializable(ENCOUNTER_DATA, curEncounterListData);

        // Save the instance data
        super.onSaveInstanceState(outState);
    }

    public void updateMainButton() {
        if (roundNumber > 1) {
            mainButton.setText(R.string.resume_encounter);
        } else {
            mainButton.setText(R.string.begin_encounter);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == COMBATANT_LIST_CODE) {
            // Don't know what else it would be...
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    // Get the Combatant list
                    EncounterCombatantList newCombatantList = (EncounterCombatantList) data.getSerializableExtra(COMBATANT_LIST);
                    if (newCombatantList != null && !newCombatantList.isEmpty()) {
                        curEncounterListData = newCombatantList; // Save the Encounter meta-data
                        combatantLists = new AllFactionCombatantLists(newCombatantList); // Get a Combatant list back from the Encounter
                        adapter.setCombatantList(combatantLists); // Save this Combatant list to the adapter
                    }

                    // Get the current and max round numbers
                    roundNumber = data.getIntExtra(ROUND_NUMBER, 1);
                    maxRoundNumber = data.getIntExtra(MAX_ROUND_ROLLED, 0);
//                    curActiveCombatant = data.getIntExtra(ACTIVE_COMBATANT_NUMBER, EncounterCombatantRecyclerAdapter.PREP_PHASE);
                    updateMainButton();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the theme if needed
        if (curTheme != PrefsHelper.getTheme(getApplicationContext())) {
            // If the theme has been changed, then recreate the Activity
            recreate();
        }

        updateNoCombatantMessage();

        // Update the ad location, if needed (based on if we THINK the user bought ads)
        displayAd(!purchaseHandler.wasPurchased(REMOVE_ADS_SKU));
        purchaseHandler.queryPurchases(); // Check to see if anything has changed on the Purchases front...
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    void updateNoCombatantMessage() {
        boolean haveCombatants = false;
        if (adapter.getCombatantList() != null) {
            // If there are factions, check if any of them have combatants.  Otherwise, display the no Combatant message
//            haveCombatants = !adapter.getCombatantList().isEmpty();
            haveCombatants = !adapter.getCombatantList().isVisibleEmpty();
        }

        if (haveCombatants) {
            // If there are combatants, don't display the message
            noCombatantMessage.setVisibility(View.GONE);
        } else {
            // If there are no combatants, display the message
            noCombatantMessage.setVisibility(View.VISIBLE);
        }
    }

    // Interface methods

    @Override
    public void receiveChosenCombatant(Combatant selectedCombatant) {
        // Receive a Combatant from a ListCombatantRecyclerAdapter
        // Do nothing, because this is "receiving" a Combatant from the configure list, which shouldn't do anything on being touched
    }

    @Override
    public Context getContext() {
        return ConfigureCombatantListActivity.this;
    }

    @Override
    public void notifyCombatantListChanged() {
        // The Combatant List has (maybe) been changed, so we should update the "No Combatants" view information
        updateNoCombatantMessage();
    }

    @Override
    public void notifyIsMultiSelecting(boolean isMultiSelecting) {
        // Do nothing, because we shouldn't be multi-selecting with this adapter anyway...
        Log.e("ConfigCombatant", "Notified of multi-selecting even though it shouldn't be activated for the child adapter.");
    }

    @Override
    public boolean safeToDelete(Combatant combatant) {
        if (curEncounterListData == null) {
            return true; // If there is no encounter list, then this Combatant is safe to delete
        } else {
            return !curEncounterListData.contains(combatant); // A Combatant is safe to delete if the encounter Combatant list does not contain it
        }
    }

    //    @Override
//    public void removeCombatant(Combatant combatantToRemove) {
//        // Remove the Combatant indicated
//        combatantLists.remove(combatantToRemove);
//
//        // A Combatant in one of the Fragments was just removed.  Update the list, in case one of the Fragments needs to be removed
//        updateFactionFragmentDisplay();
//    }
//
//    @Override
//    public void addCombatant(Combatant combatantToAdd) {
//        // Add the Combatant indicated
//        combatantLists.addCombatant(combatantToAdd);
//
//        // Update the Fragment displace
//        updateFactionFragmentDisplay();
//    }

    @Override
    public void receiveAddedCombatant(Combatant addedCombatant) {
        // Receive a new Combatant from the AddCombatantToList dialog
        adapter.addCombatant(addedCombatant); // Add the new Combatant to OUR adapter

        // Create/destroy/update any faction fragments as needed
        updateNoCombatantMessage();
    }

    @Override
    public void onBackPressed() {
        // Make sure the user really wants to exit, before their data is lost
        new AlertDialog.Builder(this)
                .setTitle(R.string.leave_app_title)
                .setMessage(R.string.leave_app_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Leave the app
                        ConfigureCombatantListActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .show();
    }

    // Related to the toolbar

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_configure, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.settings:
                // Open the settings menu
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                if (curEncounterListData != null) {
                    settingsIntent.putExtra(SettingsFragment.IS_MID_COMBAT, curEncounterListData.isMidCombat()); // Let the Settings Activity know if we are mid-combat
                }
                startActivity(settingsIntent);
                return true;
            case R.id.open_bookmarks:
                // Open the bookmarked Combatants menu
                FragmentManager fm_ob = getSupportFragmentManager();
                ViewSavedCombatantsFragment viewBookmarksFrag = ViewSavedCombatantsFragment.newModifySavedCombatantListInstance();
                viewBookmarksFrag.show(fm_ob, "Open Bookmarks");
                return true;
            case R.id.add_combatant:
                // Open the add Combatant menu
                FragmentManager fm_ac = getSupportFragmentManager();
//                ViewSavedCombatantsFragment addCombatantFrag = ViewSavedCombatantsFragment.newAddCombatantToListInstance(this, adapter.getCombatantList());
                ViewSavedCombatantsFragment addCombatantFrag = ViewSavedCombatantsFragment.newAddCombatantToListInstance(adapter.getCombatantList());
                addCombatantFrag.show(fm_ac, "AddCombatantToList");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Activity getActivity() {
        return this;
    }
}
