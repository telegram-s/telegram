package org.telegram.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import org.telegram.android.R;
import org.telegram.android.base.TelegramActivity;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.model.WebSearchResult;
import org.telegram.android.media.Optimizer;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.GifView;
import org.telegram.android.util.IOUtils;
import uk.co.senab.photoview.PhotoView;

import java.io.File;
import java.io.IOException;

/**
 * Created by ex3ndr on 15.03.14.
 */
public class WebSearchPreviewFragment extends TelegramFragment {
    private WebSearchResult webSearchResult;
    private boolean isClosed = false;
    private PhotoView imageView;
    private GifView gifView;

    public WebSearchPreviewFragment(WebSearchResult webSearchResult) {
        this.webSearchResult = webSearchResult;
    }

    public WebSearchPreviewFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.web_search_preview, container, false);
        imageView = (PhotoView) res.findViewById(R.id.image);
        gifView = (GifView) res.findViewById(R.id.gif);

        if (webSearchResult.getContentType().equals("image/gif")) {
            imageView.setVisibility(View.GONE);
            gifView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            gifView.setVisibility(View.GONE);
        }

        res.findViewById(R.id.sendImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runUiTask(new AsyncAction() {

                    private String fileName;

                    @Override
                    public void execute() throws AsyncException {
                        try {
                            byte[] data = application.getUiKernel().getWebImageStorage().tryLoadData(webSearchResult.getFullUrl());
                            if (data == null) {
                                data = IOUtils.downloadFile(webSearchResult.getFullUrl());
                            }
                            fileName = getUploadTempFile("upload.jpg");
                            IOUtils.writeAll(fileName, data);
                            application.getUiKernel().getWebImageStorage().saveFile(webSearchResult.getFullUrl(), data);
                            application.getDataSourceKernel().getWebSearchSource().onSearchResultSent(webSearchResult.toTlResult());
                        } catch (IOException e) {
                            throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
                        }
                    }

                    @Override
                    public void afterExecute() {
                        getActivity().setResult(Activity.RESULT_OK, new Intent().setData(Uri.fromFile(new File(fileName))));
                        getActivity().finish();
                    }
                });
            }
        });
        isClosed = false;
        new Thread() {
            @Override
            public void run() {
                while (!isClosed) {
                    try {
                        byte[] data = application.getUiKernel().getWebImageStorage().tryLoadData(webSearchResult.getFullUrl());

                        if (data == null) {
                            data = IOUtils.downloadFile(webSearchResult.getFullUrl());
                        }
                        String fileName = getUploadTempFile("upload.jpg");
                        IOUtils.writeAll(fileName, data);
                        application.getUiKernel().getWebImageStorage().saveFile(webSearchResult.getFullUrl(), data);

                        if (webSearchResult.getContentType().equals("image/gif")) {
                            secureCallback(new Runnable() {
                                @Override
                                public void run() {
                                    gifView.loadGif(application.getUiKernel().getWebImageStorage().getImageFileName(webSearchResult.getFullUrl()));
                                }
                            });
                        } else {
                            final Bitmap bitmap = Optimizer.optimize(data);
                            secureCallback(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageBitmap(bitmap);
                                }
                            });
                        }
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        return res;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_web_search_preview));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
        ((TelegramActivity) getActivity()).fixBackButton();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isClosed = true;
    }
}
