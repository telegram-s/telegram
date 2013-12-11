package org.telegram.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.extradea.framework.images.tasks.FileSystemImageTask;
import com.extradea.framework.images.ui.FastWebImageView;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLLocalFileEmpty;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;
import org.telegram.android.media.*;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.PhotoImageView;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.TLFileLocation;
import uk.co.senab.photoview.PhotoViewAttacher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 07.08.13 18:05
 */
public class ImagePreviewFragment extends StelsFragment {
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

        View res = inflater.inflate(R.layout.media_images, container, false);
        bottomPanel = res.findViewById(R.id.bottomPanel);
        if (getSherlockActivity().getSupportActionBar().isShowing()) {
            bottomPanel.setVisibility(View.VISIBLE);
        } else {
            bottomPanel.setVisibility(View.GONE);
        }

        imagesPager = (ViewPager) res.findViewById(R.id.imagesPager);

        senderNameView = (TextView) res.findViewById(R.id.senderName);
        recordDateView = (TextView) res.findViewById(R.id.date);

        if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            res.findViewById(R.id.shareButton).setVisibility(View.INVISIBLE);
        } else {
            res.findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    share();
                }
            });
            res.findViewById(R.id.shareButton).setVisibility(View.VISIBLE);
        }

        res.findViewById(R.id.deleteButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
            }
        });

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
                        PreparedQuery<MediaRecord> query = null;
                        try {
                            QueryBuilder<MediaRecord, Long> builder = getEngine().getMediasDao().queryBuilder();
                            builder.where().eq("peerType", peerType).and().eq("peerId", peerId);
                            builder.orderBy("date", true);
                            query = builder.prepare();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        nRecords = application.getEngine().getMediasDao().query(query).toArray(new MediaRecord[0]);
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

            @Override
            public Object instantiateItem(ViewGroup collection, int position) {
                final MediaRecord record = records == null ? mainRecord : records[position];
                final View res;
                if (record.getPreview() instanceof TLLocalVideo) {
                    res = View.inflate(getActivity(), R.layout.media_video, null);
                } else {
                    res = View.inflate(getActivity(), R.layout.media_image, null);
                }

                final ProgressBar progressBar = (ProgressBar) res.findViewById(R.id.loading);
                final ImageButton downloadButton = (ImageButton) res.findViewById(R.id.download);

                if (record.getPreview() instanceof TLLocalVideo) {
                    final ImageButton playButton = (ImageButton) res.findViewById(R.id.play);
                    FastWebImageView imageView = (FastWebImageView) res.findViewById(R.id.previewImage);
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            onImageTap();
                        }
                    });
                    imageView.setScaleTypeImage(FastWebImageView.SCALE_TYPE_FIT);

                    final TLLocalVideo video = (TLLocalVideo) record.getPreview();
                    final String key = DownloadManager.getVideoKey(video);
                    TextView duration = (TextView) res.findViewById(R.id.duration);
                    duration.setVisibility(View.VISIBLE);
                    duration.setText(TextUtil.formatDuration(video.getDuration()));

                    if (video.getPreviewH() != 0 && video.getPreviewW() != 0) {
                        if (video.getFastPreview().length > 0) {
                            imageView.requestTask(new CachedImageTask(video));
                        } else if (video.getPreviewLocation() instanceof TLLocalFileLocation) {
                            TLLocalFileLocation location = (TLLocalFileLocation) video.getPreviewLocation();
                            imageView.requestTask(new StelsImageTask(new TLFileLocation(location.getDcId(), location.getVolumeId(), location.getLocalId(), location.getSecret())));
                        } else {
                            imageView.requestTask(null);
                        }
                    } else {
                        imageView.requestTask(null);
                    }

                    playButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getVideoFileName(key))), "video/*");
                            startActivity(intent);
                        }
                    });
                    downloadButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            application.getDownloadManager().requestDownload((TLLocalVideo) record.getPreview());
                        }
                    });

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
                        final PhotoImageView imageView = (PhotoImageView) res.findViewById(R.id.previewImage);
                        imageView.getPhotoViewAttacher().setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                            @Override
                            public void onViewTap(View view, float x, float y) {
                                onImageTap();
                            }
                        });
                        if (photo.getFastPreviewH() != 0 && photo.getFastPreviewW() != 0) {
                            imageView.setImageBitmap(BitmapFactory.decodeByteArray(photo.getFastPreview(), 0, photo.getFastPreview().length));
                        }

                        downloadButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                application.getDownloadManager().requestDownload((TLLocalPhoto) record.getPreview());
                            }
                        });

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
                                    imageView.setImageTask(new FileSystemImageTask(application.getDownloadManager().getPhotoFileName(key)));
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
                View previewImage = ((View) view).findViewById(R.id.previewImage);
                if (previewImage != null && previewImage instanceof PhotoImageView) {
                    ((PhotoImageView) previewImage).getPhotoViewAttacher().cleanup();
                }
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
        application.getEngine().deleteSentMessage(application.getEngine().getMessageById(record.getMid()).getDatabaseId());
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
                fileName = application.getDownloadManager().getVideoFileName(key);
                fileNameDest = key + ".mp4";
            } else if (record.getPreview() instanceof TLLocalPhoto) {
                key = DownloadManager.getPhotoKey((TLLocalPhoto) record.getPreview());
                fileName = application.getDownloadManager().getPhotoFileName(key);
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