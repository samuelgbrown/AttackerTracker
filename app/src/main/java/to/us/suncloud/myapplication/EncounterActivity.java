package to.us.suncloud.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

public class EncounterActivity extends AppCompatActivity implements EncounterCombatantRecyclerAdapter.combatProgressInterface {
    EncounterCombatantList masterCombatantList;
    EncounterCombatantRecyclerAdapter adapter; // The adapter for the main Combatant list

    int roundNumber = 1;
    int maxRoundRolled = 0;

    ConstraintLayout prepBanner;
    TextView prepMessage;
    RecyclerView encounterRecyclerView;
    Toolbar encounterToolbar;
    TextView noCombatantTextView;
    Button nextButton;
    ImageButton previousButton;
    TextView roundCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encounter);

        // Get info from the intent that started this activity
        Intent intent = getIntent();
        Bundle thisBundleData = intent.getExtras();

        masterCombatantList = new EncounterCombatantList(getApplicationContext());

        if (savedInstanceState != null) {
            // If there is a saved state, then there was probably just a configuration change, so savedInstanceState is the most up-to-date information we have
            // Get the current round number
            if (savedInstanceState.containsKey(ConfigureCombatantListActivity.ROUND_NUMBER)) {
                roundNumber = savedInstanceState.getInt(ConfigureCombatantListActivity.ROUND_NUMBER, 0);
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

        // TODO: Get saved Instance state data?  Probably happens if we go back to the Configure screen and then come back here without destroying the Activity.  Learn how to use backstacks!!!! :P

        // Retrieve all Views
        encounterToolbar = findViewById(R.id.encounter_toolbar);
        encounterRecyclerView = findViewById(R.id.encounter_recycler_view);
        prepBanner = findViewById(R.id.encounter_prepare_banner);
        prepMessage = findViewById(R.id.encounter_prepare_message);
        noCombatantTextView = findViewById(R.id.encounter_combatant_empty);
        nextButton = findViewById(R.id.encounter_continue);
        previousButton = findViewById(R.id.encounter_previous_combatant);
        roundCounter = findViewById(R.id.encounter_round_counter);

        // Set up the RecyclerView
        adapter = new EncounterCombatantRecyclerAdapter(this, masterCombatantList);
        encounterRecyclerView.setAdapter(adapter);
        encounterRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        // Initialize the adapter as needed
        adapter.setRoundNumber(roundNumber);
        adapter.updateCombatProgress();
        adapter.notifyCombatantsChanged(); // Let the adapter know that this is the initial state (update all "memory" parameters for the combatant list and the currently active Combatant)

        // Set up the banner message
        prepMessage.setText(getString(R.string.prepare_message, InitPrefsHelper.getModString(getApplicationContext())));

        // Set up the buttons
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If an EditText is focused, confirm the choice by stealing the focus away
                encounterToolbar.requestFocus();

                // TODO START HERE: Big visual bug: If modifying a Combatant's modifier and hit roll initiative while edit text is still focused, that Combatant's view is not properly updated
                //  Possible solution, get rid of any onfocuschange logic?  Rely on user to hit "Done" to update values?

                // Hide the keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(nextButton.getWindowToken(), 0);

                adapter.incrementCombatStep();
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

                adapter.decrementCombatStep();
            }
        });

        // Setup various GUI elements
        updateNoCombatantView(); // The no combatant message
        updateGUIState(); // The next/previous buttons

        // Setup the toolbar
        setSupportActionBar(encounterToolbar);
        getSupportActionBar().setTitle(R.string.encounter_title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Make sure the EncounterCombatantList has the most up-to-date preference information
        // TODO START HERE: Reset the Combat round if the new preferences have a different dice value.  Set all to be selected? Set the round number of Activity?  Do stuff!
        adapter.updatePrefs(getApplicationContext());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save a few useful things (don't let Google see this, they'll get pissed off at how much data I'm saving in outState...whoops...)
        outState.putInt(ConfigureCombatantListActivity.ROUND_NUMBER, roundNumber);
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

        // Next, get the current active combatant and current round
//        int currentlyActiveCombatant = adapter.getCurActiveCombatant();
        int currentlyActiveCombatant = masterCombatantList.calcActiveCombatant();
        roundNumber = adapter.getCurRoundNumber();

        // Change GUI elements based on the current state
        if (adapter.getCombatantList().size() == 0) {
            // If there aren't any Combatants, then make the next button Gone
            nextButtonVisibility = View.GONE;
        }

        if (currentlyActiveCombatant == EncounterCombatantRecyclerAdapter.PREP_PHASE) {
            // If the currently active combatant is unset, then we are currently between rounds, waiting to roll initiative
            nextButtonText = R.string.encounter_roll_initiative;
            prepBannerVisibility = View.VISIBLE;
            if (roundNumber == 1) {
                // If this is the 1st round prepare phase, then don't display the previous button
                previousButtonVisibility = View.GONE;
            }
        } else {
            // Check if we just moved into Combat
            // TODO: Take care of this!  Put all of the changing stuff in a postDelayed thread IF there is an animation (use doAnim above)
            if (currentlyActiveCombatant == 0 && roundNumber > maxRoundRolled && nextButton.getText().equals(getResources().getString(R.string.encounter_roll_initiative))) {
                // If we just started the round, then emphasize the Roll Initiative button (for fun)!
                doAnim = true;
                maxRoundRolled = roundNumber; // Remember that we already did the fun animation for this round
                nextButton.setEnabled(false); // Disable the button momentarily while the animation plays out
                nextButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.emphize_wobble));
                // TODO LATER: Will probably need to make sure that text change (below) only happens AFTER the animation finishes, otherwise it won't make much sense!
            }

            // The next button's test depends on what happens if the currently selected Combatant gets checked off
            EncounterCombatantList testList = masterCombatantList.clone();
            testList.get(currentlyActiveCombatant).setSelected(true); // Simulate the user checking off the current Combatant
            int nextActiveCombatant = testList.calcActiveCombatant();

            // Based on what the next step of the combat cycle is, set up the next button text
            if (nextActiveCombatant == EncounterCombatantRecyclerAdapter.PREP_PHASE) {
                // If the currently active combatant is the final Combatant before the prep phase, then the next action will be to prepare for the next round of combat
                nextButtonText = R.string.encounter_prepare_next_round;
            } else {
                nextButtonText = R.string.encounter_next_combatant;
            }
        }


        int delay = doAnim ? 500 : 0;
        prepBanner.setVisibility(prepBannerVisibility);

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
        // Update the visibility of the no combatant view message
        if (masterCombatantList.isEmpty()) {
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO: Fill in option menu items!!!
        int id = item.getItemId();
        switch (id) {
            case R.id.sort_alpha:
                // Sort the Combatants alphabetically by faction
                adapter.sort(EncounterCombatantList.SortMethod.ALPHABETICALLY_BY_FACTION);
                return true;
            case R.id.sort_initiative:
                // (Attempt to) sort the Combatants by initiative
                adapter.sort(EncounterCombatantList.SortMethod.INITIATIVE);
                return true;
            case R.id.dice_cheat:
                // Toggle the dice cheat mode on and off
                adapter.toggleDiceCheat();
                return true;
            case R.id.settings:
                // Open the settings menu
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                settingsIntent.putExtra(SettingsFragment.IS_MID_COMBAT, masterCombatantList.isMidCombat()); // Let the Settings Activity know if we are mid-combat
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void onBackPressed() {
        // TODO SOON: Preserve value of curActiveCombatant!  Allow user to add Combatants mid-round.  Will need to roll initiative on new Combatant if they come in mid-round (if curActiveCombatant != 0...or just do it every time...?)
        Intent returnIntent = new Intent();
        returnIntent.putExtra(ConfigureCombatantListActivity.COMBATANT_LIST, adapter.getCombatantList()); // Create an Intent that has the current Combatant List (complete with rolls, modifiers, etc)
        returnIntent.putExtra(ConfigureCombatantListActivity.ROUND_NUMBER, roundNumber); // Add the round number to the intent
//        returnIntent.putExtra(ConfigureCombatantListActivity.ACTIVE_COMBATANT_NUMBER, adapter.getCurActiveCombatant()); // Add the currently active Combatant to the intent
        setResult(RESULT_OK, returnIntent);
        super.onBackPressed();
    }
}