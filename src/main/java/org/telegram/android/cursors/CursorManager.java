package org.telegram.android.cursors;

import android.database.Cursor;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 31.07.12
 * Time: 2:59
 */
public interface CursorManager {
    public void registerCursor(Cursor cursor);

    public void unregisterCursor(Cursor cursor);
}
