package de.mcterranova.guilds.util;

import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;

public class TimeUtil {
    public static long getTicksUntilMidnight() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);

        // Next midnight => plus 1 day, set hour=0, minute=0
        LocalDateTime tomorrowMidnight = now
                .plusDays(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        long targetMillis = tomorrowMidnight.atZone(zone).toInstant().toEpochMilli();
        long nowMillis = System.currentTimeMillis();
        long diffMillis = targetMillis - nowMillis;
        if (diffMillis < 0) diffMillis = 1000; // fallback

        return diffMillis / 50; // 20 ticks/sec => 1s = 1000ms => 50ms = 1 tick
    }

    public static long getTicksUntilNextMonth() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);

        // If it's currently Jan 15, we'll schedule for Feb 1 at 00:00
        // Step 1: figure out the 'first day of next month' in local time
        LocalDate firstDayOfNextMonth = now
                .withDayOfMonth(1)
                .plusMonths(1)  // e.g. if now is Jan 15 => plusMonths(1) => Feb 1
                .toLocalDate();

        // Convert to LocalDateTime at midnight
        LocalDateTime nextMonthMidnight = LocalDateTime.of(firstDayOfNextMonth, LocalTime.MIDNIGHT);
        // Then to epoch millis
        long nextMonthMillis = nextMonthMidnight
                .atZone(zone)
                .toInstant()
                .toEpochMilli();

        long nowMillis = Instant.now().toEpochMilli();
        long diffMillis = nextMonthMillis - nowMillis;
        if (diffMillis < 0) {
            // edge case: if for some reason we're already past that time (server clock issues)
            // schedule 1 tick or handle differently
            diffMillis = 1000;
        }
        // Convert to ticks
        return diffMillis / 50; // 1000ms => 20 ticks, so 1ms => 0.02 ticks
    }


    public static boolean isMoreThanAMonthAgo(long timestamp) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        long now = System.currentTimeMillis();
        cal.setTimeInMillis(timestamp);
        cal.add(Calendar.MONTH,1);
        return cal.getTimeInMillis() < now;
    }
}
