package de.hahnphilipp.watchwith.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class FocusRecyclerView extends RecyclerView {

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
}
