package org.telegram.android.cursors;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.LruObjectCache;
import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.stmt.PreparedQuery;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 02.07.12
 * Time: 21:59
 */
public abstract class OrmCursorAdapter<T> extends BaseOrmCursorAdapter<T> {

    private ObjectCache cache = new LruObjectCache(50);
    private final PreparedQuery<T> query;

    private static final String checkedStatement(PreparedQuery query) {
        try {
            return query.getStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public OrmCursorAdapter(Activity context, SQLiteDatabase database, PreparedQuery<T> query) {
        super(context, database, checkedStatement(query));
        this.query = query;
    }

    @Override
    protected T getObjectFromCursor(Cursor cursor) {
        AndroidDatabaseResults results = new AndroidDatabaseResults(cursor, cache);
        try {
            return query.mapRow(results);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
