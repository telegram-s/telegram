package org.telegram.android.base;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import org.telegram.android.R;
import org.telegram.android.activity.CropImageActivity;
import org.telegram.android.media.Optimizer;
import org.telegram.android.ui.pick.PickIntentClickListener;
import org.telegram.android.ui.pick.PickIntentDialog;
import org.telegram.android.ui.pick.PickIntentItem;
import org.telegram.android.video.VideoRecorderActivity;

import java.io.*;
import java.util.*;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 6:25
 */
public class MediaReceiverFragment extends TelegramFragment {

    private static final int REQ_M = 7;

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

    protected String getUploadTempAudioFile() {
        return getUploadTempFile(".m4a");
    }

    protected String getUploadTempFile() {
        return getUploadTempFile(".jpg");
    }

    protected String getUploadVideoTempFile() {
        return getUploadTempFile(".mp4");
    }

    public void requestPhotoChooserWithDelete(final int requestId) {
        imageFileName = getTempExternalFile(".jpg");
        final Uri fileUri = Uri.fromFile(new File(imageFileName));

        ArrayList<PickIntentItem> items = new ArrayList<PickIntentItem>();
        Collections.addAll(items, createPickIntents(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));
        Collections.addAll(items, createPickIntents(new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")));
        items.add(new PickIntentItem(R.drawable.holo_light_ic_delete, "Delete"));

        PickIntentDialog dialog = new PickIntentDialog(getActivity(),
                items.toArray(new PickIntentItem[items.size()]),
                secure(new PickIntentClickListener() {
                    @Override
                    public void onItemClicked(int index, PickIntentItem item) {
                        if (item.getIntent() == null) {
                            onPhotoDeleted(requestId);
                        } else {
                            if (MediaStore.ACTION_IMAGE_CAPTURE.equals(item.getIntent().getAction())) {
                                startActivityForResult(item.getIntent().putExtra(MediaStore.EXTRA_OUTPUT, fileUri),
                                        requestId * REQ_M + REQUEST_BASE);
                            } else if (Intent.ACTION_GET_CONTENT.equals(item.getIntent().getAction())) {
                                startActivityForResult(item.getIntent(), requestId * REQ_M + REQUEST_BASE + 1);
                            } else {
                                startActivityForResult(item.getIntent(), requestId * REQ_M + REQUEST_BASE + 4);
                            }
                        }
                    }
                }));
        dialog.setTitle("Edit photo");
        dialog.show();
    }

    public void requestWallpaperChooser(final int requestId) {
        imageFileName = getTempExternalFile(".jpg");
        final Uri fileUri = Uri.fromFile(new File(imageFileName));

        ArrayList<PickIntentItem> items = new ArrayList<PickIntentItem>();
        items.add(new PickIntentItem(R.drawable.app_icon, "Built-In").setTag("built-in"));
        items.add(new PickIntentItem(R.drawable.app_icon, "Default").setTag("default"));
        items.add(new PickIntentItem(R.drawable.holo_light_ic_delete, "No Wallpaper").setTag("empty"));
        if (hasApplication("com.whatsapp") && hasApplication("com.whatsapp.wallpaper")) {
            Collections.addAll(items, createPickIntents(new Intent().setClassName("com.whatsapp", "com.whatsapp.wallpaper.WallpaperPicker")));
        }
        Collections.addAll(items, createPickIntents(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));
        Collections.addAll(items, createPickIntents(new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")));


        PickIntentDialog dialog = new PickIntentDialog(getActivity(),
                items.toArray(new PickIntentItem[items.size()]),
                secure(new PickIntentClickListener() {
                    @Override
                    public void onItemClicked(int index, PickIntentItem item) {
                        if ("empty".equals(item.getTag())) {
                            application.getUserSettings().setWallpaperSet(true);
                            application.getUserSettings().setWallpaperSolid(true);
                            application.getUserSettings().setCurrentWallpaperId(0);
                            application.getUserSettings().setCurrentWallpaperSolidColor(0xffdae8f3);
                            application.getWallpaperHolder().dropCache();
                        } else if ("default".equals(item.getTag())) {
                            application.getUserSettings().setWallpaperSet(false);
                            application.getWallpaperHolder().dropCache();
                        } else if ("built-in".equals(item.getTag())) {
                            getRootController().openWallpaperSettings();
                        } else if (item.getIntent() == null) {
                            onPhotoDeleted(requestId);
                        } else {
                            if (MediaStore.ACTION_IMAGE_CAPTURE.equals(item.getIntent().getAction())) {
                                startActivityForResult(item.getIntent().putExtra(MediaStore.EXTRA_OUTPUT, fileUri),
                                        requestId * REQ_M + REQUEST_BASE);
                            } else if (Intent.ACTION_GET_CONTENT.equals(item.getIntent().getAction())) {
                                startActivityForResult(item.getIntent(), requestId * REQ_M + REQUEST_BASE + 1);
                            } else {
                                startActivityForResult(item.getIntent(), requestId * REQ_M + REQUEST_BASE + 4);
                            }
                        }
                    }
                }));
        dialog.setTitle(getStringSafe(R.string.st_receiver_pick_wallpaper));
        dialog.show();
    }

    public void requestPhotoChooser(final int requestId) {
        imageFileName = getTempExternalFile(".jpg");
        final Uri fileUri = Uri.fromFile(new File(imageFileName));

        ArrayList<PickIntentItem> items = new ArrayList<PickIntentItem>();
        Collections.addAll(items, createPickIntents(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));
        Collections.addAll(items, createPickIntents(new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")));

        PickIntentDialog pickIntentDialog = new PickIntentDialog(getActivity(),
                items.toArray(new PickIntentItem[items.size()]),
                secure(new PickIntentClickListener() {
                    @Override
                    public void onItemClicked(int index, PickIntentItem item) {
                        if (MediaStore.ACTION_IMAGE_CAPTURE.equals(item.getIntent().getAction())) {
                            startActivityForResult(item.getIntent().putExtra(MediaStore.EXTRA_OUTPUT, fileUri),
                                    requestId * REQ_M + REQUEST_BASE);
                        } else {
                            startActivityForResult(item.getIntent(), requestId * REQ_M + REQUEST_BASE + 1);
                        }
                    }
                }));
        pickIntentDialog.setTitle(getStringSafe(R.string.st_receiver_pick_photo));
        pickIntentDialog.show();
    }

    public void requestVideo(final int requestId) {
        try {
            videoFileName = getTempExternalFile(".mp4");

            final Uri fileUri = Uri.fromFile(new File(videoFileName));

            ArrayList<PickIntentItem> items = new ArrayList<PickIntentItem>();
            Collections.addAll(items, createPickIntents(new Intent(MediaStore.ACTION_VIDEO_CAPTURE)));
            Collections.addAll(items, createPickIntents(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*")));
            items.add(new PickIntentItem(R.drawable.app_icon, "Built-in [alpha]"));

            PickIntentDialog pickIntentDialog = new PickIntentDialog(getActivity(),
                    items.toArray(new PickIntentItem[items.size()]),
                    secure(new PickIntentClickListener() {
                        @Override
                        public void onItemClicked(int index, PickIntentItem item) {
                            if (item.getIntent() != null) {
                                if (MediaStore.ACTION_VIDEO_CAPTURE.equals(item.getIntent().getAction())) {
                                    startActivityForResult(item.getIntent().putExtra(MediaStore.EXTRA_OUTPUT, fileUri),
                                            requestId * REQ_M + REQUEST_BASE + 2);
                                } else {
                                    startActivityForResult(item.getIntent(), requestId * REQ_M + REQUEST_BASE + 5);
                                }
                            } else {
                                startActivityForResult(new Intent()
                                        .putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                                        .setClass(application, VideoRecorderActivity.class),
                                        requestId * REQ_M + REQUEST_BASE + 2);
                            }
                        }
                    }));
            pickIntentDialog.setTitle(getStringSafe(R.string.st_receiver_pick_video));
            pickIntentDialog.show();
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.st_error_unsupported, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean cropSupported(Uri data) {
        return true;
    }

    public void requestCrop(String fileName, int width, int height, int requestId) {
        try {
            imageFileName = getUploadTempFile();
            String cropFileName = getUploadTempFile();
            Optimizer.optimize(fileName, cropFileName);
            Intent intent = CropImageActivity.cropIntent(cropFileName, 1, 1, imageFileName, getActivity());
            startActivityForResult(intent, requestId * REQ_M + REQUEST_BASE + 3);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestCrop(Uri uri, int width, int height, int requestId) {
        try {
            imageFileName = getUploadTempFile();
            String cropFileName = getUploadTempFile();
            Optimizer.optimize(uri.toString(), application, cropFileName);
            Intent intent = CropImageActivity.cropIntent(cropFileName, 1, 1, imageFileName, getActivity());
            startActivityForResult(intent, requestId * REQ_M + REQUEST_BASE + 3);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    protected void onVideoArrived(Uri uri, int requestId) {

    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode >= REQUEST_BASE) {
                final int originalRequest = (requestCode - REQUEST_BASE) / REQ_M;
                final int internalRequest = requestCode - REQUEST_BASE - originalRequest * REQ_M;

                if (internalRequest == 0) {
                    if (imageFileName == null) {
                        return;
                    }

                    int width = 0;
                    int height = 0;

                    try {
                        Optimizer.BitmapInfo info = Optimizer.getInfo(imageFileName);
                        width = info.getWidth();
                        height = info.getHeight();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    final int finalWidth = width;
                    final int finalHeight = height;
                    secureCallback(new Runnable() {
                        @Override
                        public void run() {
                            onPhotoArrived(imageFileName, finalWidth, finalHeight, originalRequest);
                        }
                    });
                } else if (internalRequest == 1) {
                    if (data == null || data.getData() == null || data.getData().getPath() == null)
                        return;

                    final Uri selectedImageUri = data.getData();

                    int width = 0;
                    int height = 0;

                    try {
                        Optimizer.BitmapInfo info = Optimizer.getInfo(selectedImageUri, application);
                        width = info.getWidth();
                        height = info.getHeight();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    final int finalWidth = width;
                    final int finalHeight = height;
                    secureCallback(new Runnable() {
                        @Override
                        public void run() {
                            if (selectedImageUri.getScheme().equals("file")) {
                                onPhotoArrived(selectedImageUri.getPath(), finalWidth, finalHeight, originalRequest);
                            } else {
                                onPhotoArrived(selectedImageUri, finalWidth, finalHeight, originalRequest);
                            }
                        }
                    });
                } else if (internalRequest == 2) {
                    if (videoFileName == null) {
                        return;
                    }
                    secureCallback(new Runnable() {
                        @Override
                        public void run() {
                            onVideoArrived(videoFileName, originalRequest);
                        }
                    });
                } else if (internalRequest == 3) {
                    if (imageFileName == null) {
                        return;
                    }
                    secureCallback(new Runnable() {
                        @Override
                        public void run() {
                            onPhotoCropped(Uri.fromFile(new File(imageFileName)), originalRequest);
                        }
                    });
                } else if (internalRequest == 4) {
                    try {
                        Integer resourceId = data.getExtras().getInt("redId");
                        BitmapDrawable drawable = (BitmapDrawable) application.getPackageManager().getResourcesForApplication("com.whatsapp.wallpaper").getDrawable(resourceId);
                        Bitmap bitmap = drawable.getBitmap();
                        String fileName = getUploadTempFile();
                        FileOutputStream outputStream = new FileOutputStream(fileName);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 87, outputStream);
                        outputStream.close();
                        onPhotoArrived(fileName, bitmap.getWidth(), bitmap.getHeight(), originalRequest);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (internalRequest == 5) {
                    if (data == null || data.getData() == null || data.getData().getPath() == null)
                        return;

                    final Uri videoUri = data.getData();

                    secureCallback(new Runnable() {
                        @Override
                        public void run() {
                            onVideoArrived(videoUri, originalRequest);
                        }
                    });
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
