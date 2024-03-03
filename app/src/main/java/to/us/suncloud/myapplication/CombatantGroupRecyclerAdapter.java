package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import static to.us.suncloud.myapplication.CombatantsInGroupDiffUtil.DIFF_FACTION;
import static to.us.suncloud.myapplication.CombatantsInGroupDiffUtil.DIFF_ICON;
import static to.us.suncloud.myapplication.CombatantsInGroupDiffUtil.DIFF_MULTIPLES;
import static to.us.suncloud.myapplication.CombatantsInGroupDiffUtil.DIFF_MULTISELECT;
import static to.us.suncloud.myapplication.CombatantsInGroupDiffUtil.DIFF_NAME;
import static to.us.suncloud.myapplication.CombatantsInGroupDiffUtil.DIFF_SELECTED;

public class CombatantGroupRecyclerAdapter extends RecyclerView.Adapter<CombatantGroupRecyclerAdapter.CombatantViewHolder> implements GroupCombatantMultPickerFragment.HandleMultPicker {
    private static final String TAG = "GroupCombatantRecycler";

    MasterCombatantGroupKeeper parent; // If this is set, then the selected Combatant will be sent to the parent

    private final AllFactionFightableLists referenceAFFL; // The master version of the list
    private AllFactionFightableLists referenceAFFL_Memory; // A copy of the reference list for calculating view differences
    private final GroupCombatantMultPickerFragment.HandleMultPicker thisAdapter = this;
    private final CombatantGroup thisGroup; // The CombatantGroup that we are viewing/modifying
    private CombatantGroup thisGroup_Memory; // A copy of the Combatant group to keep for calculating view differences

    boolean isMultiSelecting = false; // Is the adapter currently in multiselect mode
    ArrayList<Integer> iconResourceIds; // A list of resource ID's of the icons that will be used for each Combatant

    public CombatantGroupRecyclerAdapter(MasterCombatantGroupKeeper parent, CombatantGroup thisGroup, AllFactionFightableLists referenceAFFL) {
        this.parent = parent;
        this.referenceAFFL = referenceAFFL; // Save the reference

        this.thisGroup = thisGroup;

        // Make copies of the data, to calculate display differences later
        this.thisGroup_Memory = (CombatantGroup) thisGroup.clone();
        this.referenceAFFL_Memory = referenceAFFL.clone();

        setupIconResourceIDs();
        updateMultiSelectStatus();
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
        // Initialize the multi-select to have no selections
        // Re-initialize the isSelected list to be all false
        ArrayList<Combatant> allCurrentCombatants = thisGroup.convertToCombatants(referenceAFFL);
        for ( Combatant combatant : allCurrentCombatants ) {
            combatant.setSelected(false);
        }

        // Now notify the adapter that we have changed the selection status (this will update isMultiSelecting)
        notifyFightableListChanged();

        // Let the Fragment know that we are no longer multi-selecting, to remove any other related buttons in the Dialog
        parent.notifyIsMultiSelecting(isMultiSelecting);
    }

    public void updateMultiSelectStatus() {
        // Update the isMultiSelecting variable based on the current values in isSelectedList
        isMultiSelecting = false;
        ArrayList<Combatant> allCurrentCombatants = thisGroup.convertToCombatants(referenceAFFL);
        for ( Combatant combatant : allCurrentCombatants ) {
            if (combatant.isSelected()) {
                isMultiSelecting = true;
                break;
            }
        }

        // Update any Activity/fragment GUI elements that are related to multi-select
        parent.notifyIsMultiSelecting(isMultiSelecting);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void removeSelectedCombatants() {
        // Use the multi-select tool to delete the selected combatants
        thisGroup.removeSelectedCombatants(referenceAFFL);
        notifyFightableListChanged();
    }

    public void removeAllCombatants() {
        thisGroup.removeAllCombatants();
        notifyFightableListChanged();
    }


    class CombatantViewHolder extends RecyclerView.ViewHolder implements Serializable {
        View itemView;
        TextView NameView;
        ImageButton CombatantRemove; // Only change visibility through setCombatantRemoveVisibility
        ImageButton CombatantCopy; // Only change visibility through setCombatantCopyVisibility
        ImageView CombatantIcon;
        ConstraintLayout CombatantIconBorder;
        ConstraintLayout CombatantMultipleCopiesPane;
        TextView CombatantMultipleCopiesText;
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
            super(itemView);

            this.itemView = itemView;
            NameView = itemView.findViewById(R.id.combatant_mod_name);
            CombatantRemove = itemView.findViewById(R.id.combatant_mod_remove);
            CombatantIcon = itemView.findViewById(R.id.combatant_mod_icon);
            CombatantIconBorder = itemView.findViewById(R.id.combatant_mod_icon_border);
            CombatantCopy = itemView.findViewById(R.id.combatant_mod_copy);
            CombatantMultipleCopiesPane = itemView.findViewById(R.id.multiple_copies_pane);
            CombatantMultipleCopiesText = itemView.findViewById(R.id.multiple_copies_text);
            CombatantMultiSelect = itemView.findViewById(R.id.combatant_multi_select_pane);

            // Set up the interface with the ViewHolders for multi-selecting
            View.OnClickListener toggleSelectedListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isMultiSelecting) {
                        // If we are currently multi-selecting, then we just want to toggle this Combatant's isSelected status, and update any GUI elements
                        boolean newIsSelected = !getThisCombatant().isSelected(); // Toggle the current value

                        // Update the master list
                        getThisCombatant().setSelected(newIsSelected);
                    } else {
                        // If we aren't multi-selecting right now, then the user wants to start multi-selecting, and also select this Combatant
                        // Update the value of isSelectedList
                        getThisCombatant().setSelected(true);
                    }

                    // Update the GUI, and notify the adapter
                    notifyFightableListChanged();
                }
            };

            CombatantIcon.setOnClickListener(toggleSelectedListener);
            NameView.setOnClickListener(toggleSelectedListener);
            itemView.setOnClickListener(toggleSelectedListener);
            CombatantMultiSelect.setOnClickListener(toggleSelectedListener);
            CombatantMultipleCopiesPane.setOnClickListener(toggleSelectedListener);
            CombatantMultipleCopiesText.setOnClickListener(toggleSelectedListener);

            setMultiSelectingMode(isMultiSelecting);
            CombatantRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if ( thisGroup.size() == 1 ) {
                        new AlertDialog.Builder(itemView.getContext())
                                .setTitle(R.string.confirm_delete_group_title)
                                .setMessage(R.string.confirm_delete_group_message_single)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        thisGroup.removeAllCombatants();
                                        parent.dismiss();
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
                        // If we're allowed to remove this Combatant, ask the user if they definitely want to do it
                        int messageID;
                        if ( thisGroup.getNumMultiplesOfCombatant(getAdapterPosition()) > 1 ) {
                            messageID = R.string.confirm_single_remove_from_group_multiple_copies;
                        } else {
                            messageID = R.string.confirm_single_remove_from_group_one_copy;
                        }
                        new AlertDialog.Builder(itemView.getContext())
                                .setTitle(R.string.confirm_remove_combatant_from_group_title)
                                .setMessage(messageID)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Remove this from the Combatant list
                                        thisGroup.removeCombatant(getAdapterPosition(), referenceAFFL);
                                        notifyFightableListChanged();
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
                }
            });

            // Allow user to change the number of multiples of this Combatant in the group
            View.OnClickListener multiplesListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Multiples changes will be handled in recieveCombatantMultiplesValue()
                    if (!isMultiSelecting) {
                        FragmentManager fm = scanForActivity(view.getContext()).getSupportFragmentManager();

                        GroupCombatantMultPickerFragment
                                .newInstance(thisAdapter, getAdapterPosition(), thisGroup.getNumMultiplesOfCombatant(getAdapterPosition()))
                                .show(fm, "GroupCombatantMultPickerFragment");
                    }
                }
            };
            CombatantCopy.setOnClickListener(multiplesListener);
            CombatantMultipleCopiesPane.setOnClickListener(multiplesListener);
            CombatantMultipleCopiesText.setOnClickListener(multiplesListener);

        }

        public void bind(int position) {
            // Make sure the corresponding Fightable is actually a Combatant
            Combatant thisCombatant = getThisCombatant();
            if (thisCombatant != null) {
                // Set information about Combatant
                setSelected(thisCombatant.isSelected());
                setIcon(thisCombatant.getIconIndex());
                setFaction(thisCombatant.getFaction());
                setName(thisCombatant.getName());

                // Set group-specific value(s)
                setMultiples(thisGroup.getNumMultiplesOfCombatant(position));
                setMultiSelectingMode(isMultiSelecting);
            } else {
                Log.e(TAG, "CombatantViewHolder bind got a non-Combatant Fightable! Re-verifying list...");
                thisGroup.verifyGroupAgainstList(referenceAFFL);

                // Notify that the fightable list has changed, in the main thread
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyFightableListChanged();
                    }
                });
            }

        }

        private Combatant getThisCombatant() {
            return thisGroup.getCombatant(referenceAFFL, getAdapterPosition());
        }

        // Methods to update parts of the ViewHolder
        public void setMultiples(int numMultiples) {
            int paneVisibility;
            if ( numMultiples > 1 ) {
                // If we have multiples of this Combatant
                paneVisibility = View.VISIBLE;
            } else {
                paneVisibility = View.GONE;
            }

            CombatantMultipleCopiesPane.setVisibility(paneVisibility);
            CombatantMultipleCopiesText.setText(Integer.toString(numMultiples));
        }

        public void setName(String name) {
            NameView.setText(name);
        }

        public void setFaction(Fightable.Faction faction) {
            int colorId = -1;
            switch (faction) {
                case Group:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorGroup);
                    break;
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
            if (isSelected) {
                // If we can multi-select in this list, AND this Combatant is selected, then make the multi-select pane visible
                visibility = View.VISIBLE;
            } else {
                visibility = View.GONE;
            }

            // Update the GUI
            CombatantMultiSelect.setVisibility(visibility);
        }

        public void setMultiSelectingMode(boolean isMultiSelecting) {
            // This will be called when the adapter enters or exits multi selecting mode (for ALL viewHolders)
            final int visibility;
            if (isMultiSelecting) {
                visibility = View.GONE;
            } else {
                visibility = View.VISIBLE;
            }

            // Update the GUI
            setCombatantRemoveVisibility(visibility);
            setCombatantCopyVisibility(visibility);
        }

        private void setCombatantRemoveVisibility(int newVis) {
            CombatantRemove.setVisibility(newVis);
        }

        private void setCombatantCopyVisibility(int newVis) {
            CombatantCopy.setVisibility(newVis);
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

    @Override
    public void recieveCombatantMultiplesValue(int combatantInd, int newMultiple) {
        thisGroup.setNumMultiplesOfCombatant(combatantInd, newMultiple);

        notifyFightableListChanged();
    }

    public boolean allCombatantsSelected( ) {
        return thisGroup.allCombatantsAreSelected(referenceAFFL);
    }

    @Override
    public void onBindViewHolder(@NonNull CombatantGroupRecyclerAdapter.CombatantViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            // If the payload is not empty
            if (payloads.get(0) instanceof Bundle) {
                // If the payload is a Bundle
                Bundle args = (Bundle) payloads.get(0);
                for (String key : args.keySet()) {
                    if (key.equals(DIFF_NAME)) {
                        holder.setName(args.getString(key));
                    }
                    if (key.equals(DIFF_FACTION)) {
                        Fightable.Faction fac = (Fightable.Faction) args.getSerializable(key);
                        if (fac != null) {
                            holder.setFaction(fac);
                        }
                    }
                    if (key.equals(DIFF_ICON)) {
                        holder.setIcon(args.getInt(key));
                    }
                    if (key.equals(DIFF_SELECTED)) {
                        holder.setSelected(args.getBoolean(key));
                    }
                    if (key.equals(DIFF_MULTISELECT)) {
                        boolean newMultiSelectingMode =
                                args.getSerializable(key) == CombatantsInGroupDiffUtil.MultiSelectVisibilityChange.START_MULTISELECT;
                        // Else, assume it is equal to END_MULTISELECT
                        holder.setMultiSelectingMode(newMultiSelectingMode);
                    }
                    if (key.equals(DIFF_MULTIPLES)) {
                        holder.setMultiples(args.getInt(key));
                    }
                }
            }
            return; // The payload was not empty, so don't bother going on to the the non-payload onBindViewHolder
        }

        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onBindViewHolder(@NonNull CombatantViewHolder holder, int position) {
        holder.bind(position);
    }

    @NonNull
    @Override
    public CombatantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.combatant_item_mod, parent, false);
        return new CombatantViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return thisGroup.size();
    }

    private void notifyFightableListChanged() {
//        // Update the list of combatants to be displayed, taking into account the current filter string

        // Check if the list is now empty (will only happen if something unexpected occurs)
        if (thisGroup.size() == 0) {
            parent.dismiss();
        }

        // Make sure that the multi-select status is updated
        CombatantsInGroupDiffUtil.MultiSelectVisibilityChange visChange =
                CombatantsInGroupDiffUtil.MultiSelectVisibilityChange.NO_CHANGE;
        boolean oldIsMultiSelecting = isMultiSelecting;
        updateMultiSelectStatus();
        if ( oldIsMultiSelecting && !isMultiSelecting ) {
            visChange = CombatantsInGroupDiffUtil.MultiSelectVisibilityChange.END_MULTISELECT;
        } else if ( isMultiSelecting && !oldIsMultiSelecting ) {
            visChange = CombatantsInGroupDiffUtil.MultiSelectVisibilityChange.START_MULTISELECT;
        }

        // If anything about the combatants has changed (specifically the viewed version of the list according to fightableFilteredIndices), see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantsInGroupDiffUtil(thisGroup_Memory, thisGroup, referenceAFFL_Memory, referenceAFFL, visChange));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the memory list
        thisGroup_Memory = (CombatantGroup) thisGroup.clone();
        referenceAFFL_Memory = referenceAFFL.clone();
    }

    // The interface calling this adapter MUST have control of a master list of combatants such that it can judge a Combatant's name to be mutually exclusive
    interface MasterCombatantGroupKeeper extends Serializable {
        Context getContext(); // Get Context from the calling Activity/Fragment
        void dismiss(); // To close the dialog (in case all Combatants are removed)

        void notifyIsMultiSelecting(boolean isMultiSelecting); // Let the parent know that the multi-selecting status may have changed, so we may need to update the GUI
    }
}
