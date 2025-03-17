package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class FocusRecyclerView extends RecyclerView {

    /**
     * The horizontal distance (in pixels) scrolled is > 0
     */
    public static final int SCROLL_DIRECTION_LEFT = 0;

    /**
     * The horizontal distance scrolled (in pixels) is < 0
     */
    public static final int SCROLL_DIRECTION_RIGHT = 1;

    private int mScrollDirection;

    public FocusRecyclerView(@NonNull Context context) {
        super(context);
    }

    public FocusRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        return false;
    }

    /*@Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        setScrollDirection(dy);
    }

    private void setScrollDirection(int dy) {
        mScrollDirection = dy >= 0 ? SCROLL_DIRECTION_LEFT : SCROLL_DIRECTION_RIGHT;
    }

    private float getPercentageFromCenter(View child) {
        float centerY = (getMeasuredHeight() / 2);
        float childCenterY = child.getY() + (child.getHeight() / 2);
        float offSet = Math.max(centerY, childCenterY) - Math.min(centerY, childCenterY);
        int maxOffset = (getMeasuredHeight() / 2) + child.getHeight();
        return (offSet / maxOffset);
    }

    private int findCenterViewIndex() {
        int count = getChildCount();
        int index = -1;
        int closest = Integer.MAX_VALUE;
        int centerY = (getMeasuredHeight() / 2);

        for (int i = 0; i < count; ++i) {
            View child = getLayoutManager().getChildAt(i);
            int childCenterY = (int) (child.getY() + (child.getHeight() / 2));
            int distance = Math.abs(centerY - childCenterY);
            if (distance < closest) {
                closest = distance;
                index = i;
            }
        }

        if (index == -1) {
            throw new IllegalStateException("Can\'t find central view.");
        } else {
            return index;
        }
    }

    private void smoothScrollToCenter() {
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();
        int lastVisibleView = linearLayoutManager.findLastVisibleItemPosition();
        int firstVisibleView = linearLayoutManager.findFirstVisibleItemPosition();
        View firstView = linearLayoutManager.findViewByPosition(firstVisibleView);
        View lastView = linearLayoutManager.findViewByPosition(lastVisibleView);
        int screenHeight = this.getHeight();

        //since views have variable sizes, we need to calculate side margins separately.
        int leftMargin = (screenHeight - lastView.getHeight()) / 2;
        int rightMargin = (screenHeight - firstView.getHeight()) / 2 + firstView.getHeight();
        int leftEdge = lastView.getTop();
        int rightEdge = firstView.getBottom();
        int scrollDistanceLeft = leftEdge - leftMargin;
        int scrollDistanceRight = rightMargin - rightEdge;

        if (mScrollDirection == SCROLL_DIRECTION_LEFT) {
            smoothScrollBy(0, scrollDistanceLeft);
        } else if (mScrollDirection == SCROLL_DIRECTION_RIGHT) {
            smoothScrollBy(0, -scrollDistanceRight);
        }
    }*/
}
