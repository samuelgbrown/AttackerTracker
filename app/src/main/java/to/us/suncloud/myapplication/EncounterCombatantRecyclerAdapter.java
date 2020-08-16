package to.us.suncloud.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EncounterCombatantRecyclerAdapter extends RecyclerView.Adapter<EncounterCombatantRecyclerAdapter.CombatantViewHolder> {
    private static final int UNSET = -1;

    RecyclerView combatantRecyclerView;

    private ArrayList<Combatant> combatantList;
    private ArrayList<Combatant> combatantList_Memory; // A memory version of the list, to see what changes have occurred

    private int curActiveCombatant = UNSET; // The currently active combatant, as an index in combatantList (if -1, there is no active combatant)

    EncounterCombatantRecyclerAdapter(List<Combatant> combatantList) {
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
        boolean hasCompleted; // Has this combatant completed its turn?

        TextView NameView;
        TextView TotalInitiativeView;
        TextView RollView;
        TextView SpeedFactorView;
        ConstraintLayout ActiveCombatantBorder;

        ImageButton CombatantRemove;
        CheckBox CombatantCompletedCheck;
        ConstraintLayout CombatantGrayout;

        // TODO SOON: Figure out how to use different ViewHolders for different activities, while minimizing boiler plate (Configure-, Add- should be using different viewholders, but otherwise similar implementations of CombatantGroupFragment; Add will need search/filtering-by-String-start support, and NEITHER need the encounter viewholder used below...)
        public CombatantViewHolder(@NonNull View itemView) {
            super(itemView);

            // TODO: Figure out how to display the combatant's faction! -> Use background color
            NameView = itemView.findViewById(R.id.combatant_enc_name);
            TotalInitiativeView = itemView.findViewById(R.id.combatant_enc_total_initiative);
            RollView = itemView.findViewById(R.id.combatant_enc_roll);
            SpeedFactorView = itemView.findViewById(R.id.combatant_enc_speed_factor);
            ActiveCombatantBorder = itemView.findViewById(R.id.active_combatant_border);
            CombatantRemove = itemView.findViewById(R.id.combatant_enc_remove);
            CombatantCompletedCheck = itemView.findViewById(R.id.combatant_enc_completed_check);
            CombatantGrayout = itemView.findViewById(R.id.combatant_enc_grayout);

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

            CombatantCompletedCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    // The has completed value should follow the state of the boolean
                    setHasCompleted(b);
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

        public void setHasCompleted(boolean hasCompleted) {
            if (this.hasCompleted != hasCompleted) {
                // If there is a new setting for the completed setting, then change the grayout
                float targetAlpha;
                if (hasCompleted) {
                    // If this combatant has completed its turn, gray it out by increasing the alpha of the grayout view
                    targetAlpha = .5f;
                } else {
                    targetAlpha = 0f;
                }

                // Get the current alpha value
                float currentAlpha = CombatantGrayout.getAlpha();

                // Now animate the grayout
                AlphaAnimation alphaAnim = new AlphaAnimation(currentAlpha, targetAlpha);
                alphaAnim.setDuration(300); // Animation should take .3s
                alphaAnim.setFillAfter(true); // Persist the new alpha after the animation ends
                CombatantGrayout.startAnimation(alphaAnim);
            }
        }
    }

    @NonNull
    @Override
    public CombatantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View layoutView = inflater.inflate(R.layout.combatant_item_encounter, parent, false);

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

    public void setCombatantList(ArrayList<Combatant> newCombatantList) {
        combatantList = new ArrayList<>(newCombatantList);
        notifyCombatantsChanged();
    }

    public void notifyCombatantsChanged() {
        // If anything about the combatants has changed, see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantDiffUtil(combatantList_Memory, combatantList));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the combatantList
        combatantList_Memory = combatantList;
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
}
