package to.us.suncloud.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CombatantRecyclerAdapter extends RecyclerView.Adapter<CombatantRecyclerAdapter.CombatantViewHolder> {
    private static final int UNSET = -1;

    RecyclerView combatantRecyclerView;

    private ArrayList<Combatant> combatantList;
    private ArrayList<Combatant> combatantList_Memory; // A memory version of the list, to see what changes have occurred

    private int curActiveCombatant = UNSET; // The currently active combatant, as an index in combatantList (if -1, there is no active combatant)

    CombatantRecyclerAdapter(List<Combatant> combatantList) {
        this.combatantList = new ArrayList<>(combatantList); // Hold onto the combatant list
        this.combatantList_Memory = new ArrayList<>(combatantList); // Keep a second copy, for memory
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        // Save a reference to the RecyclerView
        combatantRecyclerView = recyclerView;
    }

    class CombatantViewHolder extends RecyclerView.ViewHolder {

        int position = UNSET;
        TextView NameView;
        TextView TotalInitiativeView;
        TextView RollView;
        TextView SpeedFactorView;
        ConstraintLayout ActiveCombatantBorder;

        ImageButton CombatantRemove;
        CheckBox CombatantCompletedCheck;

        public CombatantViewHolder(@NonNull View itemView) {
            super(itemView);
            // TODO: Figure out how to display the combatant's faction!
            NameView = itemView.findViewById(R.id.combatant_name);
            TotalInitiativeView = itemView.findViewById(R.id.combatant_total_initiative);
            RollView = itemView.findViewById(R.id.combatant_roll);
            SpeedFactorView = itemView.findViewById(R.id.combatant_speed_factor);
            ActiveCombatantBorder = itemView.findViewById(R.id.active_combatant_border);
            CombatantRemove = itemView.findViewById(R.id.combatant_remove);
            CombatantCompletedCheck = itemView.findViewById(R.id.combatant_completed_check);

            // TODO: Set up callback for the delete button and checkboxes
            CombatantRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (position != UNSET) {
                        // First, adjust the active combatant display, if required
                        if (curActiveCombatant == position) {
                            // (If curActiveCombatant is UNSET, then we won't get here)
                            if (position == (combatantList.size() - 1)) {
                                // If this is the last combatant in the list (before removing this combatant), then reset the currently active combatant to the new final combatant in the list
                                curActiveCombatant = position - 1; // The new final index in combatantList, once this combatant is removed, will be position - 1 (one behind the current combatant position, which is currently the last combatant)
                            }
                        }

                        // Next, remove this from the combatant list
                        combatantList.remove(position);

                        // Let the adapter know that the list has been modified, and it should rearrange things as needed
//                        notifyItemRemoved(position); // Don't think I need this...?
                        notifyCombatantsChanged();

                        // Finally, update the active combatant border, in case anything has changed
                        updateActiveCombatantGUI();
                    }
                }
            });

            CombatantCompletedCheck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Basically just want to gray out this combatant so it is still readable, but clearly visibly different
                }
            });
        }

        public void bind(int combatant_ind) {
            position = combatant_ind;
            NameView.setText(combatantList.get(combatant_ind).getName());
            TotalInitiativeView.setText(combatantList.get(combatant_ind).getTotalInitiative());
            RollView.setText(combatantList.get(combatant_ind).getRoll());
            SpeedFactorView.setText(combatantList.get(combatant_ind).getSpeedFactor());
        }

        public void setActiveCombatant(boolean isActiveCombatant) {
            // Update whether or not this is the active combatant
            if (isActiveCombatant) {
                ActiveCombatantBorder.setBackgroundColor(ActiveCombatantBorder.getContext().getResources().getColor(R.color.activeCombatant));
            } else {
                ActiveCombatantBorder.setBackgroundColor(ActiveCombatantBorder.getContext().getResources().getColor(R.color.standardBackground));
            }
        }
    }

    @NonNull
    @Override
    public CombatantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View layoutView = inflater.inflate(R.layout.combatant_item, parent, false);

        return new CombatantViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull CombatantViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return combatantList.size();
    }

    public void notifyCombatantsChanged() {
        // If anything about the combatants has changed, see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantDiffUtil(combatantList_Memory, combatantList));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around
    }

    public void setCurActiveCombatant(int curActiveCombatant) {
        if (curActiveCombatant < combatantList.size()) {
            this.curActiveCombatant = curActiveCombatant;
        } else {
            this.curActiveCombatant = -1;
        }

        updateActiveCombatantGUI();
    }

    private void updateActiveCombatantGUI() {
        // Update the GUI with respect to which combatant is active;
        for (int i = 0; i < combatantList.size(); i++) {
            // Go through each combatant and make sure that it is not displaying as the active combatant, unless it really is
            RecyclerView.ViewHolder vH = combatantRecyclerView.findViewHolderForLayoutPosition(i);
            if (vH instanceof CombatantViewHolder) {
                ((CombatantViewHolder) vH).setActiveCombatant(i == curActiveCombatant);
            }
        }
    }

    class CombatantDiffUtil extends DiffUtil.Callback {
        ArrayList<Combatant> oldList;
        ArrayList<Combatant> newList;

        CombatantDiffUtil(ArrayList<Combatant> oldList, ArrayList<Combatant> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Combatants are unique by name...ideally...
            return oldList.get(oldItemPosition).getName().equals(newList.get(newItemPosition).getName());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Check if ALL of the values are the same
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
