package to.us.suncloud.myapplication;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EncounterCombatantRecyclerAdapter extends RecyclerView.Adapter<EncounterCombatantRecyclerAdapter.CombatantViewHolder> {
    public static final int UNSET = -1;
    private static final float GRAYED_OUT = 0.5f;
    private static final float CLEAR = 0f;

    private static final String PAYLOAD_CHECK = "payloadCheck"; // Used to indicate that a Combatant should become checked or unchecked (infers an update progress call as well)
    private static final String PAYLOAD_UPDATE_PROGRESS = "payloadUpdateProgress"; // Used any time a Combatant becomes checked/unchecked (this may happen by the user, or programmatically)
    private static final String PAYLOAD_DICE_CHEAT = "payloadDiceCheat"; // Used any time dice cheat mode is turned on/off
    private static final String PAYLOAD_DUPLICATE_INDICATOR = "payloadDuplicateIndicator"; // Used any time the color of the duplicate indicator may be changed (if initiative is re-rolled, or if the combatantList is modified by addition/removal)

    RecyclerView combatantRecyclerView;

    private EncounterCombatantList combatantList;
    private EncounterCombatantList combatantList_Memory; // A memory version of the list, to see what changes have occurred

    ArrayList<Integer> iconResourceIds; // A list of resource ID's of the icons that will be used for each Combatant

    boolean diceCheatModeOn = false; // Are we currently in the dice cheat mode?

    private int curActiveCombatant = UNSET; // The currently active combatant, as an index in combatantList (if -1, there is no active combatant)
    private int curRoundNumber = 1; // The current round number (iterated each time the active combatant loops around)

    EncounterCombatantRecyclerAdapter(Context context, AllFactionCombatantLists combatantList) {
        // Turn the AllFactionCombatantList into an EncounterCombatantList
        this.combatantList = new EncounterCombatantList(combatantList); // Hold onto the combatant list (deep copy clone)
        this.combatantList_Memory = this.combatantList.clone(); // Keep a second copy, for memory (a second clone)

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
        boolean selfChangingText = false;

        private TextView NameView;
        private TextView TotalInitiativeView;
        private TextView RollView;
        private EditText RollViewEdit;
        private EditText SpeedFactorView;
        private ImageView CombatantIcon;

        private ConstraintLayout CombatantStatusBorder;
        private ConstraintLayout CombatantIconBorder;
        private ConstraintLayout RollLayout;
        private ConstraintLayout DuplicateIndicator;

        private CheckBox CombatantCompletedCheck;
        private ConstraintLayout CombatantGrayout;

        public CombatantViewHolder(@NonNull View itemView) {
            super(itemView);

            NameView = itemView.findViewById(R.id.combatant_enc_name);
            TotalInitiativeView = itemView.findViewById(R.id.combatant_enc_total_initiative);
            RollView = itemView.findViewById(R.id.combatant_enc_roll);
            RollViewEdit = itemView.findViewById(R.id.combatant_enc_roll_edit); // For CHEATERS
            SpeedFactorView = itemView.findViewById(R.id.combatant_enc_speed_factor);
            CombatantStatusBorder = itemView.findViewById(R.id.encounter_combatant_border);
            CombatantCompletedCheck = itemView.findViewById(R.id.combatant_enc_completed_check);
            CombatantGrayout = itemView.findViewById(R.id.combatant_enc_grayout);
            CombatantIcon = itemView.findViewById(R.id.encounter_icon);
            CombatantIconBorder = itemView.findViewById(R.id.encounter_icon_border);
            RollLayout = itemView.findViewById(R.id.combatant_enc_roll_layout);
            DuplicateIndicator = itemView.findViewById(R.id.enc_dup_background);

            CombatantCompletedCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    // The has completed value should follow the state of the boolean
                    setCombatProgression();
                }
            });

            SpeedFactorView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    // If this is being called due to a programmatic change, ignore it
                    if (selfChangingText) {
                        return;
                    }

                    // First, make sure the value here is valid
                    int currentSpeedFactor = combatantList.get(position).getSpeedFactor(); // The current roll for this Combatant
                    int newSpeedFactor  = currentSpeedFactor; // Initial value never used, but at least the IDE won't yell at me...
                    boolean needToRevert;
                    try {
                        newSpeedFactor = Integer.parseInt(editable.toString()); // Get the value that was entered into the text box

                        // If the new entered roll value is not in the d20 range, then reject it
                        needToRevert = newSpeedFactor < 0; // needToRevert becomes false (we accept the input) if newSpeedFactor is 0 or greater
                    } catch (NumberFormatException e) {
                        needToRevert = true;
                    }

                    // Update the value of the EditText, if needed
                    if (needToRevert) {
                        // The entered text was not valid, so reset it and finish
                        selfChangingText = true;
                        RollViewEdit.setText(String.valueOf(currentSpeedFactor));
                        selfChangingText = false;
                        return;
                    } else if (newSpeedFactor == currentSpeedFactor) {
                        // If the new speed factor value is the same as the current speed factor, then don't do anything
                        return;
                    }

                    // If the entered text was valid and different than the current speed factor, then change this Combatant, resort the list, and update the GUI
                    setSpeedFactor(position, newSpeedFactor);
                }
            });

            RollViewEdit.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    // If this is being called due to a programmatic change, ignore it
                    if (selfChangingText) {
                        return;
                    }

                    // First, make sure the value here is valid
                    int currentRoll = combatantList.get(position).getRoll(); // The current roll for this Combatant
                    int newRollVal = currentRoll; // Initial value never used, but at least the IDE won't yell at me...
                    boolean needToRevert;
                    try {
                        newRollVal = Integer.parseInt(editable.toString()); // Get the value that was entered into the text box

                        // If the new entered roll value is not in the d20 range, then reject it
                        needToRevert = newRollVal <= 0 || 20 < newRollVal; // needToRevert becomes false (we accept the input) if newRollVal is between [1 20]
                    } catch (NumberFormatException e) {
                        needToRevert = true;
                    }

                    // Update the value of the EditText, if needed
                    if (needToRevert) {
                        // The entered text was not valid, so reset it and finish
                        selfChangingText = true;
                        RollViewEdit.setText(String.valueOf(currentRoll));
                        selfChangingText = false;
                        return;
                    } else if (newRollVal == currentRoll) {
                        // If the new roll value is the same as the current roll, then don't do anything
                        return;
                    }

                    // If the entered text was valid and different than the current roll, then change this Combatant, resort the list, and update the GUI
                    RollView.setText(String.valueOf(newRollVal)); // Set this value in the TextView
                    setRollValue(position, newRollVal);
                }
            });
        }

        public void bind(int combatant_ind) {
            position = combatant_ind;
            Combatant thisCombatant = combatantList.get(position);

            NameView.setText(thisCombatant.getName());
            TotalInitiativeView.setText(thisCombatant.getTotalInitiative());
            RollView.setText(thisCombatant.getRoll());

            selfChangingText = true;
            SpeedFactorView.setText(thisCombatant.getSpeedFactor());
            RollViewEdit.setText(thisCombatant.getRoll());
            selfChangingText = false;

            // Load the icon image
            int iconIndex = thisCombatant.getIconIndex();
            if (iconIndex == 0) {
                // If the icon index is 0, then the icon is blank
                CombatantIcon.setImageResource(android.R.color.transparent);
            } else {
                // Otherwise, get the corresponding icon image
                CombatantIcon.setImageDrawable(CombatantIcon.getContext().getDrawable(iconResourceIds.get(iconIndex - 1)));
            }

            // Set the color of the icon and the icon's border
            int colorId = -1;
            switch (thisCombatant.getFaction()) {
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

            setCombatProgression(); // Set up the GUI elements related to progression
            setDiceCheatMode(); // Set up the dice roll Views
            setDuplicateInitiativeView(); // Set up the duplicate indicator
        }

        public void setDiceCheatMode() {
            // Use the current setting of diceCheatModeOn to see which View should be seen for the viewHolder
            if (diceCheatModeOn) {
                // If we are in dice cheat mode, make sure that the EditText is visible and the TextView is hidden
                RollViewEdit.setVisibility(View.VISIBLE);
                RollView.setVisibility(View.GONE);
            } else {
                // If we are not in dice cheat mode, make sure that the TextView is visible and the EditText is hidden
                RollViewEdit.setVisibility(View.GONE);
                RollView.setVisibility(View.VISIBLE);
            }
        }

        public void setDuplicateInitiativeView() {
            // Update the background of the Checkbox to reflect whether or not this Combatant's total initiative is shared across multiple Combatant's
            int resourceID = R.color.combatantTab; // If this Combatant is not a duplicate, then set the background to the standard background
            if (combatantList.isDuplicate(position)) {
                // If this is a duplicate, get the color that it should be
                switch (combatantList.getDuplicateColor(position)) {
                    case 0:
                        resourceID = R.color.duplicateInitiative1;
                        break;
                    case 1:
                        resourceID = R.color.duplicateInitiative2;
                        break;
                }
            }

            // Set the background color for the duplicate indicator
            DuplicateIndicator.setBackgroundResource(resourceID);
        }

        public void setCombatProgression() {
            // Set up variables for everything that may change
            float targetAlpha;
            int borderColor = R.color.standardBackground; // Initialize to a "blank" border
            int initiativeRollVisibility;
            int checkLayoutVisibility;

            // Get some useful parameters
            float currentAlpha = CombatantGrayout.getAlpha();
            int positionInInitiative = combatantList.getInitiativeIndexOf(position);
            boolean isChecked = CombatantCompletedCheck.isChecked();

            // Get the current state, and assign appropriate values to the variables
            if (isChecked) {
                // If the Combatant is checked off, then they should be grayed out
                // TODO CHECK: If Combatant is currently active but checked, what should happen...?  Nothing?  Should they be skipped?  Settings option? "Autoskip checked Combatants"?
                targetAlpha = GRAYED_OUT;
            } else {
                // If the Combatant is unchecked, then their ViewHolder should be visible
                targetAlpha = CLEAR;
            }

            if (curActiveCombatant == positionInInitiative) {
                // If this is the currently active combatant
                borderColor = R.color.activeCombatant; // The border should always reflect the fact that they are the currently selected combatant
            } else if (curActiveCombatant > positionInInitiative && !isChecked) {
                // If this Combatant has already had their turn, but they are unchecked
                borderColor = R.color.returnToCombatant; // Let the user know to return back to this Combatant later
            }

            if (curActiveCombatant == UNSET) {
                // If we are currently between rounds, then hide the roll and total initiative
                initiativeRollVisibility = View.INVISIBLE;
                checkLayoutVisibility = View.GONE;
            } else {
                // If we are in the middle of the round, then make sure both roll and total initiative are visible
                initiativeRollVisibility = View.VISIBLE;
                checkLayoutVisibility = View.VISIBLE;
            }

            // Finally, assign the settings to the variable GUI elements
            // Set the Combatant to be grayed out, if needed
            if (currentAlpha != targetAlpha) {
                // If the current alpha value is not what we want it to be, animate the change
                AlphaAnimation alphaAnim = new AlphaAnimation(currentAlpha, targetAlpha);
                alphaAnim.setDuration(300); // Animation should take .3s
                alphaAnim.setFillAfter(true); // Persist the new alpha after the animation ends
                CombatantGrayout.startAnimation(alphaAnim);
            }

            // Set the Combatant's ViewHolder border color
            CombatantStatusBorder.setBackgroundColor(CombatantStatusBorder.getContext().getResources().getColor(borderColor));

            // Set the visibility of the roll, total initiative, and checkbox views
            RollLayout.setVisibility(initiativeRollVisibility);
            TotalInitiativeView.setVisibility(initiativeRollVisibility);
            CombatantCompletedCheck.setVisibility(checkLayoutVisibility);
        }

        public void setChecked(boolean isChecked) {
            // Should the Combatant's checkbox be checked or not?
            CombatantCompletedCheck.setChecked(isChecked); // The function setCombatantProgression will be automatically called due to the listener on the CheckBox, so the GUI will be updated
        }
    }

    private void setRollValue(int combatantInd, int newRollVal) {
        // Edit the indicated Combatant, resort the list, and update the GUI
        combatantList.get(combatantInd).setRoll(newRollVal);
        reSortInitiative();
    }

    private void setSpeedFactor(int combatantInd, int newSpeedFactor) {
        // Edit the indicated Combatant, resort the list, and update the GUI
        combatantList.get(combatantInd).setSpeedFactor(newSpeedFactor);
        reSortInitiative();
    }

    private void reSortInitiative() {
        if (combatantList.getCurrentSortMethod() == EncounterCombatantList.SortMethod.INITIATIVE) {
            // If the Combatant list is currently sorted by initiative, then resort the list and reset the GUI
            combatantList.reSort();

            // Let the adapter know that at least one Combatant has changed its Initiative values, so all of the orders may now be different
            notifyCombatantsChanged();

            // Also, make ALL Combatants update their progress-related GUI
            Bundle payload = new Bundle();
            payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true);

            notifyItemRangeChanged(0, combatantList.size(), payload); // Let the Combatant know it know that it should just update its progress-related GUI, and nothing else
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
    public void onBindViewHolder(@NonNull CombatantViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.get(0) instanceof Bundle) {
            // If the payload is not empty, deal with the payload
            Bundle args = (Bundle) payloads.get(0);

            if (args.containsKey(PAYLOAD_CHECK)) {
                // If the payload is a boolean, then it represents whether the Combatant should or should not be checked off
                // Case 1: The holder represents the combatant whose turn just finished, and it will become checked
                // Case 2: The round just ended, so all Combatants become unchecked
                holder.setChecked(args.getBoolean(PAYLOAD_CHECK));
            }

            if (args.containsKey(PAYLOAD_UPDATE_PROGRESS)) {
                // If the payload is not a boolean (probably just an Object), then the Combatant's combat progress related GUI elements should be updated, with no other changes
                // Likely case: The holder represents the currently active Combatant
                holder.setCombatProgression();
            }

            if (args.containsKey(PAYLOAD_DICE_CHEAT)) {
                holder.setDiceCheatMode();
            }

            if (args.containsKey(PAYLOAD_DUPLICATE_INDICATOR)) {
                holder.setDuplicateInitiativeView();
            }
        } else {
            // If the payload is empty, then continue on the binding process
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return combatantList.size();
    }

    // TODO: Do I need this...?  Perhaps for Activity management stuffs?
    public void setCombatantList(ArrayList<Combatant> newCombatantList) {
        combatantList = new EncounterCombatantList(newCombatantList);

        // Let the adapter know that it should refresh the Views
        Bundle payload = new Bundle();
        payload.putBoolean(PAYLOAD_DUPLICATE_INDICATOR, true); // Initiative has been rolled, so there may now be duplicates
        payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true); // Combatant order has been changed, so progress may have changed

        notifyItemRangeChanged(0, combatantList.size(), payload); // Let the adapter know that every Combatant's view should update its duplicate indicator
        notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange
    }

    public void notifyCombatantsChanged() {
        // If anything about the combatants has changed, see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantDiffUtil(combatantList_Memory, combatantList));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the combatantList
        combatantList_Memory = combatantList;

    }

    public void sort(EncounterCombatantList.SortMethod sortMethod) {
        combatantList.sort(sortMethod); // Perform the sorting
        notifyCombatantsChanged(); // Update the display
    }

    public void toggleDiceCheat() {
        diceCheatModeOn = !diceCheatModeOn; // Toggle the state of Dice Cheat mode

        // Let each Combatant know that it should update its dice cheat mode status
        Bundle payload = new Bundle();
        payload.putBoolean(PAYLOAD_DICE_CHEAT, diceCheatModeOn);

        notifyItemRangeChanged(0, combatantList.size(), payload); // Send the payload to every Combatant
    }

    public int getCurActiveCombatant() {
        return curActiveCombatant;
    }

    public int getCurRoundNumber() {
        return curRoundNumber;
    }

    public void iterateCombatStep() {
        // Before changing the combatant step, let the currently selected Combatant (if it exists) that they should be checked
        if (curActiveCombatant != UNSET) {
            Bundle payload = new Bundle();
            payload.putBoolean(PAYLOAD_CHECK, true);

            notifyItemChanged(curActiveCombatant, payload); // Holder will become checked
        }

        // Depending on where we are in the combat cycle, iterate the curActiveCombatant value differently
        if (curActiveCombatant == UNSET) {
            curActiveCombatant = 0; // If the currently active combatant is unset, initialize the currently active combatant

            // Also, ROLL INITIATIVE!!!!!
            combatantList.rollInitiative(); // TODO CHECK: This technically means that going back after this gives you the chance to re-roll initiative, which COULD be an issue.  Can just keep track of which rounds have been rolled already
            combatantList.sort(EncounterCombatantList.SortMethod.INITIATIVE); // Sort by initiative, now that we have calculated it

            // Let the adapter know that it should refresh the Views
            // TODO CHECK: Try not including this notifyItemRangeChanged, see if notifyCombatantsChanged would work on its own...? Change for future similar calls in this and reverseCombatStep()
            Bundle payload = new Bundle();
            payload.putBoolean(PAYLOAD_DUPLICATE_INDICATOR, true); // Initiative has been rolled, so there may now be duplicates
            payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true); // Combatant order has been changed, so progress may have changed TODO: May not be needed?

            notifyItemRangeChanged(0, combatantList.size(), payload); // Let the adapter know that every Combatant's view should update its duplicate indicator
            notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange

            // TODO LATER: Keep track of all rolls, so that you can go backwards in time!
            // TODO SOON: How do I deal with ties in initiative? Go by alphabet, favor party, "connect" the tied Combatants together somehow...?  May need to be a setting...
        } else if (curActiveCombatant == (combatantList.size() - 1)) {
            curActiveCombatant = UNSET; // If we are at the end of the Combatant list, start preparing for the next round

            combatantList.sort(EncounterCombatantList.SortMethod.ALPHABETICALLY_BY_FACTION); // Go back to sorting by alphabet/faction, for pre-round prep

            // Let the adapter know to change the Views
            Bundle payload = new Bundle();
            payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true); // We are ending the combat round, so progress must be changed (roll and initiative views will go away, etc)
            payload.putBoolean(PAYLOAD_CHECK, false); // Uncheck all of the combatants

            notifyItemRangeChanged(0, combatantList.size(), payload); // Let the adapter know that every Combatant's view should update its duplicate indicator
            notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange
        } else {
            curActiveCombatant++; // Otherwise, just increment the combatant number
        }

        // Now that we've changed the combat step, let the currently selected Combatant (if it exists) that it should update its GUI.  If we are now between rounds, let EVERY Combatant know to update its GUI
        if (curActiveCombatant != UNSET) {
            // If we are still in the combat round
            Bundle payload = new Bundle();
            payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true);

            notifyItemChanged(curActiveCombatant, payload); // Let the currently active Combatant know it should update its combat-related progress
        } else {
            // If we are no longer in the combat round and are now waiting to roll initiative for the next round, increment the round number
            curRoundNumber++; // This value will most likely be queried by the Activity right after this function is called
        }
    }

    public void reverseCombatStep() {
        // Before changing the combatant step, let the currently selected Combatant (if it exists) to update its progression GUI, because it will no longer the currently selected Combatant
        if (curActiveCombatant != UNSET) {
            Bundle payload = new Bundle();
            payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true);

            notifyItemChanged(curActiveCombatant, payload); // Let the currently active Combatant know it should update its combat-related progress
        }

        // Move backwards along the active Combatant in the list
        if (curActiveCombatant == UNSET) {
            curActiveCombatant = combatantList.size() - 1; // If the currently active combatant is unset, go back to the end of the list

            combatantList.sort(EncounterCombatantList.SortMethod.INITIATIVE); // Sort by initiative, now that we are back in the previous round

            // Let the adapter know to change the Views
            Bundle payload = new Bundle();
            payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true); // We are ending the combat round, so progress must be changed (roll and initiative views will go away, etc)
            payload.putBoolean(PAYLOAD_CHECK, true); // Check all of the combatants

            notifyItemRangeChanged(0, combatantList.size(), payload); // Let the adapter know that every Combatant's view should update its duplicate indicator
            notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange
        } else if (curActiveCombatant == 0) {
            curActiveCombatant = UNSET; // If we are at the beginning of the list, go to "UNSET" for now

            combatantList.sort(EncounterCombatantList.SortMethod.ALPHABETICALLY_BY_FACTION); // Go back to sorting by alphabet/faction, for pre-round prep

            // Let the adapter know to change the Views
            Bundle payload = new Bundle();
            payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true); // We are ending the combat round, so progress must be changed (roll and initiative views will go away, etc)

            notifyItemRangeChanged(0, combatantList.size(), payload); // Let the adapter know that every Combatant's view should update its duplicate indicator
            notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange
        } else {
            curActiveCombatant--; // Otherwise, just decrement
        }

        // Now selected is UNSET - nothing (no one was checked anyway)
        // Now selected is final - everyone except final becomes checked
        // Now selected is not UNSET and not final - the current Combatant becomes unchecked

        // Change the round number, if needed
        if (curActiveCombatant != (combatantList.size() - 1)) {
            // If the currently selected Combatant is not the last Combatant...
            if (curActiveCombatant != UNSET) {
                // ...and we are not between rounds, then the now currently active Combatant should be unchecked
                Bundle payload = new Bundle();
                payload.putBoolean(PAYLOAD_CHECK, false);

                notifyItemChanged(curActiveCombatant, payload); // Holder will become unchecked
            }
        } else {
            // If the currently active Combatant is the last Combatant, we just went back into the previous round of Combat
            curRoundNumber--; // This value will most likely be queried by the Activity right after this function is called
        }
    }
}
