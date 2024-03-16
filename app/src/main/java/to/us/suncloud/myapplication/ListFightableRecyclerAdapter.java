package to.us.suncloud.myapplication;

import static to.us.suncloud.myapplication.FightableFilteredDiffUtil.DIFF_FACTION;
import static to.us.suncloud.myapplication.FightableFilteredDiffUtil.DIFF_ICON;
import static to.us.suncloud.myapplication.FightableFilteredDiffUtil.DIFF_MULTISELECT;
import static to.us.suncloud.myapplication.FightableFilteredDiffUtil.DIFF_NAME;
import static to.us.suncloud.myapplication.FightableFilteredDiffUtil.DIFF_NUM_ENEMY;
import static to.us.suncloud.myapplication.FightableFilteredDiffUtil.DIFF_NUM_NEUTRAL;
import static to.us.suncloud.myapplication.FightableFilteredDiffUtil.DIFF_NUM_PARTY;
import static to.us.suncloud.myapplication.FightableFilteredDiffUtil.DIFF_SELECTED;
import static to.us.suncloud.myapplication.ViewGroupFragment.ARG_AFFL;
import static to.us.suncloud.myapplication.ViewGroupFragment.ARG_AUTO_ACCEPT_GROUP;
import static to.us.suncloud.myapplication.ViewGroupFragment.ARG_INITIALIZE_GROUP_CHANGED;
import static to.us.suncloud.myapplication.ViewGroupFragment.ARG_PARENT;
import static to.us.suncloud.myapplication.ViewGroupFragment.ARG_THIS_GROUP;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListFightableRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Serializable, Filterable, ReceiveNewOrModFightablesInterface {
    // TODO NOTE: Got serializable error - check that all contents can be serialized...? (1/15/24 -quick search shows no members that don't implement Serializable)

    // TODO GROUP SOON: Profile app!  If there are a large number of Combatants (~20?) scrolling is not smooth...see if we can fix that!
    public static final int COMBATANT_VIEW = 0;
    public static final int GROUP_VIEW = 1;
    public static final int BANNER_VIEW = 2;

//    public static final String SET_MULTI_SELECT = "setMultiSelect";
    public static final String MODIFY_FIGHTABLE_ID = "modifyFightableLocation";
    public static final String MODIFY_FIGHTABLE_DELETED = "modifyFightableDeleted";

    private static final String TAG = "ListFightableRecycler";

    MasterAFFLKeeper parent; // If this is set, then the selected Combatant will be sent to the parent
    boolean adapterCanModify; // Can the adapter modify the Combatant (used so that we can use this adapter for both Combatant display and modify+display purposes, because they are VERY similar)
    boolean adapterCanCopy; // Can the adapter copy Combatants
    boolean adapterCanMultiSelect; // Can the adapter allow multi-selecting of Combatants (does that make sense in context?)
    boolean adapterIsRoster; // Is this a roster (holding base names of Fightables), or does this handle individual instances of Fightables?

    private AllFactionFightableLists fightableList_Master; // The master version of the list
    private AllFactionFightableLists fightableList_Memory; // A memory version of the list, to see what changes have occurred
    private ArrayList<ArrayList<Integer>> fightableFilteredIndices; // The indices in fightableList_Master that contain the given filter string

    boolean isMultiSelecting = false; // Is the adapter currently in multiselect mode
    ArrayList<Integer> iconResourceIds; // A list of resource ID's of the icons that will be used for each Combatant

    private String filteredText = ""; // The string that is currently being used to filter the list

    public ListFightableRecyclerAdapter(MasterAFFLKeeper parent, AllFactionFightableLists combatantList, LFRAFlags flags) {
        this.parent = parent;
        this.adapterCanModify = flags.adapterCanModify;
        this.adapterCanCopy = flags.adapterCanCopy;
        this.adapterCanMultiSelect = flags.adapterCanMultiSelect;
        this.adapterIsRoster = !flags.adapterAllowsOrdinals;
        this.fightableList_Master = combatantList; // Save the reference (master will be modified directly)
//        this.combatantList_Display = new FactionFightableList(combatantList); // COPY the main list for these two lists, so that the master is not changed
        this.fightableList_Memory = combatantList.clone();

//        clearMultiSelect(); // More trouble than it's worth...
        setupIconResourceIDs();
        fightableFilteredIndices = fightableList_Master.getIndicesThatMatch(filteredText);

        updateMultiSelectStatus();
    }

    private void setupIconResourceIDs() {
        // Preload a list of resources that will be used to load svg's into the grid
        iconResourceIds = new ArrayList<>();
        int curNum = 0;
        while (true) {
            // Generate filenames for every icon that we will use in order, and check if it exists
            String resourceName = String.format(Locale.US, "icon_%02d", curNum); // Oh, the horror...
            int id = parent.getContext().getResources().getIdentifier(resourceName, "drawable", parent.getContext().getPackageName());

            if (id > 0) {
                // If the id is valid
                iconResourceIds.add(id);
            } else {
                // If the id is invalid (equal to 0), then there are no more icons to load
                break;
            }

            curNum++;
        }
    }

    public void clearMultiSelect() {
        // Initialize the multi-select to have no selections
        // Re-initialize the isSelected list to be all false
        fightableList_Master.clearSelected();

        // Now notify the adapter that we have changed the selection status (this will update isMultiSelecting)
        notifyFightableListChanged();

        parent.notifyIsMultiSelecting(isMultiSelecting);
    }

    public void updateMultiSelectStatus() {
        // Update the isMultiSelecting variable based on the current values in isSelectedList
        isMultiSelecting = false;
        if (adapterCanMultiSelect) {
            for (int i = 0; i < fightableList_Master.size(); i++) {
                if (fightableList_Master.get(i).isSelected()) {
                    isMultiSelecting = true;
                    break;
                }
            }

            // Update any Activity/fragment GUI elements that are related to multi-select
            parent.notifyIsMultiSelecting(isMultiSelecting);
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    interface bindableVH {
        void bind(int position);
    }

    interface FightableViewHolder {
        void setName(String newName);
        void setFaction(Fightable.Faction newFaction);
        void setSelected(boolean isSelected);
        void setMultiSelectingMode(boolean isMultiSelecting);
    }

    class CombatantGroupViewHolder extends RecyclerView.ViewHolder implements Serializable, bindableVH, FightableViewHolder {
        View itemView;
        public TextView mGroupName;
        public TextView mPartyNum;
        public TextView mNeutralNum;
        public TextView mEnemyNum;
        private final CardView mPartyCard;
        private final CardView mNeutralCard;
        private final CardView mEnemyCard;
        private final ConstraintLayout CombatantGroupMultiSelect;
        ImageButton mGroupRemove; // Only change visibility through setCombatantRemoveVisibility
        ImageButton mGroupModify; // Only change visibility through setCombatantChangeCombatantVisibility
        ImageButton mGroupCopy; // Only change visibility through setCombatantCopyVisibility

        public CombatantGroupViewHolder(@NonNull final View itemView) {
            super(itemView);

            this.itemView = itemView;

            mGroupName = itemView.findViewById(R.id.group_name);
            mPartyNum = itemView.findViewById(R.id.party_count);
            mNeutralNum = itemView.findViewById(R.id.neutral_count);
            mEnemyNum = itemView.findViewById(R.id.enemy_count);
            mPartyCard = itemView.findViewById(R.id.party_card);
            mNeutralCard = itemView.findViewById(R.id.neutral_card);
            mEnemyCard = itemView.findViewById(R.id.enemy_card);
            CombatantGroupMultiSelect = itemView.findViewById(R.id.group_multi_select_pane);
            mGroupRemove = itemView.findViewById(R.id.group_mod_remove);
            mGroupModify = itemView.findViewById(R.id.group_mod_change);
            mGroupCopy = itemView.findViewById(R.id.group_mod_copy);

            // If this adapter can multi-select, set up the interface with the ViewHolders
            View.OnLongClickListener multiSelectStartListener = v -> {
                if (adapterCanMultiSelect && !isMultiSelecting) {
                    // If we can multi-select, but we aren't multi-selecting right now, then the user wants to start multi-selecting, and also select this Combatant
                    // Update the value of isSelectedList
                    getThisFightable().setSelected(true);

                    // Let the adapter know that this has become selected
                    notifyFightableListChanged();
                    return true; // Let Android know that we handled this click here, so we don't need to activate the standard onClickListener
                } // If not multi-selecting, click will be handled as normal click

                return false; // Nothing happened, to go back to the standard onClickListener
            };

            itemView.setOnLongClickListener(multiSelectStartListener);
            mGroupName.setOnLongClickListener(multiSelectStartListener);
            mPartyNum.setOnLongClickListener(multiSelectStartListener);
            mNeutralNum.setOnLongClickListener(multiSelectStartListener);
            mEnemyNum.setOnLongClickListener(multiSelectStartListener);

            View.OnClickListener groupClickListener = view -> {
                final Fightable thisFightable = getThisFightable();
                if (isMultiSelecting) {
                    // If we are currently multi-selecting, then we just want to toggle this Combatant's isSelected status, and update any GUI elements
                    boolean newIsSelected = !getThisFightable().isSelected(); // Toggle the current value

                    // Update the master list
                    getThisFightable().setSelected(newIsSelected);

                    // Update the GUI, and notify the adapter
                    notifyFightableListChanged();
                } else {
                    if ((thisFightable instanceof CombatantGroup) && (parent != null)) {
                        final CombatantGroup thisGroup = (CombatantGroup) thisFightable;
                        String titleString = itemView.getContext().getString(
                                R.string.confirm_add_group_combatants, thisGroup.numTotalCombatants(), thisGroup.getName());
                        new AlertDialog.Builder(itemView.getContext())
                                .setTitle(titleString)
                                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                                    parent.receiveChosenFightable(thisFightable); // Fragment will separate into Combatants and deal with any hiccups
                                })
                                .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                                    // Do nothing
                                })
                                .show();
                    }

                }
            };

            itemView.setOnClickListener(groupClickListener);
            mGroupName.setOnClickListener(groupClickListener);
            mPartyNum.setOnClickListener(groupClickListener);
            mNeutralNum.setOnClickListener(groupClickListener);
            mEnemyNum.setOnClickListener(groupClickListener);

            setSelected(false);

            // Set up modification buttons
            setMultiSelectingMode(isMultiSelecting);
            if (adapterCanModify) {
                // If the adapter can modify the Combatants/the Combatant list, then allow the user to do so through these buttons
                mGroupModify.setOnClickListener(view -> {
                    Fightable thisFightable = fightableList_Master.getFromVisible(posToFightableInd(getAdapterPosition()), getFilteredIndices());
                    if (thisFightable instanceof CombatantGroup) {
                        // Create a clone of the master Fightable list, to send as a reference to the ViewGroupFragment
                        AllFactionFightableLists referenceAFFL = fightableList_Master.clone();

                        referenceAFFL.clearSelected();// Clear any selected from the groupFragmentAFFL, so nothing appears selected in the ViewGroup dialog

                        FragmentManager fm = scanForActivity(view.getContext()).getSupportFragmentManager();

                        Bundle bundle = new Bundle(); // Bundle that contains info that ViewGroupFragment needs
                        bundle.putSerializable(ARG_AFFL, referenceAFFL);
                        bundle.putSerializable(ARG_PARENT, ListFightableRecyclerAdapter.this);
                        bundle.putSerializable(ARG_THIS_GROUP, thisFightable);

                        ViewGroupFragment.newInstance(bundle).show(fm, "ViewGroupFragment");
                    } else {
                        Log.e(TAG, "CombatantGroupViewHolder mGroupModify got a non-CombatantGroup Fightable!");
                    }
                });

                mGroupRemove.setOnClickListener(view -> {
                    // Ask the user if they definitely want to remove the CombatantGroup
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle(R.string.confirm_delete_group)
                            .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                                // Remove this from the Fightable list
                                removeFightable(posToFightableInd(getAdapterPosition()));
                            })
                            .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                                // Do nothing
                            })
                            .show();
                });
            }
            if (adapterCanCopy) {
                mGroupCopy.setOnClickListener(view -> {
                    // Ask the user if they definitely want to copy the CombatantGroup
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle(R.string.confirm_copy_group)
                            .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                                // Copy this in the Combatant list
                                copyFightable(posToFightableInd(getAdapterPosition()));
                            })
                            .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                                // Do nothing
                            })
                            .show();
                });
            }
        }

        private Fightable getThisFightable() {
            return displayList().get(posToFightableInd(getAdapterPosition()));
        }

        @Override
        public void bind(int groupIndexIn) {
            Fightable thisFightable = getThisFightable();
            if (thisFightable instanceof CombatantGroup) {
                // If this represents an existing group...
                CombatantGroup thisGroup = (CombatantGroup) thisFightable;

                setName(thisGroup.getName());
                setNumParty(thisGroup.getTotalCombatantsInFaction(Fightable.Faction.Party));
                setNumNeutral(thisGroup.getTotalCombatantsInFaction(Fightable.Faction.Neutral));
                setNumEnemy(thisGroup.getTotalCombatantsInFaction(Fightable.Faction.Enemy));
                setSelected(thisGroup.isSelected());
                setMultiSelectingMode(isMultiSelecting);
            } else {
                Log.e(TAG, "CombatantGroupViewHolder bind got a non-CombatantGroup Fightable!");
            }
        }

        public void setNumParty( int numParty ) {
            if (numParty > 0) {
                mPartyCard.setVisibility(View.VISIBLE);
            } else {
                mPartyCard.setVisibility(View.GONE);
            }
            mPartyNum.setText(Integer.toString(numParty));
        }

        public void setNumNeutral( int numNeutral) {
            if (numNeutral > 0) {
                mNeutralCard.setVisibility(View.VISIBLE);
            } else {
                mNeutralCard.setVisibility(View.GONE);
            }
            mNeutralNum.setText(Integer.toString(numNeutral));
        }

        public void setNumEnemy( int numEnemy) {
            if (numEnemy > 0) {
                mEnemyCard.setVisibility(View.VISIBLE);
            } else {
                mEnemyCard.setVisibility(View.GONE);
            }
            mEnemyNum.setText(Integer.toString(numEnemy));
        }

        @Override
        public void setName(String newName) {
            mGroupName.setText(newName);
        }

        @Override
        public void setFaction(Fightable.Faction newFaction) {
            // Do Nothing - should always be Fightable.Faction.Group
        }

        @Override
        public void setSelected(boolean isSelected) {
            // This will get called by onBindViewHolder via payload in the event that all must be deselected (if the Combatant list gets modified in ANY WAY)
            // Set the visibility of the multi-select pane based on the input
            final int visibility;
            if (isSelected) {
                // If we can multi-select in this list, AND this Combatant is selected, then make the multi-select pane visible
                visibility = View.VISIBLE;
            } else {
                visibility = View.GONE;
            }

            // Update the GUI
            CombatantGroupMultiSelect.setVisibility(visibility);
        }

        private void setGroupRemoveVisibility(int newVis) {
            if ( adapterCanModify ) {
                mGroupRemove.setVisibility(newVis);
            } else {
                mGroupRemove.setVisibility(View.GONE);
            }
        }
        private void setGroupChangeCombatantVisibility(int newVis) {
            if ( adapterCanModify ) {
                mGroupModify.setVisibility(newVis);
            } else {
                mGroupModify.setVisibility(View.GONE);
            }
        }
        private void setGroupCopyVisibility(int newVis) {
            if ( adapterCanCopy ) {
                mGroupCopy.setVisibility(newVis);
            } else {
                mGroupCopy.setVisibility(View.GONE);
            }
        }

        @Override
        public void setMultiSelectingMode(boolean isMultiSelecting) {
            // This will be called when the adapter enters or exits multi selecting mode (for ALL viewHolders)
            final int visibility;
            if (isMultiSelecting) {
                visibility = View.GONE;
            } else {
                visibility = View.VISIBLE;
            }

            // Update the GUI
            setGroupRemoveVisibility(visibility);
            setGroupChangeCombatantVisibility(visibility);
            setGroupCopyVisibility(visibility);
        }
    }

    class CombatantViewHolder extends RecyclerView.ViewHolder implements Serializable, bindableVH, FightableViewHolder {
        View itemView;
        TextView NameView;
        ImageButton CombatantRemove; // Only change visibility through setCombatantRemoveVisibility
        ImageButton CombatantChangeCombatant; // Only change visibility through setCombatantChangeCombatantVisibility
        ImageButton CombatantCopy; // Only change visibility through setCombatantCopyVisibility
        ImageView CombatantIcon;
        ConstraintLayout CombatantIconBorder;
        ConstraintLayout CombatantMultiSelect;

        // Difference between Add and configure:
        //      1. Configure will have gear button to change aspects of Combatant
        //      2. Remove will be different - Configure will be to remove from the list, add will be to remove from the file
        //          Note that this difference in removal behavior may be accomplished just through the RecyclerAdapter (i.e. where the list gets sent after the fragment this recycler is in closes)
        // Current game-plan:
        //  Use the SAME ViewHolder for both add and configure.  Fragment will do different things with the final list that this recycler is viewing/modifying (configure: return it to the main activity, add: save the list to file and return a single Combatant)
        //  For Add, perhaps double-check with user if they want to save the modified list to file?  Either at Combatant modification or when fragment is returning Combatant (in this case, this adapter doesn't need to differentiate add from configure)
        //
        // On second thought, figure out way to not have gear show up for Add Combatant version?

        public CombatantViewHolder(@NonNull final View itemView) {
            super(itemView);

            this.itemView = itemView;
            NameView = itemView.findViewById(R.id.combatant_mod_name);
            CombatantRemove = itemView.findViewById(R.id.combatant_mod_remove);
            CombatantChangeCombatant = itemView.findViewById(R.id.combatant_mod_change_combatant);
            CombatantIcon = itemView.findViewById(R.id.combatant_mod_icon);
            CombatantIconBorder = itemView.findViewById(R.id.combatant_mod_icon_border);
            CombatantCopy = itemView.findViewById(R.id.combatant_mod_copy);
            CombatantMultiSelect = itemView.findViewById(R.id.combatant_multi_select_pane);

            // Set up click functionality for rest of viewHolder (i.e. name, "itemView" [the background], and the icon)
            View.OnClickListener returnCombatantListener = view -> {
                // If we are expecting a Combatant to return, then figure out what the user is trying to do (otherwise, ignore the click)
                if (isMultiSelecting) {
                    // If we are currently multi-selecting, then we just want to toggle this Combatant's isSelected status, and update any GUI elements
                    boolean newIsSelected = !getThisFightable().isSelected(); // Toggle the current value

                    // Update the master list
                    getThisFightable().setSelected(newIsSelected);

                    // Update the GUI, and notify the adapter
                    notifyFightableListChanged();
                } else {
                    if (parent != null) {
                        // Get the current position in the adapter, use it to find the Combatant position in fightableList_Master (taking into account the banners), use that to find this Combatant in the master list (taking into account the filter string with "subList()"), and make a unique clone of it to send back to the parent (phew...)
                        parent.receiveChosenFightable(getThisFightable().cloneUnique());
                    }
                }
            };

            CombatantIcon.setOnClickListener(returnCombatantListener);
            NameView.setOnClickListener(returnCombatantListener);
            itemView.setOnClickListener(returnCombatantListener);

            // If this adapter can multi-select, set up the interface with the ViewHolders
            View.OnLongClickListener multiSelectStartListener = v -> {
                if (adapterCanMultiSelect && !isMultiSelecting) {
                    // If we can multi-select, but we aren't multi-selecting right now, then the user wants to start multi-selecting, and also select this Combatant
                    // Update the value of isSelectedList
                    getThisFightable().setSelected(true);

                    // Let the adapter know that this has become selected
                    notifyFightableListChanged();
                    return true; // Let Android know that we handled this click here, so we don't need to activate the standard onClickListener
                } // If not multi-selecting, click will be handled as normal click

                return false; // Nothing happened, to go back to the standard onClickListener
            };

            CombatantIcon.setOnLongClickListener(multiSelectStartListener);
            NameView.setOnLongClickListener(multiSelectStartListener);
            itemView.setOnLongClickListener(multiSelectStartListener);
            CombatantMultiSelect.setOnLongClickListener(multiSelectStartListener);

            setSelected(false);

            setMultiSelectingMode(isMultiSelecting);
            if (adapterCanModify) {
                // If the adapter can modify the Combatants/the Combatant list, then allow the user to do so through these buttons
                CombatantChangeCombatant.setOnClickListener(view -> {
                    FragmentManager fm = scanForActivity(view.getContext()).getSupportFragmentManager();
                    Fightable thisFightable = fightableList_Master.getFromVisible(posToFightableInd(getAdapterPosition()), getFilteredIndices());
                    if (thisFightable instanceof Combatant) {
                        CreateOrModCombatant newDiag = CreateOrModCombatant.newInstance(ListFightableRecyclerAdapter.this, (Combatant) thisFightable); // Make a clone of this Combatant (such that the ID is the same, so it gets put back in the same spot when it returns)
                        newDiag.show(fm, "CreateOrModCombatant");
                    } else {
                        Log.e(TAG, "CombatantViewHolder CombatantChangeCombatant got a non-Combatant Fightable!");
                    }
                });

                CombatantRemove.setOnClickListener(view -> {
                    // Determine if the combatant is in a group or not, to tell which message to display
                    int titleStrId = getCombatantList().combatantIsInAGroup((Combatant) getThisFightable()) ?
                            R.string.confirm_delete_combatant_in_group : R.string.confirm_delete_combatant;

                    // Ask the user if they definitely want to remove the Combatant
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle(R.string.confirm_delete_combatant_title)
                            .setMessage(parent.getContext().getString(titleStrId, getThisFightable().getName()))
                            .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                                // Remove this from the Combatant list
                                removeFightable(posToFightableInd(getAdapterPosition()));
                            })
                            .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                                // Do nothing
                            })
                            .show();
                });
            }
            if (adapterCanCopy) {
                CombatantCopy.setOnClickListener(view -> {
                    // Ask the user if they definitely want to copy the Combatant
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle(R.string.confirm_copy_combatant)
                            .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                                // Copy this in the Combatant list
                                copyFightable(posToFightableInd(getAdapterPosition()));
                            })
                            .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                                // Do nothing
                            })
                            .show();
                });
            }
        }

        public void bind(int position) {
            // Get the corresponding Fightable ind
            int combatantInd = posToFightableInd(position);

            // Make sure the corresponding Fightable is actually a Combatant
            Fightable thisFightable = fightableList_Master.getFromVisible(combatantInd, getFilteredIndices());
            if (thisFightable instanceof Combatant) {
                Combatant thisCombatant = (Combatant) thisFightable;

                // Make sure that the Combatant is not selected
                setSelected(thisCombatant.isSelected());

                // Set the multi-selecting mode
                setMultiSelectingMode(isMultiSelecting);

                // Load the icon image
                setIcon(thisCombatant.getIconIndex());

                // Set the color of the icon and the icon's border
                setFaction(thisCombatant.getFaction());

                setName(thisCombatant.getName());
            } else {
                Log.e(TAG, "CombatantViewHolder bind got a non-Combatant Fightable!");
            }

        }
        
        private Fightable getThisFightable() {
            return displayList().get(posToFightableInd(getAdapterPosition()));
        }

        // Methods to update parts of the ViewHolder
        public void setName(String name) {
            // TO_DO: Display the name differently based on the filter text
//            if (filteredText.isEmpty()) {
//                // If the filter text is blank, then just display the name as is
                NameView.setText(name);
//            } else {
//                // If the filter text is NOT blank, then bold the relevant part of the name
//                // First, find the beginning and end of all occurrences of the string
//                SpannableStringBuilder spannable = new SpannableStringBuilder(name);
//                StyleSpan span = new StyleSpan(BOLD);
//                int filteredTextIndex = name.indexOf(filteredText);
//                if (filteredTextIndex != -1) {
//                    // If the string appears in the name, then make it bold
//                    spannable.setSpan(span, filteredTextIndex, filteredTextIndex + filteredText.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
//
//                    for (int i = -1; (i = name.indexOf(filteredText, i + 1)) != -1; i++) {
//                        // Find all occurrences forward
//                        spannable.setSpan(span, i, i + filteredText.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    }
//                }
//
//                NameView.setText(spannable);
//            }
        }

        public void setFaction(Fightable.Faction faction) {
            int colorId = -1;
            switch (faction) {
                case Group:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorGroup);
                    break;
                case Party:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorParty);
                    break;
                case Enemy:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorEnemy);
                    break;
                case Neutral:
                    colorId = CombatantIcon.getContext().getResources().getColor(R.color.colorNeutral);
            }

            CombatantIcon.setImageTintList(ColorStateList.valueOf(colorId));
            CombatantIconBorder.setBackgroundColor(colorId);
        }

        public void setIcon(int iconIndex) {
            if (iconIndex == 0) {
                // If the icon index is 0, then the icon is blank
                CombatantIcon.setImageResource(android.R.color.transparent);
            } else {
                // Otherwise, get the corresponding icon image
                CombatantIcon.setImageDrawable(CombatantIcon.getContext().getDrawable(iconResourceIds.get(iconIndex - 1)));
            }
        }

        public void setSelected(boolean isSelected) {
            // This will get called by onBindViewHolder via payload in the event that all must be deselected (if the Combatant list gets modified in ANY WAY)
            // Set the visibility of the multi-select pane based on the input
            final int visibility;
            if (isSelected) {
                // If we can multi-select in this list, AND this Combatant is selected, then make the multi-select pane visible
                visibility = View.VISIBLE;
            } else {
                visibility = View.GONE;
            }

            // Update the GUI
            CombatantMultiSelect.setVisibility(visibility);
        }

        public void setMultiSelectingMode(boolean isMultiSelecting) {
            // This will be called when the adapter enters or exits multi selecting mode (for ALL viewHolders)
            final int visibility;
            if (isMultiSelecting) {
                visibility = View.GONE;
            } else {
                visibility = View.VISIBLE;
            }

            // Update the GUI
            setCombatantRemoveVisibility(visibility);
            setCombatantChangeCombatantVisibility(visibility);
            setCombatantCopyVisibility(visibility);
        }

        private void setCombatantRemoveVisibility(int newVis) {
            if ( adapterCanModify ) {
                CombatantRemove.setVisibility(newVis);
            } else {
                CombatantRemove.setVisibility(View.GONE);
            }
        }
        private void setCombatantChangeCombatantVisibility(int newVis) {
            if ( adapterCanModify ) {
                CombatantChangeCombatant.setVisibility(newVis);
            } else {
                CombatantChangeCombatant.setVisibility(View.GONE);
            }
        }
        private void setCombatantCopyVisibility(int newVis) {
            if ( adapterCanCopy ) {
                CombatantCopy.setVisibility(newVis);
            } else {
                CombatantCopy.setVisibility(View.GONE);
            }
        }
    }

    private static AppCompatActivity scanForActivity(Context cont) {
        if (cont == null)
            return null;
        else if (cont instanceof AppCompatActivity)
            return (AppCompatActivity) cont;
        else if (cont instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper) cont).getBaseContext());

        return null;
    }

    class FactionBannerViewHolder extends RecyclerView.ViewHolder implements Serializable, bindableVH {
        TextView FactionName;

        public FactionBannerViewHolder(@NonNull View itemView) {
            super(itemView);
            FactionName = itemView.findViewById(R.id.faction_name);
        }

        public void bind(int position) {
            int bannerInd = posToFightableInd(position);
            // Position here will just indicate which Faction in fightableList_Master this banner represents
            int textInd = R.string.party_header;
            switch (bannerInd) {
                case -1:
                    textInd = R.string.group_header;
                    break;
                case -2:
                    textInd = R.string.party_header;
                    break;
                case -3:
                    textInd = R.string.enemy_header;
                    break;
                case -4:
                    textInd = R.string.neutral_header;
                    break;
            }

            FactionName.setText(textInd);
        }
    }

    private int posToFightableInd(int position) {
        // Convert any adapter position to an index in fightableList_Master (adapter position will include banners as well as filter text)
        return displayList().posToFightableInd(position);
    }

    private AllFactionFightableLists displayList() {
        // Return a shallow copy of the list that is being displayed (including information about filter text and visibility status)
        return fightableList_Master.subListVisible(getFilteredIndices());
    }

    static class ReceiveFightableOptions {
        public static final String IS_COPY = "is_copy";
        public static final String FROM_FILE = "from_file";
    }

    @Override
    public void notifyListChanged() {
        notifyFightableListChanged();
    }

    public void receiveFightable(Fightable receivedFightable) {
        receiveFightable(receivedFightable, new Bundle());
    }

    public void receiveFightable(Fightable receivedFightable, Bundle fighterBundleData) {
        class FinalAction_Enum {
            public static final int DO_ADD_FIGHTABLE = 0; // Default - add the Fightable
            public static final int DO_WRITE_EXCEPT_NAME = 1; // Write all values except the name to the existing Fightable
            public static final int ASK_RESURRECT_NEW = 2; // Ask the user if they want to resurrect the Combatant
            public static final int ASK_OVERWRITE = 3; // Ask the user if they want to over-write the existing Combatant
            public static final int ASK_RESURRECT_MODIFIED = 4; // Resurrect the Combatant for the user
        }

        // Extract bundle
        boolean isCopy = false;
        boolean fromFile = false;
        if ( fighterBundleData.containsKey(ReceiveFightableOptions.IS_COPY)) {
            isCopy = fighterBundleData.getBoolean(ReceiveFightableOptions.IS_COPY);
        }
        if ( fighterBundleData.containsKey(ReceiveFightableOptions.FROM_FILE)) {
            fromFile = fighterBundleData.getBoolean(ReceiveFightableOptions.FROM_FILE);
        }

        // Gather some required input data
        Fightable.Faction receivedFaction = receivedFightable.getFaction();
        boolean receivedIsCombatant = receivedFaction != Fightable.Faction.Group;
        boolean usingOrdinal = !(adapterIsRoster && receivedIsCombatant);
        final Fightable unmodifiedReceivedFightable = receivedFightable.clone();

        Fightable originalFightable = fightableList_Master.getFightableWithID(receivedFightable.getId()); // The original version of the recently modified Fightable (if it exists, otherwise is null)
        Fightable collisionFightable;

        // If there is no "original", then the received Fightable is new
        boolean isNewFightable = originalFightable == null;

        if ( !usingOrdinal ) {
            // For Combatants in rosters - No ordinals allowed
            receivedFightable.setNameOrdinal(Fightable.NO_ORDINAL);
        }

        // Parameters to track
        int finalAction = FinalAction_Enum.DO_ADD_FIGHTABLE;

        // Determine which actions to take on the Fightable (and possibly other existing Fightables), and do minor pre-processing if required
        boolean resolved; // In most cases, one run through the logic will resolve all actions.  If the receivedFightable is modified, though, multiple runs will be needed

        do {
            resolved = true;
            collisionFightable = fightableList_Master.getFightableOfType(receivedFightable.getName(), receivedFaction); // To check for name collisions
            if (isNewFightable) {
                // For new Fightables
                if (usingOrdinal) {
                    // For non-rosters and any Group - Can use ordinals
                    // Check for uniqueness (Combatants and Groups must be unique among their respective collections, but not necessarily across [i.e., Combatant and Group may share a name])
                    if (collisionFightable == null) {
                        // There is no exact name match
                        if (receivedFightable.getOrdinal() == Fightable.NO_ORDINAL) {
                            // If there is no ordinal, we must see if any other Fightable exists with this base name.
                            int highestVisibleExistingOrdinal = fightableList_Master.getHighestVisibleOrdinalInstance(receivedFightable);
                            if (highestVisibleExistingOrdinal != Fightable.DOES_NOT_APPEAR) {
                                // If base name appears, set ordinal to next highest (highestVisibleExistingOrdinal != NO_ORDINAL if we get here, otherwise there would be a name collision), then continue with standard add Fightable
                                receivedFightable.setNameOrdinal(highestVisibleExistingOrdinal + 1);
                                resolved = false; // Make sure that this new name does not collide with any existing Combatants
                            } // If no other Fightable with this base name appears, then no modification needed!
                        } // If a new Fightable has a unique ordinal, then no modification needed!
                    } else {
                        // AFFL contains Fightable of type with this exact name (i.e., not unique within Combatants or Groups)
                        if (receivedIsCombatant && !collisionFightable.isVisible()) {
                            // If the collision Combatant is not visible (implied - we're NOT in a roster, so resurrections make sense!)
                            finalAction = FinalAction_Enum.ASK_RESURRECT_NEW;
                        } else {
                            // If it's an "alive" Combatant or a Group, then simply increment its ordinal
                            int collisionFightableOrdinal = collisionFightable.getOrdinal();
                            if (collisionFightableOrdinal == Fightable.NO_ORDINAL) {
                                // If both Fightables don't have a visible ordinal, then set one for each, then continue with standard add Fightable
                                receivedFightable.setNameOrdinal(2);
                                collisionFightable.setNameOrdinal(1);
                                resolved = false; // Make sure that this new name does not collide with any existing Combatants
                            } else {
                                // If both Fightables have a visible (and identical) ordinal...
                                if ( isCopy ) {
                                    receivedFightable.setNameOrdinal( collisionFightableOrdinal + 1 );
                                    resolved = false; // Make sure that this new name does not collide with any existing Combatants
                                } else {
                                    // check if the user is trying to over-write the existing Fightables
                                    finalAction = FinalAction_Enum.ASK_OVERWRITE;
                                }
                            }
                        }
                    }
                } else {
                    // For Combatants in rosters
                    if (collisionFightable != null) {
                        // If there IS a name collision and this is a new Combatant, check if the user is trying to over-write the existing Combatant
                        finalAction = FinalAction_Enum.ASK_OVERWRITE;
                    } // If the name is unique, then continue with standard add Fightable
                }
            } else {
                // If we are modifying an existing Fightable...
                if (collisionFightable != null) {
                    // ...and there is a name collision...
                    if (!collisionFightable.getId().equals(originalFightable.getId())) {
                        // ...and the Fightable we collided with is different than the Fightable we were originally modifying, reset the name and let the user know they messed up
                        if ( collisionFightable.isVisible() ) {
                            finalAction = FinalAction_Enum.DO_WRITE_EXCEPT_NAME;
                        } else {
                            finalAction = FinalAction_Enum.ASK_RESURRECT_MODIFIED;
                        }
                    }
                } // If the name is unique (not including Fightable we are modifying), then continue with standard add Fightable
            }
        } while (!resolved);

        // Process the Fightable
        final Fightable finalCollisionFightable = fightableList_Master.getFightableOfType(receivedFightable.getName(), receivedFaction); // To check for name collisions
        switch ( finalAction ) {
            case FinalAction_Enum.DO_WRITE_EXCEPT_NAME:
                if (!fromFile) {
                    // Let the user know they did something silly
                    Toast.makeText(parent.getContext(),
                                    parent.getContext().getString(R.string.name_already_used, receivedFightable.getName()),
                                    Toast.LENGTH_SHORT)
                            .show();

                    // Change the name back, but leave everything else
                    receivedFightable.setName(originalFightable.getName());

                    // Use this combatant to modify an existing Combatant
                    addFightableToList(receivedFightable);
                }
                break;
            case FinalAction_Enum.ASK_RESURRECT_NEW:
                if (!fromFile) {
                    // The user may be attempting to resurrect a Combatant by ADDING A NEW Combatant!
                    // Check to see their intentions
                    new AlertDialog.Builder(parent.getContext())
                            .setTitle(R.string.add_combatant_resurrect_check_title)
                            .setMessage(R.string.add_combatant_resurrect_check_message)
                            .setPositiveButton(R.string.copy_combatant, (dialog, which) -> {
                                // The user wants to copy this Combatant as a new version
                                // Modify this Combatant to use a higher ordinal
                                int highestExistingOrdinal = fightableList_Master.getHighestOrdinalInstance(finalCollisionFightable); // Cannot be DOES_NOT_EXIST
                                if (highestExistingOrdinal == Fightable.NO_ORDINAL) {
                                    finalCollisionFightable.setNameOrdinal(1); // Even though it's "dead", change the ordinal to be "1"
                                    receivedFightable.setNameOrdinal(2);
                                } else {
                                    receivedFightable.setNameOrdinal(highestExistingOrdinal + 1);
                                }

                                // Let the user know that any new version of this Combatant will be assumed to be copies
                                Toast.makeText(parent.getContext(), R.string.copy_combatant_warning, Toast.LENGTH_LONG).show();

                                // Add the new copy
                                addFightableToList(receivedFightable);
                            })
                            .setNegativeButton(R.string.resurrect_combatant, (dialog, which) -> {
                                // The user wants to resurrect the old version of the Combatant
                                finalCollisionFightable.setVisible(true); // Set the Combatant as visible

                                // Simply copy Combatant into list as is
                                addFightableToList(finalCollisionFightable);
                            })
                            .show();
                }
                break;
            case FinalAction_Enum.ASK_OVERWRITE:
                if (!fromFile) {
                    // Ask the user if they're ok with over-writing an existing roster Fightable (collisionFightable, NOT originalFightable!!!) with the new Fightable that they just created
                    String messageString = receivedIsCombatant ?
                            parent.getContext().getString(R.string.add_combatant_overwrite_check_message, receivedFightable.getName()) :
                            parent.getContext().getString(R.string.add_group_overwrite_check_message, receivedFightable.getName());
                    new AlertDialog.Builder(parent.getContext())
                            .setTitle(receivedIsCombatant ? R.string.add_combatant_overwrite_check_title : R.string.add_group_overwrite_check_title)
                            .setMessage(messageString)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                // Overwrite the existing Fightable's data
                                receivedFightable.setID(finalCollisionFightable.getId()); // Copy the ID from the existing Fightable
                                addFightableToList(receivedFightable); // "Modify" the collided Fightable with the data from receivedFightable
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> {
                                // Do NOT over-write the existing Fightable
                                FragmentManager fm = scanForActivity(parent.getContext()).getSupportFragmentManager();
                                if (receivedIsCombatant) {
                                    // Go back to the CreateOrModCombatant Fragment to modify this Combatant again
                                    CreateOrModCombatant createCombatantFragment = CreateOrModCombatant.newInstance(
                                            ListFightableRecyclerAdapter.this, (Combatant) unmodifiedReceivedFightable, true);
                                    createCombatantFragment.show(fm, "CreateNewCombatant");
                                } else {
                                    // Go back to the ViewGroupFragment to modify this Group
                                    Bundle bundle = new Bundle(); // Bundle that contains info that ViewGroupFragment needs
                                    bundle.putSerializable(ARG_AFFL, fightableList_Master);
                                    bundle.putSerializable(ARG_PARENT, ListFightableRecyclerAdapter.this);
                                    bundle.putSerializable(ARG_THIS_GROUP, unmodifiedReceivedFightable);
                                    bundle.putBoolean(ARG_AUTO_ACCEPT_GROUP, false); // If the user tries to exit via back button to allow them to discard the interaction, as they could before, given the complication of the over-write
                                    bundle.putBoolean(ARG_INITIALIZE_GROUP_CHANGED, true); // Make sure the fragment confirms with the user before closing

                                    ViewGroupFragment.newInstance(bundle).show(fm, "ViewGroupFragment");
                                }
                            })
                            .show();
                }
                break;
            case FinalAction_Enum.ASK_RESURRECT_MODIFIED:
                if (!fromFile) {
                    // The user may be attempting to resurrect a Combatant by MODIFYING an existing Combatant!
                    // Check to see their intentions
                    new AlertDialog.Builder(parent.getContext())
                            .setTitle(R.string.mod_combatant_resurrect_check_title)
                            .setMessage(R.string.mod_combatant_resurrect_check_message)
                            .setPositiveButton(R.string.keep_combatant, (dialog, which) -> {
                                // The user wants to copy this Combatant as a new version
                                // Reset this Combatant's name
                                receivedFightable.setName(originalFightable.getName());

                                // Add the new copy
                                addFightableToList(receivedFightable);
                            })
                            .setNegativeButton(R.string.resurrect_combatant, (dialog, which) -> {
                                // The user wants to resurrect the old version of the Combatant
                                // Remove originalFightable (originalFightable != null if we are here)
                                removeFightable(originalFightable, false); // Don't notify the adapter just yet

                                // The user wants to resurrect the old version of the Combatant
                                finalCollisionFightable.setVisible(true); // Set the Combatant as visible

                                // Finally, add the Combatant into list as is
                                addFightableToList(finalCollisionFightable);
                            })
                            .show();
                }
                break;
            default:
                // Nothing fancy, just add that Fightable!
                addFightableToList( receivedFightable );
        }
    }

    private void addFightableToList( Fightable receivedFightable ) {
        // Note: This should only be called once we know the Fightable's name meets requirements!
        // Use receiveFightable to modify Fightable and master list entries to be compatible.
        // receivedFightable may be new, or a modified version of an existing Fightable
        boolean success = fightableList_Master.addOrModifyFightable( receivedFightable );

        // Let the Adapter know that we have added the new Fightable
        if ( success ) {
            clearMultiSelect(); // Clear the multi-select list
            notifyFightableListChanged();
        }
    }

    public void removeFightable(Fightable fightableToRemove) {
        removeFightable(fightableToRemove, true);
    }

    private void removeFightable(Fightable fightableToRemove, boolean doNotify) {
        // Tell the parent to remove a Combatant in the list, based on the current position in the displayed List
        if (parent.safeToDelete(fightableToRemove)) {
            // If the parent says we can fully delete this Combatant, then do so
            fightableList_Master.remove(fightableToRemove);
        } else {
            // Make the Combatant invisible (can be made visible again by adding a new Combatant with the same name)
            fightableList_Master.getFightableWithID(fightableToRemove.getId()).setVisible(false); // Make sure we use a fresh reference
        }

        if ( doNotify ) {
            // Let the Adapter know that we have modified the Combatant list
            clearMultiSelect(); // Clear the multi-select list
            notifyFightableListChanged();
        }
    }

    private void removeFightable(int position) {
        removeFightable(displayList().get(position));
    }

    public void copyFightable(int position) {
        // Copy the Fightable at the given position
        Fightable newFightable = displayList().get(position).cloneUnique(); // Create a clone fo the Combatant to copy, but with a unique ID

        // Add the new Fightable to the list
        Bundle fighterBundleData = new Bundle();
        fighterBundleData.putBoolean(ReceiveFightableOptions.IS_COPY, true); // Allow receive Fightable to handle this Fightable as a copy, rather than a brand new Fightable
        receiveFightable(newFightable, fighterBundleData);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (holder instanceof FightableViewHolder) {
            if (!payloads.isEmpty()) {
                // If the payload is not empty
                if (payloads.get(0) instanceof Bundle) {
                    // If the payload is a Bundle
                    Bundle args = (Bundle) payloads.get(0);
                    boolean newMultiSelectingMode = false;
                    for (String key : args.keySet()) {
                        switch (key) {
                            case DIFF_NAME:
                                ((FightableViewHolder) holder).setName(args.getString(key));
                                break;
                            case DIFF_FACTION:
                                Fightable.Faction fac = (Fightable.Faction) args.getSerializable(key);
                                if (fac != null) {
                                    ((FightableViewHolder) holder).setFaction(fac);
                                }
                                break;
                            case DIFF_SELECTED:
                                ((FightableViewHolder) holder).setSelected(args.getBoolean(key));
                                break;
                            case DIFF_MULTISELECT:
                                if (args.getSerializable(key) == FightableFilteredDiffUtil.MultiSelectVisibilityChange.START_MULTISELECT) {
                                    newMultiSelectingMode = true;
                                } else if ( args.getSerializable(key) == FightableFilteredDiffUtil.MultiSelectVisibilityChange.END_MULTISELECT) {
                                    newMultiSelectingMode = false;
                                }
                                ((FightableViewHolder) holder).setMultiSelectingMode(newMultiSelectingMode);
                                break;
                            case DIFF_ICON:
                                ((CombatantViewHolder) holder).setIcon(args.getInt(key));
                                break;
                            case DIFF_NUM_PARTY:
                                ((CombatantGroupViewHolder) holder).setNumParty(args.getInt(key));
                                break;
                            case DIFF_NUM_NEUTRAL:
                                ((CombatantGroupViewHolder) holder).setNumNeutral(args.getInt(key));
                                break;
                            case DIFF_NUM_ENEMY:
                                ((CombatantGroupViewHolder) holder).setNumEnemy(args.getInt(key));
                                break;
                            default:
                                // Do nothing
                        }
                    }
                }
                return; // The payload was not empty, so don't bother going on to the the non-payload onBindViewHolder
            }
        }

        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemViewType(int position) {
        int combatantInd = posToFightableInd(position); // Get the position in the Combatant list (negative numbers indicate a banner view)
        if (combatantInd >= 0) {
            // Can be either a Combatant or a Group
            if ( fightableList_Master.isFightableAGroup(combatantInd, getFilteredIndices()) ) {
                return GROUP_VIEW;
            } else {
                return COMBATANT_VIEW;
            }
        } else {
            return BANNER_VIEW;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if ( holder instanceof bindableVH ) {
            ((bindableVH) holder).bind(position);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == COMBATANT_VIEW) {
            view = inflater.inflate(R.layout.combatant_item_mod, parent, false);
            return new CombatantViewHolder(view);
        } else if (viewType == GROUP_VIEW) {
            view = inflater.inflate(R.layout.group_list_item, parent, false);
            return new CombatantGroupViewHolder(view);
        } else if (viewType == BANNER_VIEW) {
            view = inflater.inflate(R.layout.faction_banner, parent, false);
            return new FactionBannerViewHolder(view);
        } else {
            Log.e(TAG, "Got illegal viewType");
            view = inflater.inflate(R.layout.faction_banner, parent, false);
            return new FactionBannerViewHolder(view);
        }
    }

    public ArrayList<Fightable> getAllSelectedFightables() {
        // Return an ArrayList of all of the currently selected Combatants
        return fightableList_Master.getSelected();
    }


    @Override
    public int getItemCount() {
        return fightableList_Master.subListVisible(getFilteredIndices()).sizeWithBanners(); // Get the size of the post-filtering list, including the banners
    }

    private ArrayList<ArrayList<Integer>> getFilteredIndices() {
        if ( fightableList_Master.numFactionLists() != fightableFilteredIndices.size() ) {
            // Update the filtered indices, because something has gone wrong!
            fightableFilteredIndices = fightableList_Master.getIndicesThatMatch(filteredText);
        }

        return fightableFilteredIndices;
    }

    private void notifyFightableListChanged() {
        // Update the list of combatants to be displayed, taking into account the current filter string
        fightableFilteredIndices = fightableList_Master.getIndicesThatMatch(filteredText);

        // Make sure that the multi-select status is updated
        FightableFilteredDiffUtil.MultiSelectVisibilityChange visChange =
                FightableFilteredDiffUtil.MultiSelectVisibilityChange.NO_CHANGE;
        boolean oldIsMultiSelecting = isMultiSelecting;
        updateMultiSelectStatus();
        if ( oldIsMultiSelecting && !isMultiSelecting ) {
            visChange = FightableFilteredDiffUtil.MultiSelectVisibilityChange.END_MULTISELECT;
        } else if ( isMultiSelecting && !oldIsMultiSelecting ) {
            visChange = FightableFilteredDiffUtil.MultiSelectVisibilityChange.START_MULTISELECT;
        }

        // If anything about the combatants has changed (specifically the viewed version of the list according to fightableFilteredIndices), see if we need to rearrange the list
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FightableFilteredDiffUtil(fightableList_Memory, fightableList_Master.subListVisible(getFilteredIndices()), visChange));
        diffResult.dispatchUpdatesTo(this); // If anything has changed, move the list items around

        // Update the memory list
        fightableList_Memory = fightableList_Master.subListVisible(getFilteredIndices()).clone();

        // Let the parent know that the Combatant List changed (maybe)
        parent.notifyFightableListChanged();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                filteredText = charSequence.toString().toLowerCase();

                return new FilterResults();
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                notifyFightableListChanged();
            }
        };
    }

    public AllFactionFightableLists getCombatantList() {
        return fightableList_Master;
    }

    public void setCombatantList(AllFactionFightableLists combatantList_Master) {
        this.fightableList_Master = combatantList_Master;
        clearMultiSelect();
        notifyFightableListChanged();
    }

    public void clearCombatantList() {
        fightableList_Master.clear();
        notifyFightableListChanged();
    }

    // The interface calling this adapter MUST have control of a master list of combatants such that it can judge a Combatant's name to be mutually exclusive
    interface MasterAFFLKeeper extends Serializable {
        void receiveChosenFightable(Fightable selectedFightable); // Receive selected Fightable back from this Adapter

        Context getContext(); // Get Context from the calling Activity/Fragment

        void notifyFightableListChanged(); // Let the parent know that the Combatant list changed, so it can update any views (such as the "no combatants" view)

        void notifyIsMultiSelecting(boolean isMultiSelecting); // Let the parent know that the multi-selecting status may have changed, so we may need to update the GUI

        boolean safeToDelete(Fightable fightable); // Is a given Combatant safe to delete (does not appear in the EncounterCombatantList), or should it simply be turned invisible (appears in the list)
    }

    // A simple class that holds onto a bunch of input parameters for the ListFightableRecyclerAdapter.  Really only exists because having 3+ flags input to the constructor IN ADDITION to a bunch of other stuff just kinda makes me sad...
    static public class LFRAFlags implements Serializable {
        public boolean adapterCanModify = false;
        public boolean adapterCanMultiSelect = false;
        public boolean adapterCanCopy = false;
        public boolean adapterAllowsOrdinals = false;
    }
}
