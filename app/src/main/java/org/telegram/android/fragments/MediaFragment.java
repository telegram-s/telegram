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
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.MediaSource;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.MediaRecord;
import org.telegram.android.core.model.media.TLLocalDocument;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.media.DownloadState;
import org.telegram.android.preview.PreviewConfig;
import org.telegram.android.preview.SmallPreviewView;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.source.ViewSourceListener;
import org.telegram.android.ui.source.ViewSourceState;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Korshakov Stepan
 * Created: 07.08.13 17:13
 */
public class MediaFragment extends TelegramFragment implements ViewSourceListener {
    private int peerType;
    private int peerId;

    private GridView gridView;
    private View loading;
    private View empty;

    private MediaSource mediaSource;
    private ArrayList<MediaRecord> records;
    private BaseAdapter adapter;
    private boolean isLoading = false;
    private boolean isLoadingError = false;

    public MediaFragment(int peerType, int peerId) {
        this.peerType = peerType;
        this.peerId = peerId;
    }

    public MediaFragment() {

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            peerType = savedInstanceState.getInt("peerType");
            peerId = savedInstanceState.getInt("peerId");
        }

        View res = inflater.inflate(R.layout.media_view, container, false);
        gridView = (GridView) res.findViewById(R.id.mediaGrid);
        loading = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        gridView.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);

        mediaSource = application.getDataSourceKernel().getMediaSource(peerType, peerId);
        mediaSource.getSource().onConnected();
        records = mediaSource.getSource().getCurrentWorkingSet();
        // final List<MediaRecord> lazyList = application.getEngine().getMediaEngine().lazyQueryMedia(peerType, peerId);

        gridView.setPadding(0, PreviewConfig.MEDIA_SPACING, 0, PreviewConfig.MEDIA_SPACING);
        gridView.setNumColumns(PreviewConfig.MEDIA_ROW_COUNT);
        gridView.setColumnWidth(PreviewConfig.MEDIA_PREVIEW);
        gridView.setVerticalSpacing(PreviewConfig.MEDIA_SPACING);
        gridView.setHorizontalSpacing(PreviewConfig.MEDIA_SPACING);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i >= 0 && i < records.size()) {
                    getRootController().openImage(records.get(i).getMid(), peerType, peerId);
                } else {
                    mediaSource.requestLoadMore(records.size());
                }
            }
        });

        final Context context = getActivity();
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                if (isLoading) {
                    return records.size() + 1;
                } else {
                    return records.size();
                }
            }

            @Override
            public MediaRecord getItem(int i) {
                if (i >= 0 && i < records.size()) {
                    return records.get(i);
                } else {
                    return null;
                }
            }

            @Override
            public long getItemId(int i) {
                if (i >= 0 && i < records.size()) {
                    return records.get(i).getDatabaseId();
                } else {
                    return 0;
                }
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public int getItemViewType(int i) {
                if (i >= 0 && i < records.size()) {
                    return 0;
                } else {
                    return 1;
                }
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int i) {
                return i >= 0 && i < records.size() || isLoadingError;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (i >= 0 && i < records.size()) {
                    mediaSource.getSource().onItemsShown(i);

                    MediaRecord record = getItem(i);
                    if (view == null) {
                        view = newView(context, record, viewGroup);
                    }
                    bindView(view, context, record, i);
                    return view;
                } else {
                    if (view == null) {
                        view = inflater.inflate(R.layout.media_loading, viewGroup, false);
                        ViewGroup.LayoutParams imageParams = new ViewGroup.LayoutParams(PreviewConfig.MEDIA_PREVIEW, PreviewConfig.MEDIA_PREVIEW);
                        view.setLayoutParams(imageParams);
                    }

                    if (isLoadingError) {
                        view.findViewById(R.id.progress).setVisibility(View.GONE);
                        view.findViewById(R.id.retry).setVisibility(View.VISIBLE);
                    } else {
                        view.findViewById(R.id.progress).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.retry).setVisibility(View.GONE);
                    }

                    return view;
                }
            }

            public View newView(Context context, MediaRecord object, ViewGroup parent) {
                GridView.LayoutParams layoutParams = new GridView.LayoutParams(PreviewConfig.MEDIA_PREVIEW, PreviewConfig.MEDIA_PREVIEW);
                FrameLayout frameLayout = new FrameLayout(context);
                frameLayout.setLayoutParams(layoutParams);

                SmallPreviewView smallPreviewView = new SmallPreviewView(context);
                FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(PreviewConfig.MEDIA_PREVIEW, PreviewConfig.MEDIA_PREVIEW);
                smallPreviewView.setLayoutParams(imageParams);

                ImageView videoLogo = new ImageView(context);
                videoLogo.setImageResource(R.drawable.st_media_ic_play);

                FrameLayout.LayoutParams timeParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                timeParams.gravity = Gravity.CENTER;
                videoLogo.setLayoutParams(timeParams);

                frameLayout.addView(smallPreviewView);
                frameLayout.addView(videoLogo);

                return frameLayout;
            }

            public void bindView(View view, Context context, MediaRecord object, int index) {
                SmallPreviewView imageView = (SmallPreviewView) ((ViewGroup) view).getChildAt(0);
                View videoView = ((ViewGroup) view).getChildAt(1);

                if (object.getPreview() instanceof TLLocalVideo) {
                    videoView.setVisibility(View.VISIBLE);
                } else {
                    videoView.setVisibility(View.INVISIBLE);
                }

                if (object.getPreview() instanceof TLLocalPhoto) {
                    TLLocalPhoto localPhoto = (TLLocalPhoto) object.getPreview();
                    String key = DownloadManager.getPhotoKey(localPhoto);
                    if (application.getDownloadManager().getState(key) == DownloadState.COMPLETED) {
                        imageView.requestFile(application.getDownloadManager().getFileName(key));
                    } else {
                        if (localPhoto.getFastPreviewW() != 0 && localPhoto.getFastPreviewH() != 0) {
                            imageView.requestFast(localPhoto);
                        } else {
                            imageView.clearImage();
                        }
                    }
                } else if (object.getPreview() instanceof TLLocalVideo) {
                    TLLocalVideo localVideo = (TLLocalVideo) object.getPreview();
                    if (localVideo.getFastPreview().length > 0) {
                        imageView.requestFast(localVideo);
                    } else {
                        imageView.clearImage();
                    }
                } else if (object.getPreview() instanceof TLLocalDocument) {
                    TLLocalDocument localVideo = (TLLocalDocument) object.getPreview();
                    if (localVideo.getFastPreview().length > 0) {
                        imageView.requestFast(localVideo);
                    } else {
                        imageView.clearImage();
                    }
                } else {
                    imageView.clearImage();
                }
            }
        };
        gridView.setAdapter(adapter);

        return res;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mediaSource != null) {
            mediaSource.getSource().addListener(this);
            onSourceDataChanged();
            onSourceStateChanged();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaSource != null) {
            mediaSource.getSource().removeListener(this);
        }
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

    @Override
    public void onSourceStateChanged() {
        isLoadingError = mediaSource.getSource().getState() == ViewSourceState.LOAD_MORE_ERROR;
        isLoading = mediaSource.getSource().getState() == ViewSourceState.IN_PROGRESS || isLoadingError;
        if (records.size() == 0) {
            goneView(gridView);
            if (isLoading) {
                showView(loading);
                goneView(empty);
            } else {
                goneView(loading);
                showView(empty);
            }
        } else {
            showView(gridView);
            goneView(loading);
            goneView(empty);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSourceDataChanged() {
        records = mediaSource.getSource().getCurrentWorkingSet();
        adapter.notifyDataSetChanged();
    }
}