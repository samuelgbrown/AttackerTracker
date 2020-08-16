package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CreateOrModCombatant#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreateOrModCombatant extends DialogFragment {

    private static final String TAG = "CreateOrMod";
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ORIGINAL_COMBATANT = "originalCombatant";
    private static final String COMBATANT_LIST_TO_BE_ADDED_TO = "combatantListToBeAddedTo";
    private static final String RECEIVER = "receiver";

    DialogFragment thisDialog = this;

    receiveNewOrModCombatantInterface receiver;

    // Combatant that we are creating or modifying
    Combatant currentCombatant;
    ArrayList<String> allCombatantNameList; // The list of combatant that the new/modified combatant will be added to once this is complete (used to check naming)
    boolean creatingCombatant = true; // Are we creating a combatant (true), or modifying one (false)?
    boolean hasCombatantChanged = false; // TODO: Has the combatant changed (i.e. should the app check with the user before throwing out changes)?


    // The views that are part of this fragment
    TextView title;
    EditText combatantName;
    Spinner combatantFaction;
    Spinner combatantIcon;
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
    public static CreateOrModCombatant newInstance(receiveNewOrModCombatantInterface receiver, Combatant originalCombatant, AllFactionCombatantLists factionCombatantListToBeAddedTo) {
        // To modify an existing combatant
        // Pre-process the faction list to get a list of all combatant names

        CreateOrModCombatant fragment = new CreateOrModCombatant();
        Bundle args = new Bundle();
        args.putSerializable(ORIGINAL_COMBATANT, originalCombatant);
        args.putSerializable(RECEIVER, receiver);
        args.putSerializable(COMBATANT_LIST_TO_BE_ADDED_TO, factionCombatantListToBeAddedTo.getCombatantNamesList());
        fragment.setArguments(args);
        return fragment;
    }

    public static CreateOrModCombatant newInstance(receiveNewOrModCombatantInterface receiver, AllFactionCombatantLists factionCombatantListToBeAddedTo) {
        // To create a new combatant
        CreateOrModCombatant fragment = new CreateOrModCombatant();
        Bundle args = new Bundle();
        args.putSerializable(RECEIVER, receiver);
        args.putSerializable(COMBATANT_LIST_TO_BE_ADDED_TO, factionCombatantListToBeAddedTo.getCombatantNamesList());
        fragment.setArguments(args);
        return fragment;
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

            // Finally, extract the combatant that may or may not be there
            if (args.getSerializable(ORIGINAL_COMBATANT) != null) {
                // If a combatant was passed to this Fragment (to be modified), load in the data
                currentCombatant = (Combatant) args.getSerializable(ORIGINAL_COMBATANT);

                // Remove the current combatant from this list, so that we don't error if the user does not change the name
                allCombatantNameList.remove(currentCombatant.getName());

                // We are modifying a combatant
                creatingCombatant = false;

                // TODO: If modifying combatant, calling fragment must deal with new combatant properly!  Old version must be removed and replaced with this new version

            } else {
                // If no combatant was passed along, then create a new Combatant to modify
                // TODO START HERE: Finish this create or mod combatant fragment, create a layout, set up the behavior!!!
                currentCombatant = new Combatant(allCombatantNameList);

                // We are creating a combatant
                creatingCombatant = true;
            }
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

        // TODO: Save references to all of the Views that we will be using, and add behavior to each button!
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
                String newName = editable.toString();

                // Check if the new name is unique to the allCombatantNameList
                if (allCombatantNameList.contains(newName)) {
                    // The name is not unique, so we should revert the editable and let the user know

                    // Replace the name
                    editable.clear();
                    editable.append(currentCombatant.getName());

                    // Let the user know
                    Toast.makeText(getActivity().getApplicationContext(), "The name " + newName + " is already being used", Toast.LENGTH_SHORT).show();

                } else {
                    // The name is NOT unique, so we should change the Combatant's name to this
                    currentCombatant.setName(newName);

                    // The combatant has changed
                    hasCombatantChanged = true;
                }
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

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptToClose();
            }
        });

        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send the combatant to the receiving fragment
                receiver.receiveCombatant(currentCombatant);

                // Close this dialog
                thisDialog.dismiss();
            }
        });

        return fragView;
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
        if (hasCombatantChanged) {
            // Check with the user to see if they want to close the dialog without saving
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.mod_create_close_message)
                    .setTitle(R.string.mod_create_close_title)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            thisDialog.dismiss();
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
            thisDialog.dismiss();
        }
    }

    interface receiveNewOrModCombatantInterface extends Serializable {
        void  receiveCombatant(Combatant newCombatant);
    }
}