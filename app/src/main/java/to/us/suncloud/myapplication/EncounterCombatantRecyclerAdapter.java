package to.us.suncloud.myapplication;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

// To display combatants in a list for the encounter.  Supports reordering and tracking who has gone already.
public class EncounterCombatantRecyclerAdapter extends RecyclerView.Adapter<EncounterCombatantRecyclerAdapter.CombatantViewHolder> {
    public static final int UNSET = -1;
    private static final float GRAYED_OUT = 0.5f;
    private static final float CLEAR = 0f;

    private static final String PAYLOAD_CHECK = "payloadCheck"; // Used to indicate that a Combatant should become checked or unchecked (infers an update progress call as well)
    private static final String PAYLOAD_UPDATE_PROGRESS = "payloadUpdateProgress"; // Used any time a Combatant becomes checked/unchecked (this may happen by the user, or programmatically)
    private static final String PAYLOAD_DICE_CHEAT = "payloadDiceCheat"; // Used any time dice cheat mode is turned on/off
//    private static final String PAYLOAD_DUPLICATE_INDICATOR = "payloadDuplicateIndicator"; // Used any time the color of the duplicate indicator may be changed (if initiative is re-rolled, or if the combatantList is modified by addition/removal)

    RecyclerView combatantRecyclerView;

    private EncounterCombatantList combatantList;
    private EncounterCombatantList combatantList_Memory; // A memory version of the list, to see what changes have occurred
    //    private ArrayList<Boolean> isCheckedList;
    private HashMap<String, Boolean> isCheckedMap; // A Map to keep track of which Combatants have been checked off

    ArrayList<Integer> iconResourceIds; // A list of resource ID's of the icons that will be used for each Combatant

    boolean diceCheatModeOn = false; // Are we currently in the dice cheat mode?

    private int curActiveCombatant = UNSET; // The currently active combatant, as an index in combatantList (if -1, there is no active combatant)
    private int curRoundNumber = 1; // The current round number (iterated each time the active combatant loops around)

    EncounterCombatantRecyclerAdapter(Context context, AllFactionCombatantLists combatantList) {
        // Turn the AllFactionCombatantList into an EncounterCombatantList
        this.combatantList = new EncounterCombatantList(combatantList); // Hold onto the combatant list (deep copy clone)
        this.combatantList_Memory = this.combatantList.clone(); // Keep a second copy, for memory (a second clone)
//        isCheckedList = new ArrayList<>(Collections.nCopies(this.combatantList.size(), false));

        // Initialize the isCheckedMap using the
        isCheckedMap = new HashMap<>();

        for (int combatantNum = 0; combatantNum < combatantList.size(); combatantNum++) {
            isCheckedMap.put(combatantList.get(combatantNum).getName(), false); // Initialize each Combatant to being unchecked
        }

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
        boolean modifyingSelf = false;

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
            DuplicateIndicator = itemView.findViewById(R.id.check_enc_layout);

            CombatantCompletedCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (modifyingSelf) {
                        return;
                    }

                    // Update the isChecked List
                    isCheckedMap.put(combatantList.get(getAdapterPosition()).getName(), b);

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
                    if (modifyingSelf) {
                        return;
                    }

                    // First, make sure the value here is valid
                    int currentSpeedFactor = combatantList.get(getAdapterPosition()).getSpeedFactor(); // The current roll for this Combatant
                    int newSpeedFactor = currentSpeedFactor; // Initial value never used, but at least the IDE won't yell at me...
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
                        modifyingSelf = true;
                        RollViewEdit.setText(String.valueOf(currentSpeedFactor));
                        modifyingSelf = false;
                        return;
                    } else if (newSpeedFactor == currentSpeedFactor) {
                        // If the new speed factor value is the same as the current speed factor, then don't do anything
                        return;
                    }

                    // If the entered text was valid and different than the current speed factor, then change this Combatant, resort the list, and update the GUI
                    setSpeedFactor(getAdapterPosition(), newSpeedFactor);
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
                    // TODO SOON: if initiative is altered mid-combat, and the Combatant moves before the currently active combatant, then the currently active combatant appears to become a Combatant that already went.  Fix this!!

                    // If this is being called due to a programmatic change, ignore it
                    if (modifyingSelf) {
                        return;
                    }

                    // First, make sure the value here is valid
                    int currentRoll = combatantList.get(getAdapterPosition()).getRoll(); // The current roll for this Combatant
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
                        modifyingSelf = true;
                        RollViewEdit.setText(String.valueOf(currentRoll));
                        modifyingSelf = false;
                        return;
                    } else if (newRollVal == currentRoll) {
                        // If the new roll value is the same as the current roll, then don't do anything
                        return;
                    }

                    // If the entered text was valid and different than the current roll, then change this Combatant, resort the list, and update the GUI
                    RollView.setText(String.valueOf(newRollVal)); // Set this value in the TextView
                    setRollValue(getAdapterPosition(), newRollVal);
                }
            });
        }

        public void bind(int combatant_ind) {
            Combatant thisCombatant = combatantList.get(combatant_ind);

            NameView.setText(thisCombatant.getName());


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

            // Update the GUI indicators
            updateState();
            setInitValues();
            setDiceCheatGUI();
        }

        public void updateState() {
            // Update the full state of the Combatant
            setDuplicateInitiativeView();
            setInitValues();
            setCombatProgression();
        }

        public void setInitValues() {
            // Update the values of the initiative counters
            Combatant thisCombatant = combatantList.get(getAdapterPosition());
            TotalInitiativeView.setText(String.valueOf(thisCombatant.getTotalInitiative()));
            RollView.setText(String.valueOf(thisCombatant.getRoll()));

            modifyingSelf = true;
            SpeedFactorView.setText(String.valueOf(thisCombatant.getSpeedFactor()));
            RollViewEdit.setText(String.valueOf(thisCombatant.getRoll()));
            modifyingSelf = false;
        }

        public void setDiceCheatGUI() {
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
            if (curActiveCombatant == UNSET || !combatantList.isDuplicate(getAdapterPosition())) {
                // If this is not a duplicate initiative, or we are between rounds
                resourceID = R.color.softBackground;
            } else {
                // If this is a duplicate, get the color that it should be
                switch (combatantList.getDuplicateColor(getAdapterPosition())) {
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
            int position = getAdapterPosition();
            int positionInInitiative = getInitiativeInd(position); // Get the initiative index
//            int positionInInitiative = position;

            // Set up variables for everything that may change
            float targetAlpha;
            int borderColor = R.color.standardBackground; // Initialize to a "blank" border
            int initiativeRollVisibility;
            int checkLayoutVisibility;

            // Get some useful parameters
            float currentAlpha = CombatantGrayout.getAlpha();
            boolean isChecked;
            try {
                isChecked = isCheckedMap.get(combatantList.get(position).getName());
            } catch (NullPointerException e) {
                isChecked = false;
            }

            // Get the current state, and assign appropriate values to the variables

            // If the Combatant is checked off in the ArrayList (could happen without the ViewHolder knowing if the user uses the Next/Previous buttons), then make sure the GUI checkbox is checked
            modifyingSelf = true;
            CombatantCompletedCheck.setChecked(isChecked); // Will not run code in Listener as long as modifyingSelf is true during the call
            modifyingSelf = false;

            // See if the Combatant should be grayed out or not
            if (!isChecked || curActiveCombatant == UNSET) {
                // TODO CHECK: If Combatant is currently active but checked, what should happen...?  Nothing?  Should they be skipped?  Settings option? "Autoskip checked Combatants"?
                // If the Combatant is unchecked, or we are between combat rounds, then their ViewHolder should be visible
                targetAlpha = CLEAR;
            } else {
                // If the Combatant is checked off and we are in combat, then they should be grayed out
                targetAlpha = GRAYED_OUT;
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
//                AlphaAnimation alphaAnim = new AlphaAnimation(currentAlpha, targetAlpha);
//                alphaAnim.setDuration(300); // Animation should take .3s
//                alphaAnim.setFillAfter(true); // Persist the new alpha after the animation ends
//                CombatantGrayout.startAnimation(alphaAnim);
                CombatantGrayout.animate().alpha(targetAlpha).setDuration(300).setListener(null);

            }

            // Set the Combatant's ViewHolder border color
            CombatantStatusBorder.setBackgroundColor(CombatantStatusBorder.getContext().getResources().getColor(borderColor));

            // Set the visibility of the roll, total initiative, and checkbox views
            RollLayout.setVisibility(initiativeRollVisibility);
            TotalInitiativeView.setVisibility(initiativeRollVisibility);
            CombatantCompletedCheck.setVisibility(checkLayoutVisibility);
        }

        public void setChecked(boolean isChecked) {
            if (!modifyingSelf) {
                // Should the Combatant's checkbox be checked or not?
                CombatantCompletedCheck.setChecked(isChecked); // The function setCombatantProgression will be automatically called due to the listener on the CheckBox, so the GUI will be updated
            }
        }
    }

    private int getInitiativeInd(int position) {
        // Given a Combatant index in combatantList, get that Combatant's index in the initiative order
        return combatantList.getInitiativeIndexOf(position);
    }

    private int getViewInd(int position) {
        // Given a Combatant's position in the initiative order, get that Combatant's index in the current order
        return combatantList.getViewIndexOf(position);
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
            combatantList.resort();
        }

        // Let the adapter know that at least one Combatant has changed its Initiative values, so all of the orders may now be different
        notifyCombatantsChanged();

        // Also, make ALL Combatants update their progress-related GUI
        updateAllViewStates();
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
                // Update all GUI indicators
                holder.updateState();
            }

            if (args.containsKey(PAYLOAD_DICE_CHEAT)) {
                // Update only the dice-cheat indicators
                holder.setDiceCheatGUI();
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
        updateAllViewStates();
        notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange
    }

    public AllFactionCombatantLists getCombatantList() {
        return new AllFactionCombatantLists(combatantList);
    }

    public void notifyCombatantsChanged() {
        // If anything about the combatants has changed, see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantDiffUtil(combatantList_Memory, combatantList));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the combatantList
        combatantList_Memory = combatantList.clone();
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

    public void setRoundNumber(int curRoundNumber) {
        this.curRoundNumber = curRoundNumber;
    }

    public void iterateCombatStep() {
        // Before changing the combatant step, let the currently selected Combatant (if it exists) know that they should be checked off before we move on
        if (curActiveCombatant != UNSET) {
            // TODO SOON: Really nail down exactly what curActiveCombatant refers to, and how to get the Combatant we want using getInitiativeInd
            setViewIsChecked(getViewInd(curActiveCombatant), true);
        }

        // Depending on where we are in the combat cycle, iterate the curActiveCombatant value differently
        if (curActiveCombatant == UNSET) {
            // Going from the prepare phase to combat
            curActiveCombatant = 0; // Initialize the currently active combatant to the first Combatant

            // Also, ROLL INITIATIVE!!!!!
            combatantList.rollInitiative(); // TODO CHECK: This technically means that going back after this gives you the chance to re-roll initiative, which COULD be an issue.  Can just keep track of which rounds have been rolled already
            combatantList.sort(EncounterCombatantList.SortMethod.INITIATIVE); // Sort by initiative, now that we have calculated it

            // Let the adapter know that it should refresh the Views
            // TODO CHECK: Try not including this notifyItemRangeChanged, see if notifyCombatantsChanged would work on its own...? Change for future similar calls in this and reverseCombatStep()
            notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange

            // TODO LATER: Keep track of all rolls, so that you can go backwards in time!
            // TODO SOON: How do I deal with ties in initiative? Go by alphabet, favor party, "connect" the tied Combatants together somehow...?  May need to be a setting...
        } else if (curActiveCombatant == (combatantList.size() - 1)) {
            // Going from the last Combatant to the prepare phase
            curActiveCombatant = UNSET; // If we are at the end of the Combatant list, start preparing for the next round

            combatantList.sort(EncounterCombatantList.SortMethod.ALPHABETICALLY_BY_FACTION); // Go back to sorting by alphabet/faction, for pre-round prep

            // Let the adapter know to change the Views
            setAllViewIsChecked(false); // Uncheck all of the combatants
            notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange
        } else {
            // Staying in combat
            curActiveCombatant++; // Otherwise, just increment the combatant number
        }

        // Update the round number, if needed
        if (curActiveCombatant == UNSET) {
            curRoundNumber++; // This value will most likely be queried by the Activity right after this function is called
        }

        // Finally, update all GUI states
        updateAllViewStates();
    }

    public void reverseCombatStep() {
        // Move backwards along the active Combatant in the list
        if (curActiveCombatant == UNSET) {
            // Going from the prepare phase back to the last Combatant of the last round
            curActiveCombatant = combatantList.size() - 1; // If the currently active combatant is unset, go back to the end of the list

            combatantList.sort(EncounterCombatantList.SortMethod.INITIATIVE); // Sort by initiative, now that we are back in the previous round

            // Let the adapter know to change the Views
            setAllViewIsChecked(true);

            notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange
        } else if (curActiveCombatant == 0) {
            // Going from first Combatant back to the prepare phase
            curActiveCombatant = UNSET; // If we are at the beginning of the list, go to "UNSET" for now

            combatantList.sort(EncounterCombatantList.SortMethod.ALPHABETICALLY_BY_FACTION); // Go back to sorting by alphabet/faction, for pre-round prep

            // Let the adapter know to change the Views
            notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange
        } else {
            // Staying in combat
            curActiveCombatant--; // Otherwise, just decrement
        }

        // Now selected is UNSET - nothing (no one was checked anyway)
        // Now selected is last Combatant - everyone except final becomes checked
        // Now selected is not UNSET and not last Combatant - the current Combatant becomes unchecked

        // Uncheck the current Combatant
        if (curActiveCombatant != UNSET) {
            // ...and we are not between rounds, then the now currently active Combatant should be unchecked
            setViewIsChecked(getViewInd(curActiveCombatant), false);
        }

        if (curActiveCombatant == (combatantList.size() - 1)) {
            // If the currently active Combatant is the last Combatant, we just went back into the previous round of Combat
            curRoundNumber--; // This value will most likely be queried by the Activity right after this function is called
        }

        // Finally, update all GUI states
        updateAllViewStates();
    }

    private void updateAllViewStates() {
        // Update the GUI of all Combatants
        Bundle payload = new Bundle();
        payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true); // We are ending the combat round, so progress must be changed (roll and initiative views will go away, etc)

        notifyItemRangeChanged(0, combatantList.size(), payload); // Let the adapter know that every Combatant's view should update its duplicate indicator
//        notifyDataSetChanged();
    }

    private void setAllViewIsChecked(boolean isChecked) {
        for (int i = 0;i < combatantList.size();i++) {
            isCheckedMap.put(combatantList.get(i).getName(), isChecked);
        }
    }

    private void setViewIsChecked(int viewPos, boolean isChecked) {
        // Only check a single Combatant (viewPos is according to the current layout)
//        setViewIsChecked(viewPos, 1, isChecked);
        isCheckedMap.put(combatantList.get(viewPos).getName(), isChecked);
    }

//    private void setViewIsChecked(int positionStart, int numViews, boolean isChecked) {
//        // Set a range of Combatants to be checked off (positions all according to the initiative sorted order
//        for (int i = 0; i < numViews; i++) {
//            // Find the Combatant currently at this position *in the initiative order*, then use its name as a key to update the current value of isChecked
//            isCheckedMap.put(combatantList.get(getInitiativeInd(positionStart)).getName(), isChecked);
//        }
//
////        notifyDataSetChanged();
//
////        Bundle payload = new Bundle();
////        payload.putBoolean(PAYLOAD_CHECK, isChecked); // Check all of the combatants
////        notifyItemRangeChanged(positionStart, numViews, payload); // Let the adapter know that all Combatants except the last should now become checked (the last Combatant is technically never checked off...)
//    }

    public void toggleDiceCheat() {
        diceCheatModeOn = !diceCheatModeOn; // Toggle the state of Dice Cheat mode

        // Let each Combatant know that it should update its dice cheat mode status
        Bundle payload = new Bundle();
        payload.putBoolean(PAYLOAD_DICE_CHEAT, diceCheatModeOn);

        notifyItemRangeChanged(0, combatantList.size(), payload); // Send the payload to every Combatant
    }
}
