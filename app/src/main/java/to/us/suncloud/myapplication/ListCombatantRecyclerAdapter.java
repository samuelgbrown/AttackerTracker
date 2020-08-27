package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
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

import static android.graphics.Typeface.BOLD;

public class ListCombatantRecyclerAdapter extends RecyclerView.Adapter<ListCombatantRecyclerAdapter.CombatantViewHolder> implements Filterable, CreateOrModCombatant.receiveNewOrModCombatantInterface {
    private static final int UNSET = -1;

    private static final String TAG = "ListCombatantRecycler";

    MasterCombatantKeeper parent = null; // If this is set, then the selected combatant will be sent to the parent
    boolean adapterCanModify = false; // Can the adapter modify the Combatant (used so that we can use this adapter for both Combatant display and modify+display purposes, because they are VERY similar)
    boolean adapterCanCopy = false; // Can the adapter copy the Combatant (used in the Configure Activity, but not for viewing the saved Combatants

    private FactionCombatantList combatantList_Master; // The master version of the list
    //    private FactionCombatantList combatantList_Display; // The version of the list that will be used for display (will take into account filtering from the search)
    private ArrayList<Integer> combatantFilteredIndices; // The indices in combatantList_Master that contain the given filter string
    private FactionCombatantList combatantList_Memory; // A memory version of the list, to see what changes have occurred

    ArrayList<Integer> iconResourceIds; // A list of resource ID's of the icons that will be used for each Combatant

    private String filteredText = ""; // The string that is currently being used to filter the list
    private String filteredText_Memory = ""; // The string that was last used to filter the list, before the last call to notifyCombatantsChanged

    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, Context context, FactionCombatantList combatantList) {
        this.parent = parent;
        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
//        this.combatantList_Display = combatantList.clone(); // COPY the main list for these two lists, so that the master is not changed
        this.combatantList_Memory = combatantList.clone();

        setupIconResourceIDs(context);
        initializeCombatantFilteredIndices();
    }

    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, Context context, FactionCombatantList combatantList, boolean adapterCanModify) {
        this.parent = parent;
        this.adapterCanModify = adapterCanModify;
        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
//        this.combatantList_Display = new FactionCombatantList(combatantList); // COPY the main list for these two lists, so that the master is not changed
        this.combatantList_Memory = new FactionCombatantList(combatantList);

        setupIconResourceIDs(context);
        initializeCombatantFilteredIndices();
    }

    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, Context context, FactionCombatantList combatantList, boolean adapterCanModify, boolean adapterCanCopy) {
        this.parent = parent;
        this.adapterCanModify = adapterCanModify;
        this.adapterCanCopy = adapterCanCopy;
        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
//        this.combatantList_Display = new FactionCombatantList(combatantList); // COPY the main list for these two lists, so that the master is not changed
        this.combatantList_Memory = new FactionCombatantList(combatantList);

        setupIconResourceIDs(context);
        initializeCombatantFilteredIndices();
    }

    private void setupIconResourceIDs(Context context) {
        // Preload a list of resources that will be used to load svg's into the grid
        iconResourceIds = new ArrayList<>();
        int curNum = 0;
        while (true) {
            // Generate filenames for every icon that we will use in order, and check if it exists
            String resourceName = String.format(Locale.US, "icon_%02d", curNum); // Oh, the horror...
            int id = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

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

    private void initializeCombatantFilteredIndices() {
//                 If there is something in the filter string, then filter out the combatants based on the string
//                combatantList_Display = new FactionCombatantList(combatantList_Master.faction());

        combatantFilteredIndices = new ArrayList<>();
        for (int combatantInd = 0; combatantInd < combatantList_Master.size(); combatantInd++) {
            if (filteredText.isEmpty() || combatantList_Master.get(combatantInd).getName().toLowerCase().contains(filteredText)) {
                // If this combatant's name contains the string (or the string is empty), then include it in the display results
//                        combatantList_Display.add(combatantList_Master.get(combatantInd).cloneUnique());
                combatantFilteredIndices.add(combatantInd);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    class CombatantViewHolder extends RecyclerView.ViewHolder {

        int position = UNSET;
        boolean hasCompleted; // Has this combatant completed its turn?

        TextView NameView;
        ImageButton CombatantRemove;
        ImageButton CombatantChangeCombatant;
        ImageButton CombatantCopy;
        ImageView CombatantIcon;
        ConstraintLayout CombatantIconBorder;

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

            NameView = itemView.findViewById(R.id.combatant_mod_name);
            CombatantRemove = itemView.findViewById(R.id.combatant_mod_remove);
            CombatantChangeCombatant = itemView.findViewById(R.id.combatant_mod_change_combatant);
            CombatantIcon = itemView.findViewById(R.id.combatant_mod_icon);
            CombatantIconBorder = itemView.findViewById(R.id.combatant_mod_icon_border);
            CombatantCopy = itemView.findViewById(R.id.combatant_mod_copy);

            // Set up click functionality for rest of viewHolder (i.e. name, "itemView" [the background], and the icon)
            View.OnClickListener returnCombatantListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (parent != null) {
//                        parent.receiveChosenCombatant(combatantList_Display.get(getAdapterPosition()));
                        parent.receiveChosenCombatant(combatantList_Master.get(combatantFilteredIndices.get(getAdapterPosition())).cloneUnique());
                    }
                }
            };

            CombatantIcon.setOnClickListener(returnCombatantListener);
            NameView.setOnClickListener(returnCombatantListener);
            itemView.setOnClickListener(returnCombatantListener);

            if (adapterCanModify) {
                // If the adapter can modify the Combatants/the Combatant list, then allow the user to do so through these buttons
                CombatantChangeCombatant.setVisibility(View.VISIBLE);
                CombatantRemove.setVisibility(View.VISIBLE);

                CombatantChangeCombatant.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FragmentManager fm = ((AppCompatActivity) view.getContext()).getSupportFragmentManager();
                        Bundle returnBundle = new Bundle(); // Put information into this Bundle so that we know where to put the new Combatant when it comes back
                        returnBundle.putInt(CreateOrModCombatant.MODIFY_COMBATANT_LOCATION, getAdapterPosition());
                        CreateOrModCombatant newDiag = CreateOrModCombatant.newInstance(ListCombatantRecyclerAdapter.this, combatantList_Master.get(combatantFilteredIndices.get(getAdapterPosition())), parent.getMasterCombatantList(), returnBundle);
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
                                        removeCombatant(getAdapterPosition());
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
                                        copyCombatant(getAdapterPosition());
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

        public void bind(int combatant_ind) {
            position = combatant_ind;
            String combatantName = combatantList_Master.get(combatant_ind).getName();

            // Load the icon image
            int iconIndex = combatantList_Master.get(getAdapterPosition()).getIconIndex();
            if (iconIndex == 0) {
                // If the icon index is 0, then the icon is blank
                CombatantIcon.setImageResource(android.R.color.transparent);
            } else {
                // Otherwise, get the corresponding icon image
                CombatantIcon.setImageDrawable(CombatantIcon.getContext().getDrawable(iconResourceIds.get(iconIndex - 1)));
            }

            // Set the color of the icon and the icon's border
            int colorId = -1;
            switch (combatantList_Master.get(combatant_ind).getFaction()) {
                case Party:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorParty);
                    break;
                case Enemy:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorEnemy);
                    break;
                case Neutral:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorNeutral);
            }

            CombatantIcon.setImageTintList(ColorStateList.valueOf(colorId)); // TODO CHECK: Check that this actually changes the tint of the icon...
            CombatantIconBorder.setBackgroundColor(colorId);

            // Display the name differently based on the filter text
            if (filteredText.isEmpty()) {
                // If the filter text is blank, then just display the name as is
                NameView.setText(combatantName);
            } else {
                // If the filter text is NOT blank, then bold the relevant part of the name
                // First, find the beginning and end of all occurrences of the string
                SpannableStringBuilder spannable = new SpannableStringBuilder(combatantName);
                StyleSpan span = new StyleSpan(BOLD);
                int filteredTextIndex = combatantName.indexOf(filteredText);
                if (filteredTextIndex != -1) {
                    // If the string appears in the name, then make it bold
                    spannable.setSpan(span, filteredTextIndex, filteredTextIndex + filteredText.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);

                    for (int i = -1; (i = combatantName.indexOf(filteredText, i + 1)) != -1; i++) {
                        // Find all occurrences forward
                        spannable.setSpan(span, i, i + filteredText.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                NameView.setText(spannable);
            }
        }
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

        // TODO CHECK: Make sure this ACTUALLY changes the Combatant (Java and its sneaky references...)
        // Let the Adapter know that the combatant list has been changed
        notifyCombatantListChanged();
    }

    @Override
    public void receiveCombatant(Combatant newCombatant, Bundle returnBundle) {
        if (returnBundle.containsKey(CreateOrModCombatant.MODIFY_COMBATANT_LOCATION)) {
            int modCombatantLocation = returnBundle.getInt(CreateOrModCombatant.MODIFY_COMBATANT_LOCATION); // Get the location of the Combatant being modified
            // NOTE: This location is relative to combatantList_Master(combatantFilteredIndices).
            combatantList_Master.remove(combatantFilteredIndices.get(modCombatantLocation)); // Easiest to just remove the Combatant and add it again (the add function will take care of any "smart naming" needs)
        }

        // Add a new combatant to master list
        parent.getMasterCombatantList().addCombatant(newCombatant); // Add the Combatant to the master list (which will "trickle down" to this Fragment)
//        combatantList_Master.add(newCombatant);

        // Let the Adapter know that we have modified the combatant list
        notifyCombatantListChanged();
        parent.combatantListModified();
    }

    private void removeCombatant(int position) {
        // Remove the Combatant at the given position
//        // Removal will be relative to combatantDisplayList, because each removal request will originate from the viewholder, whose position is relative to the display list
//        Combatant combatantToRemove = combatantList_Display.get(position);

        // Remove the given combatant
//        combatantList_Display.remove(combatantToRemove);
//        combatantList_Master.remove(combatantToRemove);
        combatantList_Master.remove(combatantFilteredIndices.get(position));

        // Let the Adapter know that we have modified the combatant list
        notifyCombatantListChanged();

        // Let the parent know that a Combatant in the list was removed
        parent.combatantListModified();
    }

    private void copyCombatant(int position) {
        // Copy the Combatant at the given position
        Combatant newCombatant = combatantList_Master.get(combatantFilteredIndices.get(position)).cloneUnique(); // Create a clone fo the Combatant to copy, but with a unique ID

        // This is a little bit hacky, but I THINK it should work...
        // Add the Combatant to the Master list (so that a new Combatant name is generated that is unique to the entire Combatant list)
        parent.getMasterCombatantList().addCombatant(newCombatant); // TODO CHECK: Now, after this, the AllFactionsCombatantList owned by the calling Activity/Fragment should have changed, as well as the combatantList_Master (a FactionCombatantList).

        // Finally, let the adapter know that we have updated the list
        notifyCombatantListChanged();
    }

//    private void replaceCombatant(int position, Combatant newCombatant) {
//        // TO_DO CHECK: Is this supposed to be used somewhere...?  Perhaps not, because Combatant name changes are done in place now?
//        // Replace the Combatant at the given position
//        // Removal will be relative to combatantDisplayList, because each removal request will originate from the viewholder, whose position is relative to the display list
//        Combatant combatantToRemove = combatantList_Display.get(position);
//
//        // Get the index of this combatant in the Master list (so the changed Combatant goes into the same position)
//        int masterListPosition = combatantList_Master.indexOf(combatantToRemove);
//
//        // Remove the given combatant
//        combatantList_Display.remove(combatantToRemove);
//        combatantList_Master.remove(combatantToRemove);
//
//        // Add the new Combatant to the master list
//        combatantList_Display.add(position, newCombatant);
//        combatantList_Master.add(masterListPosition, newCombatant);
//
//        // Let the Adapter know that we have modified the combatant list
//        notifyCombatantListChanged();
//    }

    @Override
    public void onBindViewHolder(@NonNull ListCombatantRecyclerAdapter.CombatantViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
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
//        return combatantList.size();
        return combatantFilteredIndices.size();
    }

    public void notifyCombatantListChanged() {
        // Update the list of combatants to be displayed, taking into account the current filter string
        initializeCombatantFilteredIndices();

        // If anything about the combatants has changed (specifically the viewed version of the list according to combatantFilteredIndices), see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantFilteredDiffUtil(combatantList_Memory, filteredText_Memory, combatantList_Master.subList(combatantFilteredIndices), filteredText));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the memory list
        combatantList_Memory = combatantList_Master.subList(combatantFilteredIndices).clone();
        filteredText_Memory = filteredText;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String filterString = charSequence.toString().toLowerCase();
                filteredText = filterString;


                FilterResults results = new FilterResults();
//                results.values = combatantList_Display;
                results.values = combatantFilteredIndices;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
//                combatantList_Display = (FactionCombatantList) filterResults.values;
                combatantFilteredIndices = (ArrayList<Integer>) filterResults.values;
                notifyCombatantListChanged();
            }
        };
    }

    // The interface calling this adapter MUST have control of a master list of combatants such that it can judge a Combatant's name to be mutually exclusive
    interface MasterCombatantKeeper extends Serializable {
        // TODO: May be able to get rid of this method?  Just do changes on the master list and notify that the list was modified?  Maybe?
        void receiveChosenCombatant(Combatant selectedCombatant); // Receive a selected Combatant back from this Adapter

        void combatantListModified(); // Used to notify parents that a Combatant was removed from this list, and the views may need to be laid out again

        AllFactionCombatantLists getMasterCombatantList();

        // Note: No methods associated with modifying the FactionCombatantList, because any modifications that occur (through the "gear" button) are consistent between this adapter and other locations (because Java passes references...hopefully)
    }
}
