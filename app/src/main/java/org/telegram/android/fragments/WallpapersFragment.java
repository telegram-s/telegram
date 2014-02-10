package org.telegram.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.api.*;
import org.telegram.api.engine.RpcException;
import org.telegram.api.engine.TimeoutException;
import org.telegram.api.requests.TLRequestAccountGetWallPapers;
import org.telegram.api.upload.TLFile;
import org.telegram.tl.TLBytes;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 16:26
 */
public class WallpapersFragment extends TelegramFragment {
    private TLAbsWallPaper[] wallPapers;
    private Gallery gallery;
    private FastWebImageView preview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.wallpapers_container, container, false);
        gallery = (Gallery) res.findViewById(R.id.previewGallery);
        preview = (FastWebImageView) res.findViewById(R.id.preview);
        preview.setScaleTypeImage(FastWebImageView.SCALE_TYPE_FIT_CROP);
        res.findViewById(R.id.cancel).setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        }));
        res.findViewById(R.id.setWallpaper).setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TLAbsWallPaper wallPaper = wallPapers[gallery.getSelectedItemPosition()];
                if (wallPaper instanceof TLWallPaper) {
                    final TLWallPaper tlWallPaper = (TLWallPaper) wallPaper;
                    final TLPhotoSize size = ApiUtils.getWallpaperFull(tlWallPaper.getSizes());
                    runUiTask(new AsyncAction() {
                        @Override
                        public void execute() throws AsyncException {
                            TLFileLocation location = (TLFileLocation) size.getLocation();
                            try {
                                TLFile file = application.getApi().doGetFile(location.getDcId(), new TLInputFileLocation(location.getVolumeId(), location.getLocalId(), location.getSecret()), 0, size.getSize());
                                saveWallpaper(file.getBytes());
                                application.getUserSettings().setCurrentWallpaperId(tlWallPaper.getId());
                                application.getUserSettings().setWallpaperSet(true);
                                application.getUserSettings().setWallpaperSolid(false);
                                application.getWallpaperHolder().dropCache();
                            } catch (RpcException e) {
                                throw new AsyncException(e);
                            } catch (TimeoutException e) {
                                throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
                            } catch (IOException e) {
                                throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                            }
                        }

                        @Override
                        public void afterExecute() {
                            getRootController().doBack();
                        }
                    });
                } else {
                    TLWallPaperSolid wallPaperSolid = (TLWallPaperSolid) wallPaper;
                    application.getUserSettings().setCurrentWallpaperId(wallPaperSolid.getId());
                    application.getUserSettings().setWallpaperSet(true);
                    application.getUserSettings().setWallpaperSolid(true);
                    application.getUserSettings().setCurrentWallpaperSolidColor(wallPaperSolid.getBgColor() | 0xFF000000);
                    application.getWallpaperHolder().dropCache();
                    getRootController().doBack();
                }
            }
        }));

        if (wallPapers == null) {
            runUiTask(new AsyncAction() {
                @Override
                public void execute() throws AsyncException {
                    wallPapers = rpc(new TLRequestAccountGetWallPapers()).toArray(new TLAbsWallPaper[0]);
                }

                @Override
                public void afterExecute() {
                    bindUi();
                }

                @Override
                public void onCanceled() {
                    getRootController().doBack();
                }
            });
        }
        return res;
    }

    private void bindUi() {
        final Activity context = getActivity();
        gallery.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return wallPapers.length;
            }

            @Override
            public TLAbsWallPaper getItem(int position) {
                return wallPapers[position];
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                if (view == null) {
                    view = context.getLayoutInflater().inflate(R.layout.wallpapers_item, parent, false);
                    ((FastWebImageView) view.findViewById(R.id.preview)).setScaleTypeImage(FastWebImageView.SCALE_TYPE_FIT_CROP);
                }
                TLAbsWallPaper wallPaper = getItem(position);
                if (wallPaper instanceof TLWallPaper) {
                    TLWallPaper tlWallPaper = (TLWallPaper) wallPaper;
                    TLPhotoSize size = ApiUtils.getWallpaperPreview(tlWallPaper.getSizes());
                    StelsImageTask task = new StelsImageTask((TLFileLocation) size.getLocation());
                    ((FastWebImageView) view.findViewById(R.id.preview)).requestTask(task);
                    ((FastWebImageView) view.findViewById(R.id.preview)).setBackgroundDrawable(null);
                } else {
                    ((FastWebImageView) view.findViewById(R.id.preview)).requestTask(null);
                    ((FastWebImageView) view.findViewById(R.id.preview)).setBackgroundDrawable(new ColorDrawable(((TLWallPaperSolid) wallPaper).getBgColor() | 0xFF000000));
                }
                return view;
            }
        });
        gallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (wallPapers[position] instanceof TLWallPaper) {
                    TLPhotoSize size = ApiUtils.getWallpaperFull(((TLWallPaper) wallPapers[position]).getSizes());
                    StelsImageTask task = new StelsImageTask((TLFileLocation) size.getLocation());
                    preview.requestTask(task);
                    preview.setBackgroundDrawable(null);
                } else {
                    preview.requestTask(null);
                    preview.setBackgroundDrawable(new ColorDrawable(((TLWallPaperSolid) wallPapers[position]).getBgColor() | 0xFF000000));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        getRootController().showPanel();
    }

    @Override
    public void onResume() {
        super.onResume();
        getRootController().hidePanel();
    }

    private void saveWallpaper(TLBytes bytes) throws IOException {
        FileOutputStream stream = application.openFileOutput("current_wallpaper.jpg", Context.MODE_PRIVATE);
        stream.write(bytes.getData(), bytes.getOffset(), bytes.getLength());
        stream.close();
    }
}
