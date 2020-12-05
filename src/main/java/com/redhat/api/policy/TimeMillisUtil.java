package com.redhat.api.policy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

public class TimeMillisUtil {
    public static void main(String[] args) {
        long system_date_millis = 0;
        LocalDateTime triggerTime = null;

        system_date_millis = System.currentTimeMillis();
        triggerTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(system_date_millis),
                    TimeZone.getDefault().toZoneId());
        System.out.println("current-time");
        System.out.println(system_date_millis);
        System.out.println(triggerTime);

        System.out.println();

        system_date_millis = 1607201788021L;
        triggerTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(system_date_millis),
                    TimeZone.getDefault().toZoneId());
        System.out.println("X-RateLimit-Time");
        System.out.println(system_date_millis);
        System.out.println(triggerTime);

        System.out.println();

        system_date_millis = 1607201728021L;
        triggerTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(system_date_millis),
                    TimeZone.getDefault().toZoneId());
        System.out.println("X-RateLimit-Reset");
        System.out.println(system_date_millis);
        System.out.println(triggerTime);

    }
}
