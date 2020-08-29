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

        boolean isBegin = true;
        if (thisBundleData != null) {
            // Get the combatant lists
            if (thisBundleData.containsKey(COMBATANT_LIST_LIST)) {
                // TODO: Set up interaction between this and Encounter Activity (passing combatant list back and forth [unless we never really need the list back from the Encounter Activity...?] )
                combatantLists = (AllFactionCombatantLists) thisBundleData.getSerializable(COMBATANT_LIST_LIST);
            }

            // Get the "purpose" of this activity (beginning or middle of combat)
            if (thisBundleData.containsKey(COMBAT_BEGIN)) {
                isBegin = thisBundleData.getBoolean(COMBAT_BEGIN);
            }
        }

        TextView mainButton = findViewById(R.id.finish_button);
        if (isBegin) {
            mainButton.setText(R.string.begin_encounter);
        } else {
            mainButton.setText(R.string.resume_encounter);
        }

        // TODO: Set main button funciton: Start an Encounter Activity and pass the combatantList!

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
        LinearLayout combatantGroupParent = findViewById(R.id.combatant_fragment_parent);

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
        }

        // TODO SOON: Replace all of this with a single RecyclerView with a ListCombatantRecyclerAdapter

        // Create a fragment for each faction, if we need to
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = null; // We will use this if we actually need to add a fragment

        ArrayList<FactionCombatantList> factionCombatantLists = combatantLists.getAllFactionLists(); // The list of FactionCombatantLists will now be in the order that they should be displayed
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
                // If we must add a fragment to display this faction

                // Create a recyclerAdapter for each faction's recyclerview (done here so that item click handling will be simpler
                ListCombatantRecyclerAdapter adapter = new ListCombatantRecyclerAdapter(this, getApplicationContext(), factionList, true, true);

                // Create a new container view to add to the LinearLayout
                FrameLayout thisFragmentContainer = new FrameLayout(getApplicationContext());
                thisFragmentContainer.setId(facInd + 1000); // Set some id for the FrameLayout
                combatantGroupParent.addView(thisFragmentContainer, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

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
                combatantGroupParent.removeView(factionFragmentMap.get(factionList.faction()).getContainer());

                // Remove the fragment from the View
                fragTransaction.remove(factionFragmentMap.get(factionList.faction()).getFragment());

                // Delete the key from the factionFragmentMap (it is no longer being displayed)
                factionFragmentMap.remove(factionList.faction());

                continue;
            }

            if (factionFragmentMap.containsKey(factionList.faction())) {
                // Let the adapter know that the Combatants list *may* have changed, if we are neither adding a Fragment (nothing changed because we just initialized it) nor removing one (it no longer exists, so doesn't need to update)
                factionFragmentMap.get(factionList.faction()).getFragment().getAdapter().notifyCombatantListChanged();

                // Remove and re-add the Fragment view, so that the views are all in order, if we are neither adding a Fragment (already added this loop) nor removing a Fragment (don't need to worry about it anymore)
                combatantGroupParent.removeView(factionFragmentMap.get(factionList.faction()).getContainer());
                combatantGroupParent.addView(factionFragmentMap.get(factionList.faction()).getContainer());
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

    // Interface methods

    @Override
    public void receiveChosenCombatant(Combatant selectedCombatant) {
        // Receive a combatant from a ListCombatantRecyclerAdapter
        // Do nothing, because this is "receiving" a combatant from the configure list, which shouldn't do anything on being touched
    }

    @Override
    public void removeCombatant(Combatant combatantToRemove) {
        // Remove the Combatant indicated
        combatantLists.remove(combatantToRemove);

        // A Combatant in one of the Fragments was just removed.  Update the list, in case one of the Fragments needs to be removed
        updateFactionFragmentDisplay();
    }

    @Override
    public void addCombatant(Combatant combatantToAdd) {
        // Add the Combatant indicated
        combatantLists.addCombatant(combatantToAdd);

        // Update the Fragment displace
        updateFactionFragmentDisplay();
    }

    @Override
    public AllFactionCombatantLists getMasterCombatantList() {
        // Give the ListCombatantRecyclerAdapter a copy of the master combatant list, so that it can guarantee name uniqueness
        return combatantLists;
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
