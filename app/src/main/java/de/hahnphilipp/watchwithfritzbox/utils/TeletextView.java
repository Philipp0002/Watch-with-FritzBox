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

import java.util.Arrays;

public class TeletextView extends View {

    private MediaPlayer.Teletext teletext;
    private Integer[] colorPalette;
    private Paint paint = new Paint();
    private Typeface typeface;

    public TeletextView(Context context) {
        super(context);

        initView();
    }

    public TeletextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        initView();
    }

    private void initView() {
        typeface = Typeface.createFromAsset(getContext().getAssets(), "bedstead.otf");
    }

    public void setTeletext(MediaPlayer.Teletext teletext) {
        this.teletext = teletext;
        if (teletext != null) {
            colorPalette = Arrays.stream(teletext.getPalette()).map(s -> Color.parseColor("#" + s)).toArray(Integer[]::new);
        }
        invalidate();
    }

    int cellWidth,cellHeight,textSize = 0;

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        paint.setTypeface(typeface);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        int canvasWidth = getWidth();
        int canvasHeight = getHeight();

        if(canvasWidth < canvasHeight) {
            canvasHeight = canvasWidth;
        } else {
            canvasWidth = canvasHeight;
        }

        canvas.drawRect(0, 0, canvasWidth, canvasHeight, paint);

        if (teletext == null) {
            return;
        }

        if(cellWidth == 0) {
            cellWidth = canvasWidth / teletext.getSizeColumns();
            cellHeight = canvasHeight / teletext.getSizeRows();
            textSize = determineMaxTextSize(cellHeight);
        }

        for (int i1 = 0; i1 < teletext.getSizeRows(); i1++) {
            int y = i1 * cellHeight;
            for (int i2 = 0; i2 < teletext.getSizeColumns(); i2++) {
                int x = i2 * cellWidth;
                MediaPlayer.TeletextCell cell = teletext.getCells()[i1][i2];

                int convertedUnicode = convertToUnicode(cell.getRawUnicode());
                String character = ((char) convertedUnicode)+"";
                int foregroundColor = Color.WHITE;
                int backgroundColor = Color.BLACK;
                try {
                    foregroundColor = colorPalette[cell.getForegroundPaletteIndex()];
                }catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    backgroundColor = colorPalette[cell.getBackgroundPaletteIndex()];
                } catch (Exception e) {
                    e.printStackTrace();
                }

                paint.setColor(backgroundColor);
                canvas.drawRect(x, y, x + cellWidth, y + cellHeight, paint);

                paint.setColor(foregroundColor);
                paint.setTextSize(textSize);

                Paint.FontMetrics fm = paint.getFontMetrics();
                canvas.drawText(character, x, y - fm.ascent, paint);
            }

        }
    }

    private int determineMaxTextSize(float maxHeight)
    {
        int size = 0;
        float fontHeight = 0;
        do {
            paint.setTextSize(++size);
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            fontHeight = Math.abs(fontMetrics.ascent) + fontMetrics.descent;
        } while(fontHeight < maxHeight);

        return size;
    }

    public int convertToUnicode(int rawUnicode) {
        // G1 Graphics (Block Mosaic) 0xEE00-0xEEFF
        // Diese werden zu 0x20-0x5F gemappt (invertiert mit XOR 0x20)
        if (rawUnicode >= 0xEE00 && rawUnicode < 0xEF00) {
            // XOR 0x20 macht aus 0xEE00 -> 0x0020, 0xEE20 -> 0x0000, etc.
            // dann subtrahieren wir 0xEE00 und addieren zum Basis-Offset
            int offset = (rawUnicode ^ 0x20) - 0xEE00;
            return 0xEE20 + offset; // Unicode Braille Patterns als Basis für Blöcke
        }

        // G3 Graphics (Smooth Mosaic) 0xEF00-0xEFFF
        if (rawUnicode >= 0xEF00 && rawUnicode < 0xF000) {
            int offset = rawUnicode - 0xEF20;
            Log.d("tttxxx", "smooth");
            return 0xEE20 + 128 + offset; // Nach G1 Graphics
        }

        // Arabic Teletext Extension 0xE600-0xE73F
        if (rawUnicode >= 0xE600 && rawUnicode < 0xE740) {
            return rawUnicode - 0xE600 + 0x0600; // Normales Arabic Unicode
        }

        // Normale Unicode-Zeichen bleiben unverändert
        return rawUnicode;
    }


}
