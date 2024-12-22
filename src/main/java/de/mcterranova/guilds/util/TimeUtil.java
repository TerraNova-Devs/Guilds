package de.mcterranova.guilds.util;

import java.util.Calendar;
import java.util.TimeZone;

public class TimeUtil {
    public static long getTicksUntilMidnight() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        long now = System.currentTimeMillis();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        long diff = cal.getTimeInMillis() - now;
        return diff / 50;
    }

    public static long getTicksUntilMonthEnd() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        long now = System.currentTimeMillis();
        cal.add(Calendar.MONTH,1);
        cal.set(Calendar.DAY_OF_MONTH,1);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        long diff = cal.getTimeInMillis() - now;
        return diff / 50;
    }
}
