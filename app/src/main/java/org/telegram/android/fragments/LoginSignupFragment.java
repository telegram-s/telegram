package org.telegram.android.fragments;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.extradea.framework.images.tasks.FileSystemImageTask;
import com.extradea.framework.images.tasks.UriImageTask;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.MediaReceiverFragment;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.core.files.UploadResult;
import org.telegram.android.media.Optimizer;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.api.TLAbsUser;
import org.telegram.api.TLInputFile;
import org.telegram.api.TLInputGeoPointEmpty;
import org.telegram.api.TLInputPhotoCropAuto;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.engine.RpcException;
import org.telegram.api.requests.TLRequestAuthSignIn;
import org.telegram.api.requests.TLRequestAuthSignUp;
import org.telegram.api.requests.TLRequestPhotosUploadProfilePhoto;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 2:29
 */
public class LoginSignupFragment extends MediaReceiverFragment {
    private String phoneNumber;
    private String phoneHash;
    private String code;

    private EditText firstName;
    private EditText lastName;

    private FastWebImageView avatar;

    private String photoUri;
    private String photoFile;

    private View progressView;
    private View focus;

    public LoginSignupFragment() {

    }

    public LoginSignupFragment(String phoneNumber, String phoneHash, String code) {
        this.phoneNumber = phoneNumber;
        this.phoneHash = phoneHash;
        this.code = code;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            phoneHash = savedInstanceState.getString("phoneHash");
            phoneNumber = savedInstanceState.getString("phoneNumber");
            code = savedInstanceState.getString("code");
            photoUri = savedInstanceState.getString("photoUri");
            photoFile = savedInstanceState.getString("photoFile");
        }

        View res = inflater.inflate(R.layout.login_signup, container, false);

        firstName = (EditText) res.findViewById(R.id.firstName);
        lastName = (EditText) res.findViewById(R.id.lastName);
        focus = res.findViewById(R.id.focuser);

        fixEditText(firstName);
        fixEditText(lastName);

        progressView = res.findViewById(R.id.progress);

        setDefaultProgressInterface(new ProgressInterface() {
            @Override
            public void showContent() {

            }

            @Override
            public void hideContent() {

            }

            @Override
            public void showProgress() {
                if (progressView != null) {
                    progressView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void hideProgress() {
                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
            }
        });

        avatar = ((FastWebImageView) res.findViewById(R.id.avatar));
        avatar.setLoadingDrawable(getResources().getDrawable(R.drawable.st_user_placeholder));
        avatar.setScaleTypeImage(FastWebImageView.SCALE_TYPE_FIT_CROP);
        avatar.setScaleTypeEmpty(FastWebImageView.SCALE_TYPE_FIT_CROP);

        res.findViewById(R.id.changeAvatarButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (photoFile != null || photoUri != null) {
                    requestPhotoChooserWithDelete(0);
                } else {
                    requestPhotoChooser(0);
                }
            }
        });

        if (photoUri != null) {
            avatar.requestTask(new UriImageTask(photoUri));
        }
        if (photoFile != null) {
            avatar.requestTask(new FileSystemImageTask(photoFile));
        }

        // First time
        if (savedInstanceState == null) {
            try {
                if (Build.VERSION.SDK_INT >= 14) {
                    Cursor c = getActivity().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, new String[]
                            {
                                    "display_name_alt", "photo_uri"
                            }, null, null, null);
                    if (c.moveToFirst()) {
                        String displayNameAlt = c.getString(0);
                        String pUri = c.getString(1);

                        String[] names = displayNameAlt.split(",");
                        String lName = names[0].trim();
                        String fName = names[1].trim();

                        firstName.setText(fName);
                        lastName.setText(lName);
//                        photoUri = pUri;
//
//                        if (photoUri != null) {
//                            avatar.requestTask(new UriImageTask(photoUri));
//                        }
                    }
                    c.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Button nextButton = (Button) res.findViewById(R.id.doNext);
        if (application.isRTL()) {
            nextButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.st_auth_next, 0, 0, 0);
        } else {
            nextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.st_auth_next, 0);
        }

        res.findViewById(R.id.doNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSignup(firstName.getText().toString(),
                        lastName.getText().toString());
            }
        });
        return res;
    }

    @Override
    protected void onPhotoArrived(String fileName, int width, int height, int requestId) {
        if (cropSupported(Uri.fromFile(new File(fileName)))) {
            requestCrop(fileName, 200, 200, 0);
        } else {
            photoFile = fileName;
            photoUri = null;

            avatar.requestTask(new FileSystemImageTask(photoFile));
        }
    }

    @Override
    protected void onPhotoArrived(Uri uri, int width, int height, int requestId) {
        if (cropSupported(uri)) {
            requestCrop(uri, 200, 200, 0);
        } else {
            photoFile = null;
            photoUri = uri.toString();

            avatar.requestTask(new UriImageTask(uri.toString()));
        }
    }

    @Override
    protected void onPhotoCropped(Uri uri, int requestId) {
        photoFile = null;
        photoUri = uri.toString();

        avatar.requestTask(new UriImageTask(uri.toString()));
    }

    @Override
    protected void onPhotoDeleted(int requestId) {
        photoFile = null;
        photoUri = null;

        avatar.requestTask(null);
    }

    public void doSignup(final String firstName2, final String lastName2) {
        hideKeyboard(firstName);
        hideKeyboard(lastName);
        focus.requestFocus();

        runUiTask(new AsyncAction() {
            private boolean expired = false;
            private TLAuthorization authorization;

            @Override
            public void execute() throws AsyncException {
                try {
                    if (authorization == null) {
                        application.prelogin();
                        authorization = rpcRaw(new TLRequestAuthSignUp(phoneNumber, phoneHash, code, firstName2, lastName2));

                        ArrayList<TLAbsUser> user = new ArrayList<TLAbsUser>();
                        user.add(authorization.getUser());
                        application.getEngine().onUsers(user);
                    }
                    if (photoUri != null || photoFile != null) {
                        String destFile = getUploadTempFile();
                        if (photoFile != null) {
                            Optimizer.optimize(photoFile, destFile);
                        } else {
                            Optimizer.optimize(photoUri, application, destFile);
                        }

                        long fileId = new Random().nextLong();
                        File file = new File(destFile);
                        FileInputStream fis = new FileInputStream(file);
                        UploadResult res = application.getUploadController().uploadFile(fis, (int) file.length(), fileId);
                        if (res == null)
                            throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);

                        rpcRaw(new TLRequestPhotosUploadProfilePhoto(new TLInputFile(fileId, res.getPartsCount(), "photo.jpg", res.getHash()),
                                "MyPhoto", new TLInputGeoPointEmpty(), new TLInputPhotoCropAuto()));
                    }
                } catch (RpcException e) {
                    if ("PHONE_NUMBER_OCCUPIED".equals(e.getErrorTag())) {
                        try {
                            authorization = rpcRaw(new TLRequestAuthSignIn(phoneNumber, phoneHash, code));
                        } catch (RpcException e1) {
                            if ("PHONE_CODE_EXPIRED".equals(e.getErrorTag())) {
                                expired = true;
                                return;
                            }
                            e1.printStackTrace();
                            throw new AsyncException(e1);
                        }
                    } else if ("PHONE_CODE_EXPIRED".equals(e.getErrorTag())) {
                        expired = true;
                        return;
                    } else {
                        throw new AsyncException(e);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                } catch (AsyncException e) {
                    if (authorization != null) {
                        throw new AsyncException(getStringSafe(R.string.st_login_signup_avatar_error));
                    } else {
                        throw e;
                    }
                }
            }

            @Override
            public void onCanceled() {
                if (authorization != null) {
                    afterExecute();
                } else {
                    if (isLargeDisplay()) {
                        showKeyboard(firstName);
                    }
                }
            }

            @Override
            public void afterExecute() {
                if (expired) {
                    Toast.makeText(getActivity(), R.string.st_login_code_expired, Toast.LENGTH_SHORT).show();
                    getActivity().onBackPressed();
                } else {
                    ((StartActivity) getActivity()).onSuccessAuth(authorization);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLargeDisplay()) {
            showKeyboard(firstName);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard(firstName);
        hideKeyboard(lastName);
        focus.requestFocus();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("phoneHash", phoneHash);
        outState.putString("phoneNumber", phoneNumber);
        outState.putString("code", code);
        outState.putString("photoUri", photoUri);
        outState.putString("photoFile", photoFile);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        firstName = null;
        lastName = null;
        avatar = null;
        progressView = null;
    }

    private boolean isLargeDisplay() {
        return application.getResources().getDisplayMetrics().heightPixels > getPx(540);
    }
}