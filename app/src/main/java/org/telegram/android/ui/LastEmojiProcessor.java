package org.telegram.android.ui;

import android.content.Context;
import org.telegram.android.critical.SafeFileWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Author: Korshakov Stepan
 * Created: 25.08.13 4:26
 */
public class LastEmojiProcessor {

    public static final int LAST_EMOJI_COUNT = 30;

    private long[] lastSmileys;
    private SafeFileWriter fileWriter;

    public LastEmojiProcessor(Context context) {
        fileWriter = new SafeFileWriter(context, "org.telegram.android.last_emoji.bin");
        try {
            byte[] data = fileWriter.loadData();
            if (data == null) {
                return;
            }
            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data));
            int count = stream.readInt();
            long[] smileys = new long[count];
            for (int i = 0; i < count; i++) {
                smileys[i] = stream.readLong();
            }
            lastSmileys = smileys;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long[] getLastSmileys() {
        if (lastSmileys == null) {
            return new long[0];
        }
        return lastSmileys;
    }

    public void setLastSmileys(long[] lastSmileys) {
        this.lastSmileys = lastSmileys;
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream outputStream = new DataOutputStream(stream);
            if (lastSmileys == null) {
                outputStream.writeInt(0);
            } else {
                outputStream.writeInt(lastSmileys.length);
                for (int i = 0; i < lastSmileys.length; i++) {
                    outputStream.writeLong(lastSmileys[i]);
                }
            }
            outputStream.close();
            fileWriter.saveData(stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
