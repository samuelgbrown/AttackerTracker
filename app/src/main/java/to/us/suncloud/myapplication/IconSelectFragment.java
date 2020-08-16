package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link IconSelectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class IconSelectFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String CURRENT_SELECTION = "currentSelection";

    RecyclerView iconGridRecyclerView;
    RecyclerView.Adapter iconGridAdapter;

    private int currentSelection = -1;

    public IconSelectFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment IconSelectFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static IconSelectFragment newInstance(int currentSelection) {
        IconSelectFragment fragment = new IconSelectFragment();
        Bundle args = new Bundle();
        args.putInt(CURRENT_SELECTION, currentSelection);
        fragment.setArguments(args);
        return fragment;
    }

    public static IconSelectFragment newInstance() {
        IconSelectFragment fragment = new IconSelectFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the current selection from the input
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(CURRENT_SELECTION)) {
                currentSelection = getArguments().getInt(CURRENT_SELECTION);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragView = inflater.inflate(R.layout.fragment_icon_select, container, false);

        iconGridRecyclerView = fragView.findViewById(R.id.icon_grid_recycler_view);
        iconGridRecyclerView.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);

        getContext().getAssets().




        return fragView;
    }
}