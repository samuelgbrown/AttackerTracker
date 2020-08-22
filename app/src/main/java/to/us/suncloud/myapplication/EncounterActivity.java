package to.us.suncloud.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toolbar;

public class EncounterActivity extends AppCompatActivity {
    private static final String COMBATANT_LIST = "combatant_list";

    AllFactionCombatantLists masterCombatantList;
    EncounterCombatantRecyclerAdapter adapter; // The adapter for the main Combatant list

    int roundNumber = 0;

    // TODO: Add "cheater mode" so you can modify dice rolls

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

        if (thisBundleData.containsKey(COMBATANT_LIST)) {
            masterCombatantList = (AllFactionCombatantLists) thisBundleData.getSerializable(COMBATANT_LIST);
        } else {
            masterCombatantList = new AllFactionCombatantLists();
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
        adapter = new EncounterCombatantRecyclerAdapter(masterCombatantList);
        encounterRecyclerView.setAdapter(adapter);
        encounterRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.iterateCombatStep();
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
    }

    private void updateGUIState() {
        // First, prepare parameters for everything that may change in the GUI
        int nextButtonText;
        int previousButtonVisibility = View.VISIBLE; // The only time this would become invisible is if we are at the Roll Initiative stage of Round 1

        // Next, get the current active combatant and current round
        int currentlyActiveCombatant = adapter.getCurActiveCombatant();
        int currentRoundNumber = adapter.getCurRoundNumber();

        // Change GUI elements based on the current state
        if (currentlyActiveCombatant == EncounterCombatantRecyclerAdapter.UNSET) {
            // If the currently active combatant is unset, then we are currently between rounds, waiting to roll initiative
            nextButtonText = R.string.encounter_roll_initiative;
        } else if (currentlyActiveCombatant == (adapter.getItemCount() - 1)) {
            // If the currently active combatant is the final Combatant, then the next action will be to prepare for the next round of combat
            nextButtonText = R.string.encounter_prepare_next_round;
            if (currentRoundNumber == 1) {
                previousButtonVisibility = View.GONE;
            }
        } else {
            // If there is a currently active combatant that is NOT the last Combatant, then we are in the middle of combat
            if (currentlyActiveCombatant == 0) {
                // If we just started the round, then emphasize the Roll Initiative button (for fun)!
                nextButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.emphize_wobble));
                // TODO LATER: Will probably need to make sure that text change (below) only happens AFTER the animation finishes, otherwise it won't make much sense!
            }
            nextButtonText = R.string.encounter_next_combatant;
        }

        // Update the text on the nextButton
        nextButton.setText(nextButtonText);

        // Make sure the previous button is visible
        previousButton.setVisibility(previousButtonVisibility);

        // Update the number on the round counter, from the adapter
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
}