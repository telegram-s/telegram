package org.telegram.android.media;

import android.os.FileObserver;
import org.telegram.android.TelegramApplication;
import org.telegram.android.log.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ex3ndr on 21.01.14.
 */
public class FileCache {
    private static final String TAG = "FileCache";

    private static final int STORAGE_UNKNOWN = -1;
    private static final int STORAGE_CACHE_EXTERNAL = 0;
    private static final int STORAGE_CACHE_INTERNAL = 1;

    private HashMap<String, FileRecord> fileRecords;

    private TelegramApplication application;
    private int currentPrimaryCache = STORAGE_UNKNOWN;

    private String internalCachePath;
    private String externalCachePath;
    private FileObserver externalCacheObserver;
    private FileObserver internalCacheObserver;

    private FileCachePersistence persistence;

    public FileCache(TelegramApplication application) {
        this.application = application;
        this.persistence = new FileCachePersistence(application);
        this.fileRecords = new HashMap<String, FileRecord>();
        updateCache();
    }

    private String getExternalCacheDir() {
        if (application.getExternalCacheDir() != null) {
            return application.getExternalCacheDir().toString();
        }
        return null;
    }

    private String getInternalCacheDir() {
        return application.getCacheDir().toString();
    }

    private void updateCache() {
        // External Cache
        currentPrimaryCache = STORAGE_UNKNOWN;

        externalCachePath = getExternalCacheDir();
        if (externalCachePath != null) {
            currentPrimaryCache = STORAGE_CACHE_EXTERNAL;
            if (externalCacheObserver == null) {
                this.externalCacheObserver = new FileObserver(externalCachePath,
                        FileObserver.DELETE_SELF | FileObserver.DELETE | FileObserver.CREATE) {
                    @Override
                    public void onEvent(int event, String s) {
                        try {
                            if (event == FileObserver.DELETE_SELF) {
                                externalCacheObserver = null;
                                externalCachePath = null;
                                onStorageDied(STORAGE_CACHE_EXTERNAL);
                            } else if (s != null && event == FileObserver.DELETE) {
                                onFileDeleted(s, STORAGE_CACHE_EXTERNAL);
                            } else if (s != null && event == FileObserver.CREATE) {
                                onFileCreated(s, STORAGE_CACHE_EXTERNAL);
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                };
                this.externalCacheObserver.startWatching();

                onStorageCreated(STORAGE_CACHE_EXTERNAL);

                File[] files = new File(externalCachePath).listFiles();
                HashSet<String> cacheFiles = new HashSet<String>();
                for (File f : files) {
                    if (!f.isFile()) {
                        continue;
                    }
                    if (!persistence.isDownloaded(f.getName())) {
                        continue;
                    }
                    cacheFiles.add(f.getName());
                    FileRecord record = fileRecords.get(f.getName());
                    if (record != null) {
                        if (record.storageType == STORAGE_UNKNOWN) {
                            record.storageType = STORAGE_CACHE_EXTERNAL;
                        }
                    } else {
                        fileRecords.put(f.getName(), new FileRecord(STORAGE_CACHE_EXTERNAL, f.getName()));
                    }
                }

                for (FileRecord record : fileRecords.values().toArray(new FileRecord[0])) {
                    if (record.storageType == STORAGE_CACHE_EXTERNAL && !cacheFiles.contains(record.fileName)) {
                        record.storageType = STORAGE_UNKNOWN;
                    }
                }
            }
        } else {
            if (externalCacheObserver != null) {
                currentPrimaryCache = STORAGE_CACHE_INTERNAL;
                externalCacheObserver.stopWatching();
                externalCacheObserver = null;
                externalCachePath = null;
                onStorageDied(STORAGE_CACHE_EXTERNAL);
            }
        }

        // Internal Cache
        if (internalCacheObserver == null) {
            if (currentPrimaryCache == STORAGE_UNKNOWN) {
                currentPrimaryCache = STORAGE_CACHE_INTERNAL;
            }
            internalCachePath = getInternalCacheDir();
            this.internalCacheObserver = new FileObserver(internalCachePath,
                    FileObserver.DELETE_SELF | FileObserver.DELETE | FileObserver.CREATE) {
                @Override
                public void onEvent(int event, String s) {
                    try {
                        if (event == FileObserver.DELETE_SELF) {
                            internalCacheObserver = null;
                            internalCachePath = null;
                            onStorageDied(STORAGE_CACHE_INTERNAL);
                        } else if (s != null && event == FileObserver.DELETE) {
                            onFileDeleted(s, STORAGE_CACHE_INTERNAL);
                        } else if (s != null && event == FileObserver.CREATE) {
                            onFileCreated(s, STORAGE_CACHE_INTERNAL);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            };
            this.internalCacheObserver.startWatching();

            onStorageCreated(STORAGE_CACHE_INTERNAL);

            File[] files = new File(internalCachePath).listFiles();
            HashSet<String> cacheFiles = new HashSet<String>();
            for (File f : files) {
                if (!f.isFile()) {
                    continue;
                }
                if (!persistence.isDownloaded(f.getName())) {
                    continue;
                }
                cacheFiles.add(f.getName());
                FileRecord record = fileRecords.get(f.getName());
                if (record != null) {
                    if (record.storageType == STORAGE_UNKNOWN) {
                        record.storageType = STORAGE_CACHE_INTERNAL;
                    }
                } else {
                    fileRecords.put(f.getName(), new FileRecord(STORAGE_CACHE_INTERNAL, f.getName()));
                }
            }

            for (FileRecord record : fileRecords.values().toArray(new FileRecord[0])) {
                if (record.storageType == STORAGE_CACHE_INTERNAL && !cacheFiles.contains(record.fileName)) {
                    record.storageType = STORAGE_UNKNOWN;
                }
            }
        }
    }

    public boolean isDownloaded(String fileName) {
        if (persistence.isDownloaded(fileName)) {
            FileRecord record = fileRecords.get(fileName);
            if (record != null) {
                if (record.storageType != STORAGE_UNKNOWN) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getFullPath(String fileName) {
        FileRecord record = fileRecords.get(fileName);
        int storage = currentPrimaryCache;
        if (record != null) {
            if (record.storageType != STORAGE_UNKNOWN) {
                storage = record.storageType;
            }
        }

        if (storage == STORAGE_CACHE_EXTERNAL) {
            return externalCachePath + "/" + fileName;
        } else {
            return internalCachePath + "/" + fileName;
        }
    }

    public void trackFile(String fileName) {
        int storage = STORAGE_UNKNOWN;
        if (externalCachePath != null) {
            if (new File(externalCachePath + "/" + fileName).exists()) {
                storage = STORAGE_CACHE_EXTERNAL;
            }
        }
        if (internalCachePath != null) {
            if (new File(internalCachePath + "/" + fileName).exists()) {
                storage = STORAGE_CACHE_EXTERNAL;
            }
        }
        persistence.markDownloaded(fileName);
        fileRecords.put(fileName, new FileRecord(storage, fileName));
    }

    public void untrackFile(String fileName) {
        persistence.markUnloaded(fileName);
        fileRecords.remove(fileName);
    }

    private void onStorageCreated(int storage) {
        Logger.d(TAG, "onStorageCreated: " + (storage == STORAGE_CACHE_EXTERNAL ? "external" : "internal"));
    }

    private void onStorageDied(int storage) {
        Logger.d(TAG, "onStorageDied: " + (storage == STORAGE_CACHE_EXTERNAL ? "external" : "internal"));
        for (FileRecord record : fileRecords.values().toArray(new FileRecord[0])) {
            if (record.storageType == storage) {
                record.storageType = STORAGE_UNKNOWN;
            }
        }
        updateCache();
    }

    private void onFileDeleted(String fileName, int storage) {
        Logger.d(TAG, "onFileDeleted: " + fileName + " at " + (storage == 0 ? "external" : "internal"));
        if (!persistence.isDownloaded(fileName)) {
            return;
        }
        FileRecord record = fileRecords.get(fileName);
        if (record != null) {
            if (record.storageType == storage) {
                record.storageType = STORAGE_UNKNOWN;
            }
        }
    }

    private void onFileCreated(String fileName, int storage) {
        Logger.d(TAG, "onFileCreated: " + fileName + " at " + (storage == 0 ? "external" : "internal"));
    }

    private class FileRecord {
        private int storageType;
        private String fileName;

        private FileRecord(int storageType, String fileName) {
            this.storageType = storageType;
            this.fileName = fileName;
        }

        public int getStorageType() {
            return storageType;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
