package org.telegram.android.cursors;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 03.07.12
 * Time: 15:39
 */
public abstract class BaseOrmCursorAdapter<T> extends ExtraCursorAdapter<T> {

    private int count = -1;

    private static Cursor getCursor(SQLiteDatabase database, String query) {
        Cursor res = database.rawQuery(query, new String[0]);
        res.getCount();
        return res;
    }

    private final String query;
    private final SQLiteDatabase database;
    private Activity context;

    public BaseOrmCursorAdapter(Activity context, SQLiteDatabase database, String query) {
        super(context, getCursor(database, query));
        if (context instanceof CursorManager) {
            ((CursorManager) context).registerCursor(getCursor());
        }
        this.query = query;
        this.database = database;
        this.context = context;
    }

    protected abstract T getObjectFromCursor(Cursor cursor);

    @Override
    public int getCount() {
        if (count != -1)
            return count;
        count = super.getCount();
        return count;
    }

    public Object prepareUpdate() {
        return getCursor(database, query);
    }

    public void applyUpdate(Object updateData) {
        count = -1;
        Cursor old = swapCursor((Cursor) updateData);
        if (old != null) {
            if (context instanceof CursorManager) {
                ((CursorManager) context).unregisterCursor(old);
            }
            old.close();
        }
    }

    public void close() {
        getCursor().close();
    }
}