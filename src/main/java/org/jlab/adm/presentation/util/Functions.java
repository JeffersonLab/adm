package org.jlab.adm.presentation.util;

import java.util.logging.Logger;

/**
 * @author ryans
 */
public final class Functions {

  private static final Logger logger = Logger.getLogger(Functions.class.getName());

  private Functions() {
    // cannot instantiate publicly
  }

  public static String addS(int x) {
    if (x != 1) {
      return "s";
    } else {
      return "";
    }
  }

  public static String millisToHumanReadable(long milliseconds, boolean stacked) {
    String time;
    if (milliseconds < 60000) {
      int seconds = (int) Math.floor(milliseconds / 1000);
      time = seconds + " second" + addS(seconds);
    } else {
      int hours = (int) Math.floor((milliseconds) / 3600000),
          remainingMilliseconds = (int) (milliseconds % 3600000),
          minutes = (int) Math.floor(remainingMilliseconds / 60000);

      time =
          (hours > 0 ? hours + " hour" + addS(hours) + " " + (stacked ? "\n" : "") : "")
              + minutes
              + " minute"
              + addS(minutes);
    }

    return time;
  }

  public static String millisToAbbreviatedHumanReadable(long milliseconds) {
    int hours = (int) Math.floor((milliseconds) / 3600000),
        remainingMilliseconds = (int) (milliseconds % 3600000),
        minutes = (int) Math.floor(remainingMilliseconds / 60000);

    return (hours > 0 ? hours + "h" + " " : "") + minutes + "m";
  }
}
