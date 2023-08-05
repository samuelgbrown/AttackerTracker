package to.us.suncloud.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ViewGroupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ViewGroupFragment extends DialogFragment implements CombatantGroupRecyclerAdapter.MasterCombatantGroupKeeper {

    private static final String ARG_AFFL = "all_faction_fightable_list";
    private static final String ARG_GROUP_IND = "group_index_in_affl";
    private static final String ARG_PARENT = "parent";

    private AllFactionFightableLists groupFragmentAFFL;
    private int thisGroupInd;
    private CombatantGroup thisGroup; // Reference to group being modified (already exists in groupFragmentAFFL)
    private ViewGroupFragmentListener parent;

    private ConstraintLayout multiRemoveFromGroupLayout;

    public ViewGroupFragment() {
        // Required empty public constructor
    }

    public static AddToGroupFragment newInstance(ViewGroupFragmentListener parent, AllFactionFightableLists inputAFFL, int groupIndex) {
        AddToGroupFragment fragment = new AddToGroupFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_AFFL, inputAFFL);
        args.putSerializable(ARG_PARENT, parent);
        args.putInt(ARG_GROUP_IND, groupIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            groupFragmentAFFL = (AllFactionFightableLists) getArguments().getSerializable(ARG_AFFL);
            parent = (ViewGroupFragmentListener) getArguments().getSerializable(ARG_PARENT);
            thisGroupInd = getArguments().getInt(ARG_GROUP_IND);
            thisGroup = (CombatantGroup) groupFragmentAFFL.getFactionList(Fightable.Faction.Group).get(thisGroupInd);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_group, container, false);

        // Set the adapter
        RecyclerView recyclerView = view.findViewById(R.id.view_group_list);
        ListFightableRecyclerAdapter.LFRAFlags flags = new ListFightableRecyclerAdapter.LFRAFlags();
        flags.adapterCanContainMultiples = true;
        flags.adapterCanMultiSelect = true;

        final CombatantGroupRecyclerAdapter adapter = new CombatantGroupRecyclerAdapter(this, groupFragmentAFFL, thisGroupInd);
        recyclerView.setAdapter(adapter);

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
        Button multiCancelMultiSelect = view.findViewById(R.id.group_cancel_multi_select);

        multiRemoveFromGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( adapter.allCombatantsSelected() ) {
                    // All Combatants are selected, so ask the user if they just want to delete the entire Group
                    new AlertDialog.Builder(getContext())
                        .setTitle(R.string.confirm_delete_group_multiple)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                groupFragmentAFFL.remove(thisGroup);
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
                    // Check if any of the selected Combatants has a multiples number greater than 1
                    int dialogStrInd;
                    if ( thisGroup.selectedCombatantsHaveMultiples(groupFragmentAFFL) ) {
                        dialogStrInd = R.string.confirm_multiselect_remove_from_group_multiple_copies;
                    } else {
                        dialogStrInd = R.string.confirm_multiselect_remove_from_group_one_copy;
                    }

                    new AlertDialog.Builder(getContext())
                        .setTitle(dialogStrInd)
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

        return view;
    }

    @Override
    public void notifyIsMultiSelecting(boolean isMultiSelecting) {
        if ( isMultiSelecting ) {
            multiRemoveFromGroupLayout.setVisibility( View.VISIBLE);
        } else {
            multiRemoveFromGroupLayout.setVisibility( View.GONE );
        }
    }

    @Override
    public void dismiss() {
        parent.onFinishModifyingGroup(true); // Notify any listeners that we successfully finished this Group interaction
        super.dismiss();
    }

    interface ViewGroupFragmentListener extends Serializable {
        void onFinishModifyingGroup( boolean combatantsConsumed ); // To notify the object that this fragment has finished viewing/modifying the group. combatantsConsumed will be true if the Combatants were successfully added to a group
    }
}