package to.us.suncloud.myapplication;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

// Recycler adapter to be used in the GroupFragment.  This fragment is for adding Combatants to a group (or creating a new group from these combatants).
public class AddToGroupRecyclerViewAdapter extends RecyclerView.Adapter<AddToGroupRecyclerViewAdapter.GroupViewHolder> {

    private static final String TAG = "AddToGroupAdapter";

    private final AllFactionFightableLists adapterAFFL;
    private final int numGroups; // The number of Groups in adapterAFFL (also equals the index that represents the "Add Group..." option)
    private final GroupListRVA_Return parentFrag;

    public AddToGroupRecyclerViewAdapter(AllFactionFightableLists items, GroupListRVA_Return parentFrag) {
        // Note: adapterAFFL will have some number of selected Combatants (or Groups), which are to be added to a group in this dialog
        adapterAFFL = items;
        numGroups = adapterAFFL.getFactionList(Fightable.Faction.Group).size();
        this.parentFrag = parentFrag;
    }

    private CombatantGroup getGroup(int groupIndex) {
        CombatantGroup thisGroup;
        Fightable thisFightable = adapterAFFL.getFactionList(Fightable.Faction.Group).get(groupIndex);
        if ( thisFightable instanceof CombatantGroup ) {
            thisGroup = (CombatantGroup) thisFightable;
        } else {
            Log.e(TAG, "Got non-Group Fightable from Add To Group Adapter!");
            thisGroup = new CombatantGroup(adapterAFFL);
        }
        return thisGroup;
    }

    @Override
    public GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new GroupViewHolder(layoutInflater.inflate(R.layout.group_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final GroupViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        // Return one extra, for the "Add Group..." item
        return (numGroups + 1);
    }

    public class GroupViewHolder extends RecyclerView.ViewHolder {
        public final View mBackground;
        public final TextView mGroupName;
        public final TextView mPartyNum;
        public final TextView mNeutralNum;
        public final TextView mEnemyNum;

        private int groupIndex;

        public GroupViewHolder(final View itemView) {
            super(itemView);
            mBackground = itemView;
            mGroupName = itemView.findViewById(R.id.group_name);
            mPartyNum = itemView.findViewById(R.id.party_count);
            mNeutralNum = itemView.findViewById(R.id.neutral_count);
            mEnemyNum = itemView.findViewById(R.id.enemy_count);
            ConstraintLayout mMultiSelectPane = itemView.findViewById(R.id.group_multi_select_pane);

            View.OnClickListener groupClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CombatantGroup thisGroup;

                    if (isAddGroup()) {
                        // If this is the "Add Group..." item, create a new CombatantGroup
                        thisGroup = new CombatantGroup(adapterAFFL);
                        String groupName = Fightable.generateUniqueName(adapterAFFL, false, parentFrag.getContext().getString(R.string.new_group_name));
                        thisGroup.setName(groupName);
                    } else {
                        // If this represents an existing group, use the existing group (overwrite the new group made above)
                        thisGroup = (CombatantGroup) getGroup(groupIndex).clone();
                    }

                    // Add the selected Combatants to the chosen Group
                    boolean gotMultiples = thisGroup.addSelected(adapterAFFL);
                    if ( gotMultiples ) {
                        Toast.makeText(parentFrag.getContext(),
                            parentFrag.getContext().getString(R.string.multiples_of_combatant_for_group),
                            Toast.LENGTH_LONG)
                            .show();
                    }

                    // Send the index of this group back to the Fragment (changes to adapterAFFL will be reflected in fragment's groupFragmentAFFL, as well)
                    parentFrag.choseGroupWithMetadata(thisGroup);
                }
            };

            mBackground.setOnClickListener(groupClickListener);
            mGroupName.setOnClickListener(groupClickListener);
            mPartyNum.setOnClickListener(groupClickListener);
            mNeutralNum.setOnClickListener(groupClickListener);
            mEnemyNum.setOnClickListener(groupClickListener);

            mMultiSelectPane.setVisibility(View.GONE);
        }

        public void bind(int groupIndexIn) {
            // This value is used in the groupClickListener
            groupIndex = groupIndexIn;
            int partyCount = 0;
            int neutralCount = 0;
            int enemyCount = 0;

            if (isAddGroup()) {
                // If this is the "Add Group..." item...
                mGroupName.setText(R.string.add_new_group);
            } else {
                // If this represents an existing group...
                CombatantGroup thisGroup = getGroup(groupIndex);

                mGroupName.setText(thisGroup.getName());
                partyCount = thisGroup.getTotalCombatantsInFaction(Fightable.Faction.Party);
                neutralCount = thisGroup.getTotalCombatantsInFaction(Fightable.Faction.Neutral);
                enemyCount = thisGroup.getTotalCombatantsInFaction(Fightable.Faction.Enemy);
            }

            // Adjust visibility of the faction count boxes
            if (partyCount > 0 ) {
                mPartyNum.setVisibility(View.VISIBLE);
            } else {
                mPartyNum.setVisibility(View.INVISIBLE);
            }
            if (neutralCount > 0 ) {
                mNeutralNum.setVisibility(View.VISIBLE);
            } else {
                mNeutralNum.setVisibility(View.INVISIBLE);
            }
            if (enemyCount > 0 ) {
                mEnemyNum.setVisibility(View.VISIBLE);
            } else {
                mEnemyNum.setVisibility(View.INVISIBLE);
            }
        }

        private boolean isAddGroup() {
            return ( groupIndex == numGroups);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mGroupName.getText() + "'";
        }
    }

    public interface GroupListRVA_Return {
        void choseGroupWithMetadata(CombatantGroup thisGroup);
        Context getContext( ); // Auxiliary function to get Context from parent
    }
}