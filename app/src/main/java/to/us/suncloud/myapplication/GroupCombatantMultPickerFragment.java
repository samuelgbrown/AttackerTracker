package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.io.Serializable;

public class GroupCombatantMultPickerFragment extends DialogFragment {

    private static final String ARG_PARENT = "parent";
    private static final String ARG_CURMULTIPLES = "current_multiples";
    private static final String ARG_COMBATANTIND = "combatant_ind";

    private HandleMultPicker parent;
    private int currentMultiples;
    private int combatantInd;

    public GroupCombatantMultPickerFragment() {
        // Required empty public constructor
    }

    public static GroupCombatantMultPickerFragment newInstance(HandleMultPicker parent, int combatantInd, int currentMultiples) {
        GroupCombatantMultPickerFragment fragment = new GroupCombatantMultPickerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARENT, parent);
        args.putInt(ARG_COMBATANTIND, combatantInd);
        args.putInt(ARG_CURMULTIPLES, currentMultiples);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            parent = (HandleMultPicker) getArguments().getSerializable(ARG_PARENT);
            currentMultiples = getArguments().getInt(ARG_COMBATANTIND);
            combatantInd = getArguments().getInt(ARG_CURMULTIPLES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_group_combatant_mult_picker, container, false);
        final EditText multiplesEditBox = view.findViewById(R.id.multiples_text_box);

        // Initialize edit box
        multiplesEditBox.setText(Integer.toString(currentMultiples));
        multiplesEditBox.addTextChangedListener(new TextWatcher() {
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
                final int lowerLim = 1;
                final int upperLim = 99;

                boolean canExit = true;
                String newString = editable.toString();
                int newInt = Integer.parseInt(newString); // Edit box disallows anything except numbers, so no exception is expected

                // Check the validity of the input
                if (newInt == 0) {
                    // Tell the user how stupid they are
                    Toast.makeText(getContext(), R.string.combatant_multiple_attempt_zero, Toast.LENGTH_SHORT).show();
                    canExit = false;

                }

                if (canExit && ((newInt < lowerLim) || (newInt > upperLim)) ) {
                    // Tell the user how stupid they are
                    Toast.makeText(getContext(), getContext().getString(R.string.combatant_multiple_range, lowerLim, upperLim), Toast.LENGTH_SHORT).show();
                    canExit = false;
                }

                if (!canExit) {
                    multiplesEditBox.setText(Integer.toString(currentMultiples));
                } else {
                    parent.recieveCombatantMultiplesValue(combatantInd, newInt);
                    dismiss();
                }
            }
        });


        return view;
    }

    public interface HandleMultPicker extends Serializable {
        void recieveCombatantMultiplesValue(int combatantInd, int newMultiple);
    }
}