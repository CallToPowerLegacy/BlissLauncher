/*
 * Copyright 2018 /e/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.indin.blisslaunchero.features.weather;

import static cyanogenmod.providers.WeatherContract.WeatherColumns.TempUnit.CELSIUS;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.indin.blisslaunchero.R;
import org.indin.blisslaunchero.framework.Preferences;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cyanogenmod.weather.WeatherInfo;
import cyanogenmod.weather.util.WeatherUtils;

public class ForecastBuilder {

    private static final String TAG = "ForecastBuilder";
    /**
     * This method is used to build the small, horizontal forecasts panel
     * @param context
     * @param smallPanel = a horizontal linearlayout that will contain the forecasts
     * @param w = the Weather info object that contains the forecast data
     */
    public static boolean buildSmallPanel(Context context, LinearLayout smallPanel, WeatherInfo w) {
        if (smallPanel == null) {
            Log.d(TAG, "Invalid view passed");
            return false;
        }

        // Get things ready
        LayoutInflater inflater
                = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int color = Preferences.weatherFontColor(context);
        final boolean useMetric = Preferences.useMetricUnits(context);

        smallPanel.removeAllViews();
        List<WeatherInfo.DayForecast> forecasts = w.getForecasts();
        if (forecasts == null || forecasts.size() <= 1) {
            smallPanel.setVisibility(View.GONE);
            return false;
        }

        TimeZone MyTimezone = TimeZone.getDefault();
        Calendar calendar = new GregorianCalendar(MyTimezone);
        int weatherTempUnit = w.getTemperatureUnit();
        int numForecasts = forecasts.size();
        int itemSidePadding = context.getResources().getDimensionPixelSize(
                R.dimen.forecast_item_padding_side);

        // Iterate through the Forecasts
        for (int count = 0; count < numForecasts; count ++) {
            WeatherInfo.DayForecast d = forecasts.get(count);

            // Load the views
            View forecastItem = inflater.inflate(R.layout.item_weather_forecast, null);

            // The day of the week
            TextView day = (TextView) forecastItem.findViewById(R.id.forecast_day);
            day.setText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT,
                    Locale.getDefault()));
            calendar.roll(Calendar.DAY_OF_WEEK, true);

            // Weather Image
            ImageView image = (ImageView) forecastItem.findViewById(R.id.weather_image);
            String iconsSet = Preferences.getWeatherIconSet(context);
            final int resId = WeatherIconUtils.getWeatherIconResource(context, iconsSet,
                    d.getConditionCode());
            if (resId != 0) {
                image.setImageResource(resId);
            } else {
                image.setImageBitmap(WeatherIconUtils.getWeatherIconBitmap(context, iconsSet,
                        color, d.getConditionCode()));
            }

            // Temperatures
            double lowTemp = d.getLow();
            double highTemp = d.getHigh();
            int tempUnit = weatherTempUnit;
            if (weatherTempUnit == FAHRENHEIT && useMetric) {
                lowTemp = cyanogenmod.weather.util.WeatherUtils.fahrenheitToCelsius(lowTemp);
                highTemp = cyanogenmod.weather.util.WeatherUtils.fahrenheitToCelsius(highTemp);
                tempUnit = CELSIUS;
            } else if (weatherTempUnit == CELSIUS && !useMetric) {
                lowTemp = cyanogenmod.weather.util.WeatherUtils.celsiusToFahrenheit(lowTemp);
                highTemp = cyanogenmod.weather.util.WeatherUtils.celsiusToFahrenheit(highTemp);
                tempUnit = FAHRENHEIT;
            }
            String dayLow = cyanogenmod.weather.util.WeatherUtils.formatTemperature(lowTemp, tempUnit);
            String dayHigh = WeatherUtils.formatTemperature(highTemp, tempUnit);
            TextView temps = (TextView) forecastItem.findViewById(R.id.weather_temps);
            temps.setText(dayLow + "\n" + dayHigh);

            // Add the view
            smallPanel.addView(forecastItem,
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            // Add a divider to the right for all but the last view
            if (count < numForecasts - 1) {
                View divider = new View(context);
                smallPanel.addView(divider, new LinearLayout.LayoutParams(
                        itemSidePadding, LinearLayout.LayoutParams.MATCH_PARENT));
            }
        }
        return true;
    }
}
