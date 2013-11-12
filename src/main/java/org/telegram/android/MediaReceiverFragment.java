package org.telegram.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import com.extradea.framework.images.utils.ImageUtils;
import org.telegram.android.video.VideoRecorderActivity;

import java.io.*;
import java.util.List;
import java.util.Random;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 6:25
 */
public class MediaReceiverFragment extends StelsFragment {

    private static final int REQUEST_BASE = 100;

    private String imageFileName;
    private String videoFileName;

    private Random rnd = new Random();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            imageFileName = savedInstanceState.getString("picker:imageFileName");
            videoFileName = savedInstanceState.getString("picker:videoFileName");
        }
    }

    protected String getUploadTempFile() {
        if (Build.VERSION.SDK_INT >= 8) {
            try {
                //return getExternalCacheDir().getAbsolutePath();
                return ((File) StelsApplication.class.getMethod("getExternalCacheDir").invoke(application)).getAbsolutePath() + "/upload_" + rnd.nextLong() + ".jpg";
            } catch (Exception e) {
                // Log.e(TAG, e);
            }
        }

        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/cache/" + application.getPackageName() + "/upload_" + rnd.nextLong() + ".jpg";
    }

    protected String getUploadVideoTempFile() {
        if (Build.VERSION.SDK_INT >= 8) {
            try {
                //return getExternalCacheDir().getAbsolutePath();
                return ((File) StelsApplication.class.getMethod("getExternalCacheDir").invoke(application)).getAbsolutePath() + "/upload_" + rnd.nextLong() + ".mp4";
            } catch (Exception e) {
                // Log.e(TAG, e);
            }
        }

        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/cache/" + application.getPackageName() + "/upload_" + rnd.nextLong() + ".mp4";
    }

    public void requestPhotoChooserWithDelete(final int requestId) {
        new AlertDialog.Builder(getActivity()).setItems(new CharSequence[]{
                getStringSafe(R.string.st_receiver_photo),
                getStringSafe(R.string.st_receiver_gallery),
                getStringSafe(R.string.st_receiver_delete)},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            requestPhotoFromCamera(requestId);
                        } else if (i == 1) {
                            requestPhotoFromGallery(requestId);
                        } else {
                            onPhotoDeleted(requestId);
                        }
                    }
                }).show();
    }

    public void requestPhotoChooser(final int requestId) {
        new AlertDialog.Builder(getActivity()).setItems(new CharSequence[]{
                getStringSafe(R.string.st_receiver_photo),
                getStringSafe(R.string.st_receiver_gallery)},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            requestPhotoFromCamera(requestId);
                        } else {
                            requestPhotoFromGallery(requestId);
                        }
                    }
                }).show();
    }

    public void requestPhotoFromCamera(int requestId) {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            imageFileName = getUploadTempFile();
            File file = new File(imageFileName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            startActivityForResult(intent, requestId * 4 + REQUEST_BASE);
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.st_error_unsupported, Toast.LENGTH_SHORT).show();
        }
    }

    public void requestPhotoFromGallery(int requestId) {
        try {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, requestId * 4 + REQUEST_BASE + 1);
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.st_error_unsupported, Toast.LENGTH_SHORT).show();
        }
    }

    public void requestVideo(int requestId) {
        try {
            videoFileName = getUploadVideoTempFile();
            Intent intent = new Intent();
            intent.setClass(getActivity(), VideoRecorderActivity.class);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(videoFileName)));
            startActivityForResult(intent, requestId * 4 + REQUEST_BASE + 2);
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.st_error_unsupported, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean cropSupported(Uri data) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
        intent.setData(data);
        List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(intent, 0);
        return list.size() != 0;
    }

    public void requestCrop(String fileName, int width, int height, int requestId) {
        requestCrop(Uri.fromFile(new File(fileName)), width, height, requestId);
    }

    public void requestCrop(Uri uri, int width, int height, int requestId) {
        imageFileName = getUploadTempFile();
        File f = new File(imageFileName);
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("outputX", width);
        intent.putExtra("outputY", height);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        startActivityForResult(intent, requestId * 4 + REQUEST_BASE + 3);
    }

    protected void onPhotoArrived(String fileName, int width, int height, int requestId) {

    }

    protected void onPhotoArrived(Uri uri, int width, int height, int requestId) {

    }

    protected void onPhotoCropped(Uri uri, int requestId) {

    }

    protected void onPhotoDeleted(int requestId) {

    }

    protected void onVideoArrived(String fileName, int requestId) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode >= REQUEST_BASE) {
                if (requestCode % 4 == 0) {
                    int width = 0;
                    int height = 0;

                    try {
                        BitmapFactory.Options o = new BitmapFactory.Options();
                        o.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(imageFileName, o);
                        width = o.outWidth;
                        height = o.outHeight;
                    } catch (OutOfMemoryError e) {

                    }

                    if (!ImageUtils.isVerticalImage(imageFileName)) {
                        int tmp = height;
                        height = width;
                        width = tmp;
                    }

                    onPhotoArrived(imageFileName, width, height, (requestCode - REQUEST_BASE) / 4);
                } else if (requestCode % 4 == 1) {
                    if (data == null)
                        return;

                    Uri selectedImageUri = data.getData();

                    int width = 0;
                    int height = 0;

                    try {
                        InputStream stream = getActivity().getContentResolver().openInputStream(selectedImageUri);
                        BitmapFactory.Options o = new BitmapFactory.Options();
                        o.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(stream, new Rect(), o);
                        width = o.outWidth;
                        height = o.outHeight;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    if (!ImageUtils.isVerticalImage(selectedImageUri, getActivity())) {
                        int tmp = height;
                        height = width;
                        width = tmp;
                    }

                    if (selectedImageUri.getScheme().equals("file")) {
                        onPhotoArrived(selectedImageUri.getPath(), width, height, (requestCode - REQUEST_BASE) / 4);
                    } else {
                        onPhotoArrived(selectedImageUri, width, height, (requestCode - REQUEST_BASE) / 4);
                    }
                } else if (requestCode % 4 == 2) {
                    onVideoArrived(videoFileName, (requestCode - REQUEST_BASE) / 4);
                } else if (requestCode % 4 == 3) {
                    /*Uri selectedImageUri = data.getData();
                    if (selectedImageUri == null) {

                    }*/
                    onPhotoCropped(Uri.fromFile(new File(imageFileName)), (requestCode - REQUEST_BASE) / 4);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("picker:imageFileName", imageFileName);
        outState.putString("picker:videoFileName", videoFileName);
    }
}
