package to.us.suncloud.myapplication;

import static to.us.suncloud.myapplication.ViewSavedCombatantsFragment.COMBATANT_LIST_SAVE_FILE;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ConfigureFightableListActivity extends AppCompatActivity implements
        ListFightableRecyclerAdapter.MasterAFFLKeeper,
        ViewSavedCombatantsFragment.ReceiveAddedCombatant,
        PurchaseHandler.purchaseHandlerInterface,
        AllEncounterSaveData.EncounterDataHolder {

    public static final String COMBATANT_LIST = "combatantListList"; // ID for inputs to the activity
    public static final String ROUND_NUMBER = "roundNumber"; // ID for inputs to the activity
    public static final String MAX_ROUND_ROLLED = "maxRoundRolled"; // ID for inputs to the activity
    //    public static final String ACTIVE_COMBATANT_NUMBER = "activeCombatantNumber"; // ID for inputs to the activity
    public static final String ENCOUNTER_DATA = "encounterData"; // ID for the encounter list data (dice rolls, etc) sorted as a EncounterCombatantList (really just for savedStateInstance, we transfer the Combatant list data with the Encounter Activity using a single, finalized EncounterCombatantList)

    public static final int COMBATANT_LIST_CODE = 0; // The Code to represent requesting a Combatant List back from the Encounter Activity, used in startActivityForResult()
    public static final int PICKED_EXPORT_FILE_CODE = 1; // The Code to represent choosing a file to export the Combatant list to
    public static final int PICKED_IMPORT_FILE_CODE = 2; // The Code to represent choosing a file to import

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
    ListFightableRecyclerAdapter adapter; // Adapter that holds the combatantList

    // Stored values for the Encounter Activity
    AllFactionFightableLists combatantLists = new AllFactionFightableLists();
    AllFactionFightableLists fightablesReadFromFile = null;
    int roundNumber = 1; // The current round number of the Encounter
    int maxRoundNumber = 0; // The max round number that has been rolled of the Encounter
    //    int curActiveCombatant = EncounterCombatantRecyclerAdapter.PREP_PHASE; // The currently active Combatant in the Encounter
    EncounterCombatantList curEncounterListData = null; // Keep track of any historical meta-data related to the encounter (dice rolls, any other things I was silly enough to try and keep consistent even though only like 2% of users will use this feature...)

//    HashMap<Fightable.Faction, FactionFragmentInfo> factionFragmentMap = new HashMap<>();

//    AllFactionFightableLists combatantLists = new AllFactionFightableLists();

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
                combatantLists = (AllFactionFightableLists) savedInstanceState.getSerializable(COMBATANT_LIST);
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
        } else {
            AllEncounterSaveData savedData = AllEncounterSaveData.readEncounterData( this );
            if ( savedData != null ) {
                // Extract the saved data
                combatantLists = savedData.savedCombatantLists;
                roundNumber = savedData.savedRoundNumber;
                maxRoundNumber = savedData.savedMaxRoundNumber;
                curEncounterListData = savedData.savedCurEncounterListData;
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
                Intent encounterIntent = new Intent(ConfigureFightableListActivity.this, EncounterActivity.class);

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

        // Create a ListFightableRecyclerAdapter, and assign it to the RecyclerView
        ListFightableRecyclerAdapter.LFRAFlags flags = new ListFightableRecyclerAdapter.LFRAFlags(); // Create flags structure (does this look worse?  It may look worse...I just wanted it to be clean!!!)
        flags.adapterCanModify = true;
        flags.adapterCanCopy = true;
        flags.adapterAllowsOrdinals = true;
        adapter = new ListFightableRecyclerAdapter(this, combatantLists, flags);
        combatantListView.setAdapter(adapter);
        combatantListView.setHasFixedSize(true);
        combatantListView.addItemDecoration(new BannerDecoration(getApplicationContext()));
        combatantListView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        notifyFightableListChanged(); // Update GUI, and save Encounter data
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == COMBATANT_LIST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    // Get the Combatant list
                    EncounterCombatantList newCombatantList = (EncounterCombatantList) data.getSerializableExtra(COMBATANT_LIST);
                    if (newCombatantList != null && !newCombatantList.isEmpty()) {
                        curEncounterListData = newCombatantList; // Save the Encounter meta-data
                        combatantLists = new AllFactionFightableLists(newCombatantList); // Get a Combatant list back from the Encounter
                        adapter.setCombatantList(combatantLists); // Save this Combatant list to the adapter
                    }

                    // Get the current and max round numbers
                    roundNumber = data.getIntExtra(ROUND_NUMBER, 1);
                    maxRoundNumber = data.getIntExtra(MAX_ROUND_ROLLED, 0);
//                    curActiveCombatant = data.getIntExtra(ACTIVE_COMBATANT_NUMBER, EncounterCombatantRecyclerAdapter.PREP_PHASE);
                    updateMainButton();
                }
            }
            adapter.notifyListChanged();
        } else if ( requestCode == PICKED_EXPORT_FILE_CODE ) {
            // Save data to the given file
            if (resultCode == RESULT_OK) {
                try {
                    FileOutputStream fileOutputStream = (FileOutputStream) getContentResolver().openOutputStream(data.getData());
                    AllFactionFightableLists list = (AllFactionFightableLists) LocalPersistence.readObjectFromFile(getContext(), COMBATANT_LIST_SAVE_FILE);
                     if ( list != null ) {
                         JSONObject jsonObject = list.toJSON();
                         String outputString = jsonObject.toString();
                         fileOutputStream.write(outputString.getBytes());
                         Toast.makeText(getContext(), "Saved combatants to file!", Toast.LENGTH_SHORT).show();
                     } else {
                         Toast.makeText(getContext(), "No combatants to export!", Toast.LENGTH_SHORT).show();
                     }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if ( requestCode == PICKED_IMPORT_FILE_CODE ) {
            // Save data to the given file
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                try {
                    // Import the json file
                    String jsonString = readTextFromUri(uri);
                    JSONObject jsonObj = new JSONObject(jsonString);
                    fightablesReadFromFile = new AllFactionFightableLists(jsonObj);

                    // Get saved Combatant list, for comparison
                    AllFactionFightableLists savedList = (AllFactionFightableLists) LocalPersistence.readObjectFromFile(getContext(), COMBATANT_LIST_SAVE_FILE);

                    // Attempt to fix Group data
                    if ( savedList != null ) {
                        for (Fightable fightable : fightablesReadFromFile.getFactionList(Fightable.Faction.Group).getFightableArrayList() ) {
                            CombatantGroup combatantGroup = (CombatantGroup) fightable;
                            for (CombatantGroup.CombatantGroupData combatantData : combatantGroup.getCombatantList()) {
                                Fightable sameIDFightable = savedList.getFightableWithID(combatantData);
                                if (sameIDFightable == null) {
                                    // A fightable with this ID no longer exists
                                    Fightable originalFightable = fightablesReadFromFile.getFightableWithID(combatantData);
                                    if (originalFightable != null) {
                                        Fightable sameNameFightable = savedList.getFightableWithName(originalFightable.getName());
                                        if (sameNameFightable != null) {
                                            combatantData.mID = sameNameFightable.getId();
                                            combatantData.mFaction = sameNameFightable.getFaction();
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // TODO: Add error handling for badly formatted data...?
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), String.format("Could not import Combatants: %s", e), Toast.LENGTH_LONG).show();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream =
                     getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
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

        if ( fightablesReadFromFile != null ) {
            // We read some Fightables from a file, so we need to go to the bookmarks fragment

            // Open the bookmarks fragment, and all of these new Fightables to the bookmarks
            FragmentManager fm_ob = getSupportFragmentManager();
            ViewSavedCombatantsFragment viewBookmarksFrag = ViewSavedCombatantsFragment.newViewBookmarkedFightablesInstance(fightablesReadFromFile);
            viewBookmarksFrag.show(fm_ob, "Open Bookmarks");
            Toast.makeText(getContext(), "Added combatants from file!", Toast.LENGTH_SHORT).show();

            fightablesReadFromFile = null;
        }
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
    public void receiveChosenFightable(Fightable selectedCombatant) {
        // Receive a Combatant from a ListFightableRecyclerAdapter
        // Do nothing, because this is "receiving" a Combatant from the configure list, which shouldn't do anything on being touched
    }

    @Override
    public Context getContext() {
        return ConfigureFightableListActivity.this;
    }

    @Override
    public void notifyFightableListChanged() {
        // The Combatant List has (maybe) been changed, so we should update the "No Combatants" view information
        updateNoCombatantMessage();

        saveEncounter();
    }

    private void saveEncounter() {
        AllEncounterSaveData.saveEncounterData(this);
    }

    private void removeSavedEncounterData() {
        AllEncounterSaveData.removeEncounterData();
    }

    @Override
    public void notifyIsMultiSelecting(boolean isMultiSelecting) {
        // Do nothing, because we shouldn't be multi-selecting with this adapter anyway...
        Log.e("ConfigCombatant", "Notified of multi-selecting even though it shouldn't be activated for the child adapter.");
    }

    @Override
    public boolean safeToDelete(Fightable fightable) {
        if (fightable instanceof Combatant) {
            if (curEncounterListData == null) {
                return true; // If there is no encounter list, then this Combatant is safe to delete
            } else {
                return !curEncounterListData.contains((Combatant) fightable); // A Combatant is safe to delete if the encounter Combatant list does not contain it
            }
        } else {
            // We have no opinion on whether or not non-Combatant Fightables get deleted, particularly because they shouldn't exist here
            return true;
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
//    public void addFightable(Combatant combatantToAdd) {
//        // Add the Combatant indicated
//        combatantLists.addFightable(combatantToAdd);
//
//        // Update the Fragment displace
//        updateFactionFragmentDisplay();
//    }

    @Override
    public void receiveAddedCombatant(final Combatant addedCombatant) {
        // Receive a new Combatant from the VSCF
        // TODO: Check if we can use receiveFightable here.  May want addFightableToList here instead, assuming no funky edge cases can happen here...?
        adapter.receiveFightable( addedCombatant ); // Add the new Combatant to OUR adapter

        // Create/destroy/update any faction fragments as needed
        updateNoCombatantMessage();

    }

    @Override
    public boolean containsCombatantWithSameBaseName(Combatant combatant) {
        return adapter.getCombatantList().containsCombatantWithBaseName(combatant.getBaseName());
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
                        ConfigureFightableListActivity.super.onBackPressed();
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

        if (id == R.id.settings) {// Open the settings menu
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            if (curEncounterListData != null) {
                settingsIntent.putExtra(SettingsFragment.IS_MID_COMBAT, curEncounterListData.isMidCombat()); // Let the Settings Activity know if we are mid-combat
            }
            startActivity(settingsIntent);
            return true;
        } else if (id == R.id.open_bookmarks) {// Open the bookmarked Combatants menu
            FragmentManager fm_ob = getSupportFragmentManager();
            ViewSavedCombatantsFragment viewBookmarksFrag = ViewSavedCombatantsFragment.newViewBookmarkedFightablesInstance();
            viewBookmarksFrag.show(fm_ob, "Open Bookmarks");
            return true;
        } else if (id == R.id.clear_encounter) {// Clear the encounter, if the user confirms
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.clear_encounter_title)
                    .setMessage(R.string.clear_encounter_message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            adapter.clearCombatantList();
                            roundNumber = 1;
                            maxRoundNumber = 0;
                            curEncounterListData = null;

                            removeSavedEncounterData();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Do nothing
                        }
                    })
                    .show();
        } else if ( id == R.id.import_roster ) {
            // TODO: START HERE - can't choose a file to open...
            Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE);
//                .putExtra(Intent.EXTRA_TITLE, getString(R.string.default_json_filename));
            startActivityForResult(chooseFile, PICKED_IMPORT_FILE_CODE);
        } else if ( id == R.id.export_roster ) {
            Intent saveFile = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("text/json")
                .putExtra(Intent.EXTRA_TITLE, getString(R.string.default_json_filename));
            startActivityForResult(saveFile, PICKED_EXPORT_FILE_CODE);
        } else {
            // Do Nothing - unrecognized ID
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public AllFactionFightableLists getSavedCombatantLists() {
        return combatantLists;
    }

    @Override
    public EncounterCombatantList getSavedCurEncounterListData() {
        return curEncounterListData;
    }

    @Override
    public int getSavedRoundNumber() {
        return roundNumber;
    }

    @Override
    public int getSavedMaxRoundNumber() {
        return maxRoundNumber;
    }
}
