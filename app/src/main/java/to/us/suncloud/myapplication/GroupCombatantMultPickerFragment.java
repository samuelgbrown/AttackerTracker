package to.us.suncloud.myapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;

public class GroupCombatantMultPickerFragment extends DialogFragment {

    private static final String ARG_PARENT = "parent";
    private static final String ARG_CURMULTIPLES = "current_multiples";
    private static final String ARG_COMBATANTIND = "combatant_ind";

    private HandleMultPicker parent;
    private int currentMultiples;
    private int combatantInd;

    EditText multiplesEditBox;
    boolean manuallySettingText = false;

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
            currentMultiples = getArguments().getInt(ARG_CURMULTIPLES);
            combatantInd = getArguments().getInt(ARG_COMBATANTIND);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        checkUserInput();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_group_combatant_mult_picker, container, false);
        multiplesEditBox = view.findViewById(R.id.multiples_text_box);

        // Initialize edit box
        manuallySetText(Integer.toString(currentMultiples));
        multiplesEditBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                        keyEvent == null ||
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    //User finished typing
                    if (!manuallySettingText) {
                        checkUserInput();
                    }
                }
                return false;
            }
        });

        // Have the edit box get focus when the dialog is created
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        multiplesEditBox.postDelayed(new Runnable() {
            @Override
            public void run() {
                multiplesEditBox.setSelectAllOnFocus(true);
                if (multiplesEditBox.requestFocus()) {
                    imm.showSoftInput(multiplesEditBox, 0);
                }
                multiplesEditBox.setSelectAllOnFocus(false);
            }
        }, 100);

        return view;
    }

    void checkUserInput() {
        final int lowerLim = 1;
        final int upperLim = 99;

        boolean canExit = true;
        String newString = multiplesEditBox.getText().toString();
        int newInt = -1;
        try {
            newInt = Integer.parseInt(newString); // Edit box disallows anything except numbers, so no exception is expected
        } catch (NumberFormatException e) {
            // Do nothing (default -1 will trigger warning for user)
        }

        // Check the validity of the input
        if (newInt == 0) {
            // Tell the user how stupid they are
            Toast.makeText(getContext(), R.string.combatant_multiple_attempt_zero, Toast.LENGTH_SHORT).show();
            canExit = false;

        }

        if (canExit && ((newInt < lowerLim) || (newInt > upperLim))) {
            // Tell the user how stupid they are
            Toast.makeText(getContext(), getContext().getString(R.string.combatant_multiple_range, lowerLim, upperLim), Toast.LENGTH_SHORT).show();
            canExit = false;
        }

        if (!canExit) {
            manuallySetText(Integer.toString(currentMultiples));
        } else {
            parent.recieveCombatantMultiplesValue(combatantInd, newInt);
            dismiss();
        }
    }

    void manuallySetText(String newStr) {
        manuallySettingText = true;
        multiplesEditBox.setText(newStr);
        manuallySettingText = false;
    }

    public interface HandleMultPicker extends Serializable {
        void recieveCombatantMultiplesValue(int combatantInd, int newMultiple);
    }

}