package com.aware.plugin.upmc.dash.utils;

import java.util.Calendar;


public class TimeStuff {

    public static void compareMinutes(int m1, int m2) {
        if (m1 >= m2) {
            System.out.println("Worked at minute level");
        }
    }

    public static void main(String[] args) {
        System.out.println("Hello World");
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 23);
        now.set(Calendar.MINUTE, 0);
        Calendar morningTime = Calendar.getInstance();
        Calendar nightTime = Calendar.getInstance();

        morningTime.set(Calendar.HOUR_OF_DAY, 5);
        morningTime.set(Calendar.MINUTE, 0);
        nightTime.set(Calendar.HOUR_OF_DAY, 23);
        nightTime.set(Calendar.MINUTE, 0);

        System.out.println("Now: " + now.get(Calendar.HOUR_OF_DAY) + " " + now.get(Calendar.MINUTE));
        System.out.println("Morn: " + morningTime.get(Calendar.HOUR_OF_DAY) + " " + morningTime.get(Calendar.MINUTE));
        System.out.println("Night: " + nightTime.get(Calendar.HOUR_OF_DAY) + " " + nightTime.get(Calendar.MINUTE));

        int now_hour = now.get(Calendar.HOUR_OF_DAY);
        int now_minute = now.get(Calendar.MINUTE);

        int morn_hour = morningTime.get(Calendar.HOUR_OF_DAY);
        int morn_minute = morningTime.get(Calendar.MINUTE);

        int night_hour = nightTime.get(Calendar.HOUR_OF_DAY);
        int night_minute = nightTime.get(Calendar.MINUTE);


        if (morn_hour > night_hour) {
            if (now_hour >= morn_hour) {
                if (now_hour == morn_hour) {
                    if (now_minute >= morn_minute) {
                        System.out.println("Worked at minute level 1");
                    }
                }
                else {
                    System.out.println("Worked at hour level 1");
                }

            } else if (now_hour <= night_hour) {
                if(now_hour == night_hour) {
                    if(now_minute <= night_minute) {
                        System.out.println("Worked at minute level 2");
                    }
                }
                else {
                    System.out.println("Worked at hour level 2");
                }

            }

        } else if (morn_hour < night_hour) {
            if ((now_hour > morn_hour) && (now_hour < night_hour)) {
                System.out.println("Worked at hour level 3");
            } else if (now_hour == morn_hour) {
                if (now_minute >= morn_minute) {
                    System.out.println("Worked at minute level 3");
                }
            } else if (now_hour == night_hour) {
                if (now_minute <= night_minute) {
                    System.out.println("Worked at minute level 4");
                }
            }
        }
    }
}
