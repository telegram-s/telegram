package org.telegram.android.core.model.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import org.telegram.android.media.Optimizer;
import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.*;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 14.12.13.
 */
public class TLUploadingDocument extends TLObject {

    public static final int KIND_GENERAL = 0;
    public static final int KIND_PHOTO = 1;
    public static final int KIND_GIF = 2;

    public static final int CLASS_ID = 0x45dfcbb9;

    private String fileUri;
    private String fileName;
    private String filePath;
    private int fileSize;
    private int kind;
    private String mimeType;
    private int fullPreviewW;
    private int fullPreviewH;

    public TLUploadingDocument(String filePath) {
        this.filePath = filePath;
        this.fileUri = "";
        File file = new File(filePath);
        this.fileName = file.getName();
        this.fileSize = (int) file.length();

        this.mimeType = "";
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, o);
        if (o.outWidth > 0 && o.outHeight > 0) {
            fullPreviewW = o.outWidth;
            fullPreviewH = o.outHeight;
            if (o.outMimeType != null && o.outMimeType.length() > 0) {
                mimeType = o.outMimeType;
            }
            if ("image/gif".equals(o.outMimeType)) {
                kind = KIND_GIF;
            } else {
                kind = KIND_PHOTO;
            }
        } else {
            kind = KIND_GENERAL;
        }

        if (mimeType.length() == 0) {
            String ext = null;
            if (fileName.indexOf('.') > -1) {
                ext = fileName.substring(fileName.lastIndexOf('.') + 1);
            }

            if (ext != null && ext.length() > 0) {
                String nMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                if (nMimeType != null) {
                    mimeType = nMimeType;
                }
            }
        }

        if (mimeType.length() == 0) {
            mimeType = "application/octet-stream";
        }
    }

    public TLUploadingDocument(String uri, Context context) {
        this.fileUri = uri;
        this.filePath = "";

        String realPath = getRealPathFromURI(Uri.parse(fileUri), context);
        if (realPath != null) {
            File file = new File(realPath);
            this.fileName = file.getName();
            this.fileSize = (int) file.length();
        } else {
            this.fileName = "file.bin";
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(Uri.parse(fileUri));
                this.fileSize = inputStream.available();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.mimeType = "";
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(Uri.parse(fileUri));
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (o.outWidth > 0 && o.outHeight > 0) {
            fullPreviewW = o.outWidth;
            fullPreviewH = o.outHeight;
            if (o.outMimeType != null && o.outMimeType.length() > 0) {
                mimeType = o.outMimeType;
            }
            if ("image/gif".equals(o.outMimeType)) {
                kind = KIND_GIF;
            } else {
                kind = KIND_PHOTO;
            }
        } else {
            kind = KIND_GENERAL;
        }

        if (mimeType.length() == 0) {
            String ext = null;
            if (fileName.indexOf('.') > -1) {
                ext = fileName.substring(fileName.lastIndexOf('.') + 1);
            }

            if (ext != null && ext.length() > 0) {
                String nMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                if (nMimeType != null) {
                    mimeType = nMimeType;
                }
            }
        }

        if (mimeType.length() == 0) {
            mimeType = "application/octet-stream";
        }
    }

    public TLUploadingDocument() {
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getFileSize() {
        return fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getKind() {
        return kind;
    }

    public int getFullPreviewW() {
        return fullPreviewW;
    }

    public int getFullPreviewH() {
        return fullPreviewH;
    }

    public String getFileUri() {
        return fileUri;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLString(filePath, stream);
        writeTLString(fileUri, stream);
        writeTLString(fileName, stream);
        writeInt(fileSize, stream);
        writeTLString(mimeType, stream);
        writeInt(kind, stream);
        writeInt(fullPreviewW, stream);
        writeInt(fullPreviewH, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        filePath = readTLString(stream);
        fileUri = readTLString(stream);
        fileName = readTLString(stream);
        fileSize = readInt(stream);
        mimeType = readTLString(stream);
        kind = readInt(stream);
        fullPreviewW = readInt(stream);
        fullPreviewH = readInt(stream);
    }

    private static String getRealPathFromURI(Uri contentUri, Context context) {
        if ("file".equals(contentUri.getScheme())) {
            return contentUri.getPath();
        } else {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor == null) {
                return null;
            }
            if (!cursor.moveToFirst()) {
                return null;
            }
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            return cursor.getString(column_index);
        }
    }
}
