package org.telegram.android.tasks;

import org.telegram.android.R;
import org.telegram.android.StelsApplication;
import org.telegram.api.engine.RpcException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: Korshakov Stepan
 * Created: 25.07.13 2:05
 */
public class AsyncException extends Exception {

    public static enum ExceptionType {
        CONNECTION_ERROR, UNKNOWN_ERROR, INTERNAL_SERVER_ERROR
    }

    private static StelsApplication application;

    public static void initLocalisation(StelsApplication application) {
        AsyncException.application = application;
    }

    private static String getErrorMessage(RpcException ex) {

        String errorType = ex.getErrorTag();

        if (errorType.equals("PHONE_CODE_EMPTY")) {
            if (application != null) {
                return application.getString(R.string.st_error_code_empty);
            }
            return "Empty activation code";
        }

        if (errorType.equals("PHONE_CODE_INVALID")) {
            if (application != null) {
                return application.getString(R.string.st_error_code_incorrect);
            }
            return "Invalid activation code";
        }

        if (errorType.equals("FIRSTNAME_INVALID")) {
            if (application != null) {
                return application.getString(R.string.st_error_first_name_incorrect);
            }
            return "Incorrect first name.";
        }

        if (errorType.equals("LASTNAME_INVALID")) {
            if (application != null) {
                return application.getString(R.string.st_error_last_name_incorrect);
            }
            return "Incorrect last name.";
        }

        if (errorType.equals("PHONE_NUMBER_INVALID")) {
            if (application != null) {
                return application.getString(R.string.st_error_invalid_phone);
            }
            return "Incorrect phone number.";
        }

        if (errorType.equals("USERS_TOO_FEW")) {
            if (application != null) {
                return application.getString(R.string.st_error_too_few_users);
            }
            return "Too few users.";
        }

        if (errorType.equals("USERS_TOO_MUCH")) {
            if (application != null) {
                return application.getString(R.string.st_error_too_much_users);
            }
            return "Too much users.";
        }

        if (errorType.equals("NAME_NOT_MODIFIED")) {
            if (application != null) {
                return application.getString(R.string.st_error_name_not_modified);
            }
            return "You doesn't change your name.";
        }

        if (errorType.equals("CHAT_TITLE_NOT_MODIFIED")) {
            if (application != null) {
                return application.getString(R.string.st_error_title_not_modified);
            }
            return "You doesn't change group title.";
        }

        if (errorType.equals("USER_ALREADY_PARTICIPANT")) {
            if (application != null) {
                return application.getString(R.string.st_error_already_participant);
            }
            return "User already group participant.";
        }

        if (errorType.equals("PARTICIPANT_VERSION_OUTDATED")) {
            if (application != null) {
                return application.getString(R.string.st_error_participant_outdated);
            }
            return "User has unsupported version of Telegram.";
        }

        return null;
    }

    private ExceptionType type;
    private boolean repeatable;

    public AsyncException(RpcException ex) {
        this(getErrorMessage(ex));
        this.repeatable = true;
        if (getMessage() == null) {
            this.type = ExceptionType.UNKNOWN_ERROR;
        }
    }

    public AsyncException(String message) {
        super(message);
        this.repeatable = true;
    }

    public AsyncException(String message, Throwable t) {
        super(message, t);
        this.repeatable = true;
    }

    public AsyncException(String message, boolean repeatable) {
        super(message);
        this.repeatable = repeatable;
    }

    public AsyncException(String message, Throwable t, boolean repeatable) {
        super(message, t);
        this.repeatable = repeatable;
    }

    public AsyncException(ExceptionType type) {
        this.type = type;
        this.repeatable = true;
    }

    public AsyncException(ExceptionType type, Throwable t) {
        super(t);
        this.type = type;
        this.repeatable = true;
    }

    public AsyncException(ExceptionType type, boolean repeatable) {
        this.type = type;
        this.repeatable = repeatable;
    }

    public AsyncException(ExceptionType type, Throwable t, boolean repeatable) {
        super(t);
        this.type = type;
        this.repeatable = repeatable;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public ExceptionType getType() {
        return type;
    }
}
