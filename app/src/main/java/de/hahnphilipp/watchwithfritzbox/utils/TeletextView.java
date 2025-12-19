package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeletextView extends View {

    private MediaPlayer.Teletext teletext;
    private Integer[] colorPalette;
    private Paint paint = new Paint();
    private Typeface typeface;

    public TeletextView(Context context) {
        super(context);
    }

    public TeletextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TeletextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TeletextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    public void setTeletext(MediaPlayer.Teletext teletext) {
        typeface = Typeface.createFromAsset(getContext().getAssets(), "MODE7GX0.TTF");
        this.teletext = teletext;
        if (teletext != null) {
            colorPalette = Arrays.stream(teletext.getPalette()).map(s -> Color.parseColor("#" + s)).toArray(Integer[]::new);
        }
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        if (teletext == null) {
            return;
        }

        int y = 0;
        for (MediaPlayer.TeletextCell[] row : teletext.getCells()) {
            int x = 0;
            int latestHeight = 0;
            for (MediaPlayer.TeletextCell cell : row) {
                //String character = String.valueOf((char) cell.getRawUnicode().intValue());
                String character = cell.getCharacter();
                int foregroundColor = Color.WHITE;
                int backgroundColor = Color.BLACK;
                try {
                    foregroundColor = colorPalette[cell.getForegroundPaletteIndex()];
                    backgroundColor = colorPalette[cell.getBackgroundPaletteIndex()];
                }catch (Exception e) {
                    e.printStackTrace();
                }
                // TODO BACKGROUND COLOR

                paint.setColor(foregroundColor);
                paint.setTypeface(typeface);

                Rect bounds = new Rect();
                paint.getTextBounds(character, 0, character.length(), bounds);

                canvas.drawText(character, x, y + bounds.height(), paint);

                x += bounds.width();
                Log.d("RENDERTXT", latestHeight + "");
                if(!character.trim().isEmpty())
                    latestHeight = bounds.height();
            }
            y += latestHeight;
        }


    }
}
