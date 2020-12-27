package to.us.suncloud.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EncounterActivity extends AppCompatActivity implements EncounterCombatantRecyclerAdapter.combatProgressInterface, PurchaseHandler.purchaseHandlerInterface {
    EncounterCombatantList masterCombatantList;
    EncounterCombatantRecyclerAdapter adapter; // The adapter for the main Combatant list

    // The sort options in the menu, to change their visibility based on state
    MenuItem sortInit;
    MenuItem sortAlpha;

//    int roundNumber = 1;
//    int maxRoundRolled = 0; // TODO: Will also use this in the Player Roll feature

    int curTheme; // The current theme of this Activity

    // Parameters related to display ads
    ConstraintLayout adContainer;
    AdView adView;
    PrefsHelper.AdLocation curAdLoc;
    String REMOVE_ADS_SKU = "attacker_tracker.remove.ads";
    //    String REMOVE_ADS_SKU = "android.test.purchased";
    PurchaseHandler purchaseHandler;

    ConstraintLayout endOfRoundBanner;
    TextView prepBanner;
    RecyclerView encounterRecyclerView;
    Toolbar encounterToolbar;
    TextView noCombatantTextView;
    Button nextButton;
    ImageButton previousButton;
    TextView roundCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the view and theme
        curTheme = PrefsHelper.getTheme(getApplicationContext());
        setTheme(curTheme);
        setContentView(R.layout.activity_encounter);

        // Initialize Ads (early because it involves a server request)
        List<String> allSKUs = new ArrayList<>();
        allSKUs.add(REMOVE_ADS_SKU);
        purchaseHandler = new PurchaseHandler(this, allSKUs);

        // Initialize the ad retrieval process
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        // Get info from the intent that started this activity
        Intent intent = getIntent();
        Bundle thisBundleData = intent.getExtras();

        // Initialize some parameters, if needed
        masterCombatantList = new EncounterCombatantList(getApplicationContext());
        int roundNumber = 0;
        int maxRoundRolled = 0;

        if (savedInstanceState != null) {
            // If there is a saved state, then there was probably just a configuration change, so savedInstanceState is the most up-to-date information we have
            // Get the current and max round numbers
            if (savedInstanceState.containsKey(ConfigureCombatantListActivity.ROUND_NUMBER)) {
                roundNumber = savedInstanceState.getInt(ConfigureCombatantListActivity.ROUND_NUMBER, 1);
            }

            if (savedInstanceState.containsKey(ConfigureCombatantListActivity.MAX_ROUND_ROLLED)) {
                maxRoundRolled = savedInstanceState.getInt(ConfigureCombatantListActivity.MAX_ROUND_ROLLED, 0);
            }

            // Get the master Combatant list
            if (savedInstanceState.containsKey(ConfigureCombatantListActivity.COMBATANT_LIST)) {
                EncounterCombatantList newList = (EncounterCombatantList) savedInstanceState.getSerializable(ConfigureCombatantListActivity.COMBATANT_LIST);
                if (newList != null) {
                    masterCombatantList = newList;
                }
            }
        } else {
            // If there is no saved state, then load in the data from the previous activity
            // Get the current round number
            if (thisBundleData.containsKey(ConfigureCombatantListActivity.ROUND_NUMBER)) {
                roundNumber = thisBundleData.getInt(ConfigureCombatantListActivity.ROUND_NUMBER);
            }

            if (thisBundleData.containsKey(ConfigureCombatantListActivity.MAX_ROUND_ROLLED)) {
                maxRoundRolled = thisBundleData.getInt(ConfigureCombatantListActivity.MAX_ROUND_ROLLED, 0);
            }

            // Get the Combatant List
            if (thisBundleData.containsKey(ConfigureCombatantListActivity.COMBATANT_LIST)) {
                EncounterCombatantList newList = (EncounterCombatantList) thisBundleData.getSerializable(ConfigureCombatantListActivity.COMBATANT_LIST);
                if (newList != null) {
                    masterCombatantList = newList;
                }
            }
        }

//        // Get the currently active Combatant
//        int curActiveCombatantIn = 1;
//        if (thisBundleData.containsKey(ConfigureCombatantListActivity.ACTIVE_COMBATANT_NUMBER)) {
//            curActiveCombatantIn = thisBundleData.getInt(ConfigureCombatantListActivity.ACTIVE_COMBATANT_NUMBER);
//        }

        // Retrieve all Views
        encounterToolbar = findViewById(R.id.encounter_toolbar);
        encounterRecyclerView = findViewById(R.id.encounter_recycler_view);
        prepBanner = findViewById(R.id.encounter_prepare_message);
        endOfRoundBanner = findViewById(R.id.encounter_end_of_round_container);
        noCombatantTextView = findViewById(R.id.encounter_combatant_empty);
        nextButton = findViewById(R.id.encounter_continue);
        previousButton = findViewById(R.id.encounter_previous_combatant);
        roundCounter = findViewById(R.id.encounter_round_counter);

        // Ready Ads for display
        // TODO SOON: Remove all log points for production
//        prepareAd();

        // Set up the RecyclerView
        adapter = new EncounterCombatantRecyclerAdapter(this, masterCombatantList, roundNumber);
        encounterRecyclerView.setAdapter(adapter);
        encounterRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        // Initialize the adapter as needed
        adapter.setMaxRoundRolled(maxRoundRolled);
        adapter.updateCombatProgress();
        adapter.notifyCombatantsChanged(); // Let the adapter know that this is the initial state (update all "memory" parameters for the Combatant list and the currently active Combatant)

        // Set up the banner message
        prepBanner.setText(getString(R.string.prepare_message, PrefsHelper.getModString(getApplicationContext())));

        // Set up the buttons
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If an EditText is focused, confirm the choice by stealing the focus away
                encounterToolbar.requestFocus();

                // Hide the keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(nextButton.getWindowToken(), 0);
                getCurrentFocus().clearFocus(); // Clear the current text focus

                // Increment the combat step
                nextButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Done in Runnable because when this is called outside runnable while an EditText is focused, the adapter's notifyCombatantsChanged() gets called twice in a row, and the RecyclerView doesn't get laid out properly
                        adapter.incrementCombatStep();
                    }
                }, 50); // Why delay?  Because Android is stupid, that's why.  The adapter wouldn't update right because two notifyCombatantsChanged would fire one after another, and apparently that makes Android sad

//                adapter.incrementCombatStep();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If an EditText is focused, confirm the choice by stealing the focus away
                encounterToolbar.requestFocus();

                // Hide the keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(nextButton.getWindowToken(), 0);
                getCurrentFocus().clearFocus(); // Clear the current text focus

                // Decrement the combat step
                previousButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Done in Runnable because when this is called outside runnable while an EditText is focused, the adapter's notifyCombatantsChanged() gets called twice in a row, and the RecyclerView doesn't get laid out properly
                        adapter.decrementCombatStep();
                    }
                }, 50); // Why delay?  Because Android is stupid, that's why.  The adapter wouldn't update right because two notifyCombatantsChanged would fire one after another, and apparently that makes Android sad

//                adapter.decrementCombatStep();
            }
        });

        // Setup various GUI elements
        updateNoCombatantView(); // The no Combatant message
        updateGUIState(); // The next/previous buttons

        // Setup the toolbar
        setSupportActionBar(encounterToolbar);
        getSupportActionBar().setTitle(R.string.encounter_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show the back arrow in the toolbar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the theme if needed
        if (curTheme != PrefsHelper.getTheme(getApplicationContext())) {
            // If the theme has been changed, then recreate the Activity
            recreate();
        }

        // Make sure the EncounterCombatantList has the most up-to-date preference information
        setSortIconVis();
        adapter.updatePrefs(getApplicationContext());

        // Update the ad location, if needed (based on if we THINK the user bought ads)
        displayAd(!purchaseHandler.wasPurchased(REMOVE_ADS_SKU));
        purchaseHandler.queryPurchases(); // Check to see if anything has changed on the Purchases front...

        // Update the GUI
        updateGUIState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save a few useful things (don't let Google see this, they'll get pissed off at how much data I'm saving in outState...whoops...)
        outState.putInt(ConfigureCombatantListActivity.ROUND_NUMBER, adapter.getRoundNumber());
        outState.putSerializable(ConfigureCombatantListActivity.COMBATANT_LIST, masterCombatantList);

        // Save the instance data
        super.onSaveInstanceState(outState);
    }

    public void updateGUIState() {
        // First, prepare parameters for everything that may change in the GUI
        int nextButtonText;
        int nextButtonVisibility = View.VISIBLE; // The only time this would become invisible is if there were no Combatants
        int previousButtonVisibility = View.VISIBLE; // The only time this would become invisible is if we are at the Roll Initiative stage of Round 1
        boolean doAnim = false; // Should we do the fun Roll Initiative animation?
        int prepBannerVisibility = View.GONE; // Should the Prepare Battle message be visible?
        int endOfRoundVisibility = View.GONE; // Should the End Of Round message be visible?

        // Next, get the current active Combatant and current round
//        int currentlyActiveCombatant = adapter.getCurActiveCombatant();
        int currentlyActiveCombatant = masterCombatantList.calcActiveCombatant();
        int roundNumber = adapter.getRoundNumber();
        int maxRoundRolled = adapter.getMaxRoundRolled();

        // Change GUI elements based on the current state
        if (adapter.getCombatantList().isVisiblyEmpty()) {
            // If there aren't any Combatants, then make the next button Gone
            nextButtonVisibility = View.GONE;
        }

        if (currentlyActiveCombatant == EncounterCombatantRecyclerAdapter.PREP_PHASE) {
            // If the currently active Combatant is unset, then we are currently between rounds, waiting to roll initiative
            if (PrefsHelper.getReRollInit(getContext())) {
                nextButtonText = R.string.encounter_roll_initiative;
            } else {
                nextButtonText = R.string.encounter_begin_round;
            }
            prepBannerVisibility = masterCombatantList.isVisiblyEmpty() ? View.GONE : View.VISIBLE; // Don't display the banner if the Combatant list is empty, that'd be just a little sad...
            if (roundNumber == 1) {
                // If this is the 1st round prepare phase, then don't display the previous button
                previousButtonVisibility = View.GONE;
            }
        } else if (currentlyActiveCombatant == EncounterCombatantRecyclerAdapter.END_OF_ROUND_ACTIONS) {
            // If we just moved into the end-of-round actions phase, then set the next button and banner accordingly
            nextButtonText = R.string.encounter_prepare_next_round;
            endOfRoundVisibility = View.VISIBLE;
        } else {
            // Check if we just moved into combat
            String curText = nextButton.getText().toString();
            if (currentlyActiveCombatant == 0 && (curText.equals(getResources().getString(R.string.encounter_roll_initiative)) || curText.equals(getResources().getString(R.string.encounter_begin_round)))) {
                // We just finished the prep phase, starting combat now

                if (roundNumber > maxRoundRolled) {
                    // We just started combat for this round for the first time
                    if (PrefsHelper.doingInitButtonAnim(getContext())) {
                        // If we just started the round, then emphasize the Roll Initiative button (for fun)!

                        doAnim = true;
                        nextButton.setEnabled(false); // Disable the button momentarily while the animation plays out
                        nextButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.emphize_wobble));
                    }
                }
            }

            // The next button's text depends on what happens if the currently selected Combatant gets checked off
            EncounterCombatantList testList = masterCombatantList.clone();
            testList.get(testList.getViewIndexOf(currentlyActiveCombatant)).setSelected(true); // Simulate the user checking off the current Combatant
            int nextActiveCombatant = testList.calcActiveCombatant();

            // Based on what the next step of the combat cycle is, set up the next button text
            switch (nextActiveCombatant) {
                case EncounterCombatantRecyclerAdapter.PREP_PHASE:
                    // If the currently active Combatant is the final Combatant before the prep phase, then the next action will be to prepare for the next round of combat
                    nextButtonText = R.string.encounter_prepare_next_round;
                    break;
                case EncounterCombatantRecyclerAdapter.END_OF_ROUND_ACTIONS:
                    // If the currently active Combatant is the final Combatant before the end-of-round phase, then the next action will be to perform any end-of-round actions
                    nextButtonText = R.string.encounter_end_of_round;
                    break;
                default:
                    nextButtonText = R.string.encounter_next_combatant;
            }
        }


        int delay = doAnim ? 500 : 0;
        prepBanner.setVisibility(prepBannerVisibility);
        endOfRoundBanner.setVisibility(endOfRoundVisibility);

        // God forgive me.
        nextButton.postDelayed(new GUIAlterThread(nextButton, previousButton, roundCounter, nextButtonText, nextButtonVisibility, previousButtonVisibility, roundNumber), delay);
    }

    private static class GUIAlterThread implements Runnable {
        int nextText;
        int nextVis;
        int roundNum;
        int prevVis;
        Button nextButton;
        ImageButton previousButton;
        TextView roundCounter;

        // NoN-fInAl VaRiAbLeS cAnNoT bE ReFeReNcEd FrOm InNeR cLaSsEs
        GUIAlterThread(Button nextButton, ImageButton previousButton, TextView roundCounter, int nextText, int nextVis, int prevVis, int roundNum) {
            this.nextText = nextText;
            this.nextVis = nextVis;
            this.prevVis = prevVis;
            this.roundNum = roundNum;
            this.nextButton = nextButton;
            this.previousButton = previousButton;
            this.roundCounter = roundCounter;
        }

        @Override
        public void run() {
            // Update the text on the nextButton
            nextButton.setText(nextText);
            nextButton.setVisibility(nextVis);
            nextButton.setEnabled(true); // Enable the button again in case the animation played and it was disabled during it

            // Make sure the previous button is visible
            previousButton.setVisibility(prevVis);

            // Update the number on the round counter, from the adapter
            roundCounter.setText(String.valueOf(roundNum));
        }
    }

    private void updateNoCombatantView() {
        // Update the visibility of the no Combatant view message
        if (masterCombatantList.isVisiblyEmpty()) {
            // If the Combatant list is empty, then make the message visible
            noCombatantTextView.setVisibility(View.VISIBLE);
        } else {
            // If the Combatant list is not empty, then make the message gone
            noCombatantTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_encounter, menu);

        // Get the two sort icons, for later
        sortAlpha = menu.findItem(R.id.sort_alpha);
        sortInit = menu.findItem(R.id.sort_initiative);

        // Set up the icon visibility
        setSortIconVis();

        return true;
    }

    private void setSortIconVis() {
        // Based on the current preferred sorting method, change the visibility of each sorting menu item
        if (sortAlpha != null && sortInit != null) {
            switch (adapter.getSortMethod()) {
                case INITIATIVE:
                    sortInit.setVisible(false);
                    sortAlpha.setVisible(true);
                    break;
                case ALPHABETICALLY_BY_FACTION:
                    sortInit.setVisible(true);
                    sortAlpha.setVisible(false);
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sort_alpha) {// Sort the Combatants alphabetically by faction
            adapter.sort(EncounterCombatantList.SortMethod.ALPHABETICALLY_BY_FACTION);
            setSortIconVis();
            return true;
        } else if (id == R.id.sort_initiative) {// (Attempt to) sort the Combatants by initiative
            adapter.sort(EncounterCombatantList.SortMethod.INITIATIVE);
            setSortIconVis();
            return true;
        } else if (id == R.id.dice_cheat) {// Toggle the dice cheat mode on and off
            adapter.toggleDiceCheat();
            return true;
        } else if (id == R.id.settings) {// Open the settings menu
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            settingsIntent.putExtra(SettingsFragment.IS_MID_COMBAT, masterCombatantList.isMidCombat()); // Let the Settings Activity know if we are mid-combat
            startActivity(settingsIntent);
            return true;
        } else if (id == R.id.restart) {// Restart Combat, if the user (really) wants it
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.restart_combat_title))
                    .setMessage(getString(R.string.restart_combat_message))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.restartCombat(); // This will update all relevant GUI's
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    })
                    .show();
            return true;
        } else if (id == android.R.id.home) {
            returnFromActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    public void displayAd(boolean isVisible) {
        // Display an ad if required, or remove it
        if (isVisible) {
            setupAd(); // Put up the ad
        } else {
            removeAd(); // Remove the ads from the screen (if the screen is displayed right now)
        }
    }

    @Override
    public void handlePurchases(HashSet<String> purchases) {
        displayAd(!purchases.contains(REMOVE_ADS_SKU)); // If the user purchased the remove_ads entitlement, do not display ads
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
                return findViewById(R.id.encounter_ad_container_top);
            case Bottom_Above:
                return findViewById(R.id.encounter_ad_container_bottom_above);
            case Bottom_Below:
                return findViewById(R.id.encounter_ad_container_bottom_below);
        }
        return findViewById(R.id.encounter_ad_container_bottom_below); // Default
    }

    @Override
    public void onBackPressed() {
        View focusedView = getCurrentFocus();
        if (focusedView instanceof EditText) {
            // If we are currently focused on an EditView, then back just means "lose the current focus"...
            focusedView.clearFocus();
        } else {
            // ...otherwise, it means go back to the configure screen
            returnFromActivity();
        }
    }

    private void returnFromActivity() {
        // Go back to the Configure Combatants screen
        Intent returnIntent = new Intent();
        returnIntent.putExtra(ConfigureCombatantListActivity.COMBATANT_LIST, adapter.getCombatantList()); // Create an Intent that has the current Combatant List (complete with rolls, modifiers, etc)
        returnIntent.putExtra(ConfigureCombatantListActivity.ROUND_NUMBER, adapter.getRoundNumber()); // Add the round number to the intent
        returnIntent.putExtra(ConfigureCombatantListActivity.MAX_ROUND_ROLLED, adapter.getMaxRoundRolled()); // Add the maximum round rolled to the intent
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}