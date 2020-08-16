package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddCombatantFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

// TODO: Make a version of this that is something like "modify saved combatants" that is identical to this (assuming you can modify saved combatants here, too) except there is no return value (i.e. nothing happens when a combatant is "selected")
public class AddCombatantFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String CURRENT_COMBATANT_LIST = "currentCombatantList";

    private static final String combatantListSaveFile = "combatantListSaveFile";

    private ArrayList<FactionCombatantList> currentFactionCombatantList = null;

    private TextView emptyCombatants;

    public AddCombatantFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AddCombatantFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddCombatantFragment newInstance(ArrayList<FactionCombatantList> currentFactionCombatantList) {
        AddCombatantFragment fragment = new AddCombatantFragment();
        Bundle args = new Bundle();
        args.putSerializable(CURRENT_COMBATANT_LIST, currentFactionCombatantList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentFactionCombatantList = (ArrayList<FactionCombatantList>) getArguments().getSerializable(CURRENT_COMBATANT_LIST);
            // This list will be null if a) this is the first time this fragment has been used on this device, or b) no combatants have been saved previously
        }

        // Load in combatants from file (process them later)
        ArrayList<FactionCombatantList> fileContents = (ArrayList<FactionCombatantList>) LocalPersistence.readObjectFromFile(getContext(), combatantListSaveFile);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layoutContents = inflater.inflate(R.layout.add_combatant, container, false);
        emptyCombatants = layoutContents.findViewById(R.id.add_combatants_empty);
        ConstraintLayout combatantGroupParent = layoutContents.findViewById(R.id.add_combatant_layout_parent);

        if (currentFactionCombatantList == null) {
            // If there is nothing in the file, then no combatants have previously been saved, so display the empty message
            emptyCombatants.setVisibility(View.VISIBLE);
        } else {
            // If there were combatants previously saved, then load them into the View
            emptyCombatants.setVisibility(View.GONE);

            FragmentManager fm = getChildFragmentManager();
            FragmentTransaction fragTransaction = fm.beginTransaction();

            for (int factionInd = 0; factionInd < currentFactionCombatantList.size(); factionInd++) {
                CombatantGroupFragment newFrag = new CombatantGroupFragment(currentFactionCombatantList.get(factionInd));
                fragTransaction.add(combatantGroupParent.getId(), newFrag, Combatant.factionToString(currentFactionCombatantList.get(factionInd).getThisFaction()) + "_add_fragment");
            }

        }
        return layoutContents;
    }
}
