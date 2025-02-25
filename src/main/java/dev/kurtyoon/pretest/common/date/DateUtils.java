package dev.kurtyoon.pretest.common.date;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static final DateTimeFormatter ISODateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String convertLocalDateTimeToString(LocalDateTime date) {
        return date.format(ISODateTimeFormatter);
    }
}
