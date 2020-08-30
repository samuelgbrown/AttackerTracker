package to.us.suncloud.myapplication;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// A quick a simple decoration class to make sure that the Faction banners all rest flush (or nearly flush) with the Combatants below them
class BannerDecoration extends RecyclerView.ItemDecoration {
    private static final int bannerOverlapDP = -8;
    private int bannerOverlapPx;

    BannerDecoration(Context context) {
        bannerOverlapPx = (int) context.getResources().getDisplayMetrics().density*bannerOverlapDP;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) >= 0) {
            // If this view is in the RecyclerView (don't know why it WOULDN'T be, but I had to put this check in after a bug, soooooo...
            if (parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)) == ListCombatantRecyclerAdapter.BANNER_VIEW) {
                outRect.set(0, 0, 0, bannerOverlapPx);
            }
        }
    }
}
