package to.us.suncloud.myapplication;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import to.us.suncloud.myapplication.placeholder.PlaceholderContent.PlaceholderItem;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 */

// Recycler adapter to be used in the GroupFragment.  This fragment is for adding Combatants to a group (or creating a new group from these combatants).
public class AddToGroupRecyclerViewAdapter extends RecyclerView.Adapter<AddToGroupRecyclerViewAdapter.GroupViewHolder> {

    private final AllFactionFightableLists adapterAFFL;
    private final int numGroups; // The number of Groups in adapterAFFL (also equals the index that represents the "Add Group..." option)

    public AddToGroupRecyclerViewAdapter(AllFactionFightableLists items) {
        adapterAFFL = items;
        numGroups = adapterAFFL.getFactionList(Fightable.Faction.Group).size();
    }

    private Fightable getGroup(int groupIndex) {
        return adapterAFFL.getFactionList(Fightable.Faction.Group).get(groupIndex);
    }

    @Override
    public GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new GroupViewHolder(layoutInflater.inflate(R.layout.group_list_layout, parent, false));
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

            View.OnClickListener groupClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CombatantGroup initialGroup;
                    if (isAddGroup()) {
                        // If this is the "Add Group..." item...
                        // TODO: Open a ViewOrModGroup Fragment with no selected Group
                        // TODO (perhaps below, combining both options in this if-statement?) add SELECTED Combatants to the Fragment as new members
                    } else {
                        // If this represents an existing group...
                        getGroup(groupIndex); // TODO: Check that this is an instance of a GROUP (not just a Fightable)

                        // TODO: Open a ViewOrModGroup Fragment with this selected group
                        // TODO (perhaps below, combining both options in this if-statement?) add SELECTED Combatants to the Fragment as new members
                        //  If there are selected Groups, they must be rendered to Combatants!  Should there be a user-dialog check for adding Groups to Groups?
                        //  Remember - Need to do a check for multiples (and let user know if there are any) - should that be here, or in new Fragment?  Perhaps new Fragment?
                    }

                    // TODO: After adjusting/adding group to adapterAFFL, new Fragment receives adapterAFFL, as well as groupIndex
                }
            };
        }

        public void bind(int groupIndexIn) {
            // This value is used in the groupClickListener
            groupIndex = groupIndexIn;
            int partyCountVisible = View.GONE;
            int neutralCountVisible = View.GONE;
            int enemyCountVisible = View.GONE;

            if (isAddGroup()) {
                // If this is the "Add Group..." item...
            } else {
                // If this represents an existing group...
                getGroup(groupIndex); // TODO: Check that this is an instance of a GROUP (not just a Fightable)

                // TODO: Start populating the text boxes - adjust visibility of party/neutral/enemy counts!
            }

            mPartyNum.setVisibility(partyCountVisible);
            mNeutralNum.setVisibility(neutralCountVisible);
            mEnemyNum.setVisibility(enemyCountVisible);
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
        void groupIndexSelected(int groupIndex);
    }
}