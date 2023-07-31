package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.w3c.dom.Text;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ViewGroupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ViewGroupFragment extends DialogFragment implements ListFightableRecyclerAdapter.MasterFightableKeeper {

    private static final String ARG_AFFL = "all_faction_fightable_list";
    private static final String ARG_GROUP_IND = "group_index_in_affl";
    private AllFactionFightableLists groupFragmentAFFL;
    private CombatantGroup thisGroup; // Reference to group being modified (already exists in groupFragmentAFFL)

    public ViewGroupFragment() {
        // Required empty public constructor
    }

    public static AddToGroupFragment newInstance(AllFactionFightableLists inputAFFL, int groupIndex) {
        AddToGroupFragment fragment = new AddToGroupFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_AFFL, inputAFFL);
        args.putInt(ARG_GROUP_IND, groupIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            groupFragmentAFFL = (AllFactionFightableLists) getArguments().getSerializable(ARG_AFFL);
            int thisGroupInd = getArguments().getInt(ARG_GROUP_IND);
            thisGroup = (CombatantGroup) groupFragmentAFFL.getFactionList(Fightable.Faction.Group).get(thisGroupInd);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_group, container, false);

        // Set the adapter
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.view_group_list);
        ListFightableRecyclerAdapter.LFRAFlags flags = new ListFightableRecyclerAdapter.LFRAFlags();
        flags.adapterCanContainMultiples = true;
        flags.adapterCanMultiSelect = true;

        recyclerView.setAdapter(new ListFightableRecyclerAdapter(this, groupFragmentAFFL, flags));

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

        return view;
    }

    // TODO GROUP START HERE: Implement all below methods, make sure LFRA treats Groups correctly, AND that the final AFFL gets its way back to original VSCF that called this whole chain
    @Override
    public void receiveChosenFightable(Fightable selectedFightable) {

    }

    @Override
    public void notifyFightableListChanged() {

    }

    @Override
    public void notifyIsMultiSelecting(boolean isMultiSelecting) {

    }

    @Override
    public boolean safeToDelete(Fightable fightable) {
        return false;
    }
}