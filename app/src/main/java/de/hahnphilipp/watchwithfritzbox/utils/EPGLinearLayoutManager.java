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

    /**
     * Requests that the given child of the RecyclerView be positioned onto the screen. This
     * method can be called for both unfocusable and focusable child views. For unfocusable
     * child views, focusedChildVisible is typically true in which case, layout manager
     * makes the child view visible only if the currently focused child stays in-bounds of RV.
     * @param parent The parent RecyclerView.
     * @param child The direct child making the request.
     * @param rect The rectangle in the child's coordinates the child
     *              wishes to be on the screen.
     * @param immediate True to forbid animated or delayed scrolling,
     *                  false otherwise
     * @param focusedChildVisible Whether the currently focused view must stay visible.
     * @return Whether the group scrolled to handle the operation
     */
    @Override
    public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent,
                                                 @NonNull View child, @NonNull Rect rect, boolean immediate,
                                                 boolean focusedChildVisible) {
        Rect recyclerViewBounds = new Rect();
        parent.getHitRect(recyclerViewBounds); // Holt die sichtbaren RecyclerView-Bounds

        boolean isPartiallyVisible = child.getLocalVisibleRect(recyclerViewBounds);
        if(isPartiallyVisible) {
            return false;
        }

        return super.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible);
    }
}
