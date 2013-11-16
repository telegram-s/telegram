package org.telegram.android.ui;

import android.content.Context;
import com.extradea.framework.persistence.ContextPersistence;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Author: Korshakov Stepan
 * Created: 25.08.13 4:26
 */
public class LastEmojiProcessor extends ContextPersistence {

    public static final int LAST_EMOJI_COUNT = 30;

    private long[] lastSmileys;

    public LastEmojiProcessor(Context context) {
        super(context);
        tryLoad();
    }

    public long[] getLastSmileys() {
        if (lastSmileys == null) {
            return new long[0];
        }
        return lastSmileys;
    }

    public void setLastSmileys(long[] lastSmileys) {
        this.lastSmileys = lastSmileys;
        trySave();
    }

    public void applyLastSmileys(long[] smileys) {
        ArrayList<Long> res = new ArrayList<Long>();
        HashSet<Long> contained = new HashSet<Long>();
        for (long l : smileys) {
            if (contained.contains(l))
                continue;
            contained.add(l);
            res.add(l);
        }
        if (lastSmileys != null) {
            for (long l : lastSmileys) {
                if (contained.contains(l))
                    continue;
                contained.add(l);
                res.add(l);
            }
        }
        long[] nSmileys;
        if (res.size() > LAST_EMOJI_COUNT) {
            nSmileys = new long[LAST_EMOJI_COUNT];
        } else {
            nSmileys = new long[res.size()];
        }
        for (int i = 0; i < nSmileys.length; i++) {
            nSmileys[i] = res.get(i);
        }
        setLastSmileys(nSmileys);
    }

    public void clearLastSmileys() {
        setLastSmileys(new long[0]);
    }
}
