package com.safar.app.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    public static String formatDeparture(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT_DISPLAY, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatShort(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT_SHORT, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String timeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < TimeUnit.MINUTES.toMillis(1)) return "Just now";
        if (diff < TimeUnit.HOURS.toMillis(1)) return (diff / TimeUnit.MINUTES.toMillis(1)) + "m ago";
        if (diff < TimeUnit.DAYS.toMillis(1)) return (diff / TimeUnit.HOURS.toMillis(1)) + "h ago";
        return (diff / TimeUnit.DAYS.toMillis(1)) + "d ago";
    }

    public static String formatChatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static boolean isUpcoming(long departureTime) {
        return departureTime > System.currentTimeMillis();
    }

    private DateUtils() {}
}
