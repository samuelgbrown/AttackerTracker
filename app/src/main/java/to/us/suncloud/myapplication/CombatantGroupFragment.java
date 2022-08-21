package to.us.suncloud.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

public class CombatantGroupFragment extends Fragment {
    final Fightable.Faction thisFaction; // The faction that this fragment represents
    RecyclerView combatantRecyclerView;
    ListFightableRecyclerAdapter combatantRecyclerAdapter;

    CombatantGroupFragment(ListFightableRecyclerAdapter combatantRecyclerAdapter, Fightable.Faction thisFaction) {
//        setCombatantList(newCombatantList.getCombatantArrayList());
        this.thisFaction = thisFaction;

        this.combatantRecyclerAdapter = combatantRecyclerAdapter;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.combatant_group, container, false);
        combatantRecyclerView = fragView.findViewById(R.id.groupCombatant_view);
        TextView GroupTextView = fragView.findViewById(R.id.group_text_view);

        // Set the adapter and layout manager for the recycler view
        combatantRecyclerView.setAdapter(combatantRecyclerAdapter);
        LinearLayoutManager manager = new LinearLayoutManager(getContext()); // {
//            @Override
//            public boolean canScrollVertically() {
//                return false; // Make the Recycler view unable to scroll
//            }
//        };
        combatantRecyclerView.setLayoutManager(manager);


        // Set the text view using the faction type
        ArrayList<String> allFactionNames = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.group_names)));
        ArrayList<Fightable.Faction> allFactionValues = new ArrayList<>(Arrays.asList(Fightable.Faction.values()));
        GroupTextView.setText(allFactionNames.get(allFactionValues.indexOf(thisFaction)));

        // Return the inflated fragment
        return fragView;
    }

    public ListFightableRecyclerAdapter getAdapter() {
        return combatantRecyclerAdapter;
    }


//    public void setCombatantList(ArrayList<Combatant> newCombatantGroup) {
//        if (recyclerAdapter != null) {
//            recyclerAdapter.setCombatantList(newCombatantGroup);
//        }
//    }

    public Fightable.Faction getThisFaction() {
        return thisFaction;
    }
}
