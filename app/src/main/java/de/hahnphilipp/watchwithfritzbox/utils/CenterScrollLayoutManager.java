package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class CenterScrollLayoutManager extends LinearLayoutManager {

    public CenterScrollLayoutManager(Context context) {
        super(context);
    }

    public CenterScrollLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public CenterScrollLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                int height = recyclerView.getHeight();
                int offset = height / 2;
                return (boxStart + offset) - viewStart;
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }

            @Override
            protected float calculateSpeedPerPixel(android.util.DisplayMetrics displayMetrics) {
                // Langsameres Scrolling (höherer Wert = langsamer)
                // Standard ist 25f, versuche Werte zwischen 50f-100f für smootheres Scrolling
                return 100f / displayMetrics.densityDpi;
            }
        };
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    @Override
    public void scrollToPosition(int position) {
        int height = getHeight();
        int offset = height/2;

        super.scrollToPositionWithOffset(position, offset);
    }
}
