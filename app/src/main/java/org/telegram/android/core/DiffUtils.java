package org.telegram.android.core;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 15.06.12
 * Time: 3:47
 */
public class DiffUtils {

    public static <T, D> boolean applyDiffUpdate(final Collection<T> newItems,
                                                 final RuntimeExceptionDao<T, D> dao,
                                                 final Comparator<T> comparator) {

        List<T> oldItems = dao.queryForAll();

        final List<T> toAdd = new ArrayList<T>();
        final List<T> toRemove = new ArrayList<T>();

        buildDiff(newItems, oldItems, toAdd, toRemove, comparator);

        if (toAdd.size() == 0 && toRemove.size() == 0)
            return false;

        dao.callBatchTasks(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                for (T f : toAdd) {
                    dao.create(f);
                }

                /*for (T f : toRemove) {
                    dao.delete(f);
                }*/
                return null;
            }
        });

        return true;
    }

    public static <T> void buildWindowDiff(T[] newItems, T[] oldItems,
                                           List<T> toAdd, List<T> toRemove, final Comparator<T> comparator) {
        List<T> sortedNewItems = Arrays.asList(newItems);
        List<T> sortedOldItems = Arrays.asList(oldItems);
        Collections.sort(sortedNewItems, new Comparator<T>() {
            @Override
            public int compare(T t, T t1) {
                return -comparator.compare(t, t1);
            }
        });
        Collections.sort(sortedOldItems, new Comparator<T>() {
            @Override
            public int compare(T t, T t1) {
                return -comparator.compare(t, t1);
            }
        });
        buildSortedWindowDiff(sortedNewItems, sortedOldItems, toAdd, toRemove, comparator);
    }

    public static <T> void buildSortedWindowDiff(List<T> sortedNewItems, List<T> sortedOldItems,
                                                 List<T> toAdd, List<T> toRemove, Comparator<T> comparator) {
        int bottomDelta = -1;

        outer:
        for (int i = 0; i < sortedNewItems.size(); i++) {
            T nItem = sortedNewItems.get(i);
            for (int j = 0; j < sortedOldItems.size(); j++) {
                if (comparator.compare(sortedOldItems.get(j), nItem) == 0) {
                    bottomDelta = i;
                    break outer;
                }
            }
        }

        if (bottomDelta == -1) {
            toAdd.addAll(sortedNewItems);
            return;
        }

        for (int i = 0; i < bottomDelta; i++) {
            toAdd.add(sortedNewItems.get(i));
        }

        List<T> workNewList = new ArrayList<T>();
        for (int i = 0; i < sortedNewItems.size() - bottomDelta; i++) {
            workNewList.add(sortedNewItems.get(i + bottomDelta));
        }

        if (workNewList.size() == 0)
            return;

        T topItem = workNewList.get(workNewList.size() - 1);
        List<T> workOldList = new ArrayList<T>();
        for (int i = 0; i < sortedOldItems.size(); i++) {
            if (comparator.compare(sortedOldItems.get(i), topItem) < 0) {
                break;
            }
            workOldList.add(sortedOldItems.get(i));
        }

        int oldIndex = 0;
        int newIndex = 0;

        while (oldIndex < workOldList.size() || newIndex < workNewList.size()) {
            if (oldIndex >= workOldList.size()) {
                toAdd.add(workNewList.get(newIndex++));
                continue;
            }

            if (newIndex >= workNewList.size()) {
                toRemove.add(workOldList.get(oldIndex++));
                continue;
            }

            int result = comparator.compare(workNewList.get(newIndex), workOldList.get(oldIndex));
            if (result == 0) {
                newIndex++;
                oldIndex++;
            } else if (result > 0) {
                toAdd.add(workNewList.get(newIndex++));
            } else if (result < 0) {
                toRemove.add(workOldList.get(oldIndex++));
            }
        }
    }

    public static <T> void buildDiff(Collection<T> newItems, Collection<T> oldItems, List<T> toAdd, List<T> toRemove, Comparator<T> comparator) {
        loop:
        for (T arrived : newItems) {
            for (T saved : oldItems) {
                if (comparator.compare(saved, arrived) == 0) {
                    continue loop;
                }
            }

            toAdd.add(arrived);
        }

        loop:
        for (T saved : oldItems) {
            for (T arrived : newItems) {
                if (comparator.compare(saved, arrived) == 0) {
                    continue loop;
                }
            }

            toRemove.add(saved);
        }
    }

}
