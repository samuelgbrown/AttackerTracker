package to.us.suncloud.myapplication;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
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
 * Use the {@link ViewSavedCombatantsFragment#newViewBookmarkedFightablesInstance} factory method to
 * create an instance of this fragment.
 */

public class ViewSavedCombatantsFragment extends DialogFragment implements ListFightableRecyclerAdapter.MasterFightableKeeper, CreateOrModCombatant.receiveNewOrModCombatantInterface {
    private static final String TAG = "ViewSavedCombatants";
    // The fragment initialization parameters
    private static final String CURRENT_COMBATANT_LIST = "currentCombatantList";

    private static final String combatantListSaveFile = "combatantListSaveFile";

//    HashMap<Fightable.Faction, FactionFragmentInfo> factionFragmentMap = new HashMap<>();


    //    private AllFactionFightableLists eligibleCombatantsList = null; // An list of Combatants that appear in the savedCombatantsList plus any Combatants that have been added
    private AllFactionFightableLists savedCombatantsList = null; // An exact copy of the Combatant list from the saved file
    private AllFactionFightableLists currentFactionCombatantList = null; // Used to generate a master Combatant list, to send to the CreateOrModCombatant dialogue

    private boolean isMultiSelecting = false; // Is the Fragment (or adapter) currently in a multi-selecting state?

    private ReceiveAddedCombatant combatantDestination = null; // The Activity/Fragment that will receive the selected Combatant

    private ListFightableRecyclerAdapter adapter;
    private View emptyCombatants;

    private Button addNewCombatant;
    private ConstraintLayout multiSelectLayout;

    public ViewSavedCombatantsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AddCombatantFragment.
     */
    static ViewSavedCombatantsFragment newViewBookmarkedFightablesInstance(AllFactionFightableLists currentFactionCombatantList) {
        ViewSavedCombatantsFragment fragment = new ViewSavedCombatantsFragment();
        Bundle args = new Bundle();
        args.putSerializable(CURRENT_COMBATANT_LIST, currentFactionCombatantList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO GROUP UPDATE: Want to allow the user to group combatants together into a group, to be added all together.  Must include ability to:
        //      1) Create a group of combatants
        //      2) See this group of combatants in the Combatant list
        //          a) Must either have useful/descriptive name, display combatants in group when in list, or show contents of group when selected?
        //      3) Select this group of combatants to add to the party
        //      Workflows:
        //          X Add Combatant and View Saved Combatants will be combined into one button (functionality is too similar to be separate)
        //          Create group / Add combatant(s) to group - Multi-select combatant *from any VSCF*, select "Add to group..."
        //              Create an AllFactionFightableLists object using these combatants, sorting each into respective lists
        //              New dialog appears with list of all current groups (plus "Create new group...")
        //                  If adding to old group, add Combatants and pull up View Group dialog on that group
        //                  If adding to new group, create new group with default name, pass to View Group dialog, pull up View Group dialog
        //              Will add REFERENCE to any non-doubled Combatants to the group
        //              If any already exist, tell user "One or more combatants are already in this group.\nAdd multiples of a Combatant by editing the group!"
        //          View group - Click gear button for group - brings up new modal window, similar to VSCF but different enough that it will be independent.  Can change name of group, and change Combatants
        //          From view group, add / remove combatant - Trash Bin icon for each Combatant
        //          From view group, adding multiples of a Combatant - "Multiple person" icon for each Combatant - Extra portion on left of viewholder (where checkmark currently lives) will indicate multiples of a Combatant iff they are greater than 1
        //          Add group to combat - Add group, same as Combatants are added.  If copy already exists, then ask user for each copy whether it should be added ("X is already in the Encounter!  Add a copy of X-nonordinal?") - use same response for copies
        //              Go through each group combatant - if exists, get response from user - then resolve copies
        //  ************If INVISIBLE version of combatant exists - ?????
        //          Modifying Combatant (if it is in a group) - No new workflow, but changes will be reflected in groups
        //          Deleting Combatant (if it is in a group) - Add text to delete confirmation ("Are you sure you want to delete this combatant?  It is in at least one group").
        //          Exiting the app - Will save entire state by default on exit (including Encounter)
        //          Stretch - Import/Export Combatants / Groups
        //          Stretch - Import/Export Encounter data

        // Load in combatants from file (process them later)
        savedCombatantsList = (AllFactionFightableLists) LocalPersistence.readObjectFromFile(requireContext(), combatantListSaveFile);
        if (savedCombatantsList == null) {
            // If there aren't any previously saved Combatants (not even a blank AllFactionCombatantList), then make a new empty list to represent them
            savedCombatantsList = new AllFactionFightableLists();
        }

        // Initialize arguments
        currentFactionCombatantList = new AllFactionFightableLists();

        // Read arguments
        if (getArguments() != null) {
            // If this Fragment is intended to add a new Combatant to the Encounter...
            if (getArguments().containsKey(CURRENT_COMBATANT_LIST)) {
                // This list will be null if a) this is the first time this fragment has been used on this device, or b) no combatants have been saved previously
                currentFactionCombatantList = ((AllFactionFightableLists) getArguments().getSerializable(CURRENT_COMBATANT_LIST)).clone(); // Get a "snapshot" clone of the incoming List, because this List may end up being modified over the lifetime of this Fragment (due to adding Combatants)
            } else {
                // Will consider combatants saved in the file, but none others
                Log.e(TAG, "Did not receive Combatant list");
            }

            // Save the Combatant destination (cannot be passed as Fragment argument, otherwise there will be Serialization errors)
            try {
                combatantDestination = (ReceiveAddedCombatant) getActivity();
            } catch (ClassCastException e) {
                Log.e(TAG, "Activity cannot be cast to ReceiveAddedCombatant: " + e.getLocalizedMessage());
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
        TextView title = layoutContents.findViewById(R.id.view_saved_combatants_title);

        addNewCombatant = layoutContents.findViewById(R.id.add_new_combatant); // Saved to toggle visibility
        multiSelectLayout = layoutContents.findViewById(R.id.multi_select_options); // Saved to toggle visibility
        Button addToGroup = layoutContents.findViewById(R.id.add_to_group);
        ImageButton multiSelectConfirm = layoutContents.findViewById(R.id.confirm_multi_select);
        ImageButton multiSelectCancel = layoutContents.findViewById(R.id.cancel_multi_select);
        title.setText(R.string.view_saved_combatants_title);

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
//            }.
//        });

        // Create an adapter and give it to the Recycler View
        ListFightableRecyclerAdapter.LFRAFlags flags = new ListFightableRecyclerAdapter.LFRAFlags(); // Create flags
        flags.adapterCanModify = true;
        flags.canMultiSelect = true;
        adapter = new ListFightableRecyclerAdapter(this, savedCombatantsList.clone(), flags); // Populate a Recycler view with the saved Combatants
        combatantListView.setAdapter(adapter);
        combatantListView.addItemDecoration(new BannerDecoration(getContext()));
        combatantListView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set up the multi-selection button, so that the user can confirm their selection if choosing multiple combatants
        multiSelectConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the list of Combatants from the adapter, and return the list to whoever called this Fragment
                ArrayList<Fightable> selectedList = adapter.getAllSelectedFightables(); // Get a list of all selected Combatants

                // Go through the entire list, and send each one to the destination, one-by-one
                for (int i = 0; i < selectedList.size(); i++) {
                    Fightable thisFightable = selectedList.get(i);
                    ArrayList<Combatant> fightableAsCombatantList = thisFightable.convertToCombatants(); // If this is some non-Combatant fightable, convert it to a list of Combatants
                    for (int j = 0;j < fightableAsCombatantList.size(); j++) {
                        combatantDestination.receiveAddedCombatant(fightableAsCombatantList.get(j).cloneUnique()); // Clone the Combatant and send it
                    }
                }

                // Next, try to add this Combatant to the Combatant list, and close up
                saveAndClose();
        }
        });

        multiSelectCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clearMultiSelect();
            }
        });
        addToGroup.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Get the list of Combatants from the adapter, and send it to the Select Group Fragment
                ArrayList<Fightable> selectedList = adapter.getAllSelectedFightables(); // Get a list of all selected Combatants

                // TODO: GROUP - Make sure that the selected fighters are cleared ONLY if successfully added to a group - otherwise, keep them selected
            }
        }));

        // Initialize the GUI
        updateNoCombatantMessage();
        notifyIsMultiSelecting(false);

        return layoutContents;
    }

    private void updateNoCombatantMessage() {
        // TO_DO LATER: This is VERY similar to the code in ConfigureFightableListActivity, could perhaps consolidate them somehow?
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
//        ArrayList<FactionFightableList> factionCombatantLists = eligibleCombatantsList.getAllFactionLists();
//        for (int facInd = 0; facInd < factionCombatantLists.size(); facInd++) {
//            FactionFightableList factionList = factionCombatantLists.get(facInd);
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
//                ListFightableRecyclerAdapter adapter = new ListFightableRecyclerAdapter(this, getContext(), eligibleCombatantsList.getAllFactionLists().get(facInd), !expectingReturnedCombatant); // If we are expecting a Combatant to return from the Fragment (we are selecting a Combatant to add), then we CANNOT modify the list.   If we are just viewing/modifying at the saved Combatants list, then we CAN modify the list
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
//                factionFragmentMap.get(factionList.faction()).getFragment().getAdapter().notifyFightableListChanged();
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
    public void receiveChosenFightable(Fightable selectedFightable) {
        // Just received a Combatant from the ListFightableRecyclerAdapter because the user selected one

        // Convert the Fightable to a CombatantList, and send the Combatants back to the calling Activity/Fragment
        ArrayList<Combatant> fightableAsCombatantList = selectedFightable.convertToCombatants(); // If this is some non-Combatant fightable, convert it to a list of Combatants
        for (int i = 0;i < fightableAsCombatantList.size(); i++) {
            combatantDestination.receiveAddedCombatant(fightableAsCombatantList.get(i).cloneUnique()); // Clone the Combatant and send it
        }

        // Next, try to add this Combatant to the Combatant list
        saveAndClose();
    }

    @Override
    public void receiveCombatant(Combatant newCombatant, Bundle returnBundle) {
        // A new Combatant was just created (must be a new Combatant, because if it was a modified combatant, then it would have been sent to the adapter)

        // Try to add this combatant to the save file (will only add the base-name)
        addCombatantToSave(newCombatant);
        updateNoCombatantMessage();

        // If the main activity is expecting a Combatant back, then send it before we need to modify the new combatant
        combatantDestination.receiveAddedCombatant(newCombatant);

        // If the reason this Fragment was called was to return a Combatant, then close it
        saveAndClose();
    }

    @Override
    public void notifyFightableListChanged() {
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
        if (isMultiSelecting) {
            multiSelectLayout.setVisibility( View.VISIBLE );
            addNewCombatant.setVisibility( View.GONE );
        } else {
            multiSelectLayout.setVisibility( View.GONE );
            addNewCombatant.setVisibility( View.VISIBLE );
        }
    }

    @Override
    public boolean safeToDelete(Fightable fightable) {
        return true; // Combatants in the bookmarked section can always be fully deleted, if need-be
    }

    private void addCombatantToSave(Combatant newCombatant) {
        // Try to add a new Combatant (by base name) to the eligible Combatant list, which will be then saved to file once we close out of this Fragment
        // If a Combatant with this base-name already exists, then don't bother saving it
        if (!adapter.getCombatantList().containsName(newCombatant.getBaseName())) {
            // If no other Combatant exists with this name, then make a clone of the Combatant with only the base name, and sanitize its roll, etc
            adapter.addFightable(newCombatant.getRaw());
            saveCombatantList();
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
//        AllFactionFightableLists newSavedCombatantsList = eligibleCombatantsList.clone(); // Create a list that contains the eligible Combatants (Combatants from the save file PLUS any Combatants that were just made)...
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


    private AllFactionFightableLists generateMasterCombatantList() {
        AllFactionFightableLists masterCombatantList = new AllFactionFightableLists();
        masterCombatantList.addAll(adapter.getCombatantList());
        masterCombatantList.addAll(currentFactionCombatantList);
        return masterCombatantList;
    }

    public interface ReceiveAddedCombatant extends Serializable {
        void receiveAddedCombatant(Combatant addedCombatant);
    }

    // TODO: Remove?  Is this unused?
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
