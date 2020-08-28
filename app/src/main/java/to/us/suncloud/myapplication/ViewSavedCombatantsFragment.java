package to.us.suncloud.myapplication;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ViewSavedCombatantsFragment#newAddCombatantToListInstance} factory method to
 * create an instance of this fragment.
 */

public class ViewSavedCombatantsFragment extends DialogFragment implements ListCombatantRecyclerAdapter.MasterCombatantKeeper, CreateOrModCombatant.receiveNewOrModCombatantInterface {
    private static final String TAG = "ViewSavedCombatants";
    // The fragment initialization parameters
    private static final String CURRENT_COMBATANT_LIST = "currentCombatantList";
    private static final String COMBATANT_DESTINATION = "combatantDestination";
    private static final String EXPECTING_RETURNED_COMBATANT = "expectingReturnedCombatant";

    private static final String combatantListSaveFile = "combatantListSaveFile";

    HashMap<Combatant.Faction, FactionFragmentInfo> factionFragmentMap = new HashMap<>();

    private AllFactionCombatantLists eligibleCombatantsList = null; // An list of Combatants that appear in the savedCombatantsList plus any Combatants that have been added
    private AllFactionCombatantLists savedCombatantsList = null; // An exact copy of the Combatant list from the saved file
    private AllFactionCombatantLists currentFactionCombatantList = null;

    private boolean expectingReturnedCombatant = false;

    private ReceiveAddedCombatant combatantDestination = null; // The Activity/Fragment that will receive the selected Combatant

    private LinearLayout combatantGroupParent;
    private TextView emptyCombatants;
    private SearchView searchView;
    private Button addNewCombatant;
    private ImageButton closeButton;
    private TextView title;

    public ViewSavedCombatantsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AddCombatantFragment.
     */
    public static ViewSavedCombatantsFragment newAddCombatantToListInstance(ReceiveAddedCombatant combatantDestination, AllFactionCombatantLists currentFactionCombatantList) {
        ViewSavedCombatantsFragment fragment = new ViewSavedCombatantsFragment();
        Bundle args = new Bundle();
        args.putSerializable(CURRENT_COMBATANT_LIST, currentFactionCombatantList);
        args.putSerializable(COMBATANT_DESTINATION, combatantDestination);
        args.putBoolean(EXPECTING_RETURNED_COMBATANT, true); // If an AddCombatant Fragment is requested, then they are expecting a Combatant to be returned to the calling Activity/Fragment
        fragment.setArguments(args);
        return fragment;
    }

    public static ViewSavedCombatantsFragment newModifySavedCombatantListInstance() {
        ViewSavedCombatantsFragment fragment = new ViewSavedCombatantsFragment();
        Bundle args = new Bundle();
        args.putBoolean(EXPECTING_RETURNED_COMBATANT, false); // If a ViewSavedCombatants Fragment is requested, then they are NOT expecting a Combatant; this is just for viewing/modifying the list
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load in combatants from file (process them later)
        savedCombatantsList = (AllFactionCombatantLists) LocalPersistence.readObjectFromFile(getContext(), combatantListSaveFile);
        if (savedCombatantsList == null) {
            // If there aren't any previously saved Combatants (not even a blank AllFactionCombatantList), then make a new empty list to represent them
            savedCombatantsList = new AllFactionCombatantLists();
        }
        eligibleCombatantsList = savedCombatantsList.clone();

        if (getArguments() != null) {
            // First, see which mode we're in
            expectingReturnedCombatant = getArguments().getBoolean(EXPECTING_RETURNED_COMBATANT);

            if (expectingReturnedCombatant) {
                // If this Fragment is intended to add a new Combatant to the Encounter...
                if (getArguments().containsKey(CURRENT_COMBATANT_LIST)) {
                    // This list will be null if a) this is the first time this fragment has been used on this device, or b) no combatants have been saved previously
                    currentFactionCombatantList = ((AllFactionCombatantLists) getArguments().getSerializable(CURRENT_COMBATANT_LIST)).clone(); // Get a "snapshot" clone of the incoming List, because this List may end up being modified over the lifetime of this Fragment (due to adding Combatants)
//                    eligibleCombatantsList.removeAll(currentFactionCombatantList); // Remove all of the Combatants in the incoming list from the list of Combatants found in the file (the user cannot add anyone that already exists in the party) (REMOVED: They user can add a second "copy" of an existing Combatant, which will be dealt with automatically)
                } else {
                    Log.e(TAG, "Did not receive Combatant list");
                    currentFactionCombatantList = new AllFactionCombatantLists(); // No other Combatants need to be considered, aside from those that are saved
                }

                if (getArguments().containsKey(COMBATANT_DESTINATION)) {
                    combatantDestination = (ReceiveAddedCombatant) getArguments().getSerializable(COMBATANT_DESTINATION);
                } else {
                    Log.e(TAG, "Did not receive Combatant Destination");
                }
            } else {
                // If this Fragment is intended only to modify the saved Combatant list?
                currentFactionCombatantList = new AllFactionCombatantLists(); // No other Combatants need to be considered, aside from those that are saved

            }

        } else {
            Log.e(TAG, "Did not receive arguments");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layoutContents = inflater.inflate(R.layout.add_combatant, container, false);
        searchView = layoutContents.findViewById(R.id.add_combatant_search);
        emptyCombatants = layoutContents.findViewById(R.id.add_combatants_empty);
        combatantGroupParent = layoutContents.findViewById(R.id.add_combatant_layout_parent);
        addNewCombatant = layoutContents.findViewById(R.id.add_new_combatant);
        closeButton = layoutContents.findViewById(R.id.view_list_close);
        title = layoutContents.findViewById(R.id.view_saved_combatants_title);

        if (expectingReturnedCombatant) {
            // We want to add a new Combatant to the encounter
            title.setText(R.string.add_combatant_title);

            closeButton.setVisibility(View.GONE);
        } else {
            // We are just looking at/modifying the bookmarked Combatants
            title.setText(R.string.mod_saved_combatant);

            closeButton.setVisibility(View.VISIBLE);
        }

        // Display the correct fragments for each faction
        updateFactionFragmentDisplay();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                // Send this text query to the Filters in each Fragment
                for (int facInd = 0; facInd < eligibleCombatantsList.getAllFactionLists().size(); facInd++) {
                    // For each faction in the Combatant list...
                    Combatant.Faction thisFac = eligibleCombatantsList.getAllFactionLists().get(facInd).faction();

                    if (factionFragmentMap.containsKey(thisFac)) {
                        // Get the Fragment associated with this Faction, and send this search string to the associated adapter (or more specifically, its Filter)
                        factionFragmentMap.get(thisFac).getFragment().getAdapter().getFilter().filter(s);
                    } else {
                        // Uh oh...
                        Log.w(TAG, "No Fragment found for Faction " + thisFac);
                    }
                }

                return true;
            }
        });

        addNewCombatant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getChildFragmentManager();
                CreateOrModCombatant createCombatantFragment = CreateOrModCombatant.newInstance(ViewSavedCombatantsFragment.this, getMasterCombatantList(), new Bundle());
                createCombatantFragment.show(fm, "CreateNewCombatant");
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Close the view
                // If changing flow, here we will want to a) return the selected combatant to the combatantDestination, and b) probably change the location of the button to which this listener is ascribed
                saveAndClose();
            }
        });

        // TODO TEST
        return layoutContents; // If there is nothing in the file, then no combatants have previously been saved, so display the empty message
    }

    void updateFactionFragmentDisplay() {
        // TODO LATER: This is VERY similar to the code in ConfigureCombatantListActivity, could perhaps consolidate them somehow?
        boolean haveCombatants = false;
        if (eligibleCombatantsList != null) {
            // If there are factions, check if any of them have combatants.  Otherwise, display the no combatant message
            ArrayList<FactionCombatantList> factionCombatantLists = eligibleCombatantsList.getAllFactionLists();
            for (int factionInd = 0; factionInd < factionCombatantLists.size(); factionInd++) {
                if (factionCombatantLists.get(factionInd).size() != 0) {
                    haveCombatants = true;
                    break;
                }
            }
        }

        if (haveCombatants) {
            // If there were combatants previously saved, then load them into the View
            emptyCombatants.setVisibility(View.GONE);
        } else {
            // If there is nothing in the file, then no combatants have previously been saved, so display the empty message
            emptyCombatants.setVisibility(View.VISIBLE);
        }

        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction fragTransaction = null;

        ArrayList<FactionCombatantList> factionCombatantLists = eligibleCombatantsList.getAllFactionLists();
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
                ListCombatantRecyclerAdapter adapter = new ListCombatantRecyclerAdapter(this, getContext(), eligibleCombatantsList.getAllFactionLists().get(facInd), !expectingReturnedCombatant); // If we are expecting a Combatant to return from the Fragment (we are selecting a Combatant to add), then we CANNOT modify the list.   If we are just viewing/modifying at the saved Combatants list, then we CAN modify the list

                // Create a new container view to add to the LinearLayout
                FrameLayout thisFragmentContainer = new FrameLayout(getContext());
                thisFragmentContainer.setId(facInd + 1000); // Set some id for the FrameLayout
                combatantGroupParent.addView(thisFragmentContainer, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                // Create a new fragment, then place it in the container
                CombatantGroupFragment newFrag = new CombatantGroupFragment(adapter, eligibleCombatantsList.getAllFactionLists().get(facInd).faction());
                fragTransaction.add(thisFragmentContainer.getId(), newFrag, Combatant.factionToString(eligibleCombatantsList.getAllFactionLists().get(facInd).faction()) + "_add_fragment");

                // Add the fragment to the map, so we can refer to it later
                factionFragmentMap.put(factionList.faction(), new FactionFragmentInfo(newFrag, thisFragmentContainer));

                continue;
            }

            if (mustRemove) {
                // If we must remove this faction's Fragment
                // Delete the view in which the Fragment is contained
                combatantGroupParent.removeView(factionFragmentMap.get(factionList.faction()).getContainer());

                // Remove the fragment
                fragTransaction.remove(factionFragmentMap.get(factionList.faction()).getFragment());

                // Delete the key from the factionFragmentMap (it is no longer being displayed)
                factionFragmentMap.remove(factionList.faction());

                continue;
            }

            // Let the adapter know that the Combatants list *may* have changed, if we are neither adding a Fragment (nothing changed because we just initialized it) nor removing one (it no longer exists, so doesn't need to update)
            if (factionFragmentMap.containsKey(factionList.faction())) {
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

    @Override
    public void receiveChosenCombatant(Combatant selectedCombatant) {
        if (expectingReturnedCombatant) {
            // TODO CHECK: For AddCombatant: Try out the flow here.  Do I want the user to be able to add/remove an indefinite number of Combatants before choosing one?  Should the Combatant be returned immediately upon exiting the CreateOrModCombatant dialog if a Combatant is created?  Should there be a confirmation dialog/other method to confirm a Combatant selection aside from a single tap?
            // Just received a Combatant from the ListCombatantRecyclerAdapter because the user selected one

            selectedCombatant.genUUID(); // Generate a new unique ID for this Combatant upon being chosen
            // First, send the Combatant back to the calling Activity/Fragment
            combatantDestination.receiveAddedCombatant(selectedCombatant.cloneUnique());
            if (currentFactionCombatantList.getCombatantNamesList().contains(selectedCombatant.getName())) {
                // Possible uh-oh, if the AllFactionCombatantList.addCombatant() function doesn't work right...
                Log.w(TAG, "Selected Combatant is already in the encounter list.  Adding another.");
            }

            // Next, try to add this Combatant to the Combatant list
            saveAndClose();
        }
        // If the calling Activity/Fragment is NOT expecting a returned Cobmatant, then do nothing
    }

    @Override
    public AllFactionCombatantLists getMasterCombatantList() {
        // This will be passed to the CreateOrModCombatant Fragment, so in this case it should send a combination of both the list from file and the list of current Combatants being used in the Encounter (there shouldn't be any doubles ANYWHERE)
        return generateMasterCombatantList();
    }

    @Override
    public void removeCombatant(Combatant combatantToRemove) {
        // Remove the Combatant
        eligibleCombatantsList.remove(combatantToRemove);

        // A Combatant in one of the Fragments was just removed.  Update the list, in case one of the Fragments needs to be removed
        updateFactionFragmentDisplay();
    }

    @Override
    public void addCombatant(Combatant combatantToAdd) {
        // Add the Combatant indicated
        eligibleCombatantsList.addCombatant(combatantToAdd);

        // Update the Fragment displace
        updateFactionFragmentDisplay();
    }

    @Override
    public void receiveCombatant(Combatant newCombatant, Bundle returnBundle) {
        // A new Combatant was just created (must be a new Combatant, because if it was a modified combatant, then it would have been sent to one of the adapter in factionFragmentMap)
        // TODO CHECK: Here is where we can change the flow.  If we don't like this, just need to add selection ability (relatively easy...) and a confirmation button (should already be there from the modifySavedCombatants version of this Fragment).

        newCombatant.genUUID(); // Give this Combatant a new ID upon being created
        // Try to add this combatant to the save file (will only add the base-name)
        addCombatantToSave(newCombatant);
        updateFactionFragmentDisplay();

        if (expectingReturnedCombatant) {
            // If the main activity is expecting a Combatant back, then send it before we need to modify the new combatant
            combatantDestination.receiveAddedCombatant(newCombatant);

            // If the reason this Fragment was called was to return a Combatant, then close it
            saveAndClose();
        }
    }

    private void addCombatantToSave(Combatant newCombatant) {
        // Try to add a new Combatant (by base name) to the eligible Combatant list, which will be then saved to file once we close out of this Fragment
        // If a Combatant with this base-name already exists, then don't bother saving it
        if (!eligibleCombatantsList.containsName(newCombatant.getBaseName())) {
            // If no other Combatant exists with this name, then make a clone of the Combatant with only the base name, and sanitize its roll, etc
            eligibleCombatantsList.addCombatant(newCombatant.getRaw());
        } else if (!expectingReturnedCombatant) {
            // If the only reason the user is here is to modify the saved Combatants, and they just added a Combatant with an ordinal, tell them how silly they are
            Toast.makeText(getContext(), "A version of " + newCombatant.getBaseName() + " is already bookmarked", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void notifyCombatantChanged(Bundle returnBundle) {
        // Should never receive this, because this Fragment can only accept NEW combatants from the CreateOrModCombatant Fragment, not MODIFIED combatants

        // Do Nothing
        Log.w(TAG, "Got notification of changed Combatant where there should not be one");
    }

    private void saveAndClose() {
        // Save the Combatant data that we have now
//        AllFactionCombatantLists newSavedCombatantsList = eligibleCombatantsList.clone(); // Create a list that contains the eligible Combatants (Combatants from the save file PLUS any Combatants that were just made)...
//        newSavedCombatantsList.addAll(currentFactionCombatantList); // ... and the Combatants from the current Encounter
        if (!eligibleCombatantsList.equals(savedCombatantsList)) {
            // If the test list is not equal to the list of Combatants from the file, that means that some Combatants were added (or possibly removed...?), so we should save the new list
            LocalPersistence.writeObjectToFile(getContext(), eligibleCombatantsList, combatantListSaveFile);
        }

        // Finally, close the Fragment
        dismiss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
//            @Override
//            public void onBackPressed() {
//                saveAndClose();
//            }

            @Override
            public void dismiss() {
                saveAndClose();
            }
        };

    }


    private AllFactionCombatantLists generateMasterCombatantList() {
        AllFactionCombatantLists masterCombatantList = new AllFactionCombatantLists();
        masterCombatantList.addAll(eligibleCombatantsList);
        masterCombatantList.addAll(currentFactionCombatantList);
        return masterCombatantList;
    }

    public interface ReceiveAddedCombatant extends Serializable {
        void receiveAddedCombatant(Combatant addedCombatant);
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
}
