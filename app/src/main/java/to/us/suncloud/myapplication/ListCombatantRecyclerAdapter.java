package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import static android.graphics.Typeface.BOLD;

public class ListCombatantRecyclerAdapter extends RecyclerView.Adapter<ListCombatantRecyclerAdapter.CombatantViewHolder> implements Filterable {
    private static final int UNSET = -1;

    RecyclerView combatantRecyclerView;

    private ArrayList<Combatant> combatantList; // The master version of the list
    private ArrayList<Combatant> combatantList_Display; // The version of the list that will be used for display (will take into account filtering from the search)
    private ArrayList<Combatant> combatantList_Memory; // A memory version of the list, to see what changes have occurred

    private String filteredText = ""; // The string that is currently being used to filter the list
    private String filteredText_Memory = ""; // The string that was last used to filter the list, before the last call to notifyCombatantsChanged

    public ListCombatantRecyclerAdapter(ArrayList<Combatant> combatantList) {
        this.combatantList = new ArrayList<>(combatantList);
        this.combatantList_Memory = new ArrayList<>(combatantList);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        // Save a reference to the RecyclerView
        combatantRecyclerView = recyclerView;
    }

    class CombatantViewHolder extends RecyclerView.ViewHolder {

        int position = UNSET;
        boolean hasCompleted; // Has this combatant completed its turn?

        TextView NameView;
        ImageButton CombatantRemove;
        ImageButton CombatantChangeName;

        // TODO SOON: Figure out how to use different ViewHolders for different activities, while minimizing boiler plate (Configure-, Add- should be using different viewholders (perhaps not?), but otherwise similar implementations of CombatantGroupFragment; Add will need search/filtering-by-String-start support, and NEITHER need the encounter viewholder used below...)
        // Difference between Add and configure:
        //      1. Configure will have gear button to change aspects of combatant
        //      2. Remove will be different - Configure will be to remove from the list, add will be to remove from the file
        //          Note that this difference in removal behavior may be accomplished just through the RecyclerAdapter (i.e. where the list gets sent after the fragment this recycler is in closes)
        // Current game-plan:
        //  Use the SAME viewholder for both add and configure.  Fragment will do different things with the final list that this recycler is viewing/modifying (configure: return it to the main activity, add: save the list to file and return a single combatant)
        //  For Add, perhaps double-check with user if they want to save the modified list to file?  Either at combatant modification or when fragment is returning combatant (in this case, this adapter doesn't need to differentiate add from configure)

        public CombatantViewHolder(@NonNull final View itemView) {
            // TODO: FINISH THIS
            super(itemView);

            // TODO: Figure out how to display the combatant's faction! -> Use background color
            NameView = itemView.findViewById(R.id.combatant_mod_name);
            CombatantRemove = itemView.findViewById(R.id.combatant_mod_remove);
            CombatantChangeName = itemView.findViewById(R.id.combatant_mod_change_name); // TODO: Make this change the name TextView to editable, move the cursor there, pull up the keyboard.  Then, when the user confirms, set the name, and bring back the static name view

            // TODO: Set up callback for the delete button and checkboxes
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
                                        removeCombatant(getAdapterPosition());
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

            // TODO: Gear button should allow name, faction, and icon to change
            // TODO: Implement icons!  I would be fun!
        }

        public void bind(int combatant_ind) {
            position = combatant_ind;
            String combatantName = combatantList.get(combatant_ind).getName();

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

    private void removeCombatant(int position) {
        // Remove the combatant at the given position
        //      Removal will be relative to combatantDisplayList, because each removal request will originate from the viewholder, whose position is relative to the display list
        Combatant combatantToRemove = combatantList_Display.get(position);

        // Remove the given combatant
        combatantList_Display.remove(combatantToRemove);
        combatantList.remove(combatantToRemove);

        // Let the adapter know that we have changed the list
        notifyCombatantsChanged();
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
        return combatantList.size();
    }

    public void notifyCombatantsChanged() {
        // If anything about the combatants has changed, see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantFilteredDiffUtil(combatantList_Memory, filteredText_Memory, combatantList_Display, filteredText));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the memory list
        combatantList_Memory = combatantList_Display;
        filteredText_Memory = filteredText;
    }

    @Override
    public Filter getFilter() {
        // TODO: Interface this with the search bar!
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String filterString = charSequence.toString().toLowerCase();
                if (filterString.isEmpty()) {
                    // If the string is empty, perform no filtering and set the display list to the same as the master list
                    combatantList_Display = combatantList;

                } else {
                    // If there is something in the filter string, then filter out the combatants based on the string
                    combatantList_Display = new ArrayList<>();
                    for (int combatantInd = 0;combatantInd < combatantList.size();combatantInd++) {
                        if (combatantList.get(combatantInd).getName().toLowerCase().contains(filterString)) {
                            // If this combatant's name contains the string, then include it in the display results
                            combatantList_Display.add(combatantList.get(combatantInd));

                            // Also, for display's sake
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = combatantList_Display;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                combatantList_Display = (ArrayList<Combatant>) filterResults.values;
                notifyCombatantsChanged();
            }
        };
    }
}
