package to.us.suncloud.myapplication;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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

// To display combatants in a list for the encounter.  Supports reordering and tracking who has gone already.
public class EncounterCombatantRecyclerAdapter extends RecyclerView.Adapter<EncounterCombatantRecyclerAdapter.CombatantViewHolder> {
    public static final int PREP_PHASE = -1;
    public static final int END_OF_ROUND_ACTIONS = -2;
    private static final float GRAYED_OUT = 0.5f;
    private static final float CLEAR = 0f;

    View focusView = null;

    //    private static final String PAYLOAD_CHECK = "payloadCheck"; // Used to indicate that a Combatant should become checked or unchecked (infers an update progress call as well)
//    private static final String PAYLOAD_UPDATE_PROGRESS = "payloadUpdateProgress"; // Used any time a Combatant becomes checked/unchecked (this may happen by the user, or programmatically)
    private static final String PAYLOAD_DICE_CHEAT = "payloadDiceCheat"; // Used any time dice cheat mode is turned on/off
//    private static final String PAYLOAD_DUPLICATE_INDICATOR = "payloadDuplicateIndicator"; // Used any time the color of the duplicate indicator may be changed (if initiative is re-rolled, or if the combatantList is modified by addition/removal)

    RecyclerView combatantRecyclerView;
    combatProgressInterface parent;

    private final EncounterCombatantList combatantList;
    private EncounterCombatantList combatantList_Memory; // A memory version of the list, to see what changes have occurred
    //    private ArrayList<Boolean> isCheckedList;
//    private HashMap<String, Boolean> isCheckedMap; // A Map to keep track of which Combatants have been checked off

    ArrayList<Integer> iconResourceIds; // A list of resource ID's of the icons that will be used for each Combatant

    boolean diceCheatModeOn = false; // Are we currently in the dice cheat mode?

    boolean doingEndOfRoundActions; // Should we be doing the end-of-round actions?


    //    private int curActiveCombatant = PREP_PHASE; // The currently active Combatant, as an index in combatantList (if -1, there is no active Combatant)
//    private int prevActiveCombatant = PREP_PHASE; // The currently active Combatant, as an index in combatantList (if -1, there is no active Combatant)
    private int curRoundNumber = 1; // The current round number (iterated each time the active Combatant loops around)
    private EncounterCombatantList.SortMethod curSortMethod; // The current method by which the Combatants should be sorted (when possible, i.e. not in Prep-phase)

    EncounterCombatantRecyclerAdapter(combatProgressInterface parent, EncounterCombatantList combatantList, int curRoundNumber) {
        // Turn the AllFactionCombatantList into an EncounterCombatantList
        this.combatantList = combatantList; // Hold onto the Combatant list (copy reference)
        this.parent = parent;
        this.curRoundNumber = curRoundNumber;
        Context context = parent.getContext();

//        isCheckedList = new ArrayList<>(Collections.nCopies(this.combatantList.size(), false));

        // Initialize the isCheckedMap using the Combatant List
//        isCheckedMap = new HashMap<>();

//        for (int combatantNum = 0; combatantNum < combatantList.size(); combatantNum++) {
//            isCheckedMap.put(combatantList.get(combatantNum).getName(), false); // Initialize each Combatant to being unchecked
//        }

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

        // Set up the endOfRound pause system
        doingEndOfRoundActions = PrefsHelper.doingEndOfRound(parent.getContext());
//        endOfRoundCompleted = !doEndOfRoundActions;

        // Set the current round for the combatantList (initializes rolls for any new Combatants that haven't been initialized yet)
        curSortMethod = EncounterCombatantList.SortMethod.INITIATIVE; // Always initialize to initiative sorting

        this.combatantList.setRoundNumber(curRoundNumber);
        this.combatantList_Memory = this.combatantList.clone(); // Keep a second copy, for memory (a clone)
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        // Save a reference to the RecyclerView
        combatantRecyclerView = recyclerView;
    }

    // Checkboxes:
    // Checkboxes initialize unchecked
    // Hitting next button checks and grays out the currently selected Combatant, then selects the next Combatant
    // Un-checking a previously checked Combatant will highlight them a different color to remind you to revisit them
    // Should be an "un-next" button (small) to scroll back up the list and undo this stuff

    // Features:
    // Must be able to edit modifiers after rolling (at any time)
    // Upon changing modifier, isChecked should NOT CHANGE (only coloring and such according to current position of selected Combatant)

    class CombatantViewHolder extends RecyclerView.ViewHolder {
        boolean modifyingSelf = false;

        private final TextView NameView;
        private final TextView TotalInitiativeView;
        private final TextView RollView;
        private final EditText RollViewEdit;
        private final EditText ModifierView;
        private final ImageView CombatantIcon;

        private final ConstraintLayout CombatantStatusBorder;
        private final ConstraintLayout CombatantIconBorder;
        private final ConstraintLayout RollLayout;
        private final ConstraintLayout DuplicateIndicator;

        private ViewPropertyAnimator alphaAnimation;

        private final CheckBox CombatantCompletedCheck;
        private final ConstraintLayout CombatantGrayout;

        public CombatantViewHolder(@NonNull View itemView) {
            super(itemView);

            NameView = itemView.findViewById(R.id.combatant_enc_name);
            TotalInitiativeView = itemView.findViewById(R.id.combatant_enc_total_initiative);
            RollView = itemView.findViewById(R.id.combatant_enc_roll);
            RollViewEdit = itemView.findViewById(R.id.combatant_enc_roll_edit); // For CHEATERS
            ModifierView = itemView.findViewById(R.id.combatant_enc_modifier);
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
                    // Check off this Combatant, and update the progress of the combat round
                    if (!modifyingSelf) {
                        // TO_DO LATER: If we want to have multiple ways of calculating the currently active Combatant, we can do a check here:
                        //  If this is NOT the currently active Combatant, but it would cause us to go into the next round (nextActiveCombatant, as calculated in EncounterActivity#updateGUIState(), is PREP_PHASE), then make sure the user actually wants to go to the next round
//                        if (b && getInitiativeInd(combatantList, getAdapterPosition()) == (combatantList.size() - 1) && getInitiativeInd(combatantList, getAdapterPosition()) != activeCombatant()) {
//                            // Otherwise, undo the check off on the GUI
//                            new AlertDialog.Builder(CombatantCompletedCheck.getContext())
//                                    .setTitle("Finish Round?")
//                                    .setMessage("Are you sure you would like to finish this combat round?")
//                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            // Continue with the rest of the onCheckedChangeListener
//                                            setAllIsChecked(true); // Check of all Combatants
//                                            curRoundNumber++; // Go to the next round
//                                            updateCombatProgress(); // Update combatantList
//                                            notifyCombatantsChanged(); // Update the GUI
//                                        }
//                                    })
//                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            // Undo the check from the GUI
//                                            modifyingSelf = true;
//                                            CombatantCompletedCheck.setChecked(false);
//                                            modifyingSelf = false;
//                                        }
//                                    })
//                                    .show();
//                            return;
//                        }

                        // Update the Combatant, and update the GUI according to the new information
                        // Update the isChecked List
                        setIsChecked(getAdapterPosition(), b);

                        // Update progression
                        int curActiveCombatant = activeCombatant();

                        // Update the round number, if needed (done here, so it can be used in updateCombatProgress)
                        if (curActiveCombatant == PREP_PHASE) {
                            curRoundNumber++;
                        }

                        // Update the combatantList based on the new active Combatant
                        updateCombatProgress(curActiveCombatant);

                        // Let the adapter know to update
                        notifyCombatantsChanged();

//                        if (getInitiativeInd(combatantList, getAdapterPosition()) == curActiveCombatant) {
//                            // If this is the currently selected Combatant, go to the next one
//                            incrementCombatStep(); // Will call notifyCombatantsChanged()
//                        } else {
//                            // The has completed value should follow the state of the boolean
//                            notifyCombatantsChanged();
//                        }
                    }
                }
            });

            // The modifier / roll value can be set by the user by either a) pressing done/return, or b) clicking out of the EditText box
            ModifierView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (v instanceof EditText) {
                        if (hasFocus) {
                            focusView = ModifierView;

                            // If we are gaining focus, select all text
                            ((EditText) v).selectAll();

                            if (combatantList.isLastVisibleCombatant(getAdapterPosition())) {
//                            if (getAdapterPosition() == (combatantList.size() - 1)) {
                                // If this is the last Combatant, then its IME action should be "Done" instead of the default "Next" (as set in the xml)
                                ((EditText) v).setImeOptions(EditorInfo.IME_ACTION_DONE);
                            }

//                            // Show the keyboard
//                            InputMethodManager imm = (InputMethodManager) parent.getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
//                            imm.showSoftInput(SpeedFactorView, 0);
                        } else {
                            // If we have lost focus, the user has likely navigated away.  So, confirm the new value

                            // It damn well better be...
                            // Perform this in a new Runnable, so we don't get an error from the RecyclerView if scrolling
                            ModifierView.post(new Runnable() {
                                @Override
                                public void run() {
                                    setModifierValueIfValid(ModifierView.getText().toString()); // Set the new modifier, if possible
                                }
                            });

//                            // Hide the keyboard
//                            InputMethodManager imm = (InputMethodManager) parent.getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
//                            imm.hideSoftInputFromWindow(SpeedFactorView.getWindowToken(), 0);
                        }
                    }
                }
            });

            ModifierView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
//                    if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        if (event == null || !event.isShiftPressed()) {
                            // The user is done typing, so attempt to set the modifier
                            ModifierView.post(new Runnable() {
                                @Override
                                public void run() {
                                    setModifierValueIfValid(ModifierView.getText().toString()); // Set the new modifier, if possible
                                }
                            });

                            // If the user hit Done, then hide the keyboard
                            InputMethodManager imm = (InputMethodManager) parent.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(ModifierView.getWindowToken(), 0);

                            // Finally, clear the focus when the Done button is pressed
                            ModifierView.clearFocus();
                            return true;
                        }
                    }
                    return false; // Pass on to other listeners.
                }
            });

            RollViewEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        // If we have lost focus, the user has likely navigated away.  So, confirm the new value
                        if (v instanceof EditText) {
                            if (combatantList.isLastVisibleCombatant(getAdapterPosition())) {
//                            if (getAdapterPosition() == (combatantList.size() - 1)) {
                                // If this is the last Combatant, then its IME action should be "Done" instead of the default "Next" (as set in the xml)
                                ((EditText) v).setImeOptions(EditorInfo.IME_ACTION_DONE);
                            }

                            // It damn well better be...
                            RollViewEdit.post(new Runnable() {
                                @Override
                                public void run() {
                                    setRollValueIfValid(RollViewEdit.getText().toString()); // Set the new modifier, if possible
                                }
                            });
                        }
                    }
                }
            });

            RollViewEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
//                    if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        if (event == null || !event.isShiftPressed()) {
                            // The user is done typing, so attempt to set the modifier
                            RollViewEdit.post(new Runnable() {
                                @Override
                                public void run() {
                                    setRollValueIfValid(RollViewEdit.getText().toString()); // Set the new modifier, if possible
                                }
                            });

                            setRollValueIfValid(v.getText().toString());
                            return true;
                        }
                    }
                    return false; // Pass on to other listeners.
                }
            });

//            SpeedFactorView.addTextChangedListener(new TextWatcher() {
//                @Override
//                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//                }
//
//                @Override
//                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//                }
//
//                @Override
//                public void afterTextChanged(Editable editable) {
//                    // If this is being called due to a programmatic change, ignore it
//                    if (modifyingSelf) {
//                        return;
//                    }
//
//                    // First, make sure the value here is valid
//                    int currentSpeedFactor = combatantList.get(getAdapterPosition()).getSpeedFactor(); // The current roll for this Combatant
//                    int newSpeedFactor = currentSpeedFactor; // Initial value never used, but at least the IDE won't yell at me...
//                    boolean needToRevert;
//                    try {
//                        newSpeedFactor = Integer.parseInt(editable.toString()); // Get the value that was entered into the text box
//
//                        // If the new entered roll value is not in the d20 range, then reject it
//                        needToRevert = newSpeedFactor < 0; // needToRevert becomes false (we accept the input) if newSpeedFactor is 0 or greater
//                    } catch (NumberFormatException e) {
//                        needToRevert = true;
//                    }
//
//                    // Update the value of the EditText, if needed
//                    if (needToRevert) {
//                        // The entered text was not valid, so reset it and finish
//                        modifyingSelf = true;
//                        RollViewEdit.setText(String.valueOf(currentSpeedFactor));
//                        modifyingSelf = false;
//                        return;
//                    } else if (newSpeedFactor == currentSpeedFactor) {
//                        // If the new modifier value is the same as the current modifier, then don't do anything
//                        return;
//                    }
//
//                    // If the entered text was valid and different than the current modifier, then change this Combatant, resort the list, and update the GUI
//                    setSpeedFactor(getAdapterPosition(), newSpeedFactor);
//                }
//            });
//            RollViewEdit.addTextChangedListener(new TextWatcher() {
//
//                @Override
//                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//                }
//
//                @Override
//                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//                }
//
//                @Override
//                public void afterTextChanged(Editable editable) {
//
//                    // If this is being called due to a programmatic change, ignore it
//                    if (modifyingSelf) {
//                        return;
//                    }
//
//                    // First, make sure the value here is valid
//                    int combatantInd = getAdapterPosition();
//                    int currentRoll = combatantList.get(combatantInd).getRoll(); // The current roll for this Combatant
//                    int newRollVal = currentRoll; // Initial value never used, but at least the IDE won't yell at me...
//                    boolean needToRevert;
//                    try {
//                        newRollVal = Integer.parseInt(editable.toString()); // Get the value that was entered into the text box
//
//                        // If the new entered roll value is not in the d20 range, then reject it
//                        needToRevert = newRollVal <= 0 || 20 < newRollVal; // needToRevert becomes false (we accept the input) if newRollVal is between [1 20]
//                    } catch (NumberFormatException e) {
//                        needToRevert = true;
//                    }
//
//                    // Update the value of the EditText, if needed
//                    if (needToRevert) {
//                        // The entered text was not valid, so reset it and finish
//                        modifyingSelf = true;
//                        RollViewEdit.setText(String.valueOf(currentRoll));
//                        modifyingSelf = false;
//                        return;
//                    } else if (newRollVal == currentRoll) {
//                        // If the new roll value is the same as the current roll, then don't do anything
//                        return;
//                    }
//
//                    // If the entered text was valid and different than the current roll, then change this Combatant, resort the list, and update the GUI
//                    RollView.setText(String.valueOf(newRollVal)); // Set this value in the TextView
//
//                }
//            });
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

            CombatantIcon.setImageTintList(ColorStateList.valueOf(colorId));
            CombatantIconBorder.setBackgroundColor(colorId);

            // Update the GUI indicators
            initializeState();
        }

        // OnClick functions for the roll value and the modifier value
        public void setRollValueIfValid(String newRollValString) {
            // This method will be used any time the user confirms a text string in the roll view EditText
            int combatantInd = getAdapterPosition();
            int currentRoll = combatantList.get(combatantInd).getRoll(); // The current roll for this Combatant
            int newRollVal = currentRoll; // Initial value never used, but at least the IDE won't yell at me...
            boolean newValIsValid = false;
            try {
                newRollVal = Integer.parseInt(newRollValString); // Get the value that was entered into the text box

                // If the new entered roll value is not in the d20 range, then reject it
                if (0 < newRollVal && newRollVal <= PrefsHelper.getDiceSize(RollView.getContext())) { // The new roll may be valid if it is within the range of the defined dice size
                    // If the roll value is within range, make sure that it is different than the current roll.  If so, then set the new value is valid
                    newValIsValid = currentRoll != newRollVal;
                }
            } catch (NumberFormatException e) {
//                newValIsValid = false;
            }

            // If the roll is NOT valid, then just revert the EditText and return
            if (!newValIsValid) {
                RollViewEdit.setText(String.valueOf(currentRoll)); // Revert to the current roll value
                return;
            }

            // If the roll IS valid, then set the Combatant's roll to the new value
            combatantList.get(combatantInd).setRoll(newRollVal);
            reSortInitiative(); // Resort the combatantList (also update the duplicate indices List), and update the Adapter  display
        }

        public void setModifierValueIfValid(String newModifierValString) {
            // This method will be used any time the user confirms a text string in the roll view EditText
            int combatantInd = getAdapterPosition(); // TODO SOON: This can sometimes return -1...?  Try testing with very large numbers of Combatants
            int currentModifier = combatantList.get(combatantInd).getModifier(); // The current roll for this Combatant
            int newModifierVal = currentModifier; // Initial value never used, but at least the IDE won't yell at me...
            boolean newValIsValid = false;
            try {
                newModifierVal = Integer.parseInt(newModifierValString); // Get the value that was entered into the text box

                // If the modifier value is valid, make sure that it is different than the current modifier.  If so, then set the new value is valid
                newValIsValid = currentModifier != newModifierVal;
            } catch (NumberFormatException e) {
//                newValIsValid = false;
            }

            // If the new modifier is NOT valid, then just revert the EditText and return
            if (!newValIsValid) {
                ModifierView.setText(String.valueOf(currentModifier)); // Revert to the current modifier
//                return false;
                return;
            }

            // If the new modifier IS valid, then set the Combatant's modifier to the new value
            combatantList.get(combatantInd).setModifier(newModifierVal);
            reSortInitiative(); // Resort the combatantList (also update the duplicate indices List), and update the adapter display
//            return true;
        }

        // Single method to initialize the ViewHolder to its most up-to-date status
        public void initializeState() {
            // Update the full state of the Combatant
            setDiceCheatGUI();

            int curActiveCombatant = activeCombatant();
            setInitValues();
            int pos = getAdapterPosition();
            setDuplicateColor(getDuplicateColor(combatantList, pos, curActiveCombatant));
            setStatus(getStatus(combatantList, pos, curActiveCombatant));
            setChecked(isCheckedState(combatantList, pos, curActiveCombatant));
        }

        // Methods to update individual aspects of the ViewHolder
        public void setInitValues() {
            // Update the values of the initiative counters
            Combatant thisCombatant = combatantList.get(getAdapterPosition());
            int  totalInit = thisCombatant.getTotalInitiative();
            if (totalInit <= 99) {
                // If we can display this value easily, then do so
                TotalInitiativeView.setText(String.valueOf(totalInit));
            } else {
                // Uh oh...we can't display more than 2 digits...
                TotalInitiativeView.setText(R.string.explode_head);
            }
            RollView.setText(String.valueOf(thisCombatant.getRoll()));

            modifyingSelf = true;
            ModifierView.setText(String.valueOf(thisCombatant.getModifier()));
            RollViewEdit.setText(String.valueOf(thisCombatant.getRoll()));
            modifyingSelf = false;
        }

        public void setDuplicateColor(int duplicateColor) {
            // Update the background of the Checkbox to reflect whether or not this Combatant's total initiative is shared across multiple Combatant's
            int resourceID = 0; // If this Combatant is not a duplicate, then set the background to the standard background
            switch (duplicateColor) {
                case 0:
                    resourceID = R.color.duplicateInitiative1;
                    break;
                case 1:
                    resourceID = R.color.duplicateInitiative2;
                    break;
                default:

            }

            // Set the background color for the duplicate indicator
            if (resourceID == 0) {
                TypedValue val = new TypedValue();
                DuplicateIndicator.getContext().getTheme().resolveAttribute(R.attr.combatantTab, val, true);
                DuplicateIndicator.setBackgroundColor(val.data);
            } else {
                DuplicateIndicator.setBackgroundResource(resourceID);
            }
        }

        public void setStatus(EncounterCombatantRecyclerAdapter.initStatus status) {
            // Set up variables for everything that may change (initialize to the "normal" case)
            int borderColor = 0; // Initialize to a "blank" border
            int initiativeRollVisibility = View.VISIBLE;
            int checkLayoutVisibility = View.VISIBLE;

            switch (status) {
                case Active:
                    borderColor = R.color.activeCombatant;
                    break;
                case Alert:
                    borderColor = R.color.returnToCombatant;
                    break;
                case Prep:
                    initiativeRollVisibility = View.INVISIBLE;
                    checkLayoutVisibility = View.GONE;
            }

            // Set the Combatant's ViewHolder border color
            int borderColorVal; // The int value of the new color itself (not the resource ID)
            if (borderColor == 0) {
                TypedValue val = new TypedValue();
                CombatantStatusBorder.getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, val, true);
                borderColorVal = val.data;
            } else {
                borderColorVal = CombatantStatusBorder.getContext().getResources().getColor(borderColor);
            }
            CombatantStatusBorder.setBackgroundColor(borderColorVal);

            // Set the visibility of the roll, total initiative, and checkbox views
            RollLayout.setVisibility(initiativeRollVisibility);
            TotalInitiativeView.setVisibility(initiativeRollVisibility);
            CombatantCompletedCheck.setVisibility(checkLayoutVisibility);
        }

        public void setChecked(int isCheckedState) {
            // Should the Combatant's checkbox be checked or not?
            boolean isChecked = (isCheckedState == 1 || isCheckedState == END_OF_ROUND_ACTIONS); // If the value is 1, then the checkbox can be checked.  If it is 0, it should be unchecked, and if it is PREP_PHASE, the checkbox should not appear (the ViewHolder must be updated, though)
            boolean isGrayed = isCheckedState == 1; // The view should only be grayed out if we are in combat and the Combatant has completed their action
            modifyingSelf = true;
            CombatantCompletedCheck.setChecked(isChecked); // The function setCombatantProgression will be automatically called due to the listener on the CheckBox, so the GUI will be updated
            modifyingSelf = false;

            // Also set the visibility of the gray-out
            final float targetAlpha = isGrayed ? GRAYED_OUT : CLEAR; // If the Combatant is checked, then gray it out; otherwise, keep it clear
            float currentAlpha = CombatantGrayout.getAlpha();

            // Set the Combatant to be grayed out, if needed
            // First, see if there is an existing alphaAnimation
            if (alphaAnimation != null) {
                // If there is an animation that is currently playing
                alphaAnimation.cancel(); // Cancel the existing animation (not exactly graceful, but...)
                CombatantGrayout.setAlpha(targetAlpha);
            } else {
                // If there is no current animation
                if (currentAlpha != targetAlpha) {
                    // If the current alpha value is not what we want it to be, animate the change
                    alphaAnimation = CombatantGrayout.animate().alpha(targetAlpha).setDuration(300).setListener(null);
                }
            }
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

//        public void setCombatProgression() {
//            int position = getAdapterPosition();
//            int positionInInitiative = getInitiativeInd(position); // Get the initiative index (useful for progression related to initiative, NOT the viewholder's current location in the list)
////            int positionInInitiative = position;
//
//            // Set up variables for everything that may change
//            float targetAlpha;
//            int borderColor = R.color.standardBackground; // Initialize to a "blank" border
//            int initiativeRollVisibility;
//            int checkLayoutVisibility;
//
//            // Get some useful parameters
//            float currentAlpha = CombatantGrayout.getAlpha();
//            boolean isChecked = combatantList.get(position).isSelected();
////            try {
////                isChecked = isCheckedMap.get(combatantList.get(position).getName());
////            } catch (NullPointerException e) {
////                isChecked = false;
////            }
//
//            // Get the current state, and assign appropriate values to the variables
//
//            // If the Combatant is checked off in the ArrayList (could happen without the ViewHolder knowing if the user uses the Next/Previous buttons), then make sure the GUI checkbox is checked
//            modifyingSelf = true;
//            CombatantCompletedCheck.setChecked(isChecked); // Will not run code in Listener as long as modifyingSelf is true during the call
//            modifyingSelf = false;
//
//            // See if the Combatant should be grayed out or not
//            if (!isChecked || curActiveCombatant == UNSET) {
//                // If the Combatant is unchecked, or we are between combat rounds, then their ViewHolder should be visible
//                targetAlpha = CLEAR;
//            } else {
//                // If the Combatant is checked off and we are in combat, then they should be grayed out
//                targetAlpha = GRAYED_OUT;
//            }
//
//            if (curActiveCombatant == positionInInitiative) {
//                // If this is the currently active Combatant
//                borderColor = R.color.activeCombatant; // The border should always reflect the fact that they are the currently selected Combatant
//            } else if (curActiveCombatant > positionInInitiative && !isChecked) {
//                // If this Combatant has already had their turn, but they are unchecked
//                borderColor = R.color.returnToCombatant; // Let the user know to return back to this Combatant later
//            }
//
//            if (curActiveCombatant == UNSET) {
//                // If we are currently between rounds, then hide the roll and total initiative
//                initiativeRollVisibility = View.INVISIBLE;
//                checkLayoutVisibility = View.GONE;
//            } else {
//                // If we are in the middle of the round, then make sure both roll and total initiative are visible
//                initiativeRollVisibility = View.VISIBLE;
//                checkLayoutVisibility = View.VISIBLE;
//            }
//
//            // Finally, assign the settings to the variable GUI elements
//            // Set the Combatant to be grayed out, if needed
//            if (currentAlpha != targetAlpha) {
//                // If the current alpha value is not what we want it to be, animate the change
////                AlphaAnimation alphaAnim = new AlphaAnimation(currentAlpha, targetAlpha);
////                alphaAnim.setDuration(300); // Animation should take .3s
////                alphaAnim.setFillAfter(true); // Persist the new alpha after the animation ends
////                CombatantGrayout.startAnimation(alphaAnim);
//                CombatantGrayout.animate().alpha(targetAlpha).setDuration(300).setListener(null);
//
//            }
//
//            // Set the Combatant's ViewHolder border color
//            CombatantStatusBorder.setBackgroundColor(CombatantStatusBorder.getContext().getResources().getColor(borderColor));
//
//            // Set the visibility of the roll, total initiative, and checkbox views
//            RollLayout.setVisibility(initiativeRollVisibility);
//            TotalInitiativeView.setVisibility(initiativeRollVisibility);
//            CombatantCompletedCheck.setVisibility(checkLayoutVisibility);
//        }
    }

    private int getViewInd(int position) {
        // Given a Combatant's position in the initiative order, get that Combatant's index in the current order
        return combatantList.getViewIndexOf(position);
    }

//    private void setRollValue(int combatantInd, int newRollVal) {
//        // Edit the indicated Combatant, resort the list, and update the GUI
//        combatantList.get(combatantInd).setRoll(newRollVal);
//        reSortInitiative();
//    }

//    private void setSpeedFactor(int combatantInd, int newSpeedFactor) {
//        // Edit the indicated Combatant, resort the list, and update the GUI
//        combatantList.get(combatantInd).setSpeedFactor(newSpeedFactor);
//        reSortInitiative();
//    }

    private void reSortInitiative() {
        if (combatantList.getCurrentSortMethod() == EncounterCombatantList.SortMethod.INITIATIVE) {
            // If the Combatant list is currently sorted by initiative, then resort the list and reset the GUI
            combatantList.resort();
        }

        // Make the combatantList update its duplicate values
        combatantList.updateDuplicateInitiatives();

        // Let the adapter know that at least one Combatant has changed its Initiative values, so all of the orders may now be different

        notifyCombatantsChanged();
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

//            if (args.containsKey(PAYLOAD_CHECK)) {
//                // If the payload is a boolean, then it represents whether the Combatant should or should not be checked off
//                // Case 1: The holder represents the Combatant whose turn just finished, and it will become checked
//                // Case 2: The round just ended, so all Combatants become unchecked
//                holder.setChecked(args.getBoolean(PAYLOAD_CHECK));
//            }
//
//            if (args.containsKey(PAYLOAD_UPDATE_PROGRESS)) {
//                // Update all GUI indicators
//                holder.updateState();
//            }

            for (String key : args.keySet()) {
                if (key.equals("Progression")) {
                    initStatus status = (initStatus) args.getSerializable(key);
                    if (status != null) {
                        holder.setStatus(status);
                    }
                }
                if (key.equals("Duplicate")) {
                    holder.setDuplicateColor(args.getInt(key, -1));
                }
                if (key.equals("InitValues")) {
                    holder.setInitValues(); // Update the initiative values from the ArrayList
                }
                if (key.equals("Checked")) {
                    holder.setChecked(args.getInt(key, 0));
                }
                if (key.equals(PAYLOAD_DICE_CHEAT)) {
                    // Update only the dice-cheat indicators
                    holder.setDiceCheatGUI();
                }
            }
        } else {
            // If the payload is empty, then continue on the binding process
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return combatantList.visibleSize();
    }

    public EncounterCombatantList getCombatantList() {
        return combatantList;
    }

    public void notifyCombatantsChanged() {
        // If anything about the combatants has changed, see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CombatantDiffUtil(combatantList_Memory, combatantList));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the combatantList
        combatantList_Memory = combatantList.clone();

        // Let the parent know that the display may have been updated
        parent.updateGUIState();


    }

    public void sort(EncounterCombatantList.SortMethod sortMethod) {
        curSortMethod = sortMethod;
        if (activeCombatant() != PREP_PHASE) {
            // Only sort if we are not in the prep-phase.  Otherwise, just store the desired sort method to be used during the combat round
            combatantList.sort(curSortMethod); // Perform the sorting
            notifyCombatantsChanged(); // Update the display
        }
    }

    public int getCurRoundNumber() {
        return curRoundNumber;
    }

    public void setRoundNumber(int curRoundNumber) {
        this.curRoundNumber = curRoundNumber;
        combatantList.setRoundNumber(curRoundNumber); // Update the round number in the combatantList
    }

    //    public void setActiveCombatant(int curActiveCombatant) {
//        this.curActiveCombatant = curActiveCombatant;
    public void updateCombatProgress() {
        // Slightly less efficient, but saves space
        updateCombatProgress(activeCombatant());
    }

    public void updateCombatProgress(int curActiveCombatant) {
        // Manage combatantList: Roll initiative/retrieve old rolls, sort the list, and scroll the RecyclerView to the currently active Combatant
        // Perform some required changes to combatantList based on the (presumably recently changed) current value of activeCombatant()

        // Set the dice rolls and modifiers as needed
        if (curActiveCombatant == 0) {
            // Either the first Combatant is now active, or we are at the last Combatant (this case needed in case the cuActiveCombatant value was decremented and we are coming from the prep phase)

            // ROLL INITIATIVE!!!!! (if needed...)
            combatantList.setRoundNumber(curRoundNumber);
        }

        // Sort the combatantList as needed
        if (curActiveCombatant == PREP_PHASE) {
            combatantList.sort(EncounterCombatantList.SortMethod.ALPHABETICALLY_BY_FACTION); // Go back to sorting by alphabet/faction, for pre-round prep, regardless of how we got here
        } else {
            combatantList.sort(curSortMethod); // Go back to sorting however the user wants to
        }

        // Scroll to the currently active Combatant (TO_DO LATER: May need to be an option to turn this off...?)
        scrollTo((curActiveCombatant == PREP_PHASE || curActiveCombatant == END_OF_ROUND_ACTIONS) ? 0 : getViewInd(curActiveCombatant));
    }

    private void scrollTo(int position) {
        if (combatantRecyclerView != null && combatantRecyclerView.getLayoutManager() != null) {
            combatantRecyclerView.getLayoutManager().scrollToPosition(position); // Zoom us up to the top of the RecyclerView (could maybe be an advanced option to disable...?)
        }
    }

    private int activeCombatant() {
        return combatantList.calcActiveCombatant();
    }

    public void incrementCombatStep() {
        // Calculate the currently active Combatant before increment
        int curActiveCombatant = activeCombatant();

        // Check off the current Combatant (if it exists)
        if (curActiveCombatant == PREP_PHASE) {
            // In the special case that we are in the prep phase, moving to start combat, uncheck all Combatants to start combat again
            setAllIsChecked(false);
        } else {
            if (curActiveCombatant == END_OF_ROUND_ACTIONS) {
                // In the special case that we are in the end-of-round action phase, let the list know that we can move on
                combatantList.setHaveHadPrepPhase(false); // A useful but incredibly strange...hack?
            } else {
                // Otherwise, just check off the currently active Combatant
                setIsChecked(getViewInd(curActiveCombatant), true);
            }
        }

        // Now, recalculate the currently active Combatant based on the new checked off Combatant(s)
        curActiveCombatant = activeCombatant();

        // Update the round number, if needed (done here so that the new round number can be used in updateCombatProgress)
        if (curActiveCombatant == PREP_PHASE) {
            curRoundNumber++;
            combatantList.initializeCombatants(); // Ensure that all Combatants are selected (done here to ensure that even invisible Combatants are selected, so the full list can be in the Prep-phase state)
            combatantList.setRoundNumber(curRoundNumber);
        }

        // Set the currently active Combatant to the new value
        updateCombatProgress(curActiveCombatant);

//        // Sort the Combatant list, if needed
//        if (curActiveCombatant == 0) {
//            combatantList.sort(curSortMethod); // We are starting combat again, so go back to the user's preferred sorting method
//            scrollTo(getViewInd(curActiveCombatant)); // Scroll to the new currently active Combatant
//        }

        // Finally, update all GUI states
        notifyCombatantsChanged();

//        // Depending on where we are in the combat cycle, iterate the curActiveCombatant value differently
//        if (curActiveCombatant == UNSET) {
//            // Going from the prepare phase to combat
//            curActiveCombatant = 0; // Initialize the currently active Combatant to the first Combatant
//
//            // Also, ROLL INITIATIVE!!!!!
//            combatantList.setRoundNumber(curRoundNumber);
//            combatantList.sort(EncounterCombatantList.SortMethod.INITIATIVE); // Sort by initiative, now that we have calculated it
//            combatantRecyclerView.getLayoutManager().scrollToPosition(0); // Zoom us up to the top of the RecyclerView (could maybe be an advanced option to disable...?)
//        } else if (curActiveCombatant == (combatantList.size() - 1)) {
//            // Going from the last Combatant to the prepare phase
//            curActiveCombatant = UNSET; // If we are at the end of the Combatant list, start preparing for the next round
//
//            // Let the adapter know to change the Views
//            setAllViewIsChecked(false); // Deselect all Combatants
//            combatantList.sort(EncounterCombatantList.SortMethod.ALPHABETICALLY_BY_FACTION); // Go back to sorting by alphabet/faction, for pre-round prep
//        } else {
//            // Staying in combat
//            curActiveCombatant++; // Otherwise, just increment the Combatant number
//        }

//        // Finally, update all GUI states
//        notifyCombatantsChanged();
    }

    public void decrementCombatStep() {
//        // Move backwards along the active Combatant in the list
//        if (curActiveCombatant == PREP_PHASE) {
//            // Going from the prepare phase back to the last Combatant of the last round
//            curActiveCombatant = combatantList.size() - 1; // If the currently active Combatant is unset, go back to the end of the list
//
//            // Let the adapter know to change the Views
//            combatantList.sort(EncounterCombatantList.SortMethod.INITIATIVE); // Sort by initiative, now that we are back in the previous round
//            setAllIsChecked(true);
//            setIsChecked(getViewInd(combatantList.size() - 1), false); // Uncheck the last Combatant
//
//            notifyCombatantsChanged(); // Let the adapter know that the views will likely need to rearrange
//        } else if (curActiveCombatant == 0) {
//            // Going from first Combatant back to the prepare phase
//            curActiveCombatant = PREP_PHASE; // If we are at the beginning of the list, go to "UNSET" for now
//
//            combatantList.sort(EncounterCombatantList.SortMethod.ALPHABETICALLY_BY_FACTION); // Go back to sorting by alphabet/faction, for pre-round prep
//        } else {
//            // Staying in combat
//            curActiveCombatant--; // Otherwise, just decrement
//        }

//        int newCurActiveCombatant = (curActiveCombatant == PREP_PHASE) ? (combatantList.size() - 1) : curActiveCombatant - 1;


        // Now selected is UNSET - nothing (no one was checked anyway)
        // Now selected is last Combatant - everyone except final becomes checked
        // Now selected is not UNSET and not last Combatant - the current Combatant becomes unchecked

        // Decrement the currently active Combatant (if we are in the prep phase, go back to the last Combatant)

        // Get the currently active Combatant
        int curActiveCombatant = activeCombatant();

        // Uncheck the new current Combatant (if it exists), if we are not between rounds
        switch (curActiveCombatant) {
            case PREP_PHASE:
                // In the special case that we are in the prep phase, we just went back into the previous round of Combat
                // Decrement the round number, and retrieve the previous rolls
                curRoundNumber--;
                combatantList.setRoundNumber(curRoundNumber);

                if (doingEndOfRoundActions) {
                    combatantList.setHaveHadPrepPhase(true); // If we are going back to the end of round actions phase, make sure the Combatant list knows that we have already completed the prep-phase for the round
                } else {
                    // If we are not doing end-of-round actions, then uncheck the last Combatant (if we ARE doing end-of-round actions, don't do anything else)

                    // In the new combatantList order (the initiative order from last round), uncheck the last Combatant
                    setIsChecked(getViewInd(combatantList.visibleSize() - 1), false);
                }
                break;
            case END_OF_ROUND_ACTIONS:
                // In the special case that we are in the end-of-round actions phase, uncheck the last Combatant

                // In the new combatantList order (the initiative order from last round), uncheck the last Combatant
                setIsChecked(getViewInd(combatantList.visibleSize() - 1), false);
                break;
            case 0:
                // In the special case that the first Combatant is active, check off all Combatants (bring us to the prep phase
                setAllIsChecked(true);
                combatantList.setHaveHadPrepPhase(false); // If we are going back to the prep phase, make sure the Combatant list thinks that we have not yet had one for this round
                break;
            default:
                // Otherwise, just uncheck the Combatant before the currently active Combatant
                setIsChecked(getViewInd(curActiveCombatant - 1), false);
        }

        // Now, recalculate the currently active Combatant based on the new checked off Combatant(s)
        curActiveCombatant = activeCombatant();

        // Set the currently active Combatant to the new value
        updateCombatProgress(curActiveCombatant);

//        // Sort the Combatant list, if needed
//        if (curActiveCombatant == (combatantList.size() - 1)) {
//            combatantList.sort(curSortMethod); // We are going back to the end of combat again, go back to the user's preferred sorting method
//            scrollTo(getViewInd(curActiveCombatant)); // Scroll to the new currently active Combatant
//        }

        // Finally, update all GUI states
        notifyCombatantsChanged();
    }

//    private void updateAllViewStates() {
//        // Update the GUI of all Combatants
//        Bundle payload = new Bundle();
//        payload.putBoolean(PAYLOAD_UPDATE_PROGRESS, true); // We are ending the combat round, so progress must be changed (roll and initiative views will go away, etc)
//
//        notifyItemRangeChanged(0, combatantList.size(), payload); // Let the adapter know that every Combatant's view should update its duplicate indicator
////        notifyDataSetChanged();
//    }

    private void setAllIsChecked(boolean isChecked) {
        // Note: Checks all Combatants, regardless of visibility
        for (int i = 0; i < combatantList.size(); i++) {
//            isCheckedMap.put(combatantList.get(i).getName(), isChecked);
            combatantList.get(i).setSelected(isChecked);
        }
    }

    private void setIsChecked(int viewPos, boolean isChecked) {
        // Only check a single Combatant (viewPos is according to the current layout)
//        setViewIsChecked(viewPos, 1, isChecked);
//        isCheckedMap.put(combatantList.get(viewPos).getName(), isChecked);
        combatantList.get(viewPos).setSelected(isChecked);
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

        notifyItemRangeChanged(0, getItemCount(), payload); // Send the payload to every Combatant
    }

    public void updatePrefs(Context context) {
        // Update the settings of the EncounterCombatantList, and make any necessary GUI changes as well
        combatantList.setPrefs(context);
        combatantList.resort(); // In case something changed with the sorting method

        // If the Combatant List got reset because of the new preferences, update the round number, and let the Activity know what happened
        if (!combatantList.isMidCombat()) {
            // combatantList will already be reset back to the first round prep-phase
            curRoundNumber = 1; // Move back to the first round
        }

        // Update the settings that this adapter needs to know about
        doingEndOfRoundActions = PrefsHelper.doingEndOfRound(context);

        notifyCombatantsChanged();
        parent.updateGUIState();
    }

    public void restartCombat() {
        combatantList.restartCombat();
        curRoundNumber = 1;
        notifyCombatantsChanged();
        parent.updateGUIState();
    }

    public EncounterCombatantList.SortMethod getSortMethod() {
        return curSortMethod;
    }

    public enum initStatus {
        Active, // The currently selected Combatant
        Normal, // Not checked, not had turn yet; or checked and had turn
        EOR_Action, // Not checked, all turns completed
        Alert, // Not checked off, turn completed
        Prep // Between rounds, do not show Roll and modifier elements
    }

    public static EncounterCombatantRecyclerAdapter.initStatus getStatus(EncounterCombatantList list, int curPosition, int activeCombatant) {
        // Determine the Combatant's status (and therefore border color) based on its current position in the order and the position of the currently active Combatant
        switch (activeCombatant) {
            case PREP_PHASE:
                // If we are in the preparatory combat phase
                return initStatus.Prep;
            case END_OF_ROUND_ACTIONS:
                // IF we are in the end-of-round actions phase
                return initStatus.EOR_Action;
        }

        if (getInitiativeInd(list, curPosition) == activeCombatant) {
            // If this is the currently selected Combatant, they are active regardless
            return initStatus.Active;
        }

        if (!list.get(curPosition).isSelected() && (getInitiativeInd(list, curPosition) < activeCombatant)) {
            // If the Combatant is not checked, but its turn has already occurred
            return initStatus.Alert;
        } else {
            return initStatus.Normal;
        }
    }

    public static int getDuplicateColor(EncounterCombatantList list, int curPosition, int activeCombatant) {
        return (activeCombatant == PREP_PHASE) ? -1 : list.getDuplicateColor(curPosition);
    }

    public static int isCheckedState(EncounterCombatantList list, int curPosition, int activeCombatant) {
        // There are 3 states of being checked.  Checked, unchecked, or PREP_PHASE (indicating that the list is the preparation phase, and while the value of isSelected is true, the selection cannot appear on screen and is fundamentally different than the checked state)
        if (activeCombatant == PREP_PHASE) {
            return PREP_PHASE;
        } else if (activeCombatant == END_OF_ROUND_ACTIONS) {
            return END_OF_ROUND_ACTIONS;
        } else {
            return list.get(curPosition).isSelected() ? 1 : 0;
        }
    }

    private static int getInitiativeInd(EncounterCombatantList list, int position) {
        // Given a Combatant index in combatantList, get that Combatant's index in the initiative order
        return list.getInitiativeIndexOf(position);
    }

    public interface combatProgressInterface {
        void updateGUIState(); // So the adapter can let the Activity know that the adapter has updated the progress of the combat cycle

        Context getContext(); // To get a Context

        Activity getActivity(); // To get an Activity
    }
}
