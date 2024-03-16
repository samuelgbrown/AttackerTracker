package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ViewSavedCombatantsFragment#newViewBookmarkedFightablesInstance} factory method to
 * create an instance of this fragment.
 */

public class ViewSavedCombatantsFragment extends DialogFragment implements ListFightableRecyclerAdapter.MasterAFFLKeeper {
    private static final String TAG = "ViewSavedCombatants";
    // The fragment initialization parameters
    private static final String NEW_FIGHTABLES_TO_ADD = "currentCombatantList";

    public static final String COMBATANT_LIST_SAVE_FILE = "COMBATANT_LIST_SAVE_FILE";

    // Keys for the recursive function to send Combatants to combat
    private static final String ARG_FIGHTABLE = "fightable";
    private static final String ARG_STARTINT = "startInt";
    private static final String ARG_USERCONFIRMHASH = "userConfirmHash";

//    HashMap<Fightable.Faction, FactionFragmentInfo> factionFragmentMap = new HashMap<>();


    //    private AllFactionFightableLists eligibleCombatantsList = null; // An list of Combatants that appear in the rosterFightablesList plus any Combatants that have been added
    private AllFactionFightableLists rosterFightablesList = null; // An exact copy of the Combatant list from the saved file

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
    static ViewSavedCombatantsFragment newViewBookmarkedFightablesInstance() {
        return new ViewSavedCombatantsFragment();
    }

    static ViewSavedCombatantsFragment newViewBookmarkedFightablesInstance(AllFactionFightableLists newFightablesToAdd) {
        ViewSavedCombatantsFragment fragment = new ViewSavedCombatantsFragment();
        Bundle args = new Bundle();
        args.putSerializable(NEW_FIGHTABLES_TO_ADD, newFightablesToAdd);
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
        //          X Create group / Add combatant(s) to group - Multi-select combatant, select "Add to group..."
        //              X Create an CombatantGroup object using these combatants
        //                  X Need to figure out how to do this with REFERENCES to Combatants, rather than the Combatants themselves...may need a new data object...
        //                  X Could pass entire AllFactionFightableLists object from VSCF, and construct Group based on actual Combatants?  May need verification method, which is tolerant to Combatant deletion/copying/Faction swapping...
        //              X New dialog appears with list of all current groups (plus "Create new group...")
        //                  X If adding to old group, add Combatants and pull up View Group dialog on that group
        //                  X If adding to new group, create new group with default name, pass to View Group dialog, pull up View Group dialog
        //              X Will add REFERENCE to any non-doubled Combatants to the group
        //              X If any already exist, tell user "One or more combatants are already in this group.\nAdd multiples of a Combatant by editing the group!"
        //              X Group must check that it has at least one Combatant - if not, then DO NOT create new Group / delete Group (with warning on final Combatant being deleted, if Group existed already)!
        //          X View group - Click gear button for group - brings up new modal window, similar to VSCF but different enough that it will be independent.  Can change name of group, and change Combatants
        //          X From view group, add / remove combatant - Trash Bin icon for each Combatant
        //          X From view group, adding multiples of a Combatant - "Multiple person" icon for each Combatant - Extra portion on left of viewholder (where checkmark currently lives) will indicate multiples of a Combatant iff they are greater than 1
        //          X Add group to combat - Add group, same as Combatants are added.  If copy already exists, then ask user for each copy whether it should be added ("X is already in the Encounter!  Add a copy of X-nonordinal?") - use same response for copies
        //              X Go through each group combatant - if exists, get response from user - then resolve copies
        //              X If INVISIBLE version of combatant exists - after checking if we want to add Combatant again from Group, normal process is followed, user is asked if they want to revive.
        //          X Modifying Combatant (if it is in a group) - No new workflow, but changes will be reflected in groups
        //          X Deleting Combatant (if it is in a group) - Add text to delete confirmation ("**combatant** will also be removed from any groups it's in. Are you sure you want to delete **combatant**?").
        //          X (check that this works) Exiting the app - Will save entire state by default on exit (including Encounter)
        //          X Fix UI issue on main screen - banners have lower section visible: should only have upper green section visible
        //          Make app change sizes for tablets!
        //          Stretch 1 - Import Combatants / Groups - (from .csv)
        //          Stretch 2 - Export Combatants / Groups - (to .csv)
        //          Stretch 3 - Import/Export Encounter data

        // Load in combatants from file (process them later)
        rosterFightablesList = (AllFactionFightableLists) LocalPersistence.readObjectFromFile(requireContext(), COMBATANT_LIST_SAVE_FILE);
        if (rosterFightablesList == null) {
            // If there aren't any previously saved Combatants (not even a blank AllFactionCombatantList), then make a new empty list to represent them
            rosterFightablesList = new AllFactionFightableLists();
        }

        // Read arguments
        if (getArguments() != null) {
            // If this Fragment is intended to add a new Combatant to the Encounter...
            if (getArguments().containsKey(NEW_FIGHTABLES_TO_ADD)) {
                // This list will be null if a) this is the first time this fragment has been used on this device, or b) no combatants have been saved previously
                AllFactionFightableLists newFightablesToAdd = ((AllFactionFightableLists) getArguments().getSerializable(NEW_FIGHTABLES_TO_ADD)).clone(); // Get a "snapshot" clone of the incoming List, because this List may end up being modified over the lifetime of this Fragment (due to adding Combatants)
                rosterFightablesList.addAll(newFightablesToAdd);
                rosterFightablesList.verifyAllCombatantGroups();

                // Save this new data to file
                LocalPersistence.writeObjectToFile(requireContext(), rosterFightablesList, COMBATANT_LIST_SAVE_FILE);
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
        final Button addToGroup = layoutContents.findViewById(R.id.add_to_group);
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

        // Set up the multi-selection button, so that the user can confirm their selection if choosing multiple combatants
        multiSelectConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the list of Combatants from the adapter, and return the list to whoever called this Fragment
                ArrayList<Fightable> selectedList = adapter.getAllSelectedFightables(); // Get a list of all selected Combatants
                for ( Fightable fightable : selectedList ) {
                    Bundle sendFightableBundle = new Bundle();
                    sendFightableBundle.putSerializable(ARG_FIGHTABLE, fightable);

                    // Send each Fightable along (groups handled smoothly within)
                    sendFightableToCombat_Recursive( sendFightableBundle );
                }

                adapter.clearMultiSelect();

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
                ArrayList<Fightable> selectedTestList = adapter.getAllSelectedFightables();
                if ( selectedTestList.size() > 0 ) {
                    final AllFactionFightableLists combatantList = adapter.getCombatantList();

                    boolean containsGroup = false;
                    for ( Fightable fightable : selectedTestList ) {
                        if ( fightable instanceof CombatantGroup ) {
                            containsGroup = true;
                            break;
                        }
                    }

                    final FragmentManager fm = getChildFragmentManager();
                    if ( containsGroup ) {
                        new AlertDialog.Builder(getContext())
                                .setTitle(R.string.confirm_add_group_to_group)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        AddToGroupFragment.newInstance(adapter, combatantList).show(fm, "AddToGroup"); // combatantList will be cloned inside newInstance function
                                    }
                                })
                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Do nothing
                                    }
                                })
                                .show();
                    } else {
                        AddToGroupFragment.newInstance(adapter, combatantList).show(fm, "AddToGroup"); // combatantList will be cloned inside newInstance function
                    }
                } else {
                    Toast.makeText(getContext(), getContext().getString(R.string.no_combatants_for_group), Toast.LENGTH_SHORT).show();
                }
            }
        }));

        // Prepare the Add New Combatant button
        addNewCombatant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getChildFragmentManager();
                CreateOrModCombatant createCombatantFragment = CreateOrModCombatant.newInstance(adapter);
                createCombatantFragment.show(fm, "CreateNewCombatant");
            }
        });

        // Create an adapter and give it to the Recycler View
        ListFightableRecyclerAdapter.LFRAFlags flags = new ListFightableRecyclerAdapter.LFRAFlags(); // Create flags
        flags.adapterCanModify = true;
        flags.adapterCanMultiSelect = true;
        adapter = new ListFightableRecyclerAdapter(this, rosterFightablesList.clone(), flags); // Populate a Recycler view with the saved Combatants
        combatantListView.setAdapter(adapter);
        combatantListView.addItemDecoration(new BannerDecoration(getContext()));
        combatantListView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the GUI
        updateNoCombatantMessage();

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
        // Just received a Fightable from the ListFightableRecyclerAdapter because the user selected one

        Bundle sendFightableBundle = new Bundle();
        sendFightableBundle.putSerializable(ARG_FIGHTABLE, selectedFightable);

        sendFightableToCombat_Recursive(sendFightableBundle);
    }

    private void sendFightableToCombat_Recursive(Bundle sendFightableBundle) {
        // Initialize variables
        int startCombatantInd = 0;
        boolean doSave = true; // Only save if we have not branched into another call of this function
        final HashMap<String, Boolean> confirmedUserWantsToAdd;
        final Fightable selectedFightable;

        // Unpack incoming bundle
        if (sendFightableBundle.containsKey(ARG_FIGHTABLE)) {
            selectedFightable = (Fightable) sendFightableBundle.getSerializable(ARG_FIGHTABLE);
        } else {
            return; // Without a fightable, we can't really do much here...
        }
        if (sendFightableBundle.containsKey(ARG_STARTINT)) {
            startCombatantInd = sendFightableBundle.getInt(ARG_STARTINT);
        }
        if (sendFightableBundle.containsKey(ARG_USERCONFIRMHASH)) {
            confirmedUserWantsToAdd = (HashMap<String, Boolean>) sendFightableBundle.getSerializable(ARG_USERCONFIRMHASH);
        } else {
            confirmedUserWantsToAdd = new HashMap<>();
        }

        // Convert the Fightable to a CombatantList, and send the Combatants back to the calling Activity/Fragment
        ArrayList<Combatant> fightableAsCombatantList = selectedFightable.convertToCombatants(adapter.getCombatantList()); // If this is some non-Combatant fightable, convert it to a list of Combatants
        boolean isGroup = fightableAsCombatantList.size() > 1 ;

        for (int combatantInd = startCombatantInd; combatantInd < fightableAsCombatantList.size(); combatantInd++ ) {
            // Clone the Combatant to send
            final Combatant clonedCombatant = (Combatant) fightableAsCombatantList.get(combatantInd).cloneUnique();

            // Check if we've asked about this Combatant yet
            boolean haveAskedUserAboutThisCombatant = confirmedUserWantsToAdd.containsKey(clonedCombatant.getBaseName());
            boolean wantToAddThisCombatant = true;
            if (haveAskedUserAboutThisCombatant) {
                wantToAddThisCombatant = Boolean.TRUE.equals(confirmedUserWantsToAdd.get(clonedCombatant.getBaseName()));
            }

            if (isGroup && !haveAskedUserAboutThisCombatant && combatantDestination.containsCombatantWithSameBaseName(clonedCombatant)) {
                final int curCombatantInd = combatantInd;
                // If we need to ask the user first (Combatant is from a group, combat already has this Combatant [visible or not!], and this is the first time this Combatant has come up in this group), check with them
                new AlertDialog.Builder(getContext())
                        .setTitle(requireContext().getString(R.string.confirm_add_combatant_from_group, clonedCombatant.getName()))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Send Combatant along, and note that we know the user DOES want it to be added
                                combatantDestination.receiveAddedCombatant(clonedCombatant);
                                confirmedUserWantsToAdd.put(clonedCombatant.getBaseName(), true);

                                Bundle sendFightableBundle = new Bundle();
                                sendFightableBundle.putSerializable(ARG_FIGHTABLE, selectedFightable);
                                sendFightableBundle.putInt(ARG_STARTINT, curCombatantInd + 1);
                                sendFightableBundle.putSerializable(ARG_USERCONFIRMHASH, confirmedUserWantsToAdd);
                                sendFightableToCombat_Recursive(sendFightableBundle);
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Do NOT send Combatant along, and note that we know the user does NOT want it to be added
                                confirmedUserWantsToAdd.put(clonedCombatant.getBaseName(), false);

                                Bundle sendFightableBundle = new Bundle();
                                sendFightableBundle.putSerializable(ARG_FIGHTABLE, selectedFightable);
                                sendFightableBundle.putInt(ARG_STARTINT, curCombatantInd + 1);
                                sendFightableBundle.putSerializable(ARG_USERCONFIRMHASH, confirmedUserWantsToAdd);
                                sendFightableToCombat_Recursive(sendFightableBundle);
                            }
                        })
                        .show();
                doSave = false;
                break; // End this call of the function - adding more Combatants will continue from the call in the dialog box
            } else if (wantToAddThisCombatant) {
                // If we don't need to ask the user, just add this Combatant to combat and make sure we know to add it from here on out
                combatantDestination.receiveAddedCombatant(clonedCombatant);
                confirmedUserWantsToAdd.put(clonedCombatant.getBaseName(), true);
            }
        }

        // Finally, save and close the Fragment
        if ( doSave ) {
            saveAndClose();
        }
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

    private void saveCombatantList() {
        // Save the Combatant data that we have now
        AllFactionFightableLists adapterList = adapter.getCombatantList();
        if (!adapterList.rawEquals(rosterFightablesList)) {
            // If the test list is not equal to the list of Combatants from the file, that means that some Combatants were added (or possibly removed...?), so we should save the new list
            LocalPersistence.writeObjectToFile(requireContext(), adapter.getCombatantList().getRawCopy(), COMBATANT_LIST_SAVE_FILE);
        }

        // Now keep track of the most recently saved batch of Combatants
        rosterFightablesList = adapterList.clone();
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

    public interface ReceiveAddedCombatant extends Serializable {
        void receiveAddedCombatant(Combatant addedCombatant);
        boolean containsCombatantWithSameBaseName(Combatant combatant);
}
}
