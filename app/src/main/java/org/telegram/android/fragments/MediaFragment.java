package org.telegram.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.extradea.framework.images.tasks.FileSystemImageTask;
import com.extradea.framework.images.ui.FastWebImageView;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;
import org.telegram.android.cursors.OrmCursorAdapter;
import org.telegram.android.media.CachedImageTask;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.media.DownloadState;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.ui.FontController;
import org.telegram.api.TLFileLocation;

/**
 * Author: Korshakov Stepan
 * Created: 07.08.13 17:13
 */
public class MediaFragment extends StelsFragment {
    private int peerType;
    private int peerId;

    private GridView gridView;

    public MediaFragment(int peerType, int peerId) {
        this.peerType = peerType;
        this.peerId = peerId;
    }

    public MediaFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            peerType = savedInstanceState.getInt("peerType");
            peerId = savedInstanceState.getInt("peerId");
        }

        View res = inflater.inflate(R.layout.media_view, container, false);
        gridView = (GridView) res.findViewById(R.id.mediaGrid);
        PreparedQuery<MediaRecord> query = null;
        try {
            QueryBuilder<MediaRecord, Long> builder = getEngine().getMediasDao().queryBuilder();
            if (peerType >= 0) {
                builder.where().eq("peerType", peerType).and().eq("peerId", peerId);
            }
            builder.orderBy("date", false);
            query = builder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final int margin = (int) (4 * getResources().getDisplayMetrics().density);
        final int cellWidth = ((Math.min(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels) - 5 * margin) / 4);

        OrmCursorAdapter<MediaRecord> adapter = new OrmCursorAdapter<MediaRecord>(getActivity(), application.getEngine().getDatabase().getReadableDatabase(), query) {
            @Override
            public View newView(Context context, MediaRecord object, ViewGroup parent) {
                GridView.LayoutParams layoutParams = new GridView.LayoutParams(cellWidth + margin, cellWidth + margin);
                FrameLayout frameLayout = new FrameLayout(context);
                frameLayout.setLayoutParams(layoutParams);

                FastWebImageView res = new FastWebImageView(context);
                FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(cellWidth, cellWidth);
                imageParams.topMargin = imageParams.leftMargin = imageParams.rightMargin = imageParams.bottomMargin = margin / 2;
                res.setLayoutParams(imageParams);
                res.setScaleTypeImage(FastWebImageView.SCALE_TYPE_FIT_CROP);

                TextView timeView = new TextView(context);
                timeView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.st_bubble_ic_video, 0, 0, 0);
                timeView.setCompoundDrawablePadding(getPx(6));
                timeView.setTextColor(0xE6FFFFFF);
                timeView.setTextSize(15);
                timeView.setTypeface(FontController.loadTypeface(context, "light"));

                FrameLayout.LayoutParams timeParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                timeParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
                timeParams.bottomMargin = getPx(6);
                timeParams.leftMargin = getPx(8);
                timeView.setLayoutParams(timeParams);

                frameLayout.addView(res);
                frameLayout.addView(timeView);

                return frameLayout;
            }

            @Override
            public void bindView(View view, Context context, MediaRecord object, int index) {
                FastWebImageView imageView = (FastWebImageView) ((ViewGroup) view).getChildAt(0);
                TextView timeView = (TextView) ((ViewGroup) view).getChildAt(1);
                if (object.getPreview() instanceof TLLocalPhoto) {
                    TLLocalPhoto localPhoto = (TLLocalPhoto) object.getPreview();
                    String key = DownloadManager.getPhotoKey(localPhoto);
                    timeView.setVisibility(View.GONE);
                    imageView.setBackgroundColor(0xffE6E6E6);
                    if (application.getDownloadManager().getState(key) == DownloadState.COMPLETED) {
                        imageView.requestTask(new FileSystemImageTask(application.getDownloadManager().getPhotoThumbSmallFileName(key)));
                    } else {
                        if (localPhoto.getFastPreviewW() != 0 && localPhoto.getFastPreviewH() != 0) {
                            imageView.requestTask(new CachedImageTask(localPhoto));
                        } else {
                            imageView.requestTask(null);
                        }
                    }
                } else if (object.getPreview() instanceof TLLocalVideo) {
                    timeView.setVisibility(View.VISIBLE);
                    imageView.setBackgroundColor(0xff000000);
                    TLLocalVideo video = (TLLocalVideo) object.getPreview();
                    timeView.setText(org.telegram.android.ui.TextUtil.formatDuration(video.getDuration()));

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
                } else {
                    imageView.requestTask(null);
                }
            }
        };
        if (adapter.getCount() == 0) {
            gridView.setVisibility(View.GONE);
            res.findViewById(R.id.empty).setVisibility(View.VISIBLE);
        } else {
            gridView.setVisibility(View.VISIBLE);
            res.findViewById(R.id.empty).setVisibility(View.GONE);
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    MediaRecord record = (MediaRecord) adapterView.getItemAtPosition(i);
                    getRootController().openImage(record.getMid(), peerType, peerId);
                }
            });
        }
        return res;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (peerType == PeerType.PEER_CHAT) {
            getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_media_title_group));
        } else if (peerType == PeerType.PEER_USER) {
            getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_media_title_user));
        } else {
            getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_media_title_all));
        }
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("peerType", peerType);
        outState.putInt("peerId", peerId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        gridView = null;
    }
}