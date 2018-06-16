package org.indin.blisslaunchero.framework.util;

/**
 * Created by falcon on 8/3/18.
 */

public class Constants {
    public static final boolean DEBUG = true;
    public static final String PREF_NAME = "BLISS_LAUNCHER";
    public static int DEFAULT_CLOCK_ID;
    public static int DEFAULT_CALENDAR_ID;

    public static final String SHOW_WEATHER = "show_weather";
    public static final String WEATHER_SOURCE = "weather_source";
    public static final String WEATHER_USE_CUSTOM_LOCATION = "weather_use_custom_location";
    public static final String WEATHER_CUSTOM_LOCATION_CITY = "weather_custom_location_city";
    public static final String WEATHER_CUSTOM_LOCATION = "weather_custom_location";
    public static final String WEATHER_LOCATION = "weather_location";
    public static final String WEATHER_SHOW_LOCATION = "weather_show_location";
    public static final String WEATHER_SHOW_TIMESTAMP = "weather_show_timestamp";
    public static final String WEATHER_USE_METRIC = "weather_use_metric";
    public static final String WEATHER_INVERT_LOWHIGH = "weather_invert_lowhigh";
    public static final String WEATHER_REFRESH_INTERVAL = "weather_refresh_interval";
    public static final String WEATHER_SHOW_WHEN_MINIMIZED = "weather_show_when_minimized";
    public static final String WEATHER_FONT_COLOR = "weather_font_color";
    public static final String WEATHER_TIMESTAMP_FONT_COLOR = "weather_timestamp_font_color";
    public static final String WEATHER_ICONS = "weather_icons";

    public static final String MONOCHROME = "mono";
    public static final String COLOR_STD = "color";

    // other shared pref entries
    public static final String WEATHER_LAST_UPDATE = "last_weather_update";
    public static final String WEATHER_DATA = "weather_data";

    // First run is used to hide the initial no-weather message for a better OOBE
    public static final String WEATHER_FIRST_UPDATE = "weather_first_update";

    public static final String DEFAULT_LIGHT_COLOR = "#ffffffff";
    public static final String DEFAULT_DARK_COLOR = "#80ffffff";
}
