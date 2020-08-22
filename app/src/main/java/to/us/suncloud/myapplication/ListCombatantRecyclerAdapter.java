package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
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
import java.util.List;

import static android.graphics.Typeface.BOLD;

public class ListCombatantRecyclerAdapter extends RecyclerView.Adapter<ListCombatantRecyclerAdapter.CombatantViewHolder> implements Filterable, CreateOrModCombatant.receiveNewOrModCombatantInterface {
    private static final int UNSET = -1;

    private static final String TAG = "ListCombatantRecycler";
    private static final String COMBATANT_LIST_POSITION = "combatantListPosition";

    MasterCombatantKeeper parent = null; // If this is set, then the selected combatant will be sent to the parent
    boolean adapterCanModify = false; // Can the adapter modify the Combatant (used so that we can use this adapter for both Combatant display and modify+display purposes, because they are VERY similar)

    private FactionCombatantList combatantList_Master; // The master version of the list
    private FactionCombatantList combatantList_Display; // The version of the list that will be used for display (will take into account filtering from the search)
    private FactionCombatantList combatantList_Memory; // A memory version of the list, to see what changes have occurred

    private String filteredText = ""; // The string that is currently being used to filter the list
    private String filteredText_Memory = ""; // The string that was last used to filter the list, before the last call to notifyCombatantsChanged

    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, FactionCombatantList combatantList) {
        this.parent = parent;
        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
        this.combatantList_Display = combatantList.clone(); // COPY the main list for these two lists, so that the master is not changed
        this.combatantList_Memory = combatantList.clone();
    }

    public ListCombatantRecyclerAdapter(MasterCombatantKeeper parent, FactionCombatantList combatantList, boolean adapterCanModify) {
        this.parent = parent;
        this.adapterCanModify = adapterCanModify;
        this.combatantList_Master = combatantList; // Save the reference (master will be modified directly)
        this.combatantList_Display = new FactionCombatantList(combatantList); // COPY the main list for these two lists, so that the master is not changed
        this.combatantList_Memory = new FactionCombatantList(combatantList);
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
        ImageView CombatantIcon;
        ConstraintLayout CombatantIconBorder;

        // TODO SOON: Figure out how to use different ViewHolders for different activities, while minimizing boiler plate (Configure-, Add- should be using different viewholders (perhaps not?), but otherwise similar implementations of CombatantGroupFragment; Add will need search/filtering-by-String-start support, and NEITHER need the encounter viewholder used below...)
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

            // TODO SOON: Add a copy function that will make a copy of this Combatant (WITH NEW UUID), such that any ordinal stuff will happen automagically.

            NameView = itemView.findViewById(R.id.combatant_mod_name);
            CombatantRemove = itemView.findViewById(R.id.combatant_mod_remove);
            CombatantChangeCombatant = itemView.findViewById(R.id.combatant_mod_change_combatant);
            CombatantIcon = itemView.findViewById(R.id.combatant_mod_icon);
            CombatantIconBorder = itemView.findViewById(R.id.combatant_mod_icon_border);

            // Set up click functionality for rest of viewHolder (i.e. name, "itemView" [the background], and the icon)
            View.OnClickListener returnCombatantListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (parent != null) {
                        parent.receiveChosenCombatant(combatantList_Display.get(position));
                    }
                }
            };

            CombatantIcon.setOnClickListener(returnCombatantListener);
            NameView.setOnClickListener(returnCombatantListener);
            itemView.setOnClickListener(returnCombatantListener);

            if (adapterCanModify) {
                // If the adapter can modify the Combatants/the Combatant list, then allow the user to do so through these buttons
                CombatantChangeCombatant.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FragmentManager fm = ((AppCompatActivity) view.getContext()).getSupportFragmentManager();
                        Bundle returnBundle = new Bundle(); // Put information into this Bundle so that we know where to put the new Combatant when it comes back
                        returnBundle.putInt(COMBATANT_LIST_POSITION, position);
                        CreateOrModCombatant newDiag = CreateOrModCombatant.newInstance(ListCombatantRecyclerAdapter.this, combatantList_Master.get(position), parent.getMasterCombatantList(), returnBundle);
                        newDiag.show(fm, "CreateOrModCombatant");
                    }
                });

                // TODO: Checkboxes should only be in encounter adapter?
                CombatantRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (position != UNSET) {
                            // Ask the user if they definiately want to remove the combatant
                            new AlertDialog.Builder(itemView.getContext())
                                    .setTitle(R.string.confirm_delete)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            // Remove this from the combatant list
                                            removeCombatant(position);
                                        }
                                    })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            // Do nothing
                                        }
                                    })
                                    .create();
                        }
                    }
                });
            } else {
                // If we cannot modify the combatants/the combatant list, then remove the options
                CombatantChangeCombatant.setVisibility(View.GONE);
                CombatantRemove.setVisibility(View.GONE);
            }
        }

        public void bind(int combatant_ind) {
            position = combatant_ind;
            String combatantName = combatantList_Master.get(combatant_ind).getName();

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
    public void notfiyCombatantChanged(Bundle returnBundle) {
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
    public void receiveNewCombatant(Combatant newCombatant, Bundle returnBundle) {
        // Add a new combatant to both lists
        combatantList_Display.add(newCombatant);
        combatantList_Master.add(newCombatant);

        // Let the Adapter know that we have modified the combatant list
        notifyCombatantListChanged();

    }

    private void removeCombatant(int position) {
        // Remove the combatant at the given position
        // Removal will be relative to combatantDisplayList, because each removal request will originate from the viewholder, whose position is relative to the display list
        Combatant combatantToRemove = combatantList_Display.get(position);

        // Remove the given combatant
        combatantList_Display.remove(combatantToRemove);
        combatantList_Master.remove(combatantToRemove);

        // Let the Adapter know that we have modified the combatant list
        notifyCombatantListChanged();

        // Let the parent know that a Combatant in the list was removed
        parent.combatantInListRemoved();
    }

    private void replaceCombatant(int position, Combatant newCombatant) {
        // TODO CHECK: Is this supposed to be used somewhere...?  Perhaps not, because Combatant name changes are done in place now?
        // Replace the combatant at the given position
        // Removal will be relative to combatantDisplayList, because each removal request will originate from the viewholder, whose position is relative to the display list
        Combatant combatantToRemove = combatantList_Display.get(position);

        // Get the index of this combatant in the Master list (so the changed Combatant goes into the same position)
        int masterListPosition = combatantList_Master.indexOf(combatantToRemove);

        // Remove the given combatant
        combatantList_Display.remove(combatantToRemove);
        combatantList_Master.remove(combatantToRemove);

        // Add the new Combatant to the master list
        combatantList_Display.add(position, newCombatant);
        combatantList_Master.add(masterListPosition, newCombatant);

        // Let the Adapter know that we have modified the combatant list
        notifyCombatantListChanged();
    }

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
        return combatantList_Display.size();
    }

    public void notifyCombatantListChanged() {
        // If anything about the combatants has changed, see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantFilteredDiffUtil(combatantList_Memory, filteredText_Memory, combatantList_Display, filteredText));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the memory list
        combatantList_Memory = combatantList_Display;
        filteredText_Memory = filteredText;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String filterString = charSequence.toString().toLowerCase();
                if (filterString.isEmpty()) {
                    // If the string is empty, perform no filtering and set the display list to the same as the master list
                    combatantList_Display = combatantList_Master;

                } else {
                    // If there is something in the filter string, then filter out the combatants based on the string
                    combatantList_Display = new FactionCombatantList(combatantList_Master.faction());
                    for (int combatantInd = 0; combatantInd < combatantList_Master.size(); combatantInd++) {
                        if (combatantList_Master.get(combatantInd).getName().toLowerCase().contains(filterString)) {
                            // If this combatant's name contains the string, then include it in the display results
                            combatantList_Display.add(combatantList_Master.get(combatantInd));
                        }
                    }
                }

                // Update the display
                notifyCombatantListChanged();

                FilterResults results = new FilterResults();
                results.values = combatantList_Display;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                combatantList_Display = (FactionCombatantList) filterResults.values;
                notifyCombatantListChanged();
            }
        };
    }

    // The interface calling this adapter MUST have control of a master list of combatants such that it can judge a Combatant's name to be mutually exclusive
    interface MasterCombatantKeeper extends Serializable {
        void receiveChosenCombatant(Combatant selectedCombatant); // Receive a selected Combatant back from this Adapter
        void combatantInListRemoved(); // Used to notify parents that a Combatant was removed from this list, and the views may need to be laid out again
        AllFactionCombatantLists getMasterCombatantList();

        // Note: No methods associated with modifying the FactionCombatantList, because any modifications that occur (through the "gear" button) are consistent between this adapter and other locations (because Java passes references...hopefully)
    }
}
