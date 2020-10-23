package to.us.suncloud.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class ConfigureCombatantListActivity extends AppCompatActivity implements ListCombatantRecyclerAdapter.MasterCombatantKeeper, ViewSavedCombatantsFragment.ReceiveAddedCombatant {

    public static final String COMBATANT_LIST = "combatantListList"; // ID for inputs to the activity
    public static final String ROUND_NUMBER = "roundNumber"; // ID for inputs to the activity
    public static final String MAX_ROUND_ROLLED = "maxRoundRolled"; // ID for inputs to the activity
    //    public static final String ACTIVE_COMBATANT_NUMBER = "activeCombatantNumber"; // ID for inputs to the activity
    public static final String ENCOUNTER_DATA = "encounterData"; // ID for the encounter list data (dice rolls, etc) sorted as a EncounterCombatantList (really just for savedStateInstance, we transfer the Combatant list data with the Encounter Activity using a single, finalized EncounterCombatantList)

    public static final int COMBATANT_LIST_CODE = 0; // The Code to represent requesting a Combatant List back from the Encounter Activity, used in startActivityForResult()

    int curTheme; // The current theme of this Activity

    TextView mainButton; // The Main button at the bottom, to transfer over to the Encounter Activity
    TextView noCombatantMessage; // Text message to show the use when there are no Combatants
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
                        adapter.setCombatantList(new AllFactionCombatantLists(newCombatantList)); // Get a Combatant list back from the Encounter
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

        // Create a fragment for each faction, if we need to
//        FragmentManager fm = getSupportFragmentManager();
//        FragmentTransaction fragTransaction = null; // We will use this if we actually need to add a fragment
//
//        ArrayList<FactionCombatantList> factionCombatantLists = combatantLists.getAllFactionLists(); // The list of FactionCombatantLists will now be in the order that they should be displayed
//        for (int facInd = 0; facInd < factionCombatantLists.size(); facInd++) {
//            FactionCombatantList factionList = factionCombatantLists.get(facInd);
//
//            // Check the two cases in which we will need to do a fragment transaction
//            boolean mustAdd = !factionList.isEmpty() && !factionFragmentMap.containsKey(factionList.faction());  // We must add a fragment if the list is not empty, but there is no fragment for this faction
//            boolean mustRemove = factionList.isEmpty() && factionFragmentMap.containsKey(factionList.faction());  // We must remove a fragment if the list is empty, but there is a fragment for this faction
//
//            // First, see if we've already started a fragment transaction
//            if ((mustAdd || mustRemove) && fragTransaction == null) {
//                // If we need to do something but we haven't started a transaction yet, then start one
//                fragTransaction = fm.beginTransaction();
//            }
//
//            if (mustAdd) {
//                // If we must add a fragment to display this faction
//
//                // Create a recyclerAdapter for each faction's recyclerview (done here so that item click handling will be simpler
//                ListCombatantRecyclerAdapter adapter = new ListCombatantRecyclerAdapter(this, getApplicationContext(), factionList, true, true);
//
//                // Create a new container view to add to the LinearLayout
//                FrameLayout thisFragmentContainer = new FrameLayout(getApplicationContext());
//                thisFragmentContainer.setId(facInd + 1000); // Set some id for the FrameLayout
//                combatantGroupParent.addView(thisFragmentContainer, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//
//                // Create a new fragment, and place it in the container
//                CombatantGroupFragment newFrag = new CombatantGroupFragment(adapter, factionList.faction());
//                fragTransaction.add(thisFragmentContainer.getId(), newFrag, Combatant.factionToString(factionCombatantLists.get(facInd).faction()) + "_configure_fragment");
//
//                // Add the fragment to the map, so we can refer to it later
//                factionFragmentMap.put(factionList.faction(), new FactionFragmentInfo(newFrag, thisFragmentContainer));
//
//                continue;
//            }
//
//            if (mustRemove) {
//                // If we must remove this faction
//                // Delete the view in which the faction fragment is contained
//                combatantGroupParent.removeView(factionFragmentMap.get(factionList.faction()).getContainer());
//
//                // Remove the fragment from the View
//                fragTransaction.remove(factionFragmentMap.get(factionList.faction()).getFragment());
//
//                // Delete the key from the factionFragmentMap (it is no longer being displayed)
//                factionFragmentMap.remove(factionList.faction());
//
//                continue;
//            }
//
//            if (factionFragmentMap.containsKey(factionList.faction())) {
//                // Let the adapter know that the Combatants list *may* have changed, if we are neither adding a Fragment (nothing changed because we just initialized it) nor removing one (it no longer exists, so doesn't need to update)
//                factionFragmentMap.get(factionList.faction()).getFragment().getAdapter().notifyCombatantListChanged();
//
//                // Remove and re-add the Fragment view, so that the views are all in order, if we are neither adding a Fragment (already added this loop) nor removing a Fragment (don't need to worry about it anymore)
//                combatantGroupParent.removeView(factionFragmentMap.get(factionList.faction()).getContainer());
//                combatantGroupParent.addView(factionFragmentMap.get(factionList.faction()).getContainer());
//            }
//        }
//
//        if (fragTransaction != null) {
//            // If we have started a fragment transaction in the above loop, commit it
//            fragTransaction.commit();
//        }
    }

//    static class FactionFragmentInfo {
//        public CombatantGroupFragment thisFrag;
//        public FrameLayout thisContainer;
//
//        FactionFragmentInfo(CombatantGroupFragment thisFrag, FrameLayout thisContainer) {
//            this.thisFrag = thisFrag;
//            this.thisContainer = thisContainer;
//        }
//
//        public CombatantGroupFragment getFragment() {
//            return thisFrag;
//        }
//
//        public FrameLayout getContainer() {
//            return thisContainer;
//        }
//    }

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
                ViewSavedCombatantsFragment addCombatantFrag = ViewSavedCombatantsFragment.newAddCombatantToListInstance(this, adapter.getCombatantList());
                addCombatantFrag.show(fm_ac, "AddCombatantToList");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
