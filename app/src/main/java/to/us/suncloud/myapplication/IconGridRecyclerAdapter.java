package to.us.suncloud.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class IconGridRecyclerAdapter extends RecyclerView.Adapter<IconGridRecyclerAdapter.iconViewHolder> {

    Context context;

    ArrayList<Integer> iconResourceIds;
    int selectedPortrait;

    public IconGridRecyclerAdapter() {
    }

    public IconGridRecyclerAdapter(int selectedPortrait) {
        this.selectedPortrait = selectedPortrait;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        context = recyclerView.getContext();
        iconResourceIds = populateResourceIdList(context); // Create a list of resources that will be used to load svg's into the grid
    }

    public class iconViewHolder extends RecyclerView.ViewHolder {
        private ConstraintLayout border;
        private ImageView icon;

        public iconViewHolder(@NonNull View itemView) {
            super(itemView);

            border = itemView.findViewById(R.id.icon_border);
            icon = itemView.findViewById(R.id.icon_image);
        }

        public void bind(int position) {
            // Set the border color
            if (position == selectedPortrait) {
                border.setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark));
            } else {
                border.setBackgroundColor(context.getResources().getColor(R.color.standardBackground));
            }

            // Load the image
            icon.setImageDrawable(context.getDrawable(iconResourceIds.get(position)));
        }
    }

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

    @Override
    public int getItemCount() {
        return iconResourceIds.size();
    }

    private ArrayList<Integer> populateResourceIdList(Context context) {
        ArrayList<Integer> resourceIdList = new ArrayList<>();
        int curNum = 0;
        while (true) {
            // Generate filenames for every icon that we will use in order, and check if it exists
            String resourceName = String.format(Locale.US, "icon_%02d", curNum); // Oh, the horror...
            int id = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

            if (id > 0) {
                // If the id is valid
                resourceIdList.add(id);
            } else {
                // If the id is invalid (equal to 0), then there are no more icons to load
                break;
            }
        }

        return resourceIdList;
    }
}
