package org.telegram.android.ui;

import android.content.Context;
import android.text.format.DateFormat;
import org.telegram.android.R;
import org.telegram.i18n.I18nUtil;
import org.telegram.i18n.UserListFormatter;
import org.telegram.mtproto.time.TimeOverlord;
import org.telegram.phones.PhoneFormat;

import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 24.05.12
 * Time: 1:24
 */
public class TextUtil {
    private static ThreadLocal<Calendar> calendarThreadLocal = new ThreadLocal<Calendar>();

    private static final long HOURS_DELTA = 12 * 60 * 60 * 1000;//For last 24 hours

    public static String formatHumanReadableLastSeen(int lastSeen, String locale) {
        if (lastSeen == 0)
            return "";

        int delta = (int) (TimeOverlord.getInstance().getServerTime() / 1000 - lastSeen);//Secs
        if (delta < 60) {
            return context.getString(R.string.lang_common_online_templates_now)
                    .replace("{time}", context.getString(R.string.lang_common_online_now));
        } else if (delta < 60 * 60) {
            int minutes = delta / 60;
            return context.getString(R.string.lang_common_online_templates_time)
                    .replace("{time}",
                            context.getString(R.string.lang_common_online_ago)
                                    .replace("{time}",
                                            I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_minutes, minutes)));
        } else if (delta < 24 * 60 * 60) {
            int hours = delta / (60 * 60);
            return context.getString(R.string.lang_common_online_templates_time)
                    .replace("{time}",
                            context.getString(R.string.lang_common_online_ago)
                                    .replace("{time}",
                                            I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_hours, hours)));
        } else {
            Date date = new Date(lastSeen * 1000L);
            return context.getString(R.string.lang_common_online_templates_date)
                    .replace("{time}", I18nUtil.getInstance().formatMonthShort(date.getDate(), date.getMonth()));
        }
    }

    public static String formatHumanReadableDuration(int duration) {
        if (duration < 60) {
            return I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_seconds, duration);
        } else if (duration < 60 * 60) {
            return I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_minutes, duration / 60);
        } else if (duration < 24 * 60 * 60) {
            return I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_hours, duration / 3600);
        } else if (duration < 7 * 24 * 60 * 60) {
            return I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_days, duration / (3600 * 24));
        } else {
            return I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_week, duration / (3600 * 24 * 7));
        }
    }

    public static String formatDuration(int duration) {
        if (duration < 60) {
            return I18nUtil.getInstance().correctFormatTwoDigit(0) + ":" + I18nUtil.getInstance().correctFormatTwoDigit(duration);
        } else if (duration < 60 * 60) {
            return I18nUtil.getInstance().correctFormatTwoDigit(duration / 60) + ":" + I18nUtil.getInstance().correctFormatTwoDigit(duration % 60);
        } else {
            return I18nUtil.getInstance().correctFormatTwoDigit(duration / 3600) + ":" + I18nUtil.getInstance().correctFormatTwoDigit(duration / 60) + ":" + I18nUtil.getInstance().correctFormatTwoDigit(duration % 60);
        }
    }

    public static String formatDate(long time, Context context) {
        Calendar calendar = calendarThreadLocal.get();
        if (calendar == null) {
            calendar = Calendar.getInstance();
            calendarThreadLocal.set(calendar);
        }
        calendar.setTimeInMillis(time * 1000);
        if (System.currentTimeMillis() - calendar.getTimeInMillis() < HOURS_DELTA) {
            if (DateFormat.is24HourFormat(context)) {
                return String.format("%s:%s",
                        I18nUtil.getInstance().correctFormatTwoDigit(calendar.get(Calendar.HOUR_OF_DAY)),
                        I18nUtil.getInstance().correctFormatTwoDigit(calendar.get(Calendar.MINUTE)));
            } else {
                int hour = calendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
                    return String.format("%s:%s AM",
                            I18nUtil.getInstance().correctFormatTwoDigit(hour),
                            I18nUtil.getInstance().correctFormatTwoDigit(calendar.get(Calendar.MINUTE)));
                } else {
                    return String.format("%s:%s PM",
                            I18nUtil.getInstance().correctFormatTwoDigit(hour),
                            I18nUtil.getInstance().correctFormatTwoDigit(calendar.get(Calendar.MINUTE)));
                }
            }
        } else {
            return I18nUtil.getInstance().formatMonthShort(calendar.get(Calendar.DATE), calendar.get(Calendar.MONTH));
        }
    }

    public static boolean areSameDays(long time1, long time2) {
        return (time1 / (60 * 60 * 24)) == (time2 / (60 * 60 * 24));
    }

    public static String formatDateLong(long time) {
        Calendar calendar = calendarThreadLocal.get();
        if (calendar == null) {
            calendar = Calendar.getInstance();
            calendarThreadLocal.set(calendar);
        }
        calendar.setTimeInMillis(time * 1000);

        return I18nUtil.getInstance()
                .formatMonth(calendar.get(Calendar.DATE), calendar.get(Calendar.MONTH));
    }

    public static String formatTime(long time, Context context) {
        Calendar calendar = calendarThreadLocal.get();
        if (calendar == null) {
            calendar = Calendar.getInstance();
            calendarThreadLocal.set(calendar);
        }
        calendar.setTimeInMillis(time * 1000);
        if (DateFormat.is24HourFormat(context)) {
            return String.format("%s:%s",
                    I18nUtil.getInstance().correctFormatTwoDigit(calendar.get(Calendar.HOUR_OF_DAY)),
                    I18nUtil.getInstance().correctFormatTwoDigit(calendar.get(Calendar.MINUTE)));
        } else {
            int hour = calendar.get(Calendar.HOUR);
            if (hour == 0) {
                hour = 12;
            }
            if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
                return String.format("%s:%s AM",
                        I18nUtil.getInstance().correctFormatTwoDigit(hour),
                        I18nUtil.getInstance().correctFormatTwoDigit(calendar.get(Calendar.MINUTE)));
            } else {
                return String.format("%s:%s PM",
                        I18nUtil.getInstance().correctFormatTwoDigit(hour),
                        I18nUtil.getInstance().correctFormatTwoDigit(calendar.get(Calendar.MINUTE)));
            }
        }
    }

    public static String formatDistance(float distance) {
        if (distance < 1) {
            return "nearby";
        } else if (distance < 1000) {
            return (int) distance + " m away";
        } else {
            return (int) (distance / 1000) + " km away";
        }
    }

    public static String formatFileSize(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return (bytes / (1024 * 1024)) + " MB";
        } else {
            return (bytes / (1024 * 1024 * 1024)) + " GB";
        }
    }

    public static String formatTyping(String[] names) {
        if (names.length >= 3) {
            String[] truncated = new String[2];
            truncated[0] = names[0];
            truncated[1] = names[1];
            return context.getString(R.string.lang_common_typing_multiple)
                    .replace("{content}", UserListFormatter.formatNamesAndMore(context.getString(R.string.st_lang), truncated, names.length - 2));
        } else {
            if (names.length == 1) {
                return context.getString(R.string.lang_common_typing_single)
                        .replace("{content}", UserListFormatter.formatNamesAndMore(context.getString(R.string.st_lang), names, 0));
            } else {
                return context.getString(R.string.lang_common_typing_multiple)
                        .replace("{content}", UserListFormatter.formatNamesAndMore(context.getString(R.string.st_lang), names, 0));
            }
        }
    }

    private static PhoneFormat phoneFormat;
    private static Context context;

    public static void init(Context context) {
        if (phoneFormat != null) {
            return;
        }
        TextUtil.context = context;
        phoneFormat = new PhoneFormat(context);
    }

    public static String formatPhone(String src) {
        try {
            return "+" + phoneFormat.format(src);
        } catch (Exception e) {
            e.printStackTrace();
            return "+" + src;
        }
    }
}