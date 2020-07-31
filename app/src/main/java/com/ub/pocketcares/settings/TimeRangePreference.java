/*
 * Copyright 2020 University at Buffalo
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

package com.ub.pocketcares.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.ub.pocketcares.R;

public class TimeRangePreference extends DialogPreference {
    private Pair<Integer, Integer> mTime;

    public TimeRangePreference(Context context) {
        this(context, null);
    }

    public TimeRangePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public TimeRangePreference(Context context, AttributeSet attrs,
                               int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public TimeRangePreference(Context context, AttributeSet attrs,
                               int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public Pair<Integer, Integer> getTime() {
        return mTime;
    }

    public void setTime(String time) {
        String[] timeValues = time.split(";");
        mTime = new Pair<>(Integer.parseInt(timeValues[0]), Integer.parseInt(timeValues[1]));
        persistString(time);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.onSetInitialValue(defaultValue);
        setTime(getPersistedString(String.valueOf(defaultValue)));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return super.onGetDefaultValue(a, index);
    }
}
