package to.us.suncloud.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class ConfigureCombatantListActivity extends AppCompatActivity implements ListCombatantRecyclerAdapter.MasterCombatantKeeper, ViewSavedCombatantsFragment.ReceiveAddedCombatant {

    public static final String COMBATANT_LIST_LIST = "COMBATANT_LIST_LIST"; // ID for inputs to the activity
    public static final String COMBAT_BEGIN = "COMBAT_BEGIN"; // Is this activity used for the beginning of combat, or in the middle of combat?

    TextView noCombatantMessage; // Text message to show the use when there are no combatants

    HashMap<Combatant.Faction, FactionFragmentInfo> factionFragmentMap = new HashMap<>();

    AllFactionCombatantLists combatantLists = new AllFactionCombatantLists();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get info from the intent that started this activity
        Intent thisIntent = getIntent();
        Bundle thisBundleData = thisIntent.getExtras();

        // Get the combatant lists
        if (thisBundleData.containsKey(COMBATANT_LIST_LIST)) {
            combatantLists = (AllFactionCombatantLists) thisBundleData.getSerializable(COMBATANT_LIST_LIST);
        }

        // Get the "purpose" of this activity (beginning or middle of combat)
        TextView mainButton = findViewById(R.id.finish_button);
        boolean isBegin = true;
        if (thisBundleData.containsKey(COMBAT_BEGIN)) {
            isBegin = thisBundleData.getBoolean(COMBAT_BEGIN);
        }

        if (isBegin) {
            mainButton.setText(R.string.begin_encounter);
        } else {
            mainButton.setText(R.string.resume_encounter);
        }

        Toolbar toolbar = findViewById(R.id.configure_toolbar);
        setSupportActionBar(toolbar); // TODO: Add toolbar buttons (Settings, like which DnD version is being used?)
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateFactionFragmentDisplay();
    }

    void updateFactionFragmentDisplay() {
        // First, get the parent for all of the combatant fragments
        LinearLayout combatantFragmentParentLayout = findViewById(R.id.combatant_fragment_parent);

        noCombatantMessage = findViewById(R.id.configure_combatant_empty);

        // (No longer true with added map functionality) TO_DO: At this point, I THINK the only view in combatantFragmentParentLayout is the noCombatantMessage...if not, then need to find a way to clear all but that view (for loop is out, because the layout will be modified during the loop, throwing off the numbers, unless you compensate for it...)

        boolean haveCombatants = false;
        if (combatantLists != null) {
            // If there are factions, check if any of them have combatants.  Otherwise, display the no combatant message
            ArrayList<FactionCombatantList> factionCombatantLists = combatantLists.getAllFactionLists();
            for (int factionInd = 0; factionInd < factionCombatantLists.size(); factionInd++) {
                if (factionCombatantLists.get(factionInd).size() != 0) {
                    haveCombatants = true;
                    break;
                }
            }
        }

        if (haveCombatants) {
            // If there are combatants, don't display the message
            noCombatantMessage.setVisibility(View.GONE);
        } else {
            // If there are no combatants, display the message
            noCombatantMessage.setVisibility(View.VISIBLE);

            // Then, skip the rest of this method (no factions to display)
            return;
        }

        // Create a fragment for each faction, if we need to
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = null; // We will use this if we actually need to add a fragment

        ArrayList<FactionCombatantList> factionCombatantLists = combatantLists.getAllFactionLists();
        for (int facInd = 0; facInd < factionCombatantLists.size(); facInd++) {
            FactionCombatantList factionList = factionCombatantLists.get(facInd);

            // Check the two cases in which we will need to do a fragment transaction
            boolean mustAdd = !factionList.isEmpty() && !factionFragmentMap.containsKey(factionList.faction());  // We must add a fragment if the list is not empty, but there is no fragment for this faction
            boolean mustRemove = factionList.isEmpty() && factionFragmentMap.containsKey(factionList.faction());  // We must remove a fragment if the list is empty, but there is a fragment for this faction

            // First, see if we've already started a fragment transaction
            if ((mustAdd || mustRemove) && fragTransaction == null) {
                // If we need to do something but we haven't started a transaction yet, then start one
                fragTransaction = fm.beginTransaction();
            }

            if (mustAdd) {
                // If we must display this faction

                // Create a recyclerAdapter for each faction's recyclerview (done here so that item click handling will be simpler
                ListCombatantRecyclerAdapter adapter = new ListCombatantRecyclerAdapter(this, getApplicationContext(), factionList, true, true);

                // Create a new container view to add to the LinearLayout
                FrameLayout thisFragmentContainer = new FrameLayout(getApplicationContext());
                combatantFragmentParentLayout.addView(thisFragmentContainer, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                // Create a new fragment, and place it in the container
                CombatantGroupFragment newFrag = new CombatantGroupFragment(adapter, factionList.faction());
                fragTransaction.add(thisFragmentContainer.getId(), newFrag, Combatant.factionToString(factionCombatantLists.get(facInd).faction()) + "_configure_fragment");

                // Add the fragment to the map, so we can refer to it later
                factionFragmentMap.put(factionList.faction(), new FactionFragmentInfo(newFrag, thisFragmentContainer));

                continue;
            }

            if (mustRemove) {
                // If we must remove this faction
                // Delete the view in which the faction fragment is contained
                combatantFragmentParentLayout.removeView(factionFragmentMap.get(factionList.faction()).getContainer());

                // Remove the fragment
                fragTransaction.remove(factionFragmentMap.get(factionList.faction()).getFragment());
            }
        }

        if (fragTransaction != null) {
            // If we have started a fragment transaction in the above loop, commit it
            fragTransaction.commit();
        }
    }

    static class FactionFragmentInfo {
        public CombatantGroupFragment thisFrag;
        public FrameLayout thisContainer;

        FactionFragmentInfo(CombatantGroupFragment thisFrag, FrameLayout thisContainer) {
            this.thisFrag = thisFrag;
            this.thisContainer = thisContainer;
        }

        public CombatantGroupFragment getFragment() {
            return thisFrag;
        }

        public FrameLayout getContainer() {
            return thisContainer;
        }
    }

    @Override
    public void receiveChosenCombatant(Combatant selectedCombatant) {
        // Receive a combatant from a ListCombatantRecyclerAdapter
        // Do nothing, because this implementation of the
        // TODO CHECK: Do nothing, probably? (I don't think a Combatant can be modified from this list, right?)
    }

    @Override
    public AllFactionCombatantLists getMasterCombatantList() {
        // Give the ListCombatantRecyclerAdapter a copy of the master combatant list, so that it can guarantee name uniqueness
        return combatantLists;
    }

    @Override
    public void combatantInListRemoved() {
        // A Combatant in one of the Fragments was just removed.  Update the list, in case one of the Fragments needs to be removed
        updateFactionFragmentDisplay();
    }

    @Override
    public void receiveAddedCombatant(Combatant addedCombatant) {
        // Receive a new Combatant from the AddCombatantToList dialog
        combatantLists.addCombatant(addedCombatant); // TODO CHECK: Adding the new Combatant to this list SHOULD update it in the fragments (the fragments have a reference to the very ArrayList that is being held by this AllFactionCombatantLists)

        // Create/destroy/update any faction fragments as needed
        updateFactionFragmentDisplay();
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
                // TODO: Open the settings menu
                return true;
            case R.id.open_bookmarks:
                // Open the bookmarked Combatants menu
                FragmentManager fm_ob = getSupportFragmentManager();
                ViewSavedCombatantsFragment viewBookmarksFrag = ViewSavedCombatantsFragment.newModifySavedCombatantListInstance();
                viewBookmarksFrag.show(fm_ob, "Open Bookmarks");
                return true;
            case R.id.add_combatant:
                // Open the add combatant menu
                FragmentManager fm_ac = getSupportFragmentManager();
                ViewSavedCombatantsFragment addCombatantFrag = ViewSavedCombatantsFragment.newAddCombatantToListInstance(this, combatantLists);
                addCombatantFrag.show(fm_ac, "AddCombatantToList");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
