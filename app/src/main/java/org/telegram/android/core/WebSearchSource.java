package org.telegram.android.core;

import android.net.Uri;
import android.util.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.android.core.model.WebSearchResult;
import org.telegram.android.ui.source.ViewSource;
import org.telegram.android.ui.source.ViewSourceListener;
import org.telegram.android.ui.source.ViewSourceState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by ex3ndr on 14.03.14.
 */
public class WebSearchSource {
    private static final String BING_KEY = "qcN+C8Z41IFbeIsQsDjq90rinInEKaNJew+y9CBAxdM";

    private ViewSource<WebSearchResult, WebSearchResult> viewSource;
    private boolean isDestroyed;
    private HttpClient client;
    private String query;
    private ViewSourceListener listener;

    public WebSearchSource() {
        isDestroyed = false;
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        client = new DefaultHttpClient(httpParams);
        client.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
    }

    public String getQuery() {
        return query;
    }

    public ViewSourceListener getListener() {
        return listener;
    }

    public void setListener(ViewSourceListener listener) {
        if (viewSource != null) {
            viewSource.removeListener(this.listener);
        }
        this.listener = listener;
        if (viewSource != null && listener != null) {
            viewSource.addListener(listener);
        }
    }

    public void newQuery(final String _query) {
        this.query = _query;

        if (viewSource != null) {
            viewSource.destroy();
            viewSource = null;
        }

        viewSource = new ViewSource<WebSearchResult, WebSearchResult>() {
            @Override
            protected WebSearchResult[] loadItems(int offset) {
                while (!isDestroyed && _query.equals(query)) {
                    try {
                        return doRequest(offset, _query);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return new WebSearchResult[0];
            }

            @Override
            protected long getSortingKey(WebSearchResult obj) {
                return obj.getIndex();
            }

            @Override
            protected long getItemKey(WebSearchResult obj) {
                return obj.getIndex();
            }

            @Override
            protected long getItemKeyV(WebSearchResult obj) {
                return obj.getIndex();
            }

            @Override
            protected ViewSourceState getInternalState() {
                return ViewSourceState.COMPLETED;
            }

            @Override
            protected WebSearchResult convert(WebSearchResult item) {
                return item;
            }
        };
        viewSource.onConnected();
        if (listener != null) {
            viewSource.addListener(listener);
        }
    }

    public ViewSource<WebSearchResult, WebSearchResult> getViewSource() {
        return viewSource;
    }

    private WebSearchResult[] doRequest(int offset, String query) throws Exception {
        String encodedQuery = Uri.encode(query);
        String url = "https://api.datamarket.azure.com/Bing/Search/v1/Image?Query='" + encodedQuery + "'&$format=json&Adult='Off'";
        HttpGet get = new HttpGet(url);

        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(BING_KEY, BING_KEY);
        BasicScheme scheme = new BasicScheme();
        Header authorizationHeader = scheme.authenticate(credentials, get);
        get.addHeader(authorizationHeader);

        HttpResponse response = client.execute(get);
        if (response.getEntity().getContentLength() == 0) {
            throw new IOException();
        }

        if (response.getStatusLine().getStatusCode() == 404) {
            throw new IOException();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outputStream);
        byte[] data = outputStream.toByteArray();
        String responseData = new String(data);
        JSONObject res = new JSONObject(responseData);
        JSONArray array = res.getJSONObject("d").getJSONArray("results");
        WebSearchResult[] results = new WebSearchResult[array.length()];
        for (int i = 0; i < results.length; i++) {
            JSONObject record = array.getJSONObject(i);
            JSONObject thumb = record.getJSONObject("Thumbnail");
            int size = record.getInt("FileSize");
            String fullUrl = record.getString("MediaUrl");
            String contentType = record.getString("ContentType");
            int fullW = record.getInt("Width");
            int fullH = record.getInt("Height");

            String thumbUrl = thumb.getString("MediaUrl");
            int thumbW = thumb.getInt("Width");
            int thumbH = thumb.getInt("Height");

            results[i] = new WebSearchResult(offset + i, size, fullW, fullH, fullUrl, thumbW, thumbH, thumbUrl, contentType);
        }
        return results;
    }
}
