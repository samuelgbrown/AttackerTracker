package to.us.suncloud.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

public class CombatantGroupFragment extends Fragment {
    final Combatant.Faction thisFaction; // The faction that this fragment represents
    RecyclerView combatantRecyclerView;
    RecyclerView.Adapter combatantRecyclerAdapter; // TODO SOON: Need to use custom adapter

    CombatantGroupFragment(RecyclerView.Adapter combatantRecyclerAdapter) {
//        setCombatantList(newCombatantList.getCombatantArrayList());
        this.thisFaction = newCombatantList.getThisFaction(); // TODO: Deal with this implementation

        this.combatantRecyclerAdapter = combatantRecyclerAdapter;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.combatant_group, container, false);
        combatantRecyclerView = fragView.findViewById(R.id.groupCombatant_view);
        TextView GroupTextView = fragView.findViewById(R.id.groupTextView);

        // Set the adapter for the recycler view
        combatantRecyclerView.setAdapter(combatantRecyclerAdapter);

        // Set the text view using the faction type
        ArrayList<String> allFactionNames = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.group_names)));
        ArrayList<Combatant.Faction> allFactionValues = new ArrayList<>(Arrays.asList(Combatant.Faction.values()));
        GroupTextView.setText(allFactionNames.get(allFactionValues.indexOf(thisFaction)));

        // Return the inflated fragment
        return fragView;
    }


//    public void setCombatantList(ArrayList<Combatant> newCombatantGroup) {
//        if (recyclerAdapter != null) {
//            recyclerAdapter.setCombatantList(newCombatantGroup);
//        }
//    }

    public Combatant.Faction getThisFaction() {
        return thisFaction;
    }
}
