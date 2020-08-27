package to.us.suncloud.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IconGridRecyclerAdapter extends RecyclerView.Adapter<IconGridRecyclerAdapter.iconViewHolder> {

    Context context;
    IconGridRecyclerAdapter.SendIconSelectionInterface parent;

    ArrayList<Integer> iconResourceIds;
    int selectedPortrait = -1;

    public IconGridRecyclerAdapter() {
    }

    public IconGridRecyclerAdapter(IconGridRecyclerAdapter.SendIconSelectionInterface parent, int selectedPortrait) {
        this.parent = parent;
        this.selectedPortrait = selectedPortrait;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        context = recyclerView.getContext();

        // TODO LATER: This code is used in a few places...move to its own class, or something?
        // Create a list of resources that will be used to load svg's into the grid
        iconResourceIds = new ArrayList<>();
        int curNum = 0;
        while (true) {
            // Generate filenames for every icon that we will use in order, and check if it exists
            String resourceName = String.format(Locale.US, "icon_%02d", curNum); // Oh, the horror...
            int id = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

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

    public class iconViewHolder extends RecyclerView.ViewHolder {
        private ConstraintLayout border;
        private ImageView icon;

        private int position;

        public iconViewHolder(@NonNull View itemView) {
            super(itemView);

            border = itemView.findViewById(R.id.icon_border);
            icon = itemView.findViewById(R.id.icon_image);

            icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Tell the adapter to update this and the previous (if any) iconViewHolder to alter the selection
//                    updateSelectedPortraits(position);

                    // Let the fragment holding this adapter that we have the icon we want
                    parent.setIconIndex(position);
                }
            });
        }

        public void bind(int position) {
            this.position = position;

            // Set this portrait as selected, if needed
            updateIsSelected();

            // Load the image
            if (position == 0) {
                // If the position is equal to zero, then we want to show a "blank" icon (the default)
                icon.setImageResource(android.R.color.transparent);
            } else {
                icon.setImageDrawable(context.getDrawable(iconResourceIds.get(position - 1)));
            }
        }

        public void updateIsSelected() {
            // Set the border color
            if (position == selectedPortrait) {
                border.setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark));
            } else {
                border.setBackgroundColor(context.getResources().getColor(R.color.standardBackground));
            }

        }
    }

//    private void updateSelectedPortraits(int newPosition) {
//        // If there was a previously selected portrait, update it
//        if (selectedPortrait != -1) {
//            notifyItemChanged(selectedPortrait, new Object()); // The inclusion of an empty Object as payload indicates that the selection has been changed (see onBinViewHolder(iconViewHolder, int, List<Object>))
//        }
//
//        // Update the portrait that was just selected
//        notifyItemChanged(newPosition, new Object()); // The inclusion of an empty Object as payload indicates that the selection has been changed
//
//        // Update the selectedPortrait variable to change which icon is selected
//        selectedPortrait = newPosition;
//    }

    @NonNull
    @Override
    public iconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View iconView = LayoutInflater.from(parent.getContext()).inflate(R.layout.icon_layout, parent, false); // Inflate a new view from XML
        return new iconViewHolder(iconView);
    }

    @Override
    public void onBindViewHolder(@NonNull iconViewHolder holder, int position) {
        holder.bind(position);
    }

//    @Override
//    public void onBindViewHolder(@NonNull iconViewHolder holder, int position, @NonNull List<Object> payloads) {
//        if (!payloads.isEmpty()) {
//            // If the payload is not empty, it means that the only reason this method was called was because the selection is being updated
//            holder.updateIsSelected();
//        } else {
//            super.onBindViewHolder(holder, position, payloads);
//        }
//    }

    @Override
    public int getItemCount() {
        return iconResourceIds.size() + 1; // There should always be a blank icon that appears first
    }
    // An interface for sending the icon selected by this adapter to the fragment
    interface SendIconSelectionInterface extends Serializable {
        void setIconIndex(int iconIndex);
    }
}
