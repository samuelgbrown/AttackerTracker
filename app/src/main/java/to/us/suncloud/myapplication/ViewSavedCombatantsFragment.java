package to.us.suncloud.myapplication;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;


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

//    HashMap<Combatant.Faction, FactionFragmentInfo> factionFragmentMap = new HashMap<>();


    //    private AllFactionCombatantLists eligibleCombatantsList = null; // An list of Combatants that appear in the savedCombatantsList plus any Combatants that have been added
    private AllFactionCombatantLists savedCombatantsList = null; // An exact copy of the Combatant list from the saved file
    private AllFactionCombatantLists currentFactionCombatantList = null; // Used to generate a master Combatant list, to send to the CreateOrModCombatant dialogue

    private boolean expectingReturnedCombatant = false;
    private boolean isMultiSelecting = false; // Is the Fragment (or adapter) currently in a multi-selecting state?

    private ReceiveAddedCombatant combatantDestination = null; // The Activity/Fragment that will receive the selected Combatant

    private ListCombatantRecyclerAdapter adapter;
    private View emptyCombatants;
    private ImageButton multiSelectConfirm;
    private ImageButton multiSelectCancel;

    public ViewSavedCombatantsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AddCombatantFragment.
     */
//    static ViewSavedCombatantsFragment newAddCombatantToListInstance(ReceiveAddedCombatant combatantDestination, AllFactionCombatantLists currentFactionCombatantList) {
    static ViewSavedCombatantsFragment newAddCombatantToListInstance(AllFactionCombatantLists currentFactionCombatantList) {
        ViewSavedCombatantsFragment fragment = new ViewSavedCombatantsFragment();
        Bundle args = new Bundle();
        args.putSerializable(CURRENT_COMBATANT_LIST, currentFactionCombatantList);
//        args.putSerializable(COMBATANT_DESTINATION, combatantDestination);
        args.putBoolean(EXPECTING_RETURNED_COMBATANT, true); // If an AddCombatant Fragment is requested, then they are expecting a Combatant to be returned to the calling Activity/Fragment
        fragment.setArguments(args);
        return fragment;
    }

    static ViewSavedCombatantsFragment newModifySavedCombatantListInstance() {
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
        savedCombatantsList = (AllFactionCombatantLists) LocalPersistence.readObjectFromFile(requireContext(), combatantListSaveFile);
        if (savedCombatantsList == null) {
            // If there aren't any previously saved Combatants (not even a blank AllFactionCombatantList), then make a new empty list to represent them
            savedCombatantsList = new AllFactionCombatantLists();
        }

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

                // Save the Combatant destination (cannot be passed as Fragment argument, otherwise there will be Serialization errors)
                try {
                    combatantDestination = (ReceiveAddedCombatant) getActivity();
                } catch (ClassCastException e) {
                    Log.e(TAG, "Activity cannot be cast to ReceiveAddedCombatant: " + e.getLocalizedMessage());
                }

//                if (getArguments().containsKey(COMBATANT_DESTINATION)) {
//                    combatantDestination = (ReceiveAddedCombatant) getArguments().getSerializable(COMBATANT_DESTINATION);
//                } else {
//                    Log.e(TAG, "Did not receive Combatant Destination");
//                }
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
        final SearchView searchView = layoutContents.findViewById(R.id.add_combatant_search);
        emptyCombatants = layoutContents.findViewById(R.id.add_combatants_empty);
        RecyclerView combatantListView = layoutContents.findViewById(R.id.view_saved_combatants_list);
        Button addNewCombatant = layoutContents.findViewById(R.id.add_new_combatant);
//        ImageButton closeButton = layoutContents.findViewById(R.id.view_list_close);
        TextView title = layoutContents.findViewById(R.id.view_saved_combatants_title);
        multiSelectConfirm = layoutContents.findViewById(R.id.confirm_multi_select);
        multiSelectCancel = layoutContents.findViewById(R.id.cancel_multi_select);

        if (expectingReturnedCombatant) {
            // We want to add a new Combatant to the encounter
            title.setText(R.string.add_combatant_title);

//            closeButton.setVisibility(View.GONE);
        } else {
            // We are just looking at/modifying the bookmarked Combatants
            title.setText(R.string.mod_saved_combatant);

//            closeButton.setVisibility(View.VISIBLE);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                // Send this text query to the Filters in the adapter
                adapter.getFilter().filter(s);
                return true;
            }
        });
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.onActionViewExpanded(); // Expand the search view when you click on it (why doesn't it do this normally???)
            }
        });

        addNewCombatant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getChildFragmentManager();
                CreateOrModCombatant createCombatantFragment = CreateOrModCombatant.newInstance(ViewSavedCombatantsFragment.this, generateMasterCombatantList(), new Bundle());
                createCombatantFragment.show(fm, "CreateNewCombatant");
            }
        });

//        closeButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // Close the view
//                // If changing flow, here we will want to a) return the selected combatant to the combatantDestination, and b) probably change the location of the button to which this listener is ascribed
//                saveAndClose();
//            }
//        });

        // Create an adapter and give it to the Recycler View
        ListCombatantRecyclerAdapter.LCRAFlags flags = new ListCombatantRecyclerAdapter.LCRAFlags(); // Create flags
        flags.adapterCanModify = !expectingReturnedCombatant;
        flags.mustReturnCombatant = expectingReturnedCombatant;
        adapter = new ListCombatantRecyclerAdapter(this, savedCombatantsList.clone(), flags); // Populate a Recycler view with the saved Combatants
        combatantListView.setAdapter(adapter);
        combatantListView.addItemDecoration(new BannerDecoration(getContext()));
        combatantListView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set up the multi-selection button, so that the user can confirm their selection if choosing multiple combatants
        multiSelectConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the list of Combatants from the adapter, and return the list to whoever called this Fragment
                if (expectingReturnedCombatant) {
                    ArrayList<Combatant> returnList = adapter.getAllSelectedCombatants(); // Get a list of all selected Combatants

                    // Go through the entire list, and send each one to the destination, one-by-one
                    for (int i = 0; i < returnList.size(); i++) {
                        combatantDestination.receiveAddedCombatant(returnList.get(i).cloneUnique()); // Clone the Combatant and send it
                    }

                    // Next, try to add this Combatant to the Combatant list, and close up
                    saveAndClose();
                }
                // If the calling Activity/Fragment is NOT expecting a returned Combatant, then do nothing
            }
        });

        multiSelectCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clearMultiSelect();
            }
        });

        // Initialize the GUI
        updateNoCombatantMessage();
        notifyIsMultiSelecting(false);

        return layoutContents;
    }

    private void updateNoCombatantMessage() {
        // TO_DO LATER: This is VERY similar to the code in ConfigureCombatantListActivity, could perhaps consolidate them somehow?
        boolean haveCombatants = false;
        if (adapter.getCombatantList() != null) {
            haveCombatants = !adapter.getCombatantList().isVisibleEmpty(); // Are any Combatant visible?
        }

        if (haveCombatants) {
            // If there were combatants previously saved, then load them into the View
            emptyCombatants.setVisibility(View.GONE);
        } else {
            // If there is nothing in the file, then no combatants have previously been saved, so display the empty message
            emptyCombatants.setVisibility(View.VISIBLE);
        }

//        FragmentManager fm = getChildFragmentManager();
//        FragmentTransaction fragTransaction = null;
//
//        ArrayList<FactionCombatantList> factionCombatantLists = eligibleCombatantsList.getAllFactionLists();
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
//                ListCombatantRecyclerAdapter adapter = new ListCombatantRecyclerAdapter(this, getContext(), eligibleCombatantsList.getAllFactionLists().get(facInd), !expectingReturnedCombatant); // If we are expecting a Combatant to return from the Fragment (we are selecting a Combatant to add), then we CANNOT modify the list.   If we are just viewing/modifying at the saved Combatants list, then we CAN modify the list
//
//                // Create a new container view to add to the LinearLayout
//                FrameLayout thisFragmentContainer = new FrameLayout(getContext());
//                thisFragmentContainer.setId(facInd + 1000); // Set some id for the FrameLayout
//                combatantGroupParent.addView(thisFragmentContainer, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//
//                // Create a new fragment, then place it in the container
//                CombatantGroupFragment newFrag = new CombatantGroupFragment(adapter, eligibleCombatantsList.getAllFactionLists().get(facInd).faction());
//                fragTransaction.add(thisFragmentContainer.getId(), newFrag, Combatant.factionToString(eligibleCombatantsList.getAllFactionLists().get(facInd).faction()) + "_add_fragment");
//
//                // Add the fragment to the map, so we can refer to it later
//                factionFragmentMap.put(factionList.faction(), new FactionFragmentInfo(newFrag, thisFragmentContainer));
//
//                continue;
//            }
//
//            if (mustRemove) {
//                // If we must remove this faction's Fragment
//                // Delete the view in which the Fragment is contained
//                combatantGroupParent.removeView(factionFragmentMap.get(factionList.faction()).getContainer());
//
//                // Remove the fragment
//                fragTransaction.remove(factionFragmentMap.get(factionList.faction()).getFragment());
//
//                // Delete the key from the factionFragmentMap (it is no longer being displayed)
//                factionFragmentMap.remove(factionList.faction());
//
//                continue;
//            }
//
//            // Let the adapter know that the Combatants list *may* have changed, if we are neither adding a Fragment (nothing changed because we just initialized it) nor removing one (it no longer exists, so doesn't need to update)
//            if (factionFragmentMap.containsKey(factionList.faction())) {
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

    @Override
    public void receiveChosenCombatant(Combatant selectedCombatant) {
        // Just received a Combatant from the ListCombatantRecyclerAdapter because the user selected one

        if (expectingReturnedCombatant) {

            // First, send the Combatant back to the calling Activity/Fragment
            combatantDestination.receiveAddedCombatant(selectedCombatant.cloneUnique());

            // Next, try to add this Combatant to the Combatant list
            saveAndClose();
        }
        // If the calling Activity/Fragment is NOT expecting a returned Combatant, then do nothing
    }

    @Override
    public void receiveCombatant(Combatant newCombatant, Bundle returnBundle) {
        // A new Combatant was just created (must be a new Combatant, because if it was a modified combatant, then it would have been sent to the adapter)

        // Try to add this combatant to the save file (will only add the base-name)
        addCombatantToSave(newCombatant);
        updateNoCombatantMessage();

        if (expectingReturnedCombatant) {
            // If the main activity is expecting a Combatant back, then send it before we need to modify the new combatant
            combatantDestination.receiveAddedCombatant(newCombatant);

            // If the reason this Fragment was called was to return a Combatant, then close it
            saveAndClose();
        }
    }

    @Override
    public void notifyCombatantListChanged() {
        // If the Combatant List was modified, check if we need to display the no Combatant message
        updateNoCombatantMessage();

        // Save the changes to the list
        saveCombatantList();
    }

    @Override
    public void notifyIsMultiSelecting(boolean isMultiSelecting) {
        // The adapter started multi-selecting
        this.isMultiSelecting = isMultiSelecting;

        // Set the visibility of multi-selection-related elements in the GUI
        int visibility = View.GONE;
        if (isMultiSelecting) {
            visibility = View.VISIBLE;
        }

        // Set the visibility of both multi-select GUI elements
        multiSelectConfirm.setVisibility(visibility);
        multiSelectCancel.setVisibility(visibility);
    }

    @Override
    public boolean safeToDelete(Combatant combatant) {
        return true; // Combatants in the bookmarked section can always be fully deleted, if need-be
    }

    private void addCombatantToSave(Combatant newCombatant) {
        // Try to add a new Combatant (by base name) to the eligible Combatant list, which will be then saved to file once we close out of this Fragment
        // If a Combatant with this base-name already exists, then don't bother saving it
        if (!adapter.getCombatantList().containsName(newCombatant.getBaseName())) {
            // If no other Combatant exists with this name, then make a clone of the Combatant with only the base name, and sanitize its roll, etc
            adapter.addCombatant(newCombatant.getRaw());
            saveCombatantList();
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

    private void saveCombatantList() {
        // Save the Combatant data that we have now
//        AllFactionCombatantLists newSavedCombatantsList = eligibleCombatantsList.clone(); // Create a list that contains the eligible Combatants (Combatants from the save file PLUS any Combatants that were just made)...
//        newSavedCombatantsList.addAll(currentFactionCombatantList); // ... and the Combatants from the current Encounter
        if (!adapter.getCombatantList().rawEquals(savedCombatantsList)) {
            // If the test list is not equal to the list of Combatants from the file, that means that some Combatants were added (or possibly removed...?), so we should save the new list
            LocalPersistence.writeObjectToFile(getContext(), adapter.getCombatantList().getRawCopy(), combatantListSaveFile);
        }

        // Now keep track of the most recently saved batch of Combatants
        savedCombatantsList = adapter.getCombatantList().clone();
    }

    private void saveAndClose() {
        // Save the current list of Combatants
        saveCombatantList();

        // Finally, close the Fragment
        dismiss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(requireActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                if (isMultiSelecting) {
                    adapter.clearMultiSelect(); // This will clear multi-select in the adapter, and eventually let this Fragment know to update the GUI
                } else {
                    dismiss();
                }
            }

            @Override
            public void dismiss() {
                saveCombatantList();

                super.dismiss();
            }
        };

    }


    private AllFactionCombatantLists generateMasterCombatantList() {
        AllFactionCombatantLists masterCombatantList = new AllFactionCombatantLists();
        masterCombatantList.addAll(adapter.getCombatantList());
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
