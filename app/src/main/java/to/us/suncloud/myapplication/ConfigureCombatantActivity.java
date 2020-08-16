package to.us.suncloud.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class ConfigureCombatantActivity extends AppCompatActivity {

    public static final String combatantListList = "COMBATANT_LIST_LIST"; // ID for inputs to the activity
    public static final String combatBegin = "COMBAT_BEGIN"; // Is this activity used for the beginning of combat, or in the middle of combat?

    TextView noCombatantMessage; // Text message to show the use when there are no combatants

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get info from the intent that started this activity
        Intent thisIntent = getIntent();
        Bundle thisBundleData = thisIntent.getExtras();

        // Get the combatant lists
        ArrayList<FactionCombatantList> combatantFractionLists = null;
        if (thisBundleData.containsKey(combatantListList)) {
            combatantFractionLists = (ArrayList<FactionCombatantList>) thisBundleData.getSerializable(combatantListList);
        }

        // Get the "purpose" of this activity (beginning or middle of combat)
        TextView mainButton = findViewById(R.id.finish_button);
        boolean isBegin = true;
        if (thisBundleData.containsKey(combatBegin)) {
            isBegin = thisBundleData.getBoolean(combatBegin);
        }

        if (isBegin) {
            mainButton.setText(R.string.begin_encounter);
        } else {
            mainButton.setText(R.string.resume_encounter);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // TODO: Add toolbar buttons

        // TODO: USE FRAGMENTS (perhaps should also change how the configure_combatants_content.xml is laid, use fragments for segment so that more factions can be added later...?
        // TODO: Convert this activity so that the MAIN view is the "randomized" combat view, but the initial view is all combatants separated by faction

        // First, get the parent for all of the combatant fragments
        ConstraintLayout combatantFragmentParentLayout = findViewById(R.id.combatant_fragment_parent);

        noCombatantMessage = findViewById(R.id.configure_combatant_empty);

        // For each faction that has combatants in it, create a new fragment and populate it

        if (combatantFractionLists == null) {
            // If there are no combatants, display the no combatant message
            noCombatantMessage.setVisibility(View.VISIBLE);
        } else {
            // If there are combatants, don't display the no combatant message
            noCombatantMessage.setVisibility(View.GONE);

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction fragTransaction = fm.beginTransaction();

            for (int factionInd = 0; factionInd < combatantFractionLists.size(); factionInd++) {
                CombatantGroupFragment newFrag = new CombatantGroupFragment(combatantFractionLists.get(factionInd));
                fragTransaction.add(noCombatantMessage.getId(), newFrag, Combatant.factionToString(combatantFractionLists.get(factionInd).getThisFaction()) + "_configure_fragment");
            }
        }

    }

    // Related to the toolbar

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.settings:
                // TODO: Open the settings menu
                return true;
            case R.id.add_combatant:
                // TODO: Open the add combatant menu
                return true;
            default:
                return false;
        }
    }
}
