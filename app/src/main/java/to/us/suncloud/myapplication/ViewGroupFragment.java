package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ViewGroupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ViewGroupFragment extends DialogFragment implements CombatantGroupRecyclerAdapter.MasterCombatantGroupKeeper {

    public static final String ARG_AFFL = "all_faction_fightable_list";
    public static final String ARG_THIS_GROUP = "this_group";
    public static final String ARG_PARENT = "parent";
    public static final String ARG_CALLINGFRAG = "calling_fragment";
    public static final String ARG_AUTO_ACCEPT_GROUP = "auto_accept_group";
    public static final String ARG_INITIALIZE_GROUP_CHANGED = "initialize_group_changed";

    private AllFactionFightableLists groupFragmentAFFL;
    private CombatantGroup thisGroup; // The group being modified (does NOT exist in groupFragmentAFFL yet...)
    private ReceiveNewOrModFightablesInterface parent;
    private DialogFragment callingFragment; // Fragment to close once this interaction is complete
    private boolean autoAcceptGroup = true; // If the user presses "back", should that imply that the changes should be automatically accepted (if there's no calling Fragment, assume we should auto-accept)
    private boolean initializeGroupChanged = false; // Assume that the group has been changed even if the changes weren't done here (so that the user must confirm when exiting)

    private ConstraintLayout multiRemoveFromGroupLayout;
    private Button doneButton;

    private CombatantGroupRecyclerAdapter adapter;
    private boolean isMultiSelecting = false; // Is the Fragment (or adapter) currently in a multi-selecting state?

    private CombatantGroup originalGroup;

    public ViewGroupFragment() {
        // Required empty public constructor
    }

    public static ViewGroupFragment newInstance(Bundle args) {
        ViewGroupFragment fragment = new ViewGroupFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            Bundle args = getArguments();
            if ( args.containsKey(ARG_AFFL)) {
                groupFragmentAFFL = (AllFactionFightableLists) args.getSerializable(ARG_AFFL);
            }
            if ( args.containsKey(ARG_PARENT)) {
                parent = (ReceiveNewOrModFightablesInterface) args.getSerializable(ARG_PARENT);
            }
            if ( args.containsKey(ARG_THIS_GROUP)) {
                thisGroup = (CombatantGroup) args.getSerializable(ARG_THIS_GROUP);
            }
            if ( args.containsKey(ARG_CALLINGFRAG)) {
                callingFragment = (DialogFragment) args.getSerializable(ARG_CALLINGFRAG);
                autoAcceptGroup = false; // User will want to return to calling fragment on hitting Back
            } else if ( args.containsKey(ARG_AUTO_ACCEPT_GROUP) ) {
                autoAcceptGroup = args.getBoolean(ARG_AUTO_ACCEPT_GROUP);
            }

            if ( args.containsKey(ARG_INITIALIZE_GROUP_CHANGED)) {
                initializeGroupChanged = args.getBoolean(ARG_INITIALIZE_GROUP_CHANGED);
            }

            originalGroup = (CombatantGroup) thisGroup.clone();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_group, container, false);

        // Prepare Group Name
        EditText groupNameText = view.findViewById(R.id.view_group_name);
        groupNameText.setText( thisGroup.getName());
        groupNameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String newName = editable.toString();
                thisGroup.setName(newName);
            }
        });

        // Set up Multi-selecting
        multiRemoveFromGroupLayout = view.findViewById(R.id.group_multi_select_options);
        Button multiRemoveFromGroup = view.findViewById(R.id.delete_from_group);
        ImageButton multiCancelMultiSelect = view.findViewById(R.id.group_cancel_multi_select);

        multiRemoveFromGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( adapter.allCombatantsSelected() ) {
                    // All Combatants are selected, so ask the user if they just want to delete the entire Group
                    TextView titleText = new TextView(getContext());
                    titleText.setText(R.string.confirm_delete_group_title);
                    new AlertDialog.Builder(getContext())
                        .setTitle(R.string.confirm_delete_group_title)
                        .setMessage(R.string.confirm_delete_group_message_multiple)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                adapter.removeAllCombatants();
                                acceptGroup();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Do nothing
                            }
                        })
                        .show();
                } else {
                    // Check if any of the selected Combatants has a multiples number greater than 1
                    int dialogStrInd;
                    if ( thisGroup.selectedCombatantsHaveMultiples(groupFragmentAFFL) ) {
                        dialogStrInd = R.string.confirm_multiselect_remove_from_group_multiple_copies;
                    } else {
                        dialogStrInd = R.string.confirm_multiselect_remove_from_group_one_copy;
                    }

                    new AlertDialog.Builder(getContext())
                        .setTitle(R.string.confirm_remove_combatant_from_group_title)
                        .setMessage(dialogStrInd)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                adapter.removeSelectedCombatants();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Do nothing
                            }
                        })
                        .show();
                }
            }
        });

        multiCancelMultiSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.clearMultiSelect();
            }
        });

        // Set up Done button
        doneButton = view.findViewById(R.id.group_done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptGroup();
            }
        });

        // Set the adapter
        RecyclerView recyclerView = view.findViewById(R.id.view_group_list);
        adapter = new CombatantGroupRecyclerAdapter(this, thisGroup, groupFragmentAFFL);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void notifyIsMultiSelecting(boolean isMultiSelecting) {
        this.isMultiSelecting = isMultiSelecting;
        // If we're multi-selecting, switch visibility between the done button and the multi-selecting pane
        if ( isMultiSelecting ) {
            multiRemoveFromGroupLayout.setVisibility( View.VISIBLE);
            doneButton.setVisibility( View.GONE );
        } else {
            multiRemoveFromGroupLayout.setVisibility( View.GONE );
            doneButton.setVisibility( View.VISIBLE );
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (callingFragment != null) {
            // Once we are done here, close the calling fragment, if needed
            callingFragment.dismiss();
        }

        if (parent != null) {
            if ( thisGroup.size() == 0 ) {
                // If we're dismissing the Fragment because we no longer have any Combatants, tell the parent to remove this group!
                parent.removeFightable(thisGroup);
            } else {
                // Otherwise, just let the parent know that something may have changed
                parent.notifyListChanged();
            }
        }

        super.onDismiss(dialog);
    }

    public void acceptGroup() {
        // If group size is 0, request that the group is removed
        if ( parent != null ) {
            if (thisGroup.size() != 0) {
                parent.receiveFightable(thisGroup); // Notify parent that we successfully finished this Group interaction
            } // If size is 0, thisGroup will be removed from parent in onDismiss
        }

        dismiss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(requireActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                if (isMultiSelecting) {
                    adapter.clearMultiSelect(); // This will clear multi-select in the adapter, and eventually let this Fragment know to update the GUI
                } else {
                    if (!autoAcceptGroup) {
                        // Check with the user before accepting the group
                        if (groupHasChanged()) {
                            // Confirm erasing data
                            new AlertDialog.Builder(getContext())
                                    .setTitle(requireContext().getString(R.string.confirm_not_create_group))
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            // Remove this fragment, as well as the calling fragment (logistical issue -
                                            // we lose the selected Fightable(s) if we go back to callingFragment, and
                                            // I'm too lazy to make it NOT lose them)
                                            dismiss();
                                        }
                                    })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            // Do nothing
                                        }
                                    })
                                    .show();
                        } else {
                            // No data to save - delete dialog
                            dismiss();
                        }
                    } else {
                        // There is no calling fragment, user wants to accept this data
                        acceptGroup();
                    }
                }
            }
        };
    }

    private boolean groupHasChanged() {
        boolean retVal = false;
        if ( !initializeGroupChanged ) {
            if (thisGroup != null && originalGroup != null) {
                retVal = !thisGroup.equals(originalGroup);
            }
        } else {
            retVal = true;
        }

        return retVal;
    }
}