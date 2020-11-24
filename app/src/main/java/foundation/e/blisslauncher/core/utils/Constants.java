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
package foundation.e.blisslauncher.core.utils;

/**
 * Created by falcon on 8/3/18.
 */

public class Constants {
    public static final boolean DEBUG = false;
    public static final String PREF_NAME = "foundation.e.blisslauncher.prefs";

    public static final String MONOCHROME = "mono";
    public static final String COLOR_STD = "color";

    public static final String DEFAULT_LIGHT_COLOR = "#ffffffff";
    public static final String DEFAULT_DARK_COLOR = "#80ffffff";

    public static final String USER_CREATION_TIME_KEY = "user_creation_time_";

    /**
     * Represents types of item displayed in Launcher.
     */
    public static final int ITEM_TYPE_APPLICATION = 0;
    public static final int ITEM_TYPE_SHORTCUT = 1;
    public static final int ITEM_TYPE_FOLDER = 2;

    /**
     * Represents types of container.
     */
    public static final int CONTAINER_DESKTOP = -100;
    public static final int CONTAINER_HOTSEAT = -101;

}
