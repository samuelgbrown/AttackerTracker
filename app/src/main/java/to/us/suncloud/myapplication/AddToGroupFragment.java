package to.us.suncloud.myapplication;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment representing a list of Items.
 */

// A fragment for adding Combatants to a group
public class AddToGroupFragment extends DialogFragment implements AddToGroupRecyclerViewAdapter.GroupListRVA_Return {

    private static final String ARG_AFFL = "all_faction_fightable_list";
    private static final String ARG_PARENT = "parent";

    private AllFactionFightableLists groupFragmentAFFL;
    private ViewGroupFragment.ViewGroupFragmentListener parent;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AddToGroupFragment() {
    }

    public static AddToGroupFragment newInstance(ViewGroupFragment.ViewGroupFragmentListener parent, AllFactionFightableLists inputAFFL) {
        AddToGroupFragment fragment = new AddToGroupFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARENT, parent);
        args.putSerializable(ARG_AFFL, inputAFFL);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            groupFragmentAFFL = (AllFactionFightableLists) getArguments().getSerializable(ARG_AFFL);
            parent = (ViewGroupFragment.ViewGroupFragmentListener) getArguments().getSerializable(ARG_PARENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.group_list_layout, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setAdapter(new AddToGroupRecyclerViewAdapter(groupFragmentAFFL, this));
        }
        return view;
    }

    @Override
    public void groupIndexSelected(int groupIndex) {
        // The group with this index in groupFragmentAFFL was selected.  Create a ViewOrModGroupFragment with this group
        FragmentManager fm = getChildFragmentManager();
        ViewGroupFragment.newInstance(parent, groupFragmentAFFL, groupIndex).show(fm, "ViewGroupFragment");

        // Close the Dialog (no need to return to choosing a Group)
        dismiss();
    }

    @Override
    public void dismiss() {
        parent.onFinishModifyingGroup(false);
        super.dismiss();
    }
}