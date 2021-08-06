package com.tomspizza.k8api.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static String getAge(String time) {
        if (time == null) {
            return null;
        }
        LocalDateTime utcTime = LocalDateTime.now(ZoneOffset.UTC);
        return getAge(time, utcTime);
    }

    public static String getAge(String time, LocalDateTime other) {
        LocalDateTime start = LocalDateTime.parse(time, dtf);
        Duration duration = Duration.between(start, other);
        if (duration.toDays() > 0L) {
            return duration.toDays() + "d";
        }
        if (duration.toHours() > 0L) {
            return duration.toHours() + "h";
        }
        return duration.toMinutes() + "m";
    }
}
