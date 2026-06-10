package wadotu.orthodoxy.orthowear.presentation;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacingPx;

    public SpacingItemDecoration(int spacingPx) {
        this.spacingPx = spacingPx;
    }

    @Override
    public void getItemOffsets(
            @NonNull Rect outRect,
            @NonNull View view,
            @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state
    ) {
        outRect.top = spacingPx / 2;
        outRect.bottom = spacingPx / 2;
    }
}