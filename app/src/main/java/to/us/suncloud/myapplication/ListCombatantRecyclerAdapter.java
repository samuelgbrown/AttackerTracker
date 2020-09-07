package to.us.suncloud.myapplication;

import android.app.Activity;
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
import java.util.jar.Attributes;

public class ListCombatantRecyclerAdapter extends RecyclerView.Adapter<ListCombatantRecyclerAdapter.bindableVH> implements Filterable, CreateOrModCombatant.receiveNewOrModCombatantInterface {
    private static final int UNSET = -1;

    public static final int COMBATANT_VIEW = 0;
    public static final int BANNER_VIEW = 1;

    public static final String SET_MULTI_SELECT = "setMultiSelect";

    private static final String TAG = "ListCombatantRecycler";

    MasterCombatantKeeper parent = null; // If this is set, then the selected combatant will be sent to the parent
    boolean adapterCanModify = false; // Can the adapter modify the Combatant (used so that we can use this adapter for both Combatant display and modify+display purposes, because they are VERY similar)
    boolean adapterCanCopy = false; // Can the adapter copy the Combatant (used in the Configure Activity, but not for viewing the saved Combatants

//    private FactionCombatantList combatantList_Master; // The master version of the list
//    //    private FactionCombatantList combatantList_Display; // The version of the list that will be used for display (will take into account filtering from the search)
//    private ArrayList<Integer> combatantFilteredIndices; // The indices in combatantList_Master that contain the given filter string
//    private FactionCombatantList combatantList_Memory; // A memory version of the list, to see what changes have occurred

    private AllFactionCombatantLists combatantList_Master; // The master version of the list
    private AllFactionCombatantLists combatantList_Memory; // A memory version of the list, to see what changes have occurred
    private ArrayList<ArrayList<Integer>> combatantFilteredIndices; // The indices in combatantList_Master that contain the given filter string

    boolean expectingReturnCombatant; // Is the adapter expected to return a Combatant (therefore, is it allowed to do multiselect)?
    boolean isMultiSelecting = false; // Is the adapter currently in multiselect mode TODO: Will need to let the Fragment know, so we can have a "select chosen Combatants" button appear".  That button will then need to be able to talk to the adapter so it can send stuff back (or it can just take the Combatant list, huh...)
    // TODO: For containing Fragment, onBackPressed should take this out of multiselect mode if it's in it (otherwise, just dismiss)
    ArrayList<Integer> iconResourceIds; // A list of resource ID's of the icons that will be used for each Combatant

    private String filteredText = ""; // The string that is currently being used to filter the list
//    private String filteredText_Memory = ""; // The string that was last used to filter the list, before the last call to notifyCombatantsChanged

//    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, Context context, FactionCombatantList combatantList) {
//        this.parent = parent;
//        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = combatantList.clone(); // COPY the main list for these two lists, so that the master is not changed
//        this.combatantList_Memory = combatantList.clone();
//
//        setupIconResourceIDs(context);
//        initializeCombatantFilteredIndices();
//    }

//    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, AllFactionCombatantLists combatantList) {
//        this.parent = parent;
//        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = combatantList.clone(); // COPY the main list for these two lists, so that the master is not changed
//        this.combatantList_Memory = combatantList.clone();
//
//        clearMultiSelect();
//        setupIconResourceIDs();
//        combatantFilteredIndices = combatantList_Master.getIndicesThatMatch(filteredText);
//    }
//
//    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, AllFactionCombatantLists combatantList, boolean adapterCanModify) {
//        this.parent = parent;
//        this.adapterCanModify = adapterCanModify;
//        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = new FactionCombatantList(combatantList); // COPY the main list for these two lists, so that the master is not changed
//        this.combatantList_Memory = new AllFactionCombatantLists(combatantList);
//
//        clearMultiSelect();
//        setupIconResourceIDs();
//        combatantFilteredIndices = combatantList_Master.getIndicesThatMatch(filteredText);
//    }

//    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, AllFactionCombatantLists combatantList, boolean adapterCanModify, boolean adapterCanCopy) {
//        this.parent = parent;
//        this.adapterCanModify = adapterCanModify;
//        this.adapterCanCopy = adapterCanCopy;
//        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = new FactionCombatantList(combatantList); // COPY the main list for these two lists, so that the master is not changed
//        this.combatantList_Memory = new AllFactionCombatantLists(combatantList);
//
//        clearMultiSelect();
//        setupIconResourceIDs();
//        combatantFilteredIndices = combatantList_Master.getIndicesThatMatch(filteredText);
//    }
//
//    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, AllFactionCombatantLists combatantList, boolean adapterCanModify, boolean adapterCanCopy, boolean canMultiSelect) {
//        this.parent = parent;
//        this.adapterCanModify = adapterCanModify;
//        this.adapterCanCopy = adapterCanCopy;
//        this.canMultiSelect = canMultiSelect;
//        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
////        this.combatantList_Display = new FactionCombatantList(combatantList); // COPY the main list for these two lists, so that the master is not changed
//        this.combatantList_Memory = new AllFactionCombatantLists(combatantList);
//
//        clearMultiSelect();
//        setupIconResourceIDs();
//        combatantFilteredIndices = combatantList_Master.getIndicesThatMatch(filteredText);
//    }

    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, AllFactionCombatantLists combatantList, LCRAFlags flags) {
        this.parent = parent;
        this.adapterCanModify = flags.adapterCanModify;
        this.adapterCanCopy = flags.adapterCanCopy;
        this.expectingReturnCombatant = flags.mustReturnCombatant;
        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
//        this.combatantList_Display = new FactionCombatantList(combatantList); // COPY the main list for these two lists, so that the master is not changed
        this.combatantList_Memory = combatantList.clone();

//        clearMultiSelect(); // More trouble than it's worth...
        setupIconResourceIDs();
        combatantFilteredIndices = combatantList_Master.getIndicesThatMatch(filteredText);
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
        if (expectingReturnCombatant) {
            // Initialize the multi-select to have no selections
            // Re-initialize the isSelected list to be all false
            for (int i = 0; i < combatantList_Master.size(); i++) {
                combatantList_Master.get(i).setSelected(false); // Set each Combatant to be deselected
            }

            // Now notify the adapter that we have changed the selection status (this will update isMultiSelecting)
            notifyCombatantListChanged();

            parent.notifyIsMultiSelecting(isMultiSelecting);
        }
    }

    public void updateMultiSelectStatus() {
        if (expectingReturnCombatant) {
            // Update the isMultiSelecting variable based on the current values in isSelectedList
            isMultiSelecting = false;
            for (int i = 0; i < combatantList_Master.size(); i++) {
                if (combatantList_Master.get(i).isSelected()) {
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
////                combatantList_Display = new FactionCombatantList(combatantList_Master.faction());
//
//        combatantFilteredIndices = new ArrayList<>();
//        for (int combatantInd = 0; combatantInd < combatantList_Master.size(); combatantInd++) {
//            if (filteredText.isEmpty() || combatantList_Master.get(combatantInd).getName().toLowerCase().contains(filteredText)) {
//                // If this combatant's name contains the string (or the string is empty), then include it in the display results
////                        combatantList_Display.add(combatantList_Master.get(combatantInd).cloneUnique());
//                combatantFilteredIndices.add(combatantInd);
//            }
//        }
//    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    static class bindableVH extends RecyclerView.ViewHolder {
        public bindableVH(@NonNull View itemView) {
            super(itemView);
        }

        void bind(int position) {}
    }

    class CombatantViewHolder extends bindableVH {

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
        //      1. Configure will have gear button to change aspects of combatant
        //      2. Remove will be different - Configure will be to remove from the list, add will be to remove from the file
        //          Note that this difference in removal behavior may be accomplished just through the RecyclerAdapter (i.e. where the list gets sent after the fragment this recycler is in closes)
        // Current game-plan:
        //  Use the SAME viewholder for both add and configure.  Fragment will do different things with the final list that this recycler is viewing/modifying (configure: return it to the main activity, add: save the list to file and return a single combatant)
        //  For Add, perhaps double-check with user if they want to save the modified list to file?  Either at combatant modification or when fragment is returning combatant (in this case, this adapter doesn't need to differentiate add from configure)
        //
        // On second thought, figure out way to not have gear show up for Add Combatant version?

        public CombatantViewHolder(@NonNull final View itemView) {
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
                    if (expectingReturnCombatant) {
                        // If we are expecting a Combatant to return, then figure out what the user is trying to do (otherwise, ignore the click)
                        if (isMultiSelecting) {
                            // If we are currently multi-selecting, then we just want to toggle this Combatant's isSelected status, and update any GUI elements
                            boolean newIsSelected = !displayList().get(posToCombatantInd(getAdapterPosition())).isSelected(); // Toggle the current value

                            // Update the master list
                            displayList().get(posToCombatantInd(getAdapterPosition())).setSelected(newIsSelected);

                            // Update the GUI, and notify the adapter
                            notifyCombatantListChanged();
                        } else {
                            if (parent != null) {
                                // Get the current position in the adapter, use it to find the Combatant position in combatantList_Master (taking into account the banners), use that to find this Combatant in the master list (taking into account the filter string with "subList()"), and make a unique clone of it to send back to the parent (phew...)
                                parent.receiveChosenCombatant(displayList().get(posToCombatantInd(getAdapterPosition())).cloneUnique());
                            }
                        }
                    }
                }
            };

            CombatantIcon.setOnClickListener(returnCombatantListener);
            NameView.setOnClickListener(returnCombatantListener);
            itemView.setOnClickListener(returnCombatantListener);

            if (expectingReturnCombatant) {
                // If this adapter can multi-select, set up the interface with the Viewholders
                View.OnLongClickListener multiSelectStartListener = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (expectingReturnCombatant) {
                            if (!isMultiSelecting) {
                                // If we can multi-select, but we aren't multi-selecting right now, then the user wants to start multi-selecting, and also select this Combatant
                                // Update the value of isSelectedList
                                displayList().get(posToCombatantInd(getAdapterPosition())).setSelected(true);

                                // Let the adapter know that this has become selected
                                notifyCombatantListChanged();
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
                        CreateOrModCombatant newDiag = CreateOrModCombatant.newInstance(ListCombatantRecyclerAdapter.this, combatantList_Master.subList(combatantFilteredIndices).get(posToCombatantInd(getAdapterPosition())), combatantList_Master, returnBundle); // Make a clone of this Combatant (such that the ID is the same, so it gets put back in the same spot when it returns)
                        newDiag.show(fm, "CreateOrModCombatant");
                    }
                });

                CombatantRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Ask the user if they definitely want to remove the combatant
                        new AlertDialog.Builder(itemView.getContext())
                                .setTitle(R.string.confirm_delete)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Remove this from the combatant list
                                        removeCombatant(posToCombatantInd(getAdapterPosition()));
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
                // If we cannot modify the combatants/the combatant list, then remove the options
                CombatantChangeCombatant.setVisibility(View.GONE);
                CombatantRemove.setVisibility(View.GONE);
            }

            if (adapterCanCopy) {
                CombatantCopy.setVisibility(View.VISIBLE);
                CombatantCopy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Ask the user if they definitely want to copy the combatant
                        new AlertDialog.Builder(itemView.getContext())
                                .setTitle(R.string.confirm_copy)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Copy this in the combatant list
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
            combatantInd = posToCombatantInd(position);
            Combatant thisCombatant = combatantList_Master.subList(combatantFilteredIndices).get(combatantInd);

            // Make sure that the Combatant is not selected
            setSelected(thisCombatant.isSelected());

            // Load the icon image
            setIcon(thisCombatant.getIconIndex());

            // Set the color of the icon and the icon's border
            setFaction(thisCombatant.getFaction());

            // TODO: Display the name differently based on the filter text
//            if (filteredText.isEmpty()) {
//                // If the filter text is blank, then just display the name as is
            setName(thisCombatant.getName());
//            } else {
//                // If the filter text is NOT blank, then bold the relevant part of the name
//                // First, find the beginning and end of all occurrences of the string
//                SpannableStringBuilder spannable = new SpannableStringBuilder(combatantName);
//                StyleSpan span = new StyleSpan(BOLD);
//                int filteredTextIndex = combatantName.indexOf(filteredText);
//                if (filteredTextIndex != -1) {
//                    // If the string appears in the name, then make it bold
//                    spannable.setSpan(span, filteredTextIndex, filteredTextIndex + filteredText.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
//
//                    for (int i = -1; (i = combatantName.indexOf(filteredText, i + 1)) != -1; i++) {
//                        // Find all occurrences forward
//                        spannable.setSpan(span, i, i + filteredText.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    }
//                }
//
//                NameView.setText(spannable);
//            }
        }

        // Methods to update parts of the ViewHolder
        public void setName(String name) {
            NameView.setText(name);
        }

        public void setFaction(Combatant.Faction faction) {
            int colorId = -1;
            switch (faction) {
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
            if (expectingReturnCombatant && isSelected) {
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

    class FactionBannerViewHolder extends bindableVH {
        TextView FactionName;

        public FactionBannerViewHolder(@NonNull View itemView) {
            super(itemView);
            FactionName = itemView.findViewById(R.id.faction_name);
        }

        @Override
        void bind(int position) {
            int bannerInd = posToCombatantInd(position);
            // Position here will just indicate which Faction in combatantList_Master this banner represents
            int textInd = R.string.party_header;
            switch (bannerInd) {
                case -1:
                    textInd = R.string.party_header;
                    break;
                case -2:
                    textInd = R.string.enemy_header;
                    break;
                case -3:
                    textInd = R.string.neutral_header;
                    break;
            }

            FactionName.setText(textInd);
        }
    }

    private int posToCombatantInd(int position) {
        // Convert any adapter position to an index in combatantList_Master (adapter position will include banners as well as filter text)
        return displayList().posToCombatantInd(position);
    }

    private AllFactionCombatantLists displayList() {
        return combatantList_Master.subList(combatantFilteredIndices);
    }

    @Override
    public void notifyCombatantChanged(Bundle returnBundle) {
        // Receiving notification from the CreateOrModCombatant Fragment that the Combatant we sent to be modified has finished being changed
//        // If we got a returnBundle with a list position, then that combatant needs to be replaced with the new one
//        if (returnBundle != null) {
//            // The return bundle states the
//            replaceCombatant(returnBundle.getInt(COMBATANT_LIST_POSITION), newCombatant);
//        } else {
//            // If we don't a bundle back (which really shouldn't happen), then something has gone horribly wrong
//            Log.e(TAG, "Did not get returnBundle from CreateOrModCombatant Fragment");
//        }

        // Let the Adapter know that the combatant list has been changed
        notifyCombatantListChanged();
    }

    @Override
    public void receiveCombatant(Combatant newCombatant, Bundle returnBundle) {
        // If a combatant was MODIFIED
        if (returnBundle.containsKey(CreateOrModCombatant.MODIFY_COMBATANT_LOCATION)) {
            int modCombatantLocation = returnBundle.getInt(CreateOrModCombatant.MODIFY_COMBATANT_LOCATION); // Get the location of the Combatant being modified
            // NOTE: This location is relative to combatantList_Master(combatantFilteredIndices).
            combatantList_Master.remove(displayList().get(modCombatantLocation)); // Get the Combatant referred to by the modCombatantLocation and remove it (easiest to just remove the Combatant and add it again (the add function will take care of any "smart naming" needs))
        }

        // Add a new combatant to master list
        combatantList_Master.addCombatant(newCombatant);

        // Let the Adapter know that we have modified the combatant list
        clearMultiSelect(); // Clear the multi-select list
        notifyCombatantListChanged();
    }

    private void removeCombatant(int position) {
        // Tell the parent to remove a Combatant in the list, based on the current position in the displayed List
        combatantList_Master.remove(displayList().get(position));

        // Let the Adapter know that we have modified the combatant list
        clearMultiSelect(); // Clear the multi-select list
        notifyCombatantListChanged();
    }

    public void copyCombatant(int position) {
        // Copy the Combatant at the given position
        Combatant newCombatant = displayList().get(position).cloneUnique(); // Create a clone fo the Combatant to copy, but with a unique ID

        // Add the new Combatant to the Combatant List
        addCombatant(newCombatant);
    }

    public void addCombatant(Combatant newCombatant) {
        // Add the new Combatant (should already be unique)
        combatantList_Master.addCombatant(newCombatant);

        clearMultiSelect(); // Clear the multi-select list
        notifyCombatantListChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ListCombatantRecyclerAdapter.bindableVH holder, int position, @NonNull List<Object> payloads) {
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
                            Combatant.Faction fac = (Combatant.Faction) args.getSerializable(key);
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

    public ArrayList<Combatant> getAllSelectedCombatants() {
        // Return an ArrayList of all of the currently selected Combatants
        ArrayList<Combatant> returnList = new ArrayList<>();
        // TODO: Implement iterator in AllFactionsCombatantList
        for (int i = 0; i < combatantList_Master.size(); i++) {
            if (combatantList_Master.get(i).isSelected()) {
                // If this Combatant is selected, then return it
                returnList.add(combatantList_Master.get(i));
            }
        }

        // Return the List
        return returnList;
    }


    @Override
    public int getItemCount() {
//        return combatantList.size();
        return combatantList_Master.subList(combatantFilteredIndices).sizeWithBanners(); // Get the size of the post-filtering list, including the banners
    }

    private void notifyCombatantListChanged() {
//        // Update the list of combatants to be displayed, taking into account the current filter string
//        initializeCombatantFilteredIndices();
        combatantFilteredIndices = combatantList_Master.getIndicesThatMatch(filteredText);

        // If anything about the combatants has changed (specifically the viewed version of the list according to combatantFilteredIndices), see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantFilteredDiffUtil(combatantList_Memory, combatantList_Master.subList(combatantFilteredIndices)));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the memory list
        combatantList_Memory = combatantList_Master.subList(combatantFilteredIndices).clone();

        // Make sure that the multi-select status is updated
        updateMultiSelectStatus();

        // Let the parent know that the Combatant List changed (maybe)
        parent.notifyCombatantListChanged();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                filteredText = charSequence.toString().toLowerCase();

                //                results.values = combatantList_Display;
//                results.values = combatantFilteredIndices;
                return new FilterResults();
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
//                combatantList_Display = (FactionCombatantList) filterResults.values;
//                combatantFilteredIndices = (ArrayList<Integer>) filterResults.values;
                notifyCombatantListChanged();
            }
        };
    }

    public AllFactionCombatantLists getCombatantList() {
        return combatantList_Master;
    }

    public void setCombatantList(AllFactionCombatantLists combatantList_Master) {
        this.combatantList_Master = combatantList_Master;
        clearMultiSelect();
        notifyCombatantListChanged();
    }

    // The interface calling this adapter MUST have control of a master list of combatants such that it can judge a Combatant's name to be mutually exclusive
    interface MasterCombatantKeeper extends Serializable {
        // TODO: May be able to get rid of this method?  Just do changes on the master list and notify that the list was modified?  Maybe?
        void receiveChosenCombatant(Combatant selectedCombatant); // Receive selected Combatant back from this Adapter

        Context getContext(); // Get Context from the calling Activity/Fragment

        void notifyCombatantListChanged(); // Let the parent know that the Combatant list changed, so it can update any views (such as the "no combatants" view)

        void notifyIsMultiSelecting(boolean isMultiSelecting); // Let the parent know that the multi-selecting status may have changed, so we may need to update the GUI

        // Note: No methods associated with modifying the FactionCombatantList, because any modifications that occur (through the "gear" button) are consistent between this adapter and other locations (because Java passes references...hopefully)
    }

    // A simple class that holds onto a bunch of input parameters for the ListCombatantRecyclerAdapter.  Really only exists because having 3+ flags input to the constructor IN ADDITION to a bunch of other stuff just kinda makes me sad...
    static public class LCRAFlags {
        public boolean adapterCanModify = false;
        public boolean adapterCanCopy = false;
        public boolean mustReturnCombatant = false;
    }
}
