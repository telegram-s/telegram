package org.telegram.android.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import org.telegram.android.R;
import org.telegram.android.core.background.MediaSender;
import org.telegram.android.core.background.SenderListener;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.log.Logger;
import org.telegram.android.media.*;
import org.telegram.android.preview.ImageHolder;
import org.telegram.android.preview.ImageReceiver;
import org.telegram.android.preview.MediaLoader;
import org.telegram.android.preview.PreviewConfig;
import org.telegram.android.ui.*;
import org.telegram.android.util.IOUtils;

import java.io.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Author: Korshakov Stepan
 * Created: 15.08.13 1:14
 */
public class MessageMediaView extends BaseDownloadView implements ImageReceiver {

    private static Movie lastMovie;
    private static long lastMovieStart;
    private static int lastDatabaseId;
    private static boolean moviePaused;
    private static Executor movieLoader = Executors.newSingleThreadExecutor();
    private static Bitmap movieDestBitmap;
    private static Canvas movieDestCanvas;

    private static final String TAG = "MessageMediaView";

    private MediaLoader loader;
    private DownloadManager downloadManager;

    private Drawable stateSent;
    private Drawable stateHalfCheck;
    private Drawable stateFailure;
    private Drawable videoIcon;
    private Drawable mapPoint;
    private Drawable unsupportedMark;
    private Drawable downloadIcon;
    private Drawable tryAgainIcon;
    private Drawable infoBg;
    private NinePatchDrawable outMask;
    private NinePatchDrawable inMask;
    private TextPaint videoDurationPaint;
    private TextPaint downloadPaint;
    private TextPaint timePaint;
    private Paint bitmapPaint;
    private Paint bitmapFilteredPaint;

    private Paint downloadBgRect;
    private Paint downloadBgLightRect;

    private Paint placeholderPaint;
    private Paint clockIconPaint;

    private Path path = new Path();
    private RectF rectF = new RectF();
    private Rect rect1 = new Rect();
    private Rect rect2 = new Rect();

    private int databaseId = -1;
    // private String key;
    private int state;
    private int prevState;
    private long stateChangeTime;

    private long previewAppearTime;

    private ImageHolder oldPreview;
    private ImageHolder preview;

    // private Bitmap previewCached;
    // private int fastPreviewHeight;
    // private int fastPreviewWidth;
    // private int previewHeight;
    // private int previewWidth;
    private boolean isOut;
    private boolean isVideo;
    // private boolean isDownloadable;
    // private boolean isUploadable;
    private boolean showMapPoint;
    private boolean isUnsupported;

    private String duration;
    private String date;

    private boolean isBindSizeCalled;
    private int desiredHeight;
    private int desiredWidth;
    private int desiredPaddingH;
    private int desiredPaddingV;

    private int timeWidth;
    private int timeHeight;

    private boolean isAnimatedProgress = true;

    public MessageMediaView(Context context) {
        super(context);
        init();
    }

    public MessageMediaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MessageMediaView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        super.init();
        loader = application.getUiKernel().getMediaLoader();
        downloadManager = application.getDownloadManager();
        videoIcon = getResources().getDrawable(R.drawable.st_bubble_media_play);
        mapPoint = getResources().getDrawable(R.drawable.st_map_pin);
        unsupportedMark = getResources().getDrawable(R.drawable.st_bubble_unknown);
        infoBg = getResources().getDrawable(R.drawable.st_bubble_media_info);

        if (FontController.USE_SUBPIXEL) {
            videoDurationPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            videoDurationPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        videoDurationPaint.setTextSize(getSp(15));
        videoDurationPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
        videoDurationPaint.setColor(0xE6FFFFFF);

        if (FontController.USE_SUBPIXEL) {
            downloadPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            downloadPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        downloadPaint.setTextSize(getSp(15));
        downloadPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
        downloadPaint.setColor(0xE6FFFFFF);

        if (FontController.USE_SUBPIXEL) {
            timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        timePaint.setTextSize(getSp(12));
        timePaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
        timePaint.setColor(0xffffffff);

        bitmapPaint = new Paint(/*Paint.ANTI_ALIAS_FLAG*/);
        //bitmapPaint.setAntiAlias(true);
        //bitmapPaint.setFilterBitmap(true);
        //bitmapPaint.setDither(true);

        bitmapFilteredPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        bitmapFilteredPaint.setAntiAlias(true);
        bitmapFilteredPaint.setFilterBitmap(true);
        bitmapFilteredPaint.setDither(true);

        downloadBgRect = new Paint();
        downloadBgRect.setColor(0xB6000000);
        downloadBgRect.setStyle(Paint.Style.FILL);
        downloadBgRect.setAntiAlias(true);

        downloadBgLightRect = new Paint();
        downloadBgLightRect.setColor(0x60ffffff);
        downloadBgLightRect.setStyle(Paint.Style.FILL);
        downloadBgLightRect.setAntiAlias(true);

        stateSent = getResources().getDrawable(R.drawable.st_bubble_ic_check_photo);
        stateHalfCheck = getResources().getDrawable(R.drawable.st_bubble_ic_halfcheck_photo);
        stateFailure = getResources().getDrawable(R.drawable.st_bubble_ic_warning);

        downloadIcon = getResources().getDrawable(R.drawable.st_bubble_ic_download);
        tryAgainIcon = getResources().getDrawable(R.drawable.st_bubble_media_retry);

        inMask = (NinePatchDrawable) getResources().getDrawable(R.drawable.st_bubble_in_media_content);
        outMask = (NinePatchDrawable) getResources().getDrawable(R.drawable.st_bubble_out_media_content);

        placeholderPaint = new Paint();
        placeholderPaint.setColor(Color.WHITE);

        clockIconPaint = new Paint();
        clockIconPaint.setStyle(Paint.Style.STROKE);
        clockIconPaint.setColor(Color.WHITE);
        clockIconPaint.setStrokeWidth(getPx(1));
        clockIconPaint.setAntiAlias(true);
        clockIconPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unbindOldPreview();
        unbindPreview();
        postInvalidate();
    }

    @Override
    public void unbind() {
        super.unbind();
        unbindOldPreview();
        unbindPreview();
    }

    private void bindSize(int w, int h) {
        int[] sizes = PreviewConfig.getSizes(w, h);
        desiredWidth = sizes[0];
        desiredHeight = sizes[1];
        desiredPaddingH = sizes[2];
        desiredPaddingV = sizes[3];
        isBindSizeCalled = true;
    }

    private void bindSizeManual(int w, int h) {
        desiredWidth = w;
        desiredHeight = h;
        desiredPaddingH = 0;
        desiredPaddingV = 0;
        isBindSizeCalled = true;
    }

    private void unbindPreview() {
        if (preview != null) {
            preview.release();
            preview = null;
        }
    }

    private void unbindOldPreview() {
        if (oldPreview != null) {
            oldPreview.release();
            oldPreview = null;
        }
    }

    private void bindMedia(MessageWireframe message) {
        clearBinding();
        this.isBindSizeCalled = false;
        this.isVideo = false;
        this.showMapPoint = false;
        this.isUnsupported = false;

        boolean isBinded = false;
        if (message.message.getExtras() instanceof TLLocalPhoto) {
            TLLocalPhoto mediaPhoto = (TLLocalPhoto) message.message.getExtras();
            bindSize(mediaPhoto.getFullW(), mediaPhoto.getFullH());

            if (!(mediaPhoto.getFullLocation() instanceof TLLocalFileEmpty)) {
                bindDownload(DownloadManager.getPhotoKey(mediaPhoto));

                String key = DownloadManager.getPhotoKey(mediaPhoto);
                if (downloadManager.getState(key) == DownloadState.COMPLETED) {
                    loader.requestFullLoading(mediaPhoto, downloadManager.getFileName(key), isOut, this);
                    isBinded = true;
                }
            }

            if (!isBinded && mediaPhoto.hasFastPreview()) {
                isBinded = true;
                loader.requestFastLoading(mediaPhoto, isOut, this);
            }
        } else if (message.message.getExtras() instanceof TLLocalVideo) {
            TLLocalVideo mediaVideo = (TLLocalVideo) message.message.getExtras();
            bindSize(mediaVideo.getPreviewW(), mediaVideo.getPreviewH());

            isVideo = true;
            duration = TextUtil.formatDuration(mediaVideo.getDuration());
            placeholderPaint.setColor(Color.BLACK);

            if (!(mediaVideo.getVideoLocation() instanceof TLLocalFileEmpty)) {
                bindDownload(DownloadManager.getVideoKey(mediaVideo));

                String key = DownloadManager.getVideoKey(mediaVideo);
                if (downloadManager.getState(key) == DownloadState.COMPLETED) {
                    loader.requestVideoLoading(downloadManager.getFileName(key), isOut, this);
                    isBinded = true;
                }
            }

            if (!isBinded) {
                if (mediaVideo.getPreviewW() != 0 && mediaVideo.getPreviewH() != 0) {
                    loader.requestFastLoading(mediaVideo, isOut, this);
                    isBinded = true;
                } else {
//                    if (downloadManager.getState(key) != DownloadState.IN_PROGRESS &&
//                            downloadManager.getState(key) != DownloadState.PENDING) {
//                        // TODO: Should we download preview?
//                    }
                }
            }
        } else if (message.message.getExtras() instanceof TLUploadingPhoto) {
            TLUploadingPhoto photo = (TLUploadingPhoto) message.message.getExtras();
            bindSize(photo.getWidth(), photo.getHeight());

            if (photo.getFileUri() != null && photo.getFileUri().length() > 0) {
                loader.requestRawUri(photo.getFileUri(), isOut, this);
                isBinded = true;
            } else if (photo.getFileName() != null && photo.getFileName().length() > 0) {
                loader.requestRaw(photo.getFileName(), isOut, this);
                isBinded = true;
            }

            bindUpload(message.databaseId);
        } else if (message.message.getExtras() instanceof TLUploadingVideo) {
            TLUploadingVideo video = (TLUploadingVideo) message.message.getExtras();
            bindSize(video.getPreviewWidth(), video.getPreviewHeight());

            loader.requestVideoLoading(video.getFileName(), isOut, this);
            isBinded = true;

            bindUpload(message.databaseId);
        } else if (message.message.getExtras() instanceof TLLocalGeo) {
            TLLocalGeo geo = (TLLocalGeo) message.message.getExtras();
            bindSizeManual(PreviewConfig.MAP_W, PreviewConfig.MAP_H);
            loader.requestGeo(geo, isOut, this);
            isBinded = true;
            showMapPoint = true;
        } else if (message.message.getExtras() instanceof TLUploadingDocument) {

            TLUploadingDocument doc = (TLUploadingDocument) message.message.getExtras();

            bindSize(doc.getFullPreviewW(), doc.getFullPreviewH());

            if (doc.getFilePath().length() > 0) {
                loader.requestRaw(doc.getFilePath(), isOut, this);
                isBinded = true;
            } else {
                loader.requestRawUri(doc.getFileUri(), isOut, this);
                isBinded = true;
            }

            bindUpload(message.databaseId);
        } else if (message.message.getExtras() instanceof TLLocalDocument) {
            TLLocalDocument document = (TLLocalDocument) message.message.getExtras();

            bindSize(document.getPreviewW(), document.getPreviewH());

            String key = DownloadManager.getDocumentKey(document);
            bindDownload(key);

            if (document.getPreviewW() != 0 && document.getPreviewH() != 0) {
                if (document.getMimeType().equals("image/gif") ||
                        document.getMimeType().equals("image/png") ||
                        document.getMimeType().equals("image/jpeg")) {
                    if (downloadManager.getState(key) == DownloadState.COMPLETED) {
                        loader.requestRaw(downloadManager.getFileName(key), isOut, this);
                        isBinded = true;
                    }
                }

                if (!isBinded && document.getFastPreview().length > 0) {
                    loader.requestFastLoading(document, isOut, this);
                    isBinded = true;
                }

                if (!isBinded && (!(document.getPreview() instanceof TLLocalFileEmpty))) {
                    // TODO: Bind preview
//                        StelsImageTask baseTask = new StelsImageTask((TLLocalFileLocation) document.getPreviewLocation());
//                        baseTask.enableBlur(3);
//                        previewTask = new ScaleTask(baseTask, scaledW, scaledH);
                }
            }
        } else {
            isUnsupported = true;
            bindSize(0, 0);
        }

        if (!isBinded) {
            loader.cancelRequest(this);
        }

        if (!isBindSizeCalled) {
            throw new RuntimeException("bindSize not called");
        }
    }

    @Override
    protected void bindNewView(MessageWireframe message) {
        long start = SystemClock.uptimeMillis();

        this.databaseId = message.message.getDatabaseId();
        this.isOut = message.message.isOut();
        this.date = TextUtil.formatTime(message.message.getDate(), getContext());
        if (isOut) {
            placeholderPaint.setColor(0xffe6ffd1);
        } else {
            placeholderPaint.setColor(Color.WHITE);
        }

        unbindOldPreview();
        unbindPreview();

        this.state = message.message.getState();
        this.prevState = -1;

        bindMedia(message);

        this.downloadStateTime = 0;

        Logger.d(TAG, "Bind in " + (SystemClock.uptimeMillis() - start) + " ms");
        requestLayout();
    }

    @Override
    protected void bindUpdate(MessageWireframe message) {
        if (this.state != message.message.getState()) {
            this.prevState = this.state;
            this.state = message.message.getState();
            this.stateChangeTime = SystemClock.uptimeMillis();
        }

        bindMedia(message);

        invalidate();
    }

    public void toggleMovie() {
        if (lastDatabaseId != databaseId) {
            lastDatabaseId = databaseId;
            lastMovieStart = 0;
            lastMovie = null;
            moviePaused = false;
            movieLoader.execute(new Runnable() {
                @Override
                public void run() {
                    if (lastDatabaseId != databaseId) {
                        return;
                    }

                    byte[] data;
                    try {
                        data = IOUtils.readAll(application.getDownloadManager().getFileName(getDownloadKey()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    final Movie fmovie = Movie.decodeByteArray(data, 0, data.length);

                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (lastDatabaseId != databaseId) {
                                return;
                            }

                            moviePaused = false;
                            lastMovie = fmovie;
                            lastMovieStart = System.currentTimeMillis();
                            invalidate();
                        }
                    });
                }
            });
        } else {
            if (moviePaused) {
                moviePaused = false;
                lastMovieStart = System.currentTimeMillis();
            } else {
                moviePaused = true;
            }
        }
        invalidate();
    }

    @Override
    protected void measureBubbleContent(int width) {
        int centerX = desiredWidth / 2;
        int centerY = desiredHeight / 2;
        int outerR = getPx(20);

        path.reset();
        path.addRect(0, 0, desiredWidth, desiredHeight, Path.Direction.CW);
        path.close();
        path.addCircle(centerX, centerY, outerR, Path.Direction.CW);
        path.close();
        path.setFillType(Path.FillType.EVEN_ODD);

        timePaint.getTextBounds(date, 0, date.length(), rect1);

        timeWidth = (int) timePaint.measureText(date);
        timeHeight = -rect1.top;
        if (isOut) {
            timeWidth += getPx(18);
        }

        setBubbleMeasuredContent(desiredWidth + desiredPaddingH * 2, desiredHeight + desiredPaddingV * 2);
    }

    @Override
    protected int getInBubbleResource() {
        return R.drawable.st_bubble_in_media_normal;
    }

    @Override
    protected int getOutBubbleResource() {
        return R.drawable.st_bubble_out_media_normal;
    }

    @Override
    protected int getOutPressedBubbleResource() {
        return R.drawable.st_bubble_out_media_overlay;
    }

    @Override
    protected int getInPressedBubbleResource() {
        return R.drawable.st_bubble_in_media_overlay;
    }

    private Drawable getStateDrawable(int state) {
        switch (state) {
            default:
            case MessageState.SENT:
                return stateSent;
            case MessageState.READED:
                return stateHalfCheck;
            case MessageState.FAILURE:
                return stateFailure;
        }
    }

    private boolean drawState(Canvas canvas, int stateId, float stateAlpha, boolean isNewState) {
        int centerX = desiredWidth / 2;
        int centerY = desiredHeight / 2;
        int internalR = getPx(18);
        int outerR = getPx(22);
        boolean isAnimated = false;
        if (isNewState && (stateId == STATE_IN_PROGRESS ||
                stateId == STATE_ERROR ||
                stateId == STATE_PENDING)) {
            // downloadBgRect.setAlpha((int) (0xA5 * stateAlpha));
            downloadBgRect.setAlpha(0xA5);
            canvas.drawCircle(centerX, centerY, outerR, downloadBgRect);
        }

        if (stateId == STATE_IN_PROGRESS) {
            downloadBgLightRect.setAlpha((int) (0xA5 * stateAlpha));
            rectF.set(centerX - internalR, centerY - internalR, centerX + internalR, centerY + internalR);
            int progressAngleStart = -90;
            int progressAngle = (int) ((downloadAnimatedProgress * 360 / 100));
            canvas.drawArc(rectF, progressAngleStart, progressAngle, true, downloadBgLightRect);
        } else if (stateId == STATE_ERROR) {
            tryAgainIcon.setBounds(
                    centerX - tryAgainIcon.getIntrinsicWidth() / 2, centerY - tryAgainIcon.getIntrinsicHeight() / 2,
                    centerX + tryAgainIcon.getIntrinsicWidth() / 2, centerY + tryAgainIcon.getIntrinsicHeight() / 2);
            tryAgainIcon.setAlpha((int) (255 * stateAlpha));
            tryAgainIcon.draw(canvas);
        } else if (stateId == STATE_PENDING) {
            downloadIcon.setBounds(
                    centerX - downloadIcon.getIntrinsicWidth() / 2, centerY - downloadIcon.getIntrinsicHeight() / 2,
                    centerX + downloadIcon.getIntrinsicWidth() / 2, centerY + downloadIcon.getIntrinsicHeight() / 2);
            downloadIcon.setAlpha((int) (255 * stateAlpha));
            downloadIcon.draw(canvas);
        }

        return isAnimated;
    }

    @Override
    protected boolean drawBubble(Canvas canvas) {
        boolean isAnimated = false;
        boolean isAnimationShown = false;


        // Start Main content
        canvas.save();
        canvas.translate(desiredPaddingH, desiredPaddingV);

        long animationTime = SystemClock.uptimeMillis() - previewAppearTime;
        Movie movie = lastMovie;
        if (movie != null && lastDatabaseId == databaseId) {
            // if (movie.duration() != 0) {
            if (!moviePaused) {
                int duration = movie.duration();
                if (duration == 0) {
                    duration = 1000;
                }
                movie.setTime((int) ((System.currentTimeMillis() - lastMovieStart) % duration));
                isAnimated = true;
            }
            //}
            // canvas.drawRect(0, 0, desiredWidth, desiredHeight, placeholderPaint);

            if (movieDestBitmap == null) {
                movieDestBitmap = Bitmap.createBitmap(PreviewConfig.MAX_PREVIEW_W, PreviewConfig.MAX_PREVIEW_H, Bitmap.Config.ARGB_8888);
                movieDestCanvas = new Canvas(movieDestBitmap);
            }

            if (movie.width() != 0 && movie.height() != 0) {
                Optimizer.clearBitmap(movieDestBitmap);

                movieDestCanvas.save();
                float scaleX = (float) desiredWidth / movie.width();
                float scaleY = (float) desiredHeight / movie.height();
                movieDestCanvas.translate((desiredWidth - scaleX * movie.width()) / 2, (desiredHeight - scaleY * movie.height()) / 2);
                movieDestCanvas.scale(scaleX, scaleY);
                movie.draw(movieDestCanvas, 0, 0);
                movieDestCanvas.restore();

                NinePatchDrawable mask = isOut ? outMask : inMask;
                mask.getPaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                mask.setBounds(0, 0, desiredWidth, desiredHeight);
                mask.draw(movieDestCanvas);
            } else {
                movie.draw(movieDestCanvas, 0, 0);
            }

            rect1.set(0, 0, desiredWidth, desiredHeight);

            canvas.drawBitmap(movieDestBitmap, rect1, rect1, bitmapPaint);

            isAnimationShown = true;
        } else if (preview != null) {
            if (animationTime > FADE_ANIMATION_TIME || !isAnimatedProgress) {
                bitmapFilteredPaint.setAlpha(255);
                rect1.set(0, 0, preview.getW(), preview.getH());
                rect2.set(0, 0, desiredWidth, desiredHeight);
                canvas.drawBitmap(preview.getBitmap(), rect1, rect2, bitmapFilteredPaint);
                if (oldPreview != null) {
                    unbindOldPreview();
                }
            } else {
                float alpha = fadeEasing((float) animationTime / FADE_ANIMATION_TIME);

                if (oldPreview != null) {
                    bitmapFilteredPaint.setAlpha(255);
                    rect1.set(0, 0, oldPreview.getW(), oldPreview.getH());
                    rect2.set(0, 0, desiredWidth, desiredHeight);
                    canvas.drawBitmap(oldPreview.getBitmap(), rect1, rect2, bitmapFilteredPaint);
                }

                bitmapFilteredPaint.setAlpha((int) (255 * alpha));
                rect1.set(0, 0, preview.getW(), preview.getH());
                rect2.set(0, 0, desiredWidth, desiredHeight);
                canvas.drawBitmap(preview.getBitmap(), rect1, rect2, bitmapFilteredPaint);

                isAnimated = true;
            }
        } else if (oldPreview != null) {
            bitmapPaint.setAlpha(255);
            rect1.set(0, 0, oldPreview.getW(), oldPreview.getH());
            rect2.set(0, 0, desiredWidth, desiredHeight);
            canvas.drawBitmap(oldPreview.getBitmap(), rect1, rect2, bitmapPaint);
        }

        if (showMapPoint) {
            mapPoint.setBounds((desiredWidth - mapPoint.getIntrinsicWidth()) / 2,
                    desiredHeight / 2 - mapPoint.getIntrinsicHeight(),
                    (desiredWidth + mapPoint.getIntrinsicWidth()) / 2,
                    desiredHeight / 2);
            mapPoint.draw(canvas);
        }

        if (isUnsupported) {
            unsupportedMark.setBounds((desiredWidth - unsupportedMark.getIntrinsicWidth()) / 2,
                    (desiredHeight - unsupportedMark.getIntrinsicHeight()) / 2,
                    (desiredWidth + unsupportedMark.getIntrinsicWidth()) / 2,
                    (desiredHeight + unsupportedMark.getIntrinsicHeight()) / 2);
            unsupportedMark.draw(canvas);
        }

        if (isVideo) {
            if ((getState() == STATE_NONE || getState() == STATE_DOWNLOADED)) {
                int centerX = desiredWidth / 2;
                int centerY = desiredHeight / 2;
                videoIcon.setBounds(
                        centerX - videoIcon.getIntrinsicWidth() / 2, centerY - videoIcon.getIntrinsicHeight() / 2,
                        centerX + videoIcon.getIntrinsicWidth() / 2, centerY + videoIcon.getIntrinsicHeight() / 2);
                videoIcon.draw(canvas);
            }
            canvas.drawText(duration, getPx(8), getPx(18), videoDurationPaint);
        }

        if (mode != MODE_NONE) {
            calculateAnimations();
            if (isInStateSwitch) {
                drawState(canvas, getState(), newStateAlpha, true);
                drawState(canvas, getPrevState(), oldStateAlpha, false);
            } else {
                drawState(canvas, getState(), 1, true);
            }
//            Paint src = new Paint();
//            src.setColor(Color.GREEN);
//            src.setAlpha((int) (255 * newStateAlpha));
//            canvas.drawRect(new Rect(10, 10, 100, 100), src);
//
//            src.setColor(Color.RED);
//            src.setAlpha((int) (255 * oldStateAlpha));
//            canvas.drawRect(new Rect(100, 100, 210, 210), src);
            isAnimated = true;
        }

        canvas.restore();

        if (!isAnimationShown) {
            int bottom = desiredHeight + desiredPaddingV * 2;
            int right = desiredWidth + desiredPaddingH * 2;

            infoBg.getPadding(rect1);

            int timeBottom = bottom;
            int timeRight = right;
            int timeStart = timeRight - rect1.right - timeWidth;
            int timeIcon = timeRight - rect1.right - getPx(10);
            int timeIconTop = timeBottom - rect1.bottom - getPx(10);
            int timeBaseline = timeBottom - rect1.bottom - (getPx(10) - timeHeight) / 2;
            int timeLeft = timeStart - rect1.left;
            int timeTop = timeBaseline - rect1.top - getPx(10);

            infoBg.setBounds(timeLeft, timeTop, timeRight, timeBottom);
            infoBg.draw(canvas);
            canvas.drawText(date, timeStart, timeBaseline, timePaint);

            if (isOut) {
                if (state == MessageState.PENDING) {
                    canvas.save();
                    canvas.translate(timeIcon, timeIconTop);
                    canvas.drawCircle(getPx(5), getPx(5), getPx(5), clockIconPaint);
                    double time = (System.currentTimeMillis() / 15.0) % (12 * 60);
                    double angle = (time / (6 * 60)) * Math.PI;

                    int x = (int) (Math.sin(-angle) * getPx(3));
                    int y = (int) (Math.cos(-angle) * getPx(3));
                    canvas.drawLine(getPx(5), getPx(5), getPx(5) + x, getPx(5) + y, clockIconPaint);

                    x = (int) (Math.sin(-angle * 12) * getPx(4));
                    y = (int) (Math.cos(-angle * 12) * getPx(4));
                    canvas.drawLine(getPx(5), getPx(5), getPx(5) + x, getPx(5) + y, clockIconPaint);
                    canvas.restore();
                    isAnimated = true;
                } else if (state == MessageState.READED && prevState == MessageState.SENT && (SystemClock.uptimeMillis() - stateChangeTime < FADE_ANIMATION_TIME)) {
                    long stateAnimationTime = SystemClock.uptimeMillis() - stateChangeTime;
                    float progress = easeStateFade(stateAnimationTime / (float) STATE_ANIMATION_TIME);
                    float scale = 1 + progress * 0.2f;
                    int alphaNew = (int) (progress * 255);

                    bounds(stateSent, timeIcon - getPx(4), timeIconTop);
                    stateSent.setAlpha(255);
                    stateSent.draw(canvas);

                    bounds(stateHalfCheck, timeIcon, timeIconTop, scale);
                    stateHalfCheck.setAlpha(alphaNew);
                    stateHalfCheck.draw(canvas);

                    isAnimated = true;
                } else {
                    Drawable drawable = getStateDrawable(state);

                    if (state == MessageState.READED) {
                        bounds(stateSent, timeIcon - getPx(4), timeIconTop);
                        stateSent.setAlpha(255);
                        stateSent.draw(canvas);

                        bounds(stateHalfCheck, timeIcon, timeIconTop);
                        stateHalfCheck.setAlpha(255);
                        stateHalfCheck.draw(canvas);
                    } else {
                        bounds(drawable, timeIcon - getPx(4), timeIconTop);
                        drawable.setAlpha(255);
                        drawable.draw(canvas);
                    }
                }
            }
        }

        return isAnimated;
    }


    @Override
    public void onImageReceived(ImageHolder holder, boolean intermediate) {
        if (this.preview != holder && this.preview != null) {
            unbindOldPreview();
            this.oldPreview = this.preview;
            this.preview = null;
        }
        unbindPreview();
        this.preview = holder;
        if (!intermediate) {
            this.previewAppearTime = SystemClock.uptimeMillis();
        } else {
            this.previewAppearTime = 0;
        }
        invalidate();
    }
}