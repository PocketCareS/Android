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

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.ub.pocketcares.R;

public class TimePreference extends DialogPreference {
    private int mTime;
    private int mDialogLayoutResId = R.layout.pref_dialog_time;


    public TimePreference(Context context) {
        this(context, null);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public TimePreference(Context context, AttributeSet attrs,
                          int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public TimePreference(Context context, AttributeSet attrs,
                          int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public int getTime() {
        return mTime;
    }

    public void setTime(int time) {
        mTime = time;
        persistInt(time);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.onSetInitialValue(defaultValue);
        setTime(getPersistedInt(mTime));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return super.onGetDefaultValue(a, index);
    }

    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }
}