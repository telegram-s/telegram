package org.telegram.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLLocalFileEmpty;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;
import org.telegram.android.media.*;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.DepthPageTransformer;
import org.telegram.android.ui.TextUtil;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 07.08.13 18:05
 */
public class ImagePreviewFragment extends TelegramFragment {
    private int mid;
    private int peerType;
    private int peerId;
    private MediaRecord mainRecord;
    private MediaRecord[] records;
    private ViewPager imagesPager;
    private TextView senderNameView;
    private TextView recordDateView;
    private PagerAdapter pagerAdapter;
    private View bottomPanel;

    public ImagePreviewFragment(int mid, int peerType, int peerId) {
        this.mid = mid;
        this.peerType = peerType;
        this.peerId = peerId;
    }

    public ImagePreviewFragment() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mid = savedInstanceState.getInt("mid");
            peerType = savedInstanceState.getInt("peerType");
            peerId = savedInstanceState.getInt("peerId");
            mainRecord = (MediaRecord) savedInstanceState.getSerializable("mainRecord");
            Object[] srcRecords = (Object[]) savedInstanceState.getSerializable("records");
            if (srcRecords != null) {
                records = new MediaRecord[srcRecords.length];
                for (int i = 0; i < srcRecords.length; i++) {
                    records[i] = (MediaRecord) srcRecords[i];
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mid = savedInstanceState.getInt("mid");
            peerType = savedInstanceState.getInt("peerType");
            peerId = savedInstanceState.getInt("peerId");
            mainRecord = (MediaRecord) savedInstanceState.getSerializable("mainRecord");
            Object[] srcRecords = (Object[]) savedInstanceState.getSerializable("records");
            if (srcRecords != null) {
                records = new MediaRecord[srcRecords.length];
                for (int i = 0; i < srcRecords.length; i++) {
                    records[i] = (MediaRecord) srcRecords[i];
                }
            }
        }

        if (mainRecord == null) {
            mainRecord = application.getEngine().findMedia(mid);
            if (mainRecord == null) {
                Toast.makeText(getActivity(), R.string.st_image_incorrect, Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mid = savedInstanceState.getInt("mid");
            peerType = savedInstanceState.getInt("peerType");
            peerId = savedInstanceState.getInt("peerId");
            mainRecord = (MediaRecord) savedInstanceState.getSerializable("mainRecord");
            Object[] srcRecords = (Object[]) savedInstanceState.getSerializable("records");
            if (srcRecords != null) {
                records = new MediaRecord[srcRecords.length];
                for (int i = 0; i < srcRecords.length; i++) {
                    records[i] = (MediaRecord) srcRecords[i];
                }
            }
        }

        setDefaultProgressInterface(null);

        View res = inflater.inflate(R.layout.media_full_main, container, false);
        bottomPanel = res.findViewById(R.id.bottomPanel);
        if (getSherlockActivity().getSupportActionBar().isShowing()) {
            bottomPanel.setVisibility(View.VISIBLE);
        } else {
            bottomPanel.setVisibility(View.GONE);
        }

        imagesPager = (ViewPager) res.findViewById(R.id.imagesPager);
        if (Build.VERSION.SDK_INT >= 11) {
            imagesPager.setPageTransformer(true, new DepthPageTransformer());
        }

        senderNameView = (TextView) res.findViewById(R.id.senderName);
        recordDateView = (TextView) res.findViewById(R.id.date);

        if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            res.findViewById(R.id.shareButton).setVisibility(View.INVISIBLE);
        } else {
            res.findViewById(R.id.shareButton).setOnClickListener(secure(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    share();
                }
            }));
            res.findViewById(R.id.shareButton).setVisibility(View.VISIBLE);
        }

        res.findViewById(R.id.deleteButton).setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
            }
        }));

        if (mainRecord == null) {
            mainRecord = application.getEngine().findMedia(mid);
            if (mainRecord == null) {
                Toast.makeText(getActivity(), R.string.st_image_incorrect, Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }

        if (mainRecord != null) {
            bindUi();
            if (records == null) {
                runUiTask(new AsyncAction() {
                    MediaRecord[] nRecords;

                    @Override
                    public void execute() throws AsyncException {
                        nRecords = application.getEngine().getMediaEngine().queryMedia(peerType, peerId);
                    }

                    @Override
                    public void afterExecute() {
                        super.afterExecute();
                        records = nRecords;
                        bindUi();
                    }
                });
            }
        }

        return res;
    }

    private void bindUi() {
        int currentIndex = 0;
        if (records != null) {
            for (int i = 0; i < records.length; i++) {
                if (records[i].getMid() == mid) {
                    currentIndex = i;
                    break;
                }
            }
        }
        pagerAdapter = new PagerAdapter() {
            @Override
            public int getCount() {
                if (records == null) {
                    return 1;
                } else {
                    return records.length;
                }
            }

            public int getItemPosition(Object object) {
                return POSITION_NONE;
            }

            private View createPhotoView(final MediaRecord record) {
                TLLocalPhoto photo = (TLLocalPhoto) record.getPreview();
                final View res = View.inflate(getActivity(), R.layout.media_full_image, null);
                final ProgressBar progressBar = (ProgressBar) res.findViewById(R.id.loading);
                final ImageButton downloadButton = (ImageButton) res.findViewById(R.id.download);
                final ImageView fastImageView = (ImageView) res.findViewById(R.id.fastPreview);
                final PhotoView fullView = (PhotoView) res.findViewById(R.id.fullImage);
                fullView.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                    @Override
                    public void onPhotoTap(View view, float x, float y) {
                        onImageTap();
                    }
                });

                progressBar.setVisibility(View.GONE);
                downloadButton.setVisibility(View.GONE);

                if (!(photo.getFullLocation() instanceof TLLocalFileEmpty)) {
                    final String downloadKey = DownloadManager.getPhotoKey(photo);
                    boolean hasFastPreview = false;
                    DownloadState state = application.getDownloadManager().getState(downloadKey);
                    if (state == DownloadState.FAILURE || state == DownloadState.NONE) {
                        application.getDownloadManager().requestDownload(photo);
                    }
                    if (photo.getFastPreviewH() != 0 && photo.getFastPreviewW() != 0) {
                        try {
                            Bitmap fast = Optimizer.load(photo.getFastPreview());
                            fast = Optimizer.crop(fast, photo.getFastPreviewW(), photo.getFastPreviewH());
                            Optimizer.blur(fast);
                            fastImageView.setImageBitmap(fast);
                            hasFastPreview = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (hasFastPreview) {
                        fastImageView.setVisibility(View.VISIBLE);
                    } else {
                        fastImageView.setVisibility(View.GONE);
                    }

                    downloadButton.setOnClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            application.getDownloadManager().requestDownload((TLLocalPhoto) record.getPreview());
                        }
                    }));

                    DownloadListener listener = new DownloadListener() {
                        @Override
                        public void onStateChanged(String _key, DownloadState state, int percent) {
                            if (res.getTag() != this) {
                                application.getDownloadManager().unregisterListener(this);
                                return;
                            }
                            if (!_key.equals(downloadKey)) {
                                return;
                            }

                            if (state == DownloadState.COMPLETED) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            final Bitmap bitmap = Optimizer.load(application.getDownloadManager().getFileName(downloadKey));
                                            fullView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    fullView.setImageBitmap(bitmap);
                                                    showView(fullView);
                                                    goneView(fastImageView);
                                                }
                                            });
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }.start();
                                downloadButton.setVisibility(View.GONE);
                                progressBar.setVisibility(View.GONE);
                            } else if (state == DownloadState.IN_PROGRESS | state == DownloadState.PENDING) {
                                downloadButton.setVisibility(View.GONE);
                                progressBar.setVisibility(View.VISIBLE);
                                progressBar.setProgress(percent);
                            } else {
                                downloadButton.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    };

                    res.setTag(listener);
                    application.getDownloadManager().registerListener(listener);
                    listener.onStateChanged(downloadKey, application.getDownloadManager().getState(downloadKey),
                            application.getDownloadManager().getDownloadProgress(downloadKey));

                } else {
                    res.setTag(null);
                    fastImageView.setVisibility(View.GONE);
                    fullView.setVisibility(View.GONE);
                }

                return res;
            }


            @Override
            public Object instantiateItem(ViewGroup collection, int position) {
                final MediaRecord record = records == null ? mainRecord : records[position];
                if (record.getPreview() instanceof TLLocalPhoto) {
                    View res = createPhotoView(record);
                    collection.addView(res);
                    return res;
                }

                final View res;
                if (record.getPreview() instanceof TLLocalVideo) {
                    res = View.inflate(getActivity(), R.layout.media_full_video, null);
                } else {
                    res = View.inflate(getActivity(), R.layout.media_full_image, null);
                }

                final ProgressBar progressBar = (ProgressBar) res.findViewById(R.id.loading);
                final ImageButton downloadButton = (ImageButton) res.findViewById(R.id.download);

                if (record.getPreview() instanceof TLLocalVideo) {
                    final ImageButton playButton = (ImageButton) res.findViewById(R.id.play);
                    final ImageView imageView = (ImageView) res.findViewById(R.id.preview);
                    // TODO: Implement
//                    FastWebImageView imageView = (FastWebImageView) res.findViewById(R.id.previewImage);
//                    imageView.setOnClickListener(secure(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View view) {
//                            onImageTap();
//                        }
//                    }));
//                    imageView.setScaleTypeImage(FastWebImageView.SCALE_TYPE_FIT);

                    final TLLocalVideo video = (TLLocalVideo) record.getPreview();
                    final String key = DownloadManager.getVideoKey(video);

                    if (video.getPreviewW() != 0 && video.getPreviewH() != 0 && video.getFastPreview().length > 0) {
                        try {
                            Bitmap fast = Optimizer.load(video.getFastPreview());
                            fast = Optimizer.crop(fast, video.getPreviewW(), video.getPreviewH());
                            Optimizer.blur(fast);
                            imageView.setImageBitmap(fast);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

//                    if (video.getPreviewH() != 0 && video.getPreviewW() != 0) {
//                        if (video.getFastPreview().length > 0) {
//                            imageView.requestTask(new CachedImageTask(video));
//                        } else if (video.getPreviewLocation() instanceof TLLocalFileLocation) {
//                            TLLocalFileLocation location = (TLLocalFileLocation) video.getPreviewLocation();
//                            imageView.requestTask(new StelsImageTask(new TLFileLocation(location.getDcId(), location.getVolumeId(), location.getLocalId(), location.getSecret())));
//                        } else {
//                            imageView.requestTask(null);
//                        }
//                    } else {
//                        imageView.requestTask(null);
//                    }

                    playButton.setOnClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getFileName(key))), "video/*");
                            startPickerActivity(intent);
                        }
                    }));
                    downloadButton.setOnClickListener(secure(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            application.getDownloadManager().requestDownload((TLLocalVideo) record.getPreview());
                        }
                    }));

                    DownloadListener listener = new DownloadListener() {
                        @Override
                        public void onStateChanged(String _key, DownloadState state, int percent) {
                            if (res.getTag() != this) {
                                application.getDownloadManager().unregisterListener(this);
                                return;
                            }
                            if (!_key.equals(key)) {
                                return;
                            }

                            if (state == DownloadState.COMPLETED) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            final VideoOptimizer.VideoMetadata metadata = VideoOptimizer.getVideoSize(application.getDownloadManager().getFileName(key));
                                            imageView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    imageView.setImageBitmap(metadata.getImg());
                                                }
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }.start();
                                if (playButton != null) {
                                    playButton.setVisibility(View.VISIBLE);
                                }
                                downloadButton.setVisibility(View.GONE);
                                progressBar.setVisibility(View.GONE);
                            } else if (state == DownloadState.IN_PROGRESS | state == DownloadState.PENDING) {
                                if (playButton != null) {
                                    playButton.setVisibility(View.GONE);
                                }
                                downloadButton.setVisibility(View.GONE);
                                progressBar.setVisibility(View.VISIBLE);
                                progressBar.setProgress(percent);
                            } else {
                                if (playButton != null) {
                                    playButton.setVisibility(View.GONE);
                                }
                                downloadButton.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    };

                    res.setTag(listener);
                    application.getDownloadManager().registerListener(listener);
                    listener.onStateChanged(key, application.getDownloadManager().getState(key),
                            application.getDownloadManager().getDownloadProgress(key));
                } else if (record.getPreview() instanceof TLLocalPhoto) {
                    TLLocalPhoto photo = (TLLocalPhoto) record.getPreview();
                    if (!(photo.getFullLocation() instanceof TLLocalFileEmpty)) {
                        final String key = DownloadManager.getPhotoKey(photo);

                        // if (application.isHighSpeedNetwork()) {
                        DownloadState state = application.getDownloadManager().getState(key);
                        if (state == DownloadState.FAILURE || state == DownloadState.NONE) {
                            application.getDownloadManager().requestDownload(photo);
                        }
                        //}
                        // TODO: Implement
//                        final PhotoImageView imageView = (PhotoImageView) res.findViewById(R.id.previewImage);
//                        imageView.getPhotoViewAttacher().setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
//                            @Override
//                            public void onViewTap(View view, float x, float y) {
//                                onImageTap();
//                            }
//                        });
                        ImageView fastImageView = (ImageView) res.findViewById(R.id.fastPreview);

                        if (photo.getFastPreviewH() != 0 && photo.getFastPreviewW() != 0) {
                            try {
                                Bitmap fast = Optimizer.load(photo.getFastPreview());
                                fast = Optimizer.crop(fast, photo.getFastPreviewW(), photo.getFastPreviewH());
                                Optimizer.blur(fast);
                                fastImageView.setImageBitmap(fast);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        downloadButton.setOnClickListener(secure(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                application.getDownloadManager().requestDownload((TLLocalPhoto) record.getPreview());
                            }
                        }));

                        DownloadListener listener = new DownloadListener() {
                            @Override
                            public void onStateChanged(String _key, DownloadState state, int percent) {
                                if (res.getTag() != this) {
                                    application.getDownloadManager().unregisterListener(this);
                                    return;
                                }
                                if (!_key.equals(key)) {
                                    return;
                                }

                                if (state == DownloadState.COMPLETED) {
                                    // TODO: Implement
                                    // imageView.setImageTask(new FileSystemImageTask(application.getDownloadManager().getFileName(key)));
                                    downloadButton.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.GONE);
                                } else if (state == DownloadState.IN_PROGRESS | state == DownloadState.PENDING) {
                                    downloadButton.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.VISIBLE);
                                    progressBar.setProgress(percent);
                                } else {
                                    downloadButton.setVisibility(View.VISIBLE);
                                    progressBar.setVisibility(View.GONE);
                                }
                            }
                        };

                        res.setTag(listener);
                        application.getDownloadManager().registerListener(listener);
                        listener.onStateChanged(key, application.getDownloadManager().getState(key),
                                application.getDownloadManager().getDownloadProgress(key));
                    }
                }

                collection.addView(res);
                return res;
            }


            @Override
            public void destroyItem(ViewGroup collection, int position, Object view) {
                ((View) view).setTag(null);
                // TODO: Implement
                //View previewImage = ((View) view).findViewById(R.id.previewImage);
//                if (previewImage != null && previewImage instanceof PhotoImageView) {
//                    ((PhotoImageView) previewImage).getPhotoViewAttacher().cleanup();
//                }
                collection.removeView((View) view);
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        };
        imagesPager.setAdapter(pagerAdapter);
        imagesPager.setOffscreenPageLimit(1);
        imagesPager.setCurrentItem(currentIndex);


        imagesPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                updateUi();
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        updateUi();
    }

    private void onImageTap() {
        if (getSherlockActivity() != null && getSherlockActivity().getSupportActionBar() != null) {
            if (getSherlockActivity().getSupportActionBar().isShowing()) {
                getSherlockActivity().getSupportActionBar().hide();
                hideView(bottomPanel);
            } else {
                getSherlockActivity().getSupportActionBar().show();
                showView(bottomPanel);
            }
        }
    }

    private void share() {
        MediaRecord record;
        if (records != null) {
            record = records[imagesPager.getCurrentItem()];
        } else {
            record = mainRecord;
        }
        getActivity().setResult(Activity.RESULT_OK, new Intent().putExtra("forward_mid", record.getMid()));
        getActivity().finish();
    }

    private void delete() {
        MediaRecord record;
        if (records != null) {
            record = records[imagesPager.getCurrentItem()];
        } else {
            record = mainRecord;
        }
        application.getEngine().deleteSentMessage(application.getEngine().getMessagesEngine().getMessageByMid(record.getMid()).getDatabaseId());
        application.getSyncKernel().getBackgroundSync().resetDeletionsSync();
        application.notifyUIUpdate();

        if (records == null || records.length == 1) {
            Toast.makeText(getActivity(), R.string.st_image_deleted, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        } else {
            ArrayList<MediaRecord> nrecords = new ArrayList<MediaRecord>();
            for (MediaRecord r : records) {
                if (r.getMid() != record.getMid()) {
                    nrecords.add(r);
                }
            }
            records = nrecords.toArray(new MediaRecord[0]);
            final int index = imagesPager.getCurrentItem();
            imagesPager.setAdapter(pagerAdapter);
            /*if (pagerAdapter != null) {
                pagerAdapter.notifyDataSetChanged();
            }*/
            if (index < pagerAdapter.getCount()) {
                imagesPager.post(new Runnable() {
                    @Override
                    public void run() {
                        imagesPager.setCurrentItem(index);
                    }
                });
            } else {
                imagesPager.post(new Runnable() {
                    @Override
                    public void run() {
                        imagesPager.setCurrentItem(pagerAdapter.getCount() - 1, false);
                    }
                });
            }
            bindUi();
            Toast.makeText(getActivity(), R.string.st_image_deleted, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUi() {
        MediaRecord record;
        if (records != null) {
            getSherlockActivity().getSupportActionBar().setTitle((imagesPager.getCurrentItem() + 1) + " of " + records.length);
            record = records[imagesPager.getCurrentItem()];
        } else {
            getSherlockActivity().getSupportActionBar().setTitle(R.string.st_image_title);
            record = mainRecord;
        }

        User sender = getEngine().getUser(record.getSenderId());
        senderNameView.setText(sender.getDisplayName());
        recordDateView.setText(TextUtil.formatDate(record.getDate(), getActivity()));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (peerType != PeerType.PEER_USER_ENCRYPTED) {
            inflater.inflate(R.menu.image_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.saveToGallery) {
            MediaRecord record;
            if (records != null) {
                record = records[imagesPager.getCurrentItem()];
            } else {
                record = mainRecord;
            }
            String key = null;
            String fileName = null;
            String fileNameDest = null;
            if (record.getPreview() instanceof TLLocalVideo) {
                key = DownloadManager.getVideoKey((TLLocalVideo) record.getPreview());
                fileName = application.getDownloadManager().getFileName(key);
                fileNameDest = key + ".mp4";
            } else if (record.getPreview() instanceof TLLocalPhoto) {
                key = DownloadManager.getPhotoKey((TLLocalPhoto) record.getPreview());
                fileName = application.getDownloadManager().getFileName(key);
                fileNameDest = key + ".jpg";
            }

            if (key != null && application.getDownloadManager().getState(key) == DownloadState.COMPLETED) {
                try {
                    application.getDownloadManager().writeToGallery(fileName, fileNameDest);
                    Toast.makeText(getActivity(), "Saved to gallery", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Unable save to gallery", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "Media not downloaded", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mid", mid);
        outState.putInt("peerType", peerType);
        outState.putInt("peerId", peerId);
        outState.putSerializable("mainRecord", mainRecord);
        outState.putSerializable("records", records);
    }
}