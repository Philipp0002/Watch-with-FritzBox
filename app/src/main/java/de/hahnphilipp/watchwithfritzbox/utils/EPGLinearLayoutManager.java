package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EPGLinearLayoutManager extends LinearLayoutManager {

    public EPGLinearLayoutManager(Context context) {
        super(context);
    }

    public EPGLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public EPGLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent,
                                                 @NonNull View child, @NonNull Rect rect, boolean immediate,
                                                 boolean focusedChildVisible) {
        Rect recyclerViewBounds = new Rect();
        parent.getHitRect(recyclerViewBounds); // Get recyclerview bounds
        parent.getLocalVisibleRect(recyclerViewBounds); // Shrink the bounds to the area visible on screen
        recyclerViewBounds.top = Integer.MIN_VALUE; // Do not check for top bound
        recyclerViewBounds.bottom = Integer.MAX_VALUE; // Do not check for bottom bound

        Rect childRect = new Rect();
        child.getHitRect(childRect);
        boolean isPartiallyVisible = childRect.intersect(recyclerViewBounds);
        if(childRect.width() < 20) {
            isPartiallyVisible = false;
        }

        if(isPartiallyVisible) {
            return true;
        }

        return super.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible);
    }
}
