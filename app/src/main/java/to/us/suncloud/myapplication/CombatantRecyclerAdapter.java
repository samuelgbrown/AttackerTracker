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
    RecyclerView combatantRecyclerView;

    private ArrayList<Combatant> combatantList;
    private ArrayList<Combatant> combatantList_Memory; // A memory version of the list, to see what changes have occurred

    private int curActiveCombatant = -1; // The currently active combatant, as an index in combatantList (if -1, there is no active combatant)

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
        private static final int UNSET = -1;

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
            // TODO: Assign all views
            NameView = itemView.findViewById(R.id.combatant_name);
            // etc...

            // TODO: Set up callback for the delete button and checkboxes
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

        CombatantViewHolder viewHolder = new CombatantViewHolder(layoutView);
        return viewHolder;
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
