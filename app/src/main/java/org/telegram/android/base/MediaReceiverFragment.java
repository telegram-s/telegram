package org.telegram.android.base;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.widget.Toast;
import org.telegram.android.R;
import org.telegram.android.activity.CropImageActivity;
import org.telegram.android.activity.PickWebImageActivity;
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

    public static final int PICK_NONE = 0;
    public static final int PICK_DELETE = 1;
    public static final int PICK_WEB = 2;
    public static final int PICK_WALLPAPER = 4;
    public static final int PICK_DEFAULT = PICK_WEB;

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
        return getUploadTempFile(".opus");
    }

    protected String getUploadTempFile() {
        return getUploadTempFile(".jpg");
    }

    protected String getUploadVideoTempFile() {
        return getUploadTempFile(".mp4");
    }

    public void requestPhotoChooser(final int requestId, int flags) {
        imageFileName = getTempExternalFile(".jpg");
        ReceiverBuilder builder = new ReceiverBuilder()
                .setTitle(R.string.st_receiver_pick_photo);

        if ((flags & PICK_WALLPAPER) != 0) {
            if (hasApplication("com.whatsapp") && hasApplication("com.whatsapp.wallpaper")) {
                builder.addIntentSource(new Intent().setClassName("com.whatsapp", "com.whatsapp.wallpaper.WallpaperPicker"), requestId * REQ_M + REQUEST_BASE + 4);
            }
            builder.addCustomPick(R.drawable.app_icon, R.string.st_picker_standart, new Runnable() {
                @Override
                public void run() {
                    getRootController().openWallpaperSettings();
                }
            });
            builder.setTitle(R.string.st_receiver_pick_wallpaper);
        }

        builder.addPhotoCapturePick(imageFileName, requestId * REQ_M + REQUEST_BASE)
                .addPhotoPick(requestId * REQ_M + REQUEST_BASE + 1);

        if ((flags & PICK_WEB) != 0) {
            builder.addIntentSource(new Intent().setClass(getActivity(), PickWebImageActivity.class), requestId * REQ_M + REQUEST_BASE + 1);
        }
        if ((flags & PICK_DELETE) != 0) {
            builder.addCustomPick(R.drawable.holo_light_ic_delete, R.string.st_picker_delete, new Runnable() {
                @Override
                public void run() {
                    onPhotoDeleted(requestId);
                }
            });
            builder.setTitle(R.string.st_receiver_edit_photo);
        }

        builder.show();
    }

    public void requestWebImage(final int requestId) {
        startActivityForResult(new Intent().setClass(getActivity(), PickWebImageActivity.class), requestId * REQ_M + REQUEST_BASE + 1);
    }

    public void requestVideo(final int requestId) {
        try {
            videoFileName = getTempExternalFile(".mp4");
            new ReceiverBuilder()
                    .setTitle(R.string.st_receiver_pick_video)
                    .addVideoCapture(videoFileName, requestId * REQ_M + REQUEST_BASE + 2)
                    .addVideoPick(requestId * REQ_M + REQUEST_BASE + 5)
                    .show();
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

    private class ReceiverBuilder {
        private String title = getStringSafe(R.string.st_picker_perform_with);
        private ArrayList<PickIntentItem> items = new ArrayList<PickIntentItem>();

        public ReceiverBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public ReceiverBuilder setTitle(int resId) {
            this.title = getStringSafe(resId);
            return this;
        }

        public ReceiverBuilder addIntentSource(Intent intent, ItemHandler runnable) {
            PickIntentItem[] pickIntentItems = createPickIntents(intent);
            for (PickIntentItem intentItem : pickIntentItems) {
                intentItem.setTag(runnable);
            }
            Collections.addAll(items, pickIntentItems);
            return this;
        }

        public ReceiverBuilder addIntentSource(Intent intent, final int requestId) {
            ItemHandler handler = new ItemHandler() {
                @Override
                public void onSelected(PickIntentItem item) {
                    startActivityForResult(item.getIntent(), requestId);
                }
            };

            return addIntentSource(intent, handler);
        }

        public ReceiverBuilder addIntentSource(Intent intent) {
            ItemHandler handler = new ItemHandler() {
                @Override
                public void onSelected(PickIntentItem item) {
                    startActivity(item.getIntent());
                }
            };

            return addIntentSource(intent, handler);
        }

        public ReceiverBuilder addVideoCapture(final String fileName, final int requestId) {
            return addIntentSource(new Intent(MediaStore.ACTION_VIDEO_CAPTURE), new ItemHandler() {
                @Override
                public void onSelected(PickIntentItem item) {
                    startActivityForResult(
                            item.getIntent()
                                    .putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(fileName))),
                            requestId
                    );
                }
            });
        }

        public ReceiverBuilder addVideoPick(final int requestId) {
            return addIntentSource(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"), requestId);
        }

        public ReceiverBuilder addPhotoCapturePick(final String fileName, final int requestId) {
            return addIntentSource(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), new ItemHandler() {
                @Override
                public void onSelected(PickIntentItem item) {
                    startActivityForResult(item.getIntent().putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(fileName))),
                            requestId);
                }
            });
        }

        public ReceiverBuilder addPhotoPick(final int requestId) {
            return addIntentSource(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), requestId);
        }

        public ReceiverBuilder addCustomPick(int icon, int title, final Runnable runnable) {
            PickIntentItem intentItem = new PickIntentItem(icon, getStringSafe(title));
            intentItem.setTag(new ItemHandler() {
                @Override
                public void onSelected(PickIntentItem item) {
                    runnable.run();
                }
            });
            items.add(intentItem);
            return this;
        }

        public PickIntentDialog build() {
            PickIntentDialog res = new PickIntentDialog(getActivity(), items.toArray(new PickIntentItem[0]), false, new PickIntentClickListener() {
                @Override
                public void onItemClicked(int index, PickIntentItem item, boolean isUseAlways) {
                    ((ItemHandler) item.getTag()).onSelected(item);
                }
            });
            res.setTitle(title);
            return res;
        }

        public PickIntentDialog show() {
            PickIntentDialog res = build();
            res.show();
            return res;
        }
    }

    private interface ItemHandler {
        public void onSelected(PickIntentItem item);
    }
}
