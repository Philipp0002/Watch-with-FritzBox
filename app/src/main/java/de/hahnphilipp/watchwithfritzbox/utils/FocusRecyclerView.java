package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class FocusRecyclerView extends RecyclerView {

    public boolean customEpgVerticalFocusSearch = false;
    public boolean customEpgHorizontalFocusSearch = false;

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

    @Override
    public View focusSearch(View focused, int direction) {
        if(customEpgVerticalFocusSearch) {
            if(direction == FOCUS_UP || direction == FOCUS_DOWN) {
                ViewHolder containingViewHolder = findContainingViewHolder(focused);
                if(containingViewHolder == null) {
                    return super.focusSearch(focused, direction);
                }
                int newPosition = containingViewHolder.getAdapterPosition() + (direction == FOCUS_UP ? -1 : 1);
                if(newPosition < 0 || newPosition >= getAdapter().getItemCount()) {
                    return super.focusSearch(focused, direction);
                }
                ViewHolder newViewHolder = findViewHolderForAdapterPosition(newPosition);
                if(newViewHolder == null) {
                    return super.focusSearch(focused, direction);
                }
                return newViewHolder.itemView;
            }
        }

        if(customEpgHorizontalFocusSearch) {
            if (direction == FOCUS_LEFT || direction == FOCUS_RIGHT) {
                View v = super.focusSearch(focused, direction);
                if(v == null) {
                    smoothScrollBy(getWidth() / 2 * (direction == FOCUS_LEFT ? -1 : 1), 0);
                    return focused;
                }
                return v;
            }
        }

        return super.focusSearch(focused, direction);
    }
}
