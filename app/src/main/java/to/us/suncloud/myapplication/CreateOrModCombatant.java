package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

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

    receiveNewOrModCombatantInterface receiver;
    Bundle returnBundle; // Bundle to be returned to calling Fragment/Activity, so that they may sort the returned Combatant properly

    // Combatant that we are creating or modifying
    Combatant currentCombatant; // The version of the combatant that will be modified using this window
    Combatant originalCombatant; // A version of the combatant before any modifications took place; used to check if anything has been changed
    ArrayList<String> allCombatantNameList; // The list of combatant that the new/modified combatant will be added to once this is complete (used to check naming)
    boolean creatingCombatant = true; // Are we creating a combatant (true), or modifying one (false)?


    // The views that are part of this fragment
    TextView title;
    EditText combatantName;
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
        // To modify an existing combatant
        // Pre-process the faction list to get a list of all combatant names

        CreateOrModCombatant fragment = new CreateOrModCombatant();
        Bundle args = new Bundle();
        args.putSerializable(ORIGINAL_COMBATANT, originalCombatant);
        args.putSerializable(RECEIVER, receiver);
        args.putSerializable(COMBATANT_LIST_TO_BE_ADDED_TO, factionCombatantListToBeAddedTo.getCombatantNamesList());
        args.putBundle(RETURN_BUNDLE, returnBundle);
        fragment.setArguments(args);
        return fragment;
    }

    public static CreateOrModCombatant newInstance(receiveNewOrModCombatantInterface receiver, AllFactionCombatantLists factionCombatantListToBeAddedTo, Bundle returnBundle) {
        // To create a new combatant
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
            // First, extract the combatant names list that must be there
            if (args.getSerializable(COMBATANT_LIST_TO_BE_ADDED_TO) != null) {
                // Pull in the list that this combatant will be added to, so that we can handle any name-uniqueness checks (each Combatant must have a unique name, which we can enforce at Combatant creation)
                allCombatantNameList = (ArrayList<String>) args.getSerializable(COMBATANT_LIST_TO_BE_ADDED_TO);
            } else {
                // Something has gone HORRIBLY wrong...
                Log.e(TAG, "Did not receive combatant list to be added to");
            }

            // Next, extract the receiver interface that must be there
            if (args.getSerializable(RECEIVER) != null) {
                // Pull in the list that this combatant will be added to, so that we can handle any name-uniqueness checks (each Combatant must have a unique name, which we can enforce at Combatant creation)
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

            // Finally, extract the combatant that may or may not be there
            if (args.getSerializable(ORIGINAL_COMBATANT) != null) {
                // If a combatant was passed to this Fragment (to be modified), load in the data
                currentCombatant = (Combatant) args.getSerializable(ORIGINAL_COMBATANT);

                // Remove the current combatant from this list, so that we don't error if the user does not change the name
                allCombatantNameList.remove(currentCombatant.getName());

                // We are modifying a combatant
                creatingCombatant = false;

            } else {
                // If no combatant was passed along, then create a new Combatant to modify
                currentCombatant = new Combatant(allCombatantNameList);

                // We are creating a combatant
                creatingCombatant = true;
            }

            // Make a copy of the new current Combatant, used to compare later
            originalCombatant = currentCombatant.clone();
        } else {
            // Something has gone HORRIBLY wrong...
            Log.e(TAG, "Did not receive bundle");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragView = inflater.inflate(R.layout.fragment_create_or_mod_combatant, container, false);

        title = fragView.findViewById(R.id.mod_create_title);
        combatantName = fragView.findViewById(R.id.combatant_edit_name);
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
//                String newName = editable.toString();
//
//                // Check if the new name is unique to the allCombatantNameList
//                if (allCombatantNameList.contains(newName)) {
//                    // The name is not unique, so we should revert the editable and let the user know
//
//                    // Replace the name
//                    editable.clear();
//                    editable.append(currentCombatant.getName());
//
//                    // Let the user know
//                    Toast.makeText(getActivity().getApplicationContext(), "The name " + newName + " is already being used by another combatant, so it will be numbered automatically", Toast.LENGTH_SHORT).show();
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
                String factionString = ((CharSequence) adapterView.getItemAtPosition(i)).toString(); // Get a string representing the faction that was chosen

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
                currentCombatant.setFaction(thisFaction); // Update the combatant
                setTitleFactionColor(thisFaction); // Update the title color(s)
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

        return fragView;
    }

    @Override
    public void setIconIndex(int iconIndex) {
        // If we receive an icon index back from the dialog, then update the combatant with it
        currentCombatant.setIconIndex(iconIndex);
    }

    private void setTitleFactionColor(Combatant.Faction faction) {
        if (title != null) {
            switch (faction) {
                case Party:
                    title.setBackgroundColor(getResources().getColor(R.color.colorParty));
                    title.setTextColor(getResources().getColor(R.color.standardBackground));
                    break;
                case Enemy:
                    title.setBackgroundColor(getResources().getColor(R.color.colorEnemy));
                    title.setTextColor(getResources().getColor(R.color.standardBackground));
                    break;
                case Neutral:
                    title.setBackgroundColor(getResources().getColor(R.color.colorNeutral));
                    title.setTextColor(getResources().getColor(R.color.colorBlack));
            }
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
            // If the current combatant has been modified, check with the user to see if they want to close the dialog without saving
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

        if (creatingCombatant) {
            // Send this new Combatant back to the calling object
            receiver.receiveNewCombatant(currentCombatant, returnBundle);
        } else {
            // Let the receiver know that the Combatant they sent has been changed
            receiver.notifyCombatantChanged(returnBundle);
        }

        // Finally, dismiss the dialog
        CreateOrModCombatant.this.dismiss();
    }

    interface receiveNewOrModCombatantInterface extends Serializable {
        void receiveNewCombatant(Combatant newCombatant, Bundle returnBundle);
        void notifyCombatantChanged(Bundle returnBundle);
    }
}