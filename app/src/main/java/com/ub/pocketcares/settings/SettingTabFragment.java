package com.ub.pocketcares.settings;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.Objects;

import com.ub.pocketcares.R;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.introduction.IntroductionFragment;

@SuppressLint("InflateParams")
public class SettingTabFragment extends Fragment
        implements OnClickListener {

    public MainActivity m_mainActivity = null;
    public Context m_context;
    public ListView lv_settings;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.settings_fragment_view, container, false);
        m_context = getActivity();
        m_mainActivity = (MainActivity) getActivity();

        lv_settings = rootView.findViewById(R.id.lv_settings);
        lv_settings.setAdapter(new ListViewAdapter(this));
        return rootView;
    }

    @Override
    public void onClick(View v) {
        int position = (Integer) v.getTag();
        int listlen = SettingStatic.ITEMLIST.length;

        Integer intid = SettingStatic.str2int.get(SettingStatic.ITEMLIST[position % listlen]);
        if (null == intid)
            return;

        Log.v("fragment", SettingStatic.ITEMLIST[position % listlen]);
        try {
            switch (intid) {
                case SettingStatic.INT_INTERFACE:
                    startActivity(new Intent(m_mainActivity, PreferenceActivity.class));
                    break;
                case SettingStatic.INT_CONTACT:
                    contactUs();
                    break;
                case SettingStatic.INT_TERMS:
                    createTermsDialog(m_mainActivity);
                    break;
                case SettingStatic.INT_REFER:
                    shareApp();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shareApp() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, IntroductionFragment.FAQ_URL);
        sendIntent.putExtra(Intent.EXTRA_TITLE, "Try PocketCare S");
        sendIntent.setType("text/plain");
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void contactUs() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"cse-pocketcares@buffalo.edu"});
        i.putExtra(Intent.EXTRA_SUBJECT, "subject");
        i.putExtra(Intent.EXTRA_TEXT, "Messages goes here...");
        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(m_mainActivity, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }


    private static void createTermsDialog(final MainActivity mActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.MyDialogTheme);
        ScrollView scrollView = new ScrollView(mActivity);
        LinearLayout aboutAppLayout = new LinearLayout(mActivity);
        aboutAppLayout.setOrientation(LinearLayout.VERTICAL);
        aboutAppLayout.setPadding(50, 50, 50, 50);
        final TextView about = new TextView(mActivity);
        about.setText(Html.fromHtml(mActivity.getString(R.string.about)));
        about.setMaxLines(7);
        TextView moreAbout = new TextView(mActivity);
        moreAbout.setText("Show More…");
        moreAbout.setTextColor(Color.WHITE);
        moreAbout.setTypeface(Typeface.DEFAULT_BOLD);
        moreAbout.setOnClickListener(v -> {
            expandCollapsedByMaxLines(about);
            v.setVisibility(View.GONE);
            about.setMovementMethod(LinkMovementMethod.getInstance());
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 70);
        moreAbout.setLayoutParams(layoutParams);
        final TextView privacy = new TextView(mActivity);
        privacy.setText(Html.fromHtml(mActivity.getString(R.string.privacy)));
        privacy.setMaxLines(5);
        TextView morePrivacy = new TextView(mActivity);
        morePrivacy.setText("Show More…");
        morePrivacy.setTextColor(Color.WHITE);
        morePrivacy.setTypeface(Typeface.DEFAULT_BOLD);
        morePrivacy.setOnClickListener(v -> {
            expandCollapsedByMaxLines(privacy);
            v.setVisibility(View.GONE);
            privacy.setMovementMethod(LinkMovementMethod.getInstance());
        });
        aboutAppLayout.addView(about);
        aboutAppLayout.addView(moreAbout);
        aboutAppLayout.addView(privacy);
        aboutAppLayout.addView(morePrivacy);
        scrollView.addView(aboutAppLayout);
        builder.setTitle("About").
                setView(scrollView);
        builder.setPositiveButton("Return", (dialog, which) ->
        {
        });
        Dialog termsDialog = builder.create();
        Objects.requireNonNull(termsDialog.getWindow()).

                setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        termsDialog.show();
    }

    @SuppressLint("Range")
    public static void expandCollapsedByMaxLines(final TextView text) {
        final int height = text.getMeasuredHeight();
        text.setHeight(height);
        text.setMaxLines(Integer.MAX_VALUE); //expand fully
        text.measure(View.MeasureSpec.makeMeasureSpec(text.getMeasuredWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.UNSPECIFIED));
        final int newHeight = text.getMeasuredHeight();
        ObjectAnimator animation = ObjectAnimator.ofInt(text, "height", height, newHeight);
        animation.setDuration(250).start();
    }

}
