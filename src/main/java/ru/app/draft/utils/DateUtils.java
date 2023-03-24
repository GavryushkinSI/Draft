package ru.app.draft.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {

   private final static SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   private final static Calendar calendar=Calendar.getInstance();

   public static String getTime(long time){
      calendar.setTimeInMillis(time*1000);
      isoFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
      return isoFormat.format(calendar.getTime());
   }

   public static String getCurrentTime(){
      calendar.setTime(new Date());
      isoFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
      return isoFormat.format(calendar.getTime());
   }
}
