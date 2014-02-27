package org.telegram.android.core.engines;

import org.telegram.android.core.model.MediaRecord;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.dao.MediaRecordDao;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by ex3ndr on 14.01.14.
 */
public class MediaDatabase {
    private MediaRecordDao mediaRecordDao;

    public MediaDatabase(ModelEngine engine) {
        mediaRecordDao = engine.getDaoSession().getMediaRecordDao();
    }

    public int getMediaCount(int peerType, int peerId) {
        return (int) mediaRecordDao.queryBuilder()
                .where(MediaRecordDao.Properties.PeerUniq.eq(uniq(peerType, peerId)))
                .count();
    }

    public MediaRecord[] queryMedia(int peerType, int peerId) {
        List<org.telegram.dao.MediaRecord> dbRes = mediaRecordDao.queryBuilder()
                .where(MediaRecordDao.Properties.PeerUniq.eq(uniq(peerType, peerId)))
                .orderDesc(MediaRecordDao.Properties.Date)
                .list();
        MediaRecord[] res = new MediaRecord[dbRes.size()];
        int index = 0;
        for (org.telegram.dao.MediaRecord db : dbRes) {
            res[index++] = convert(db);
        }
        return res;
    }

    public MediaRecord[] queryMedia(int peerType, int peerId, int offset, int limit) {
        List<org.telegram.dao.MediaRecord> dbRes = mediaRecordDao.queryBuilder()
                .where(MediaRecordDao.Properties.PeerUniq.eq(uniq(peerType, peerId)))
                .orderDesc(MediaRecordDao.Properties.Date)
                .offset(offset)
                .limit(limit)
                .list();
        MediaRecord[] res = new MediaRecord[dbRes.size()];
        int index = 0;
        for (org.telegram.dao.MediaRecord db : dbRes) {
            res[index++] = convert(db);
        }
        return res;
    }

    public List<MediaRecord> lazyQueryMedia(int peerType, int peerId) {
        return new ConvertList(mediaRecordDao.queryBuilder()
                .where(MediaRecordDao.Properties.PeerUniq.eq(uniq(peerType, peerId)))
                .orderDesc(MediaRecordDao.Properties.Date)
                .listLazy());
    }

    public MediaRecord loadMedia(int mid) {
        List<org.telegram.dao.MediaRecord> res = mediaRecordDao.queryBuilder()
                .where(MediaRecordDao.Properties.Mid.eq(mid))
                .list();

        if (res.size() > 0) {
            return convert(res.get(0));
        }

        return null;
    }

    public void saveMedia(MediaRecord record) {
        mediaRecordDao.insert(convert(record));
    }

    public void deleteMedia(int mid) {
        mediaRecordDao.queryBuilder()
                .where(MediaRecordDao.Properties.Mid.eq(mid))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    public void deleteMediaFromChat(int peerType, int peerId) {
        mediaRecordDao.queryBuilder()
                .where(MediaRecordDao.Properties.PeerUniq.eq(uniq(peerType, peerId)))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    private long uniq(int peerType, int peerId) {
        return peerId * 10L + peerType;
    }

    private org.telegram.dao.MediaRecord convert(MediaRecord src) {
        org.telegram.dao.MediaRecord res = new org.telegram.dao.MediaRecord();
        if (src.getDatabaseId() > 0) {
            res.setId(src.getDatabaseId());
        }
        res.setDate(src.getDate());
        res.setSenderId(src.getSenderId());
        res.setMid(src.getMid());
        res.setPeerUniq(uniq(src.getPeerType(), src.getPeerId()));
        if (src.getPreview() != null) {
            try {
                res.setPreview(src.getPreview().serialize());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    private MediaRecord convert(org.telegram.dao.MediaRecord src) {
        MediaRecord res = new MediaRecord();
        res.setDatabaseId(src.getId());
        res.setDate(src.getDate());
        res.setSenderId(src.getSenderId());
        res.setMid(src.getMid());
        res.setPeerId((int) (src.getPeerUniq() / 10L));
        res.setPeerType((int) (src.getPeerUniq() % 10L));
        if (src.getPreview() != null) {
            try {
                res.setPreview(TLLocalContext.getInstance().deserializeMessage(src.getPreview()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    private class ConvertList implements List<MediaRecord> {

        private List<org.telegram.dao.MediaRecord> srcList;

        public ConvertList(List<org.telegram.dao.MediaRecord> srcList) {
            this.srcList = srcList;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> objects) {
            return false;
        }

        @Override
        public MediaRecord get(int i) {
            return convert(srcList.get(i));
        }

        @Override
        public int indexOf(Object o) {
            return -1;
        }

        @Override
        public boolean isEmpty() {
            return srcList.isEmpty();
        }

        @Override
        public Iterator<MediaRecord> iterator() {
            final Iterator<org.telegram.dao.MediaRecord> srcIterator = srcList.iterator();
            return new Iterator<MediaRecord>() {
                @Override
                public boolean hasNext() {
                    return srcIterator.hasNext();
                }

                @Override
                public MediaRecord next() {
                    return convert(srcIterator.next());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int lastIndexOf(Object o) {
            return -1;
        }

        @Override
        public int size() {
            return srcList.size();
        }

        @Override
        public ListIterator<MediaRecord> listIterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<MediaRecord> listIterator(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MediaRecord> subList(int i, int i2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] ts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int i, MediaRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(MediaRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int i, Collection<? extends MediaRecord> mediaRecords) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends MediaRecord> mediaRecords) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MediaRecord remove(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> objects) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> objects) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MediaRecord set(int i, MediaRecord record) {
            throw new UnsupportedOperationException();
        }
    }

    public void clear() {
        mediaRecordDao.deleteAll();
    }
}
