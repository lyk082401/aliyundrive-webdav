package net.sf.webdav.util;

import java.util.Calendar;
import java.util.Date;

public class DateTimeUtils {
    public static Date getCurrentDateGMT() {
        Calendar time = Calendar.getInstance();
        time.add(Calendar.MILLISECOND, -time.getTimeZone().getOffset(time.getTimeInMillis()));
        return time.getTime();
    }

    public static Date convertLocalDateToGMT(Date localDate) {
        Calendar time = Calendar.getInstance();
        time.setTime(localDate);
        time.add(Calendar.MILLISECOND, -time.getTimeZone().getOffset(time.getTimeInMillis()));
        return time.getTime();
    }

    public static Date convertLocalDateToGMT(long timeMillis) {
        return convertLocalDateToGMT(new Date(timeMillis));
    }

    public static Date convertGMTDateToLocal(Date gmtDate) {
        Calendar time = Calendar.getInstance();
        time.setTime(gmtDate);
        time.add(Calendar.MILLISECOND, +time.getTimeZone().getOffset(time.getTimeInMillis()));
        return time.getTime();
    }
}
