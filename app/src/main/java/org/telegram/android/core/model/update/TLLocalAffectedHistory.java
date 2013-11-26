package org.telegram.android.core.model.update;

import org.telegram.api.messages.TLAffectedHistory;

/**
 * Created by ex3ndr on 26.11.13.
 */
public class TLLocalAffectedHistory extends TLLocalUpdate {
    private TLAffectedHistory affectedHistory;

    public TLLocalAffectedHistory(TLAffectedHistory affectedHistory) {
        this.affectedHistory = affectedHistory;
    }

    public TLAffectedHistory getAffectedHistory() {
        return affectedHistory;
    }
}
