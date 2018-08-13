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
package org.indin.blisslaunchero.framework.customviews;


import java.util.Calendar;

import android.graphics.Canvas;

/**
 * An overlay for a clock dial.
 */
public interface DialOverlay {

    /**
     * Subclasses should implement this to draw the overlay.
     *
     * @param canvas   the canvas onto which you must draw
     * @param cX       the x coordinate of the center
     * @param cY       the y coordinate of the center
     * @param w        the width of the canvas
     * @param h        the height of the canvas
     * @param calendar the desired date/time
     */
    public abstract void onDraw(Canvas canvas, int cX, int cY, int w, int h, Calendar calendar,
            boolean sizeChanged);

}
