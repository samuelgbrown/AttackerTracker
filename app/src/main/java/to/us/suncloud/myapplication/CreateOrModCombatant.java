package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CreateOrModCombatant#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreateOrModCombatant extends DialogFragment implements IconSelectFragment.ReceiveIconSelectionInterface {

    private static final String TAG = "CreateOrMod";
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ORIGINAL_COMBATANT = "originalCombatant";
    private static final String COMBATANT_LIST_TO_BE_ADDED_TO = "combatantListToBeAddedTo";
    private static final String RECEIVER = "receiver";
    private static final String RETURN_BUNDLE = "returnBundle";

    public static final String MODIFY_COMBATANT_LOCATION = "modifyCombatantLocation";

    receiveNewOrModCombatantInterface receiver;
    Bundle returnBundle; // Bundle to be returned to calling Fragment/Activity, so that they may sort the returned Combatant properly

    // Combatant that we are creating or modifying
    Combatant currentCombatant; // The version of the Combatant that will be modified using this window
    Combatant originalCombatant; // A version of the Combatant before any modifications took place; used to check if anything has been changed
    ArrayList<String> allCombatantNameList; // The list of Combatant that the new/modified Combatant will be added to once this is complete (used to check naming)
    boolean creatingCombatant = true; // Are we creating a Combatant (true), or modifying one (false)?

    ArrayList<Integer> iconResourceIds;

    // The views that are part of this fragment
    TextView title;
    TextView modTypeView;
    ConstraintLayout banner;
    ConstraintLayout iconBorder;
    EditText combatantName;
    EditText defaultModifier; // Default Speed Factor or Dex
    Spinner combatantFaction;
    ImageView combatantIcon;
    Button buttonOk;
    Button buttonCancel;

    public CreateOrModCombatant() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CreateOrModCombatant.
     */
    public static CreateOrModCombatant newInstance(receiveNewOrModCombatantInterface receiver, Combatant originalCombatant, AllFactionCombatantLists factionCombatantListToBeAddedTo, Bundle returnBundle) {
        // To modify an existing Combatant
        // Pre-process the faction list to get a list of all Combatant names

        CreateOrModCombatant fragment = new CreateOrModCombatant();
        Bundle args = new Bundle();
        args.putSerializable(ORIGINAL_COMBATANT, originalCombatant.clone()); // Create a clone of the incoming Combatant (even when modifying, the old Combatant will just be removed and the newly modified version re-added to the list.  This is a simple way to deal with issues of name uniqueness during modification)
        args.putSerializable(RECEIVER, receiver);
        args.putSerializable(COMBATANT_LIST_TO_BE_ADDED_TO, factionCombatantListToBeAddedTo.getCombatantNamesList());
        args.putBundle(RETURN_BUNDLE, returnBundle);
        fragment.setArguments(args);
        return fragment;
    }

    public static CreateOrModCombatant newInstance(receiveNewOrModCombatantInterface receiver, AllFactionCombatantLists factionCombatantListToBeAddedTo, Bundle returnBundle) {
        // To create a new Combatant
        CreateOrModCombatant fragment = new CreateOrModCombatant();
        Bundle args = new Bundle();
        args.putSerializable(RECEIVER, receiver);
        args.putSerializable(COMBATANT_LIST_TO_BE_ADDED_TO, factionCombatantListToBeAddedTo.getCombatantNamesList());
        args.putBundle(RETURN_BUNDLE, returnBundle);
        fragment.setArguments(args);
        return fragment;
    }

    // Same newInstance methods as above, with no included Bundle
    public static CreateOrModCombatant newInstance(receiveNewOrModCombatantInterface receiver, AllFactionCombatantLists factionCombatantListToBeAddedTo) {
        return newInstance(receiver, factionCombatantListToBeAddedTo, new Bundle());
    }

    public static CreateOrModCombatant newInstance(receiveNewOrModCombatantInterface receiver, Combatant originalCombatant, AllFactionCombatantLists factionCombatantListToBeAddedTo) {
        return newInstance(receiver, originalCombatant, factionCombatantListToBeAddedTo, new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            // First, extract the Combatant names list that must be there
            if (args.getSerializable(COMBATANT_LIST_TO_BE_ADDED_TO) != null) {
                // Pull in the list that this Combatant will be added to, so that we can handle any name-uniqueness checks (each Combatant must have a unique name, which we can enforce at Combatant creation)
                allCombatantNameList = (ArrayList<String>) args.getSerializable(COMBATANT_LIST_TO_BE_ADDED_TO);
            } else {
                // Something has gone HORRIBLY wrong...
                Log.e(TAG, "Did not receive Combatant list to be added to");
            }

            // Next, extract the receiver interface that must be there
            if (args.getSerializable(RECEIVER) != null) {
                // Pull in the list that this Combatant will be added to, so that we can handle any name-uniqueness checks (each Combatant must have a unique name, which we can enforce at Combatant creation)
                receiver = (receiveNewOrModCombatantInterface) args.getSerializable(RECEIVER);
            } else {
                // Something has gone HORRIBLY wrong...
                Log.e(TAG, "Did not receive receiver");
            }

            // Next, extract the return Bundle that must be there
            if (args.getBundle(RETURN_BUNDLE) != null) {
                // Get and store the returned Bundle for later
                returnBundle = args.getBundle(RETURN_BUNDLE);
            } else {
                Log.e(TAG, "Did not receive return Bundle");
            }

            // Finally, extract the Combatant that may or may not be there
            if (args.getSerializable(ORIGINAL_COMBATANT) != null) {
                // If a Combatant was passed to this Fragment (to be modified), load in the data
                currentCombatant = (Combatant) args.getSerializable(ORIGINAL_COMBATANT);

                // Remove the current Combatant from this list, so that we don't error if the user does not change the name
                allCombatantNameList.remove(currentCombatant.getName());

                // We are modifying a Combatant
                creatingCombatant = false;

            } else {
                // If no Combatant was passed along, then create a new Combatant to modify
                currentCombatant = new Combatant(allCombatantNameList);

                // We are creating a Combatant
                creatingCombatant = true;
            }

            // Make a copy of the new current Combatant, used to compare later
            originalCombatant = currentCombatant.clone();
        } else {
            // Something has gone HORRIBLY wrong...
            Log.e(TAG, "Did not receive bundle");
        }

        // Create a list of resources that will be used to load svg's into the grid
        iconResourceIds = new ArrayList<>();
        int curNum = 0;
        while (true) {
            // Generate filenames for every icon that we will use in order, and check if it exists
            String resourceName = String.format(Locale.US, "icon_%02d", curNum); // Oh, the horror...
            int id = getContext().getResources().getIdentifier(resourceName, "drawable", getContext().getPackageName());

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragView = inflater.inflate(R.layout.fragment_create_or_mod_combatant, container, false);

        String modString = PrefsHelper.getModString(getContext());

        title = fragView.findViewById(R.id.mod_create_title);
        modTypeView = fragView.findViewById(R.id.textViewMod);
        banner = fragView.findViewById(R.id.mod_create_banner);
        iconBorder = fragView.findViewById(R.id.combatant_icon_background);
        combatantName = fragView.findViewById(R.id.combatant_edit_name);
        defaultModifier = fragView.findViewById(R.id.combatant_edit_mod);
        combatantFaction = fragView.findViewById(R.id.combatant_edit_faction);
        combatantIcon = fragView.findViewById(R.id.combatant_edit_icon);
        buttonOk = fragView.findViewById(R.id.mod_create_ok);
        buttonCancel = fragView.findViewById(R.id.mod_create_cancel);

        // Set the correct title for the fragment
        if (creatingCombatant) {
            title.setText(R.string.create_combatant);
        } else {
            title.setText(R.string.mod_combatant);
        }


        // Set the modifier type to the correct String
        modTypeView.setText(modString);

        // TODO: Cassie's device: When defaultModifier is focused and the view is scrolled down
//        defaultModifier.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (v instanceof EditText) {
//                    if (hasFocus) {
//                        // If we are gaining focus, select all text
//                        ((EditText) v).selectAll();
//                    } else {
//                        // If we have lost focus, the user has likely navigated away.  So, confirm the new value
//
//                        // It damn well better be...
//                        defaultModifier.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                setModifierIfValid(defaultModifier.getText().toString()); // Set the new modifier, if possible
//                            }
//                        });
//                    }
//                }
//            }
//        });
        defaultModifier.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int newMod = Integer.parseInt(editable.toString());
                currentCombatant.setModifier(newMod);
            }
        });
        defaultModifier.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
//                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (event == null || !event.isShiftPressed()) {
//                        // The user is done typing, so attempt to set the modifier
//                        defaultModifier.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                setModifierIfValid(defaultModifier.getText().toString()); // Set the new modifier, if possible
//                            }
//                        });
//
                        // If the user hit Done, then hide the keyboard
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(defaultModifier.getWindowToken(), 0);

                        // Clear the focus
                        v.clearFocus();
                        return true;
                    }
                }
                return false; // Pass on to other listeners.
            }
        });

        combatantName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Will just hold off on changing anything the user does, and let the smart naming take care of it (unless we make smart naming optional...?)
                String newName = editable.toString();
                currentCombatant.setName(newName);

//                // Check if the new name is unique to the allCombatantNameList
//                if (allCombatantNameList.contains(newName)) {
//                    // The name is not unique, so we should revert the editable and let the user know
//
//                    // Replace the name
//                    editable.clear();
//                    editable.append(currentCombatant.getName()); // Needs to happen here, because currentCombatant is the same object as in the master list, so any changes made are immediate
//
//                    // Let the user know
//                    Toast.makeText(getActivity().getApplicationContext(), "The name " + newName + " is already being used by another Combatant, so it will be numbered automatically", Toast.LENGTH_SHORT).show();
//
//                } else {
//                    // The name is NOT unique, so we should change the Combatant's name to this
//                    currentCombatant.setName(newName);
//                }
            }
        });

        ArrayAdapter<CharSequence> factionAdapter = ArrayAdapter.createFromResource(getContext(), R.array.faction_names, android.R.layout.simple_spinner_item);
        factionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        combatantFaction.setAdapter(factionAdapter);
        combatantFaction.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String factionString = adapterView.getItemAtPosition(i).toString(); // Get a string representing the faction that was chosen

                // Based on the string of the item that was selected, set the faction of the combatantw
                Combatant.Faction thisFaction = Combatant.Faction.Party;
                switch (factionString) {
                    case "Party":
                        thisFaction = Combatant.Faction.Party;
                        break;
                    case "Enemy":
                        thisFaction = Combatant.Faction.Enemy;
                        break;
                    case "Neutral":
                        thisFaction = Combatant.Faction.Neutral;
                }

                // Once we know the faction that was selected...
                currentCombatant.setFaction(thisFaction); // Update the Combatant
                setTitleFactionColor(); // Update the title color(s)
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        combatantIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getChildFragmentManager();
                IconSelectFragment iconSelectFragment = IconSelectFragment.newInstance(CreateOrModCombatant.this, currentCombatant.getIconIndex());
                iconSelectFragment.show(fm, "Icon Select");
            }
        });
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptToClose();
            }
        });

        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Accept the changes that were made to the Combatant
                acceptCombatant();
            }
        });

        // Now, make the GUI reflect the current Combatant
        setIconImage(currentCombatant.getIconIndex()); // Load the icon
        combatantName.setText(currentCombatant.getName()); // Load the name
        updateFactionGUI(); // Load the Faction
        defaultModifier.setText(String.valueOf(currentCombatant.getModifier())); // Load the default modifier

        if (creatingCombatant) {
            // If we're making a new Combatant, ask for the Combatant's Name field to get focus, and show the keyboard (if we're just modifying a Combatant, we don't KNOW that it's the name that they want to change)
            combatantName.post(new Runnable() {
                @Override
                public void run() {
                    combatantName.setSelectAllOnFocus(true);
                    if (combatantName.requestFocus()) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(combatantName, InputMethodManager.SHOW_IMPLICIT);
                    }
                    combatantName.setSelectAllOnFocus(false);
                }
            });
        }


        return fragView;
    }

    private void updateFactionGUI() {
        // Set the Faction spinner to the current Combatant's Faction
        switch (currentCombatant.getFaction()) {
            case Party:
                combatantFaction.setSelection(0);
                break;
            case Enemy:
                combatantFaction.setSelection(1);
                break;
            case Neutral:
                combatantFaction.setSelection(2);
        }
    }

    public void setModifierIfValid(String newModifierValString) {
        int curModVal = currentCombatant.getModifier();
        int newModVal = curModVal;
        boolean newValIsValid = false;
        try {
            newModVal = Integer.parseInt(newModifierValString); // Get the value that was entered into the text box
            newValIsValid = curModVal != newModVal; // If the new modifier value is different than the current value, then allow it
        } catch (NumberFormatException e) {
//                newValIsValid = false;
        }

        // If the new speed factor is NOT valid, then just revert the EditText and return
        if (!newValIsValid) {
            defaultModifier.setText(String.valueOf(curModVal)); // Revert to the current modifier value
            return;
        }

        // If the new speed factor IS valid, then set the Combatant's modifier to the new value
        currentCombatant.setModifier(newModVal);
    }

    @Override
    public void setIconIndex(int iconIndex) {
        // If we receive an icon index back from the dialog, then update the Combatant with it
        currentCombatant.setIconIndex(iconIndex);

        // Also, update the View
        setIconImage(iconIndex);
    }

    private void setIconImage(int iconIndex) {
        // Sets the image on the layout as the icon given by iconIndex
        if (iconIndex == 0) {
            combatantIcon.setForeground(getContext().getDrawable(android.R.color.transparent));
        } else {
            combatantIcon.setForeground(getContext().getDrawable(iconResourceIds.get(iconIndex - 1)));
        }

        setTitleFactionColor(); // Set the color of the new icon
    }

    private void setTitleFactionColor() {
        TypedValue val = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.bannerTextColor, val, true);
        int themeBannerTextColor = val.data;
        Combatant.Faction faction = currentCombatant.getFaction();
        if (banner != null && title != null) {
            int mainColor = getResources().getColor(R.color.colorParty);
            int textColor = themeBannerTextColor;
            switch (faction) {
                case Party:
                    mainColor = getResources().getColor(R.color.colorParty);
                    break;
                case Enemy:
                    mainColor = getResources().getColor(R.color.colorEnemy);
                    break;
                case Neutral:
                    mainColor = getResources().getColor(R.color.colorNeutral);
                    textColor = getResources().getColor(R.color.colorBlack);
            }

            banner.setBackgroundColor(mainColor);
            iconBorder.setBackgroundColor(mainColor);
            title.setBackgroundColor(mainColor);
            title.setTextColor(textColor);
            combatantIcon.getForeground().setTint(mainColor);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                attemptToClose();
            }
        };
    }

    private void attemptToClose() {
        if (!currentCombatant.equals(originalCombatant)) {
            // If the current Combatant has been modified, check with the user to see if they want to close the dialog without saving
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.mod_create_close_message)
                    .setTitle(R.string.mod_create_close_title)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            CreateOrModCombatant.this.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Do Nothing
                        }
                    })
                    .show();
        } else {
            // If no changes have been made, just close the dialog anyway
            CreateOrModCombatant.this.dismiss();
        }
    }

    private void acceptCombatant() {
        // When the user is done creating/modifying the Combatant, then get the Combatant back to the rest of the program.
        // Depending on whether the Combatant is new or an old one being changed, do different things.

//        if (creatingCombatant) {
//            // Send this new Combatant back to the calling object
        receiver.receiveCombatant(currentCombatant, returnBundle);
//        } else {
//            // Let the receiver know that the Combatant they sent has been changed
//            receiver.notifyCombatantChanged(returnBundle);
//        }

        // Finally, dismiss the dialog
        CreateOrModCombatant.this.dismiss();
    }

    interface receiveNewOrModCombatantInterface extends Serializable {
        void receiveCombatant(Combatant newCombatant, Bundle returnBundle);

        void notifyCombatantChanged(Bundle returnBundle);
    }
}