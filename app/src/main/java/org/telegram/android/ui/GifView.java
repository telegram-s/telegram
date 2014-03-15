package org.telegram.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import org.telegram.android.util.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by ex3ndr on 15.03.14.
 */
public class GifView extends View {

    private Movie movie;
    private long lastMovieStart;

    public GifView(Context context) {
        super(context);
    }

    public GifView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GifView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void loadGif(final String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    byte[] data = IOUtils.readAll(new FileInputStream(fileName));
                    movie = Movie.decodeByteArray(data, 0, data.length);
                    lastMovieStart = System.currentTimeMillis();
                    postInvalidate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (movie != null) {
            int duration = movie.duration();
            if (duration == 0) {
                duration = 1000;
            }
            canvas.save();
            if (movie.width() != 0 && movie.height() != 0) {
                float scale = Math.min(getWidth() / (float) movie.width(), getHeight() / (float) movie.height());
                int offsetX = (int) ((getWidth() - movie.width() * scale) / 2);
                int offsetY = (int) ((getHeight() - movie.height() * scale) / 2);
                canvas.translate(offsetX, offsetY);
                canvas.scale(scale, scale);
            }
            movie.setTime((int) ((System.currentTimeMillis() - lastMovieStart) % duration));
            movie.draw(canvas, 0, 0);
            canvas.restore();
        }

        invalidate();
    }
}
