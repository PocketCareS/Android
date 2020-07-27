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
