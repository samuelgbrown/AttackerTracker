package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListFightableRecyclerAdapter extends RecyclerView.Adapter<ListFightableRecyclerAdapter.bindableVH> implements Filterable, CreateOrModCombatant.receiveNewOrModCombatantInterface {
    // TODO: GROUP ACTUALLY START HERE - 2 observed bugs: 1) Enemy Combatant will change over to Party Combatant when loading the ViewSavedCombatantsFragment, 2) the app crashes when trying to load the encounter
    private static final int UNSET = -1;

    public static final int COMBATANT_VIEW = 0;
    public static final int BANNER_VIEW = 1;

//    public static final String SET_MULTI_SELECT = "setMultiSelect";

    private static final String TAG = "ListFightableRecycler";

    MasterFightableKeeper parent = null; // If this is set, then the selected Combatant will be sent to the parent
    boolean adapterCanModify = false; // Can the adapter modify the Combatant (used so that we can use this adapter for both Combatant display and modify+display purposes, because they are VERY similar)
    boolean adapterCanCopy = false; // Can the adapter copy the Combatant (used in the Configure Activity, but not for viewing the saved Combatants

//    private FactionFightableList fightableList_Master; // The master version of the list
//    //    private FactionFightableList combatantList_Display; // The version of the list that will be used for display (will take into account filtering from the search)
//    private ArrayList<Integer> fightableFilteredIndices; // The indices in fightableList_Master that contain the given filter string
//    private FactionFightableList fightableList_Memory; // A memory version of the list, to see what changes have occurred

    private AllFactionFightableLists fightableList_Master; // The master version of the list
    private AllFactionFightableLists fightableList_Memory; // A memory version of the list, to see what changes have occurred
    private ArrayList<ArrayList<Integer>> fightableFilteredIndices; // The indices in fightableList_Master that contain the given filter string

    boolean expectingReturnFightable; // Is the adapter expected to return a Combatant (therefore, is it allowed to do multiselect)?
    boolean isMultiSelecting = false; // Is the adapter currently in multiselect mode
    ArrayList<Integer> iconResourceIds; // A list of resource ID's of the icons that will be used for each Combatant

    private String filteredText = ""; // The string that is currently being used to filter the list
//    private String filteredText_Memory = ""; // The string that was last used to filter the list, before the last call to notifyCombatantsChanged

//    public ListFightableRecyclerAdapter(MasterFightableKeeper parent, Context context, FactionFightableList combatantList) {
//        this.parent = parent;
//        this.fightableList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = combatantList.clone(); // COPY the main list for these two lists, so that the master is not changed
//        this.fightableList_Memory = combatantList.clone();
//
//        setupIconResourceIDs(context);
//        initializeCombatantFilteredIndices();
//    }

//    public ListFightableRecyclerAdapter(MasterFightableKeeper parent, AllFactionFightableLists combatantList) {
//        this.parent = parent;
//        this.fightableList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = combatantList.clone(); // COPY the main list for these two lists, so that the master is not changed
//        this.fightableList_Memory = combatantList.clone();
//
//        clearMultiSelect();
//        setupIconResourceIDs();
//        fightableFilteredIndices = fightableList_Master.getIndicesThatMatch(filteredText);
//    }
//
//    public ListFightableRecyclerAdapter(MasterFightableKeeper parent, AllFactionFightableLists combatantList, boolean adapterCanModify) {
//        this.parent = parent;
//        this.adapterCanModify = adapterCanModify;
//        this.fightableList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = new FactionFightableList(combatantList); // COPY the main list for these two lists, so that the master is not changed
//        this.fightableList_Memory = new AllFactionFightableLists(combatantList);
//
//        clearMultiSelect();
//        setupIconResourceIDs();
//        fightableFilteredIndices = fightableList_Master.getIndicesThatMatch(filteredText);
//    }

//    public ListFightableRecyclerAdapter(MasterFightableKeeper parent, AllFactionFightableLists combatantList, boolean adapterCanModify, boolean adapterCanCopy) {
//        this.parent = parent;
//        this.adapterCanModify = adapterCanModify;
//        this.adapterCanCopy = adapterCanCopy;
//        this.fightableList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = new FactionFightableList(combatantList); // COPY the main list for these two lists, so that the master is not changed
//        this.fightableList_Memory = new AllFactionFightableLists(combatantList);
//
//        clearMultiSelect();
//        setupIconResourceIDs();
//        fightableFilteredIndices = fightableList_Master.getIndicesThatMatch(filteredText);
//    }
//
//    public ListFightableRecyclerAdapter(MasterFightableKeeper parent, AllFactionFightableLists combatantList, boolean adapterCanModify, boolean adapterCanCopy, boolean canMultiSelect) {
//        this.parent = parent;
//        this.adapterCanModify = adapterCanModify;
//        this.adapterCanCopy = adapterCanCopy;
//        this.canMultiSelect = canMultiSelect;
//        this.fightableList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = new FactionFightableList(combatantList); // COPY the main list for these two lists, so that the master is not changed
//        this.fightableList_Memory = new AllFactionFightableLists(combatantList);
//
//        clearMultiSelect();
//        setupIconResourceIDs();
//        fightableFilteredIndices = fightableList_Master.getIndicesThatMatch(filteredText);
//    }

    public ListFightableRecyclerAdapter(MasterFightableKeeper parent, AllFactionFightableLists combatantList, LFRAFlags flags) {
        // TODO OLD: I think there is a problem having this object hold onto the parent object (can't be serialized...?)
        this.parent = parent;
        this.adapterCanModify = flags.adapterCanModify;
        this.adapterCanCopy = flags.adapterCanCopy;
        this.expectingReturnFightable = flags.mustReturnCombatant;
        this.fightableList_Master = combatantList; // Save the reference (master will be modified directly)
//        this.combatantList_Display = new FactionFightableList(combatantList); // COPY the main list for these two lists, so that the master is not changed
        this.fightableList_Memory = combatantList.clone();

//        clearMultiSelect(); // More trouble than it's worth...
        setupIconResourceIDs();
        fightableFilteredIndices = fightableList_Master.getIndicesThatMatch(filteredText);

    }

    private void setupIconResourceIDs() {
        // Preload a list of resources that will be used to load svg's into the grid
        iconResourceIds = new ArrayList<>();
        int curNum = 0;
        while (true) {
            // Generate filenames for every icon that we will use in order, and check if it exists
            String resourceName = String.format(Locale.US, "icon_%02d", curNum); // Oh, the horror...
            int id = parent.getContext().getResources().getIdentifier(resourceName, "drawable", parent.getContext().getPackageName());

            if (id > 0) {
                // If the id is valid
                iconResourceIds.add(id);
            } else {
                // If the id is invalid (equal to 0), then there are no more icons to load
                break;
            }

            curNum++;
        }
    }

    public void clearMultiSelect() {
        if (expectingReturnFightable) {
            // Initialize the multi-select to have no selections
            // Re-initialize the isSelected list to be all false
            for (int i = 0; i < fightableList_Master.size(); i++) {
                fightableList_Master.get(i).setSelected(false); // Set each Combatant to be deselected
            }

            // Now notify the adapter that we have changed the selection status (this will update isMultiSelecting)
            notifyFightableListChanged();

            parent.notifyIsMultiSelecting(isMultiSelecting);
        }
    }

    public void updateMultiSelectStatus() {
        if (expectingReturnFightable) {
            // Update the isMultiSelecting variable based on the current values in isSelectedList
            isMultiSelecting = false;
            for (int i = 0; i < fightableList_Master.size(); i++) {
                if (fightableList_Master.get(i).isSelected()) {
                    isMultiSelecting = true;
                    break;
                }
            }

            // Update any Activity/fragment GUI elements that are related to multi-select
            parent.notifyIsMultiSelecting(isMultiSelecting);
        }
    }

//    private void initializeCombatantFilteredIndices() {
////                 If there is something in the filter string, then filter out the combatants based on the string
////                combatantList_Display = new FactionFightableList(fightableList_Master.faction());
//
//        fightableFilteredIndices = new ArrayList<>();
//        for (int combatantInd = 0; combatantInd < fightableList_Master.size(); combatantInd++) {
//            if (filteredText.isEmpty() || fightableList_Master.get(combatantInd).getName().toLowerCase().contains(filteredText)) {
//                // If this Combatant's name contains the string (or the string is empty), then include it in the display results
////                        combatantList_Display.add(fightableList_Master.get(combatantInd).cloneUnique());
//                fightableFilteredIndices.add(combatantInd);
//            }
//        }
//    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    static class bindableVH extends RecyclerView.ViewHolder implements Serializable {
        public bindableVH(@NonNull View itemView) {
            super(itemView);
        }

        void bind(int position) {
        }
    }

    class CombatantViewHolder extends bindableVH implements Serializable {

        int combatantInd = UNSET;

        View itemView;
        TextView NameView;
        ImageButton CombatantRemove;
        ImageButton CombatantChangeCombatant;
        ImageButton CombatantCopy;
        ImageView CombatantIcon;
        ConstraintLayout CombatantIconBorder;
        ConstraintLayout CombatantMultiSelect;

        // Difference between Add and configure:
        //      1. Configure will have gear button to change aspects of Combatant
        //      2. Remove will be different - Configure will be to remove from the list, add will be to remove from the file
        //          Note that this difference in removal behavior may be accomplished just through the RecyclerAdapter (i.e. where the list gets sent after the fragment this recycler is in closes)
        // Current game-plan:
        //  Use the SAME viewholder for both add and configure.  Fragment will do different things with the final list that this recycler is viewing/modifying (configure: return it to the main activity, add: save the list to file and return a single Combatant)
        //  For Add, perhaps double-check with user if they want to save the modified list to file?  Either at Combatant modification or when fragment is returning Combatant (in this case, this adapter doesn't need to differentiate add from configure)
        //
        // On second thought, figure out way to not have gear show up for Add Combatant version?

        public CombatantViewHolder(@NonNull final View itemView) {
            // TODO: Make a viewholder for CombatantGroups
            super(itemView);

            this.itemView = itemView;
            NameView = itemView.findViewById(R.id.combatant_mod_name);
            CombatantRemove = itemView.findViewById(R.id.combatant_mod_remove);
            CombatantChangeCombatant = itemView.findViewById(R.id.combatant_mod_change_combatant);
            CombatantIcon = itemView.findViewById(R.id.combatant_mod_icon);
            CombatantIconBorder = itemView.findViewById(R.id.combatant_mod_icon_border);
            CombatantCopy = itemView.findViewById(R.id.combatant_mod_copy);
            CombatantMultiSelect = itemView.findViewById(R.id.multi_select_pane);

            // Set up click functionality for rest of viewHolder (i.e. name, "itemView" [the background], and the icon)
            View.OnClickListener returnCombatantListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (expectingReturnFightable) {
                        // If we are expecting a Combatant to return, then figure out what the user is trying to do (otherwise, ignore the click)
                        if (isMultiSelecting) {
                            // If we are currently multi-selecting, then we just want to toggle this Combatant's isSelected status, and update any GUI elements
                            boolean newIsSelected = !displayList().get(posToCombatantInd(getAdapterPosition())).isSelected(); // Toggle the current value

                            // Update the master list
                            displayList().get(posToCombatantInd(getAdapterPosition())).setSelected(newIsSelected);

                            // Update the GUI, and notify the adapter
                            notifyFightableListChanged();
                        } else {
                            if (parent != null) {
                                // Get the current position in the adapter, use it to find the Combatant position in fightableList_Master (taking into account the banners), use that to find this Combatant in the master list (taking into account the filter string with "subList()"), and make a unique clone of it to send back to the parent (phew...)
                                parent.receiveChosenFightable(displayList().get(posToCombatantInd(getAdapterPosition())).cloneUnique());
                            }
                        }
                    }
                }
            };

            CombatantIcon.setOnClickListener(returnCombatantListener);
            NameView.setOnClickListener(returnCombatantListener);
            itemView.setOnClickListener(returnCombatantListener);

            if (expectingReturnFightable) {
                // If this adapter can multi-select, set up the interface with the Viewholders
                View.OnLongClickListener multiSelectStartListener = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (expectingReturnFightable) {
                            if (!isMultiSelecting) {
                                // If we can multi-select, but we aren't multi-selecting right now, then the user wants to start multi-selecting, and also select this Combatant
                                // Update the value of isSelectedList
                                displayList().get(posToCombatantInd(getAdapterPosition())).setSelected(true);

                                // Let the adapter know that this has become selected
                                notifyFightableListChanged();
                                return true; // Let Android know that we handled this click here, so we don't need to activate the standard onClickListener
                            }
                        }

                        return false; // Nothing happened, to go back to the standard onClickListener
                    }
                };

                CombatantIcon.setOnLongClickListener(multiSelectStartListener);
                NameView.setOnLongClickListener(multiSelectStartListener);
                itemView.setOnLongClickListener(multiSelectStartListener);
            }

            if (adapterCanModify) {
                // If the adapter can modify the Combatants/the Combatant list, then allow the user to do so through these buttons
                CombatantChangeCombatant.setVisibility(View.VISIBLE);
                CombatantRemove.setVisibility(View.VISIBLE);

                CombatantChangeCombatant.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FragmentManager fm = scanForActivity(view.getContext()).getSupportFragmentManager();
                        Bundle returnBundle = new Bundle(); // Put information into this Bundle so that we know where to put the new Combatant when it comes back
                        returnBundle.putInt(CreateOrModCombatant.MODIFY_COMBATANT_LOCATION, posToCombatantInd(getAdapterPosition()));
//                        CreateOrModCombatant newDiag = CreateOrModCombatant.newInstance(ListFightableRecyclerAdapter.this, fightableList_Master.subListVisible(fightableFilteredIndices).get(posToCombatantInd(getAdapterPosition())), fightableList_Master, returnBundle); // Make a clone of this Combatant (such that the ID is the same, so it gets put back in the same spot when it returns)
                        Fightable thisFightable = fightableList_Master.getFromVisible(posToCombatantInd(getAdapterPosition()), fightableFilteredIndices);
                        if (thisFightable instanceof Combatant) {
                            CreateOrModCombatant newDiag = CreateOrModCombatant.newInstance(ListFightableRecyclerAdapter.this, (Combatant) thisFightable, fightableList_Master, returnBundle); // Make a clone of this Combatant (such that the ID is the same, so it gets put back in the same spot when it returns)
                            newDiag.show(fm, "CreateOrModCombatant");
                        } else {
                            Log.e(TAG, "CombatantViewHolder CombatantChangeCombatant got a non-Combatant Fightable!");
                        }
                    }
                });

                CombatantRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Ask the user if they definitely want to remove the Combatant
                        new AlertDialog.Builder(itemView.getContext())
                                .setTitle(R.string.confirm_delete)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Remove this from the Combatant list
                                        removeFightable(posToCombatantInd(getAdapterPosition()));
                                    }
                                })
                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Do nothing
                                    }
                                })
                                .show();
                    }
                });
            } else {
                // If we cannot modify the combatants/the Combatant list, then remove the options
                CombatantChangeCombatant.setVisibility(View.GONE);
                CombatantRemove.setVisibility(View.GONE);
            }

            if (adapterCanCopy) {
                CombatantCopy.setVisibility(View.VISIBLE);
                CombatantCopy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Ask the user if they definitely want to copy the Combatant
                        new AlertDialog.Builder(itemView.getContext())
                                .setTitle(R.string.confirm_copy)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Copy this in the Combatant list
                                        copyCombatant(posToCombatantInd(getAdapterPosition()));
                                    }
                                })
                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Do nothing
                                    }
                                })
                                .show();
                    }
                });

            } else {
                // If we can't copy the Combatant, then make the button disappear
                CombatantCopy.setVisibility(View.GONE);
            }
        }

        public void bind(int position) {
            // Get the corresponding Fightable ind
            combatantInd = posToCombatantInd(position);

            // Make sure the corresponding Fightable is actually a Combatant
            Fightable thisFightable = fightableList_Master.getFromVisible(combatantInd, fightableFilteredIndices);
            if (thisFightable instanceof Combatant) {
                Combatant thisCombatant = (Combatant) thisFightable;
                //            Combatant thisCombatant = fightableList_Master.subListVisible(fightableFilteredIndices).get(combatantInd);

                // Make sure that the Combatant is not selected
                setSelected(thisCombatant.isSelected());

                // Load the icon image
                setIcon(thisCombatant.getIconIndex());

                // Set the color of the icon and the icon's border
                setFaction(thisCombatant.getFaction());

                setName(thisCombatant.getName());
            } else {
            Log.e(TAG, "CombatantViewHolder bind got a non-Combatant Fightable!");
            }

        }

        // Methods to update parts of the ViewHolder
        public void setName(String name) {
            // TO_DO: Display the name differently based on the filter text
//            if (filteredText.isEmpty()) {
//                // If the filter text is blank, then just display the name as is
                NameView.setText(name);
//            } else {
//                // If the filter text is NOT blank, then bold the relevant part of the name
//                // First, find the beginning and end of all occurrences of the string
//                SpannableStringBuilder spannable = new SpannableStringBuilder(name);
//                StyleSpan span = new StyleSpan(BOLD);
//                int filteredTextIndex = name.indexOf(filteredText);
//                if (filteredTextIndex != -1) {
//                    // If the string appears in the name, then make it bold
//                    spannable.setSpan(span, filteredTextIndex, filteredTextIndex + filteredText.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
//
//                    for (int i = -1; (i = name.indexOf(filteredText, i + 1)) != -1; i++) {
//                        // Find all occurrences forward
//                        spannable.setSpan(span, i, i + filteredText.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    }
//                }
//
//                NameView.setText(spannable);
//            }
        }

        public void setFaction(Fightable.Faction faction) {
            int colorId = -1;
            switch (faction) {
                case Group:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorParty);
                case Party:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorParty);
                    break;
                case Enemy:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorEnemy);
                    break;
                case Neutral:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorNeutral);
            }

            CombatantIcon.setImageTintList(ColorStateList.valueOf(colorId));
            CombatantIconBorder.setBackgroundColor(colorId);
        }

        public void setIcon(int iconIndex) {
            if (iconIndex == 0) {
                // If the icon index is 0, then the icon is blank
                CombatantIcon.setImageResource(android.R.color.transparent);
            } else {
                // Otherwise, get the corresponding icon image
                CombatantIcon.setImageDrawable(CombatantIcon.getContext().getDrawable(iconResourceIds.get(iconIndex - 1)));
            }
        }

        public void setSelected(boolean isSelected) {
            // This will get called by onBindViewHolder via payload in the event that all must be deselected (if the Combatant list gets modified in ANY WAY)
            // Set the visibility of the multi-select pane based on the input
            final int visibility;
            if (expectingReturnFightable && isSelected) {
                // If we can multi-select in this list, AND this Combatant is selected, then make the multi-select pane visible
                visibility = View.VISIBLE;
            } else {
                visibility = View.GONE;
            }

            // Update the GUI
            CombatantMultiSelect.setVisibility(visibility);
        }
    }

    private static AppCompatActivity scanForActivity(Context cont) {
        if (cont == null)
            return null;
        else if (cont instanceof AppCompatActivity)
            return (AppCompatActivity) cont;
        else if (cont instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper) cont).getBaseContext());

        return null;
    }

    class FactionBannerViewHolder extends bindableVH implements Serializable{
        TextView FactionName;

        public FactionBannerViewHolder(@NonNull View itemView) {
            super(itemView);
            FactionName = itemView.findViewById(R.id.faction_name);
        }

        @Override
        void bind(int position) {
            int bannerInd = posToCombatantInd(position);
            // Position here will just indicate which Faction in fightableList_Master this banner represents
            int textInd = R.string.party_header;
            switch (bannerInd) {
                case -1:
                    textInd = R.string.group_header;
                    break;
                case -2:
                    textInd = R.string.party_header;
                    break;
                case -3:
                    textInd = R.string.enemy_header;
                    break;
                case -4:
                    textInd = R.string.neutral_header;
                    break;
            }

            FactionName.setText(textInd);
        }
    }

    private int posToCombatantInd(int position) {
        // Convert any adapter position to an index in fightableList_Master (adapter position will include banners as well as filter text)
        return displayList().posToFightableInd(position);
    }

    private AllFactionFightableLists displayList() {
        // Return a shallow copy of the list that is being displayed (including information about filter text and visibility status)
        return fightableList_Master.subListVisible(fightableFilteredIndices);
    }

    @Override
    public void notifyCombatantChanged(Bundle returnBundle) {
        // Receiving notification from the CreateOrModCombatant Fragment that the Combatant we sent to be modified has finished being changed
//        // If we got a returnBundle with a list position, then that Combatant needs to be replaced with the new one
//        if (returnBundle != null) {
//            // The return bundle states the
//            replaceCombatant(returnBundle.getInt(COMBATANT_LIST_POSITION), newCombatant);
//        } else {
//            // If we don't a bundle back (which really shouldn't happen), then something has gone horribly wrong
//            Log.e(TAG, "Did not get returnBundle from CreateOrModCombatant Fragment");
//        }

        // Let the Adapter know that the Combatant list has been changed
        notifyFightableListChanged();
    }

    @Override
    public void receiveCombatant(Combatant newCombatant, Bundle returnBundle) {
        // TODO: May need to convert this so that it receives any Fightable.  Treats them identically, or adds them in specific ways?
        // If a Combatant was MODIFIED

        if (returnBundle.containsKey(CreateOrModCombatant.MODIFY_COMBATANT_LOCATION)) {
            boolean addNewCombatant = true;
//            boolean doNotModOtherCombatants = false;
            int modCombatantLocation = returnBundle.getInt(CreateOrModCombatant.MODIFY_COMBATANT_LOCATION); // Get the location of the Combatant being modified
            // NOTE: This location is relative to fightableList_Master(fightableFilteredIndices).

            Fightable originalFightable = displayList().get(modCombatantLocation);
            if (originalFightable instanceof Combatant) {
                Combatant originalCombatant = (Combatant) originalFightable; // The original version of the recently modified Combatant
                fightableList_Master.remove(originalCombatant); // Get the Combatant referred to by the modCombatantLocation and remove it (easiest to just remove the Combatant and add it again (the add function will take care of any "smart naming" needs))

                // Add a new Combatant to master list
                if (fightableList_Master.containsName(newCombatant.getName())) {
                    // If the new name is already used by ANOTHER Combatant in the master list, then deal with it in different ways
                    Fightable existingFightableWithNewName = fightableList_Master.getFightable(newCombatant.getName());
                    if (existingFightableWithNewName instanceof Combatant) {
                        Combatant existingCombatantWithNewName = (Combatant) existingFightableWithNewName;
                        if (existingCombatantWithNewName.isVisible()) {
                            // If there is an exact match of this new Combatant's name to an existing VISIBLE Combatant, then just add the new Combatant back in without the new name, copying everything that was changed EXCEPT for the name
                            newCombatant.setName(originalCombatant.getName()); // Set the Combatant's name to its old name

                            // Let the user know that they screwed up
                            Toast.makeText(parent.getContext(), parent.getContext().getString(R.string.name_already_used, existingCombatantWithNewName.getName()), Toast.LENGTH_SHORT).show();
//
//                    // Don't modify the names of the other Combatants, only rename this one
//                    doNotModOtherCombatants = true; // This should probably *always* be true in this situation, but I don't want to break things if I'm wrong...
                        } else {
                            // If the Combatant that this modification would be replacing is invisible, then things...get a bit complicated
                            // existingCombatantWithName existed before, and we're trying to add it back again, and this is basically a way to do that
                            // We need to conserve the ID of the existing Combatant, and the new modified values of the new Combatant (as well as the name, which is the same between the two)
                            existingCombatantWithNewName.displayCopy(newCombatant); // Update the old Combatant with any new information
                            existingCombatantWithNewName.setVisible(true); // Make the old Combatant visible
                            addNewCombatant = false; // We don't need to add another Combatant, we already "added" one (made it visible)
                            fightableList_Master.sortAllLists(); // Sort the lists, since we just modified a Combatant (possibly changing its name)
                        }
                    }
                }

                // If everything checks out, add the newly modified Combatant back in
                if (addNewCombatant) {
                    fightableList_Master.addFightable(newCombatant, true, true); // A modified Combatant will always be "correct" and won't force a rename of other Combatants, unless the new name is an *exact* match to an existing Combatant (visible or invisible)
                }

                // Let the Adapter know that we have modified the Combatant list
                clearMultiSelect(); // Clear the multi-select list
                notifyFightableListChanged();
            } else {
                Log.e(TAG, "LightFightableRecyclerAdapter receiveCombatant got a nonCombatant Fightable!");
            }

        } else {
            Log.e(TAG, "No modification location was provided.");
        }
    }

    private void removeFightable(int position) {
        // Tell the parent to remove a Combatant in the list, based on the current position in the displayed List
        AllFactionFightableLists test =  displayList();
        if (parent.safeToDelete(displayList().get(position))) {
            // If the parent says we can fully delete this Combatant, then do so
            fightableList_Master.remove(displayList().get(position));
        } else {
            // If the Combatant is not safe to delete, then make it invisible
            displayList().get(position).setVisible(false); // Make the Combatant invisible (can be made visible again by adding a new Combatant with the same name)
        }

        // Let the Adapter know that we have modified the Combatant list
        clearMultiSelect(); // Clear the multi-select list
        notifyFightableListChanged();
    }

    public void copyCombatant(int position) {
        // Copy the Combatant at the given position
        Fightable newFightable = displayList().get(position).cloneUnique(); // Create a clone fo the Combatant to copy, but with a unique ID
        if (newFightable instanceof Combatant) {
            // Add the new Combatant to the Combatant List
            addFightable(newFightable);
        }
    }

    public void addFightable(final Fightable newFightable) {
        // Add the new Combatant (should already be unique)
        boolean result = fightableList_Master.addFightable(newFightable);

        if (!result) {
            // If result is false, then it must mean that we are adding a Combatant that is an exact match to an existing invisible (previously deleted) Combatant
            // Ask for user input
            new AlertDialog.Builder(parent.getContext())
                    .setTitle(R.string.add_combatant_copy_check_title)
                    .setMessage(R.string.add_combatant_copy_check_message)
                    .setPositiveButton(R.string.copy_combatant, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // The user wants to copy this Combatant as a new version
                            fightableList_Master.addFightable(newFightable, false, true);

                            // Clean up
                            clearMultiSelect(); // Clear the multi-select list
                            notifyFightableListChanged();

                            // Let the user know that any new version of this Combatant will be assumed to be copies
                            Toast.makeText(parent.getContext(), R.string.copy_combatant_warning, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(R.string.resurrect_combatant, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // The user wants to resurrect the old version of the Combatant
                            fightableList_Master.addFightable(newFightable, true, true);

                            // Clean up
                            clearMultiSelect(); // Clear the multi-select list
                            notifyFightableListChanged();
                        }
                    })
                    .show();

            // Further interaction will occur in the dialog listeners
            return;
        }

        // If the addition went smoothly (which it will 99% of the time), then clean up
        clearMultiSelect(); // Clear the multi-select list
        notifyFightableListChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ListFightableRecyclerAdapter.bindableVH holder, int position, @NonNull List<Object> payloads) {
        if (holder instanceof CombatantViewHolder) {
            if (!payloads.isEmpty()) {
                // If the payload is not empty
                if (payloads.get(0) instanceof Bundle) {
                    // If the payload is a Bundle
                    Bundle args = (Bundle) payloads.get(0);
//                if (args.containsKey(SET_MULTI_SELECT)) {
//                    // We need to adjust the selection status of this Combatant
//                    if (holder instanceof CombatantViewHolder) {
//                        // For this payload to make sense, the holder must be a CombatantViewHolder
//                        boolean newSelectionState = args.getBoolean(SET_MULTI_SELECT);
//                        ((CombatantViewHolder) holder).selectCombatant(newSelectionState);
//                    }
//                }
                    for (String key : args.keySet()) {
                        if (key.equals("Name")) {
                            ((CombatantViewHolder) holder).setName(args.getString(key));
                        }
                        if (key.equals("Faction")) {
                            Fightable.Faction fac = (Fightable.Faction) args.getSerializable(key);
                            if (fac != null) {
                                ((CombatantViewHolder) holder).setFaction(fac);
                            }
                        }
                        if (key.equals("Icon")) {
                            ((CombatantViewHolder) holder).setIcon(args.getInt(key));
                        }
                        if (key.equals("Selected")) {
                            ((CombatantViewHolder) holder).setSelected(args.getBoolean(key));
                        }
                    }
                }
                return; // The payload was not empty, so don't bother going on to the the non-payload onBindViewHolder
            }
        }

        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemViewType(int position) {
        // TODO: GROUP - Add logic here to determine which viewholder to use (Combatant, CombatantGroup, etc.)
        int combatantInd = posToCombatantInd(position); // Get the position in the Combatant list (negative numbers indicate a banner view)
        if (combatantInd >= 0) {
            return COMBATANT_VIEW;
        } else {
            return BANNER_VIEW;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull bindableVH holder, int position) {
        holder.bind(position);
    }

    @NonNull
    @Override
    public bindableVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == COMBATANT_VIEW) {
            View view = inflater.inflate(R.layout.combatant_item_mod, parent, false);
            return new CombatantViewHolder(view);
        } else if (viewType == BANNER_VIEW) {
            View view = inflater.inflate(R.layout.faction_banner, parent, false);
            return new FactionBannerViewHolder(view);
        } else {
            Log.e(TAG, "Got illegal viewType");
            return new bindableVH(new View(parent.getContext()));
        }
    }

    public ArrayList<Fightable> getAllSelectedFightables() {
        // Return an ArrayList of all of the currently selected Combatants
        ArrayList<Fightable> returnList = new ArrayList<>();
        for (int i = 0; i < fightableList_Master.size(); i++) {
            if (fightableList_Master.get(i).isSelected()) {
                // If this Combatant is selected, then return it
                returnList.add(fightableList_Master.get(i));
            }
        }

        // Return the List
        return returnList;
    }


    @Override
    public int getItemCount() {
//        return combatantList.size();
        return fightableList_Master.subListVisible(fightableFilteredIndices).sizeWithBanners(); // Get the size of the post-filtering list, including the banners
    }

    private void notifyFightableListChanged() {
//        // Update the list of combatants to be displayed, taking into account the current filter string
//        initializeCombatantFilteredIndices();
        fightableFilteredIndices = fightableList_Master.getIndicesThatMatch(filteredText);

        // If anything about the combatants has changed (specifically the viewed version of the list according to fightableFilteredIndices), see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantFilteredDiffUtil(fightableList_Memory, fightableList_Master.subListVisible(fightableFilteredIndices)));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the memory list
        fightableList_Memory = fightableList_Master.subListVisible(fightableFilteredIndices).clone();

        // Make sure that the multi-select status is updated
        updateMultiSelectStatus();

        // Let the parent know that the Combatant List changed (maybe)
        parent.notifyFightableListChanged();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                filteredText = charSequence.toString().toLowerCase();

                //                results.values = combatantList_Display;
//                results.values = fightableFilteredIndices;
                return new FilterResults();
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
//                combatantList_Display = (FactionFightableList) filterResults.values;
//                fightableFilteredIndices = (ArrayList<Integer>) filterResults.values;
                notifyFightableListChanged();
            }
        };
    }

    public AllFactionFightableLists getCombatantList() {
        return fightableList_Master;
    }

    public void setCombatantList(AllFactionFightableLists combatantList_Master) {
        this.fightableList_Master = combatantList_Master;
        clearMultiSelect();
        notifyFightableListChanged();
    }

    // The interface calling this adapter MUST have control of a master list of combatants such that it can judge a Combatant's name to be mutually exclusive
    interface MasterFightableKeeper extends Serializable {
        // TODO: Update all uses of these functions in other places (has not been updated yet as of this TODO item)
        void receiveChosenFightable(Fightable selectedFightable); // Receive selected Fightable back from this Adapter

        Context getContext(); // Get Context from the calling Activity/Fragment

        void notifyFightableListChanged(); // Let the parent know that the Combatant list changed, so it can update any views (such as the "no combatants" view)

        void notifyIsMultiSelecting(boolean isMultiSelecting); // Let the parent know that the multi-selecting status may have changed, so we may need to update the GUI

        boolean safeToDelete(Fightable fightable); // Is a given Combatant safe to delete (does not appear in the EncounterCombatantList), or should it simply be turned invisible (appears in the list)

        // Note: No methods associated with modifying the FactionFightableList, because any modifications that occur (through the "gear" button) are consistent between this adapter and other locations (because Java passes references...hopefully)
    }

    // A simple class that holds onto a bunch of input parameters for the ListFightableRecyclerAdapter.  Really only exists because having 3+ flags input to the constructor IN ADDITION to a bunch of other stuff just kinda makes me sad...
    static public class LFRAFlags implements Serializable {
        public boolean adapterCanModify = false;
        public boolean adapterCanCopy = false;
        public boolean mustReturnCombatant = false;
    }
}
