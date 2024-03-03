package to.us.suncloud.myapplication;

import static to.us.suncloud.myapplication.ViewGroupFragment.ARG_CALLINGFRAG;
import static to.us.suncloud.myapplication.ViewGroupFragment.ARG_THIS_GROUP;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;

/**
 * A fragment representing a list of Items.
 */

// A fragment for adding Combatants to a group
public class AddToGroupFragment extends DialogFragment implements AddToGroupRecyclerViewAdapter.GroupListRVA_Return, Serializable {

    private static final String ARG_AFFL = "all_faction_fightable_list";
    private static final String ARG_PARENT = "parent";

    private AllFactionFightableLists groupFragmentAFFL;
    private ReceiveNewOrModFightablesInterface parent;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AddToGroupFragment() {
    }

    public static AddToGroupFragment newInstance(ReceiveNewOrModFightablesInterface parent, AllFactionFightableLists inputAFFL) {
        AddToGroupFragment fragment = new AddToGroupFragment();

        // Get a copy of the AFFL to use as reference for the group - make sure no Fightables are selected
        AllFactionFightableLists referenceAFFL = inputAFFL.clone();

        Bundle args = new Bundle();
        args.putSerializable(ARG_PARENT, parent);
        args.putSerializable(ARG_AFFL, referenceAFFL);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            groupFragmentAFFL = (AllFactionFightableLists) getArguments().getSerializable(ARG_AFFL);
            parent = (ReceiveNewOrModFightablesInterface) getArguments().getSerializable(ARG_PARENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.group_list_layout, container, false); // TODO GROUP: This (like MANY fragments) should have a width that is a fraction of the total width of the screen

        // Set the adapter
        RecyclerView recyclerView = view.findViewById(R.id.add_to_group_recycler_view);
        recyclerView.setAdapter(new AddToGroupRecyclerViewAdapter(groupFragmentAFFL, this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;
    }

    @Override
    public void choseGroupWithMetadata(CombatantGroup thisGroup) {
        // Clear any selected from the groupFragmentAFFL, so nothing appears selected in the ViewGroup dialog
        groupFragmentAFFL.clearSelected();

        // The group with this index in groupFragmentAFFL was selected.  Create a ViewOrModGroupFragment with this group
        FragmentManager fm = getChildFragmentManager();
        Bundle bundle = new Bundle();

        bundle.putSerializable(ARG_AFFL, groupFragmentAFFL);
        bundle.putSerializable(ARG_PARENT, parent);
        bundle.putSerializable(ARG_THIS_GROUP, thisGroup);
        bundle.putSerializable(ARG_CALLINGFRAG, this);

        ViewGroupFragment newFragment = ViewGroupFragment.newInstance(bundle);
        newFragment.show(fm, "ViewGroupFragment"); // ViewGroupFragment will close this Fragment when we're done
    }
}