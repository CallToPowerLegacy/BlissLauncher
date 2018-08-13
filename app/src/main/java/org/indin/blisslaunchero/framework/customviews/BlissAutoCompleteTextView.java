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

import android.content.Context;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.widget.AutoCompleteTextView;

public class BlissAutoCompleteTextView extends AutoCompleteTextView{
    public BlissAutoCompleteTextView(Context context) {
        super(context);
    }

    public BlissAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlissAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BlissAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        // Without this drag/drop apps won't work on API <24.
        // EditTexts seem to interfere with drag/drop.
        return false;
    }
}
