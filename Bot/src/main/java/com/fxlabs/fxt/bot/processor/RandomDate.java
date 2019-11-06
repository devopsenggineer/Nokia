package com.fxlabs.fxt.bot.processor;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Intesar Shannan Mohammed
 */
public class RandomDate {

    private static int createRandomIntBetween(int start, int end) {
        return start + (int) Math.round(Math.random() * (end - start));
    }

    public static Date createRandomDate(int startYear, int endYear) {
        int day = createRandomIntBetween(1, 28);
        int month = createRandomIntBetween(1, 12);
        int year = createRandomIntBetween(startYear, endYear);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, year);
        calendar.add(Calendar.MONTH, month);
        calendar.add(Calendar.DAY_OF_MONTH, day);

        return calendar.getTime();
    }
}

