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

package com.ub.pocketcares.introduction;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.github.appintro.SlidePolicy;

import com.ub.pocketcares.R;

public class DisclaimerFragment extends Fragment implements SlidePolicy {
    TextView disclaimerDescription;
    TextView acceptBox;
    CheckBox checkBox;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_disclaimer, container, false);
        disclaimerDescription = v.findViewById(R.id.textView3);
        disclaimerDescription.setMovementMethod(new ScrollingMovementMethod());
        disclaimerDescription.setText(Html.fromHtml(getString(R.string.activity_disclaimer_paragraph)));
        checkBox = v.findViewById(R.id.checkbox);
        acceptBox = v.findViewById(R.id.checkBoxText);
        return v;
    }

    @Override
    public boolean isPolicyRespected() {
        if (checkBox != null) {
            return checkBox.isChecked();
        }
        return false;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        Toast.makeText(this.getContext(), "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
    }

}
