package to.us.suncloud.myapplication;

import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link IconSelectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class IconSelectFragment extends DialogFragment implements IconGridRecyclerAdapter.SendIconSelectionInterface {

    // The fragment initialization parameters
    private static final String CURRENT_SELECTION = "currentSelection";
    private static final String PARENT = "parent";

    RecyclerView iconGridRecyclerView;
    IconGridRecyclerAdapter iconGridAdapter;
    ReceiveIconSelectionInterface parent = null;

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
    public static IconSelectFragment newInstance(ReceiveIconSelectionInterface parent, int currentSelection) {
        IconSelectFragment fragment = new IconSelectFragment();
        Bundle args = new Bundle();
        args.putInt(CURRENT_SELECTION, currentSelection);
        args.putSerializable(PARENT, parent);
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

            if (args.containsKey(PARENT)) {
                parent = (ReceiveIconSelectionInterface) getArguments().getSerializable(PARENT);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment        layoutManager.setSp
        View fragView = inflater.inflate(R.layout.fragment_icon_select, container, false);

        // Get the main recycler view
        iconGridRecyclerView = fragView.findViewById(R.id.icon_grid_recycler_view);

        // Initialize aspects of the recycler
//        iconGridRecyclerView.setHasFixedSize(true); // We aren't adding or removing icons
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4); // Make a grid layout manager, with 4 columns
        iconGridAdapter = new IconGridRecyclerAdapter(this, currentSelection); // Create a new adapter to feed views to the grid layout manager

        // Set up the recycler view with the adapter and manager
        iconGridRecyclerView.setAdapter(iconGridAdapter);
        iconGridRecyclerView.setLayoutManager(layoutManager);


        return fragView;
    }

    @Override
    public void setIconIndex(int iconIndex) {
        // Receive an icon index from the IconGridRecyclerAdapter, and send it to the parent fragment
        if (parent != null) {
            parent.setIconIndex(iconIndex);
        } // If parent is null, then something has gone horribly wrong...

        // Now that we have sent the icon index, close the dialog
        this.dismiss();
    }

    interface ReceiveIconSelectionInterface extends Serializable {
        void setIconIndex(int iconIndex);
    }
}