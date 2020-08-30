package to.us.suncloud.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

public class EncounterActivity extends AppCompatActivity {
    AllFactionCombatantLists masterCombatantList;
    EncounterCombatantRecyclerAdapter adapter; // The adapter for the main Combatant list

    int roundNumber;

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

        // Get the Combatant List
        if (thisBundleData.containsKey(ConfigureCombatantListActivity.COMBATANT_LIST)) {
            AllFactionCombatantLists newList = (AllFactionCombatantLists) thisBundleData.getSerializable(ConfigureCombatantListActivity.COMBATANT_LIST);
            if (newList != null) {
                masterCombatantList = newList;
            }
        } else {
            masterCombatantList = new AllFactionCombatantLists();
        }

        // Get the current round number
        if (thisBundleData.containsKey(ConfigureCombatantListActivity.ROUND_NUMBER)) {
            roundNumber = thisBundleData.getInt(ConfigureCombatantListActivity.ROUND_NUMBER);
        } else {
            roundNumber = 1;
        }

        // TODO: Get saved Instance state data?  Probably happens if we go back to the Configure screen and then come back here without destroying the Activity.  Learn how to use backstacks!!!! :P

        // Retrieve all Views
        encounterToolbar = findViewById(R.id.encounter_toolbar);
        encounterRecyclerView = findViewById(R.id.encounter_recycler_view);
        noCombatantTextView = findViewById(R.id.encounter_combatant_empty);
        nextButton = findViewById(R.id.encounter_continue);
        previousButton = findViewById(R.id.encounter_previous_combatant);
        roundCounter = findViewById(R.id.encounter_round_counter);

        // Set up the RecyclerView
        adapter = new EncounterCombatantRecyclerAdapter(getApplicationContext(), masterCombatantList);
        adapter.setRoundNumber(roundNumber);
        encounterRecyclerView.setAdapter(adapter);
        encounterRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.iterateCombatStep();
                // If there is a currently active combatant that is NOT the last Combatant, then we are in the middle of combat
                if (adapter.getCurActiveCombatant() == 0) {
                    // If we just started the round, then emphasize the Roll Initiative button (for fun)!
                    nextButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.emphize_wobble));
                    // TODO LATER: Will probably need to make sure that text change (below) only happens AFTER the animation finishes, otherwise it won't make much sense!
                }
                updateGUIState();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.reverseCombatStep();
                updateGUIState();
            }
        });

        // Setup various GUI elements
        updateNoCombatantView(); // The no combatant message
        updateGUIState(); // The next/previous buttons

        // Setup the toolbar
        setSupportActionBar(encounterToolbar);
    }

    private void updateGUIState() {
        // First, prepare parameters for everything that may change in the GUI
        int nextButtonText;
        int nextButtonVisibility = View.VISIBLE; // The only time this would become invisible is if there were no Combatants
        int previousButtonVisibility = View.VISIBLE; // The only time this would become invisible is if we are at the Roll Initiative stage of Round 1

        // Next, get the current active combatant and current round
        int currentlyActiveCombatant = adapter.getCurActiveCombatant();
        int currentRoundNumber = adapter.getCurRoundNumber();

        // Change GUI elements based on the current state
        if (adapter.getCombatantList().size() == 0) {
            // If there aren't any Combatants, then make the next button Gone
            nextButtonVisibility = View.GONE;
        } else {
            nextButtonVisibility = View.VISIBLE;
        }

        if (currentlyActiveCombatant == EncounterCombatantRecyclerAdapter.UNSET) {
            // If the currently active combatant is unset, then we are currently between rounds, waiting to roll initiative
            nextButtonText = R.string.encounter_roll_initiative;
            if (currentRoundNumber == 1) {
                // If this is the 0th round prepare phase, then don't display the previous button
                previousButtonVisibility = View.GONE;
            }
        } else if (currentlyActiveCombatant == (adapter.getItemCount() - 1)) {
            // If the currently active combatant is the final Combatant, then the next action will be to prepare for the next round of combat
            nextButtonText = R.string.encounter_prepare_next_round;
        } else {
            nextButtonText = R.string.encounter_next_combatant;
        }

        // Update the text on the nextButton
        nextButton.setText(nextButtonText);
        nextButton.setVisibility(nextButtonVisibility);

        // Make sure the previous button is visible
        previousButton.setVisibility(previousButtonVisibility);

        // Update the number on the round counter, from the adapter
        roundNumber = currentRoundNumber;
        roundCounter.setText(String.valueOf(roundNumber));
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
            case R.id.settings:
                // Open the settings menu

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(ConfigureCombatantListActivity.COMBATANT_LIST, adapter.getCombatantList()); // Create an Intent that has the current Combatant List (complete with rolls, speed factors, etc)
        returnIntent.putExtra(ConfigureCombatantListActivity.ROUND_NUMBER, roundNumber); // Create an Intent that has the current Combatant List (complete with rolls, speed factors, etc)
        setResult(RESULT_OK, returnIntent);
        super.onBackPressed();
    }
}