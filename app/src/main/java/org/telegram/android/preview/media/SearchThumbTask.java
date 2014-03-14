package org.telegram.android.preview.media;

import org.telegram.android.core.model.WebSearchResult;

/**
 * Created by ex3ndr on 15.03.14.
 */
public class SearchThumbTask extends BaseTask {
    private WebSearchResult result;

    public SearchThumbTask(WebSearchResult result) {
        super(true);
        this.result = result;
    }

    public WebSearchResult getResult() {
        return result;
    }

    @Override
    public String getStorageKey() {
        return result.getThumbUrl();
    }
}
