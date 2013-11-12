package org.telegram.android.cursors;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.06.12
 * Time: 17:49
 */
public interface DataSource<T> {
    public int getCount();

    public T getItem(int index);

    public long getId(T obj);

    public Object doPreload();

    public void invalidate();

    public void invalidate(Object preload);

    public void close();
}
