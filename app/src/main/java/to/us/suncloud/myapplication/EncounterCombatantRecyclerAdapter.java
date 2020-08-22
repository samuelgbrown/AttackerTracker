package to.us.suncloud.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EncounterCombatantRecyclerAdapter extends RecyclerView.Adapter<EncounterCombatantRecyclerAdapter.CombatantViewHolder> {
    public static final int UNSET = -1;

    RecyclerView combatantRecyclerView;

    private EncounterCombatantList combatantList;
    private EncounterCombatantList combatantList_Memory; // A memory version of the list, to see what changes have occurred

    private int curActiveCombatant = UNSET; // The currently active combatant, as an index in combatantList (if -1, there is no active combatant)
    private int curRoundNumber = 1; // The current round number (iterated each time the active combatant loops around)

    EncounterCombatantRecyclerAdapter(AllFactionCombatantLists combatantList) {
        // TODO: Turn the AllFactionCombatantList into an EncounterCombatantList
        this.combatantList = new EncounterCombatantList(combatantList); // Hold onto the combatant list (deep copy clone)
        this.combatantList_Memory = this.combatantList.clone(); // Keep a second copy, for memory (a second clone)
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        // Save a reference to the RecyclerView
        combatantRecyclerView = recyclerView;
    }

    // TODO Checkboxes:
    // Checkboxes initialize unchecked
    // Hitting next button checks and grays out the currently selected combatant, then selects the next combatant
    // Un-checking a previously checked combatant will highlight them a different color to remind you to revisit them
    // Should be an "un-next" button (small) to scroll back up the list and undo this stuff

    // TODO Features:
    // Must be able to edit speed factors after rolling (at any time)
    // Upon changing speed factor, isChecked should NOT CHANGE (only coloring and such according to current position of selected combatant)

    class CombatantViewHolder extends RecyclerView.ViewHolder {

        int position = UNSET;
        boolean hasCompleted; // Has this combatant completed its turn?

        TextView NameView;
        TextView TotalInitiativeView;
        TextView RollView;
        EditText RollViewEdit;
        TextView SpeedFactorView;
        ConstraintLayout ActiveCombatantBorder;

        ImageButton CombatantRemove;
        CheckBox CombatantCompletedCheck;
        ConstraintLayout CombatantGrayout;

        // TODO: Have multiple options for ordering Combatant ViewHolders (default is in order of total initiative, should also have alphabetically (split by Faction)
        // TODO SOON: Figure out how to use different ViewHolders for different activities, while minimizing boiler plate (Configure-, Add- should be using different viewholders, but otherwise similar implementations of CombatantGroupFragment; Add will need search/filtering-by-String-start support, and NEITHER need the encounter viewholder used below...)
        public CombatantViewHolder(@NonNull View itemView) {
            super(itemView);

            // TODO: Figure out how to display the combatant's faction! -> Use ICON (as in other viewholder)
            NameView = itemView.findViewById(R.id.combatant_enc_name);
            TotalInitiativeView = itemView.findViewById(R.id.combatant_enc_total_initiative);
            RollView = itemView.findViewById(R.id.combatant_enc_roll_layout);
            RollViewEdit = itemView.findViewById(R.id.combatant_enc_roll_edit); // For CHEATERS
            SpeedFactorView = itemView.findViewById(R.id.combatant_enc_speed_factor);
            ActiveCombatantBorder = itemView.findViewById(R.id.active_combatant_border);
            CombatantCompletedCheck = itemView.findViewById(R.id.combatant_enc_completed_check);
            CombatantGrayout = itemView.findViewById(R.id.combatant_enc_grayout);

            //            CombatantRemove = itemView.findViewById(R.id.combatant_enc_remove);

//            // TO_DO: Set up callback for the delete button and checkboxes (Are we going to even want a remove button...?  I don't think so....)
//            CombatantRemove.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (position != UNSET) {
//                        // First, adjust the active combatant display, if required
//                        if (curActiveCombatant == position) {
//                            // (If curActiveCombatant is UNSET, then we won't get here)
//                            if (position == (combatantList.size() - 1)) {
//                                // If this is the last combatant in the list (before removing this combatant), then reset the currently active combatant to the new final combatant in the list
//                                curActiveCombatant = position - 1; // The new final index in combatantList, once this combatant is removed, will be position - 1 (one behind the current combatant position, which is currently the last combatant)
//                            }
//                        }
//
//                        // Next, remove this from the combatant list
//                        combatantList.remove(position);
//
//                        // Let the adapter know that the list has been modified, and it should rearrange things as needed
////                        notifyItemRemoved(position); // Don't think I need this...?
//                        notifyCombatantsChanged();
//
//                        // Finally, update the active combatant border, in case anything has changed
//                        updateActiveCombatantGUI();
//                    }
//                }
//            });

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

    // TODO: Do I need this...?
    public void setCombatantList(ArrayList<Combatant> newCombatantList) {
        combatantList = new EncounterCombatantList(newCombatantList);
        notifyCombatantsChanged();
    }

    public void notifyCombatantsChanged() {
        // If anything about the combatants has changed, see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantDiffUtil(combatantList_Memory, combatantList));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the combatantList
        combatantList_Memory = combatantList;

    }

    @Override
    public void onBindViewHolder(@NonNull CombatantViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            // If the payload is not empty, it's an indication that the currently active Combatant has changed
            // TODO: Make the holder update its GUI
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    public void setCurActiveCombatant(int curActiveCombatant) {
        if (curActiveCombatant < combatantList.size()) {
            this.curActiveCombatant = curActiveCombatant;
        } else {
            this.curActiveCombatant = -1;
        }

        updateActiveCombatantGUI();
    }

    public void sort(EncounterCombatantList.SortMethod sortMethod) {
        combatantList.sort(sortMethod); // Perform the sorting
        notifyCombatantsChanged(); // Update the display
    }

    public int getCurActiveCombatant() {
        return curActiveCombatant;
    }

    public int getCurRoundNumber() {
        return curRoundNumber;
    }

    public void iterateCombatStep() {
        // TODO: Depending on the current value of curActiveCombatant, use notifyItemRangeChanged with a payload of a new Object() to notify the ViewHolders that they need to update their activeCombatant status
        // TODO: Will need way to get a Combatant position in the Initiative sorted list, regardless of the state of the current sort, so that checkboxes/grayed-out-ness can be preserved between sort button presses
        if (curActiveCombatant == UNSET) {
            curActiveCombatant = 0; // If the currently active combatant is unset, initialize the currently active combatant

            // Also, ROLL INITIATIVE!!!!!
            rollInitiative(); // TODO CHECK: This technically means that going back after this gives you the chance to re-roll initiative, which COULD be an issue.  Can just keep track of which rounds have been rolled already
        } else if (curActiveCombatant == (combatantList.size() - 1)) {
            curActiveCombatant = UNSET; // If we are at the end of the list, go to "UNSET" for now
        } else {
            curActiveCombatant++; // Otherwise, just increment the combatant number
        }

        // Change the round number, if needed
        if (curActiveCombatant == UNSET) {
            // If the currently active Combatant is unset, we just finished the last Combatant and are now waiting to roll initiative for the next round
            curRoundNumber++;
        }
    }

    public void reverseCombatStep() {
        // Move backwards along the active Combatant list
        if (curActiveCombatant == UNSET) {
            curActiveCombatant = combatantList.size() - 1; // If the currently active combatant is unset, go back to the end of the list
        } else if (curActiveCombatant == 0) {
            curActiveCombatant = UNSET; // If we are at the beginning of the list, go to "UNSET" for now
        } else {
            curActiveCombatant--; // Otherwise, just decrement
        }

        // Change the round number, if needed
        if (curActiveCombatant == (combatantList.size() - 1)) {
            // If the currently active Combatant is the last Combatant, we just went back into the previous round of Combat
            curRoundNumber--;
        }
    }

    private void rollInitiative() {
        // TODO!!
        // TODO LATER: Keep track of all rolls, so that you can go backwards in time!
        // TODO SOON: How do I deal with ties in initiative? Go by alphabet, favor party, "connect" the tied Combatants together somehow...?  May need to be a setting...
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
