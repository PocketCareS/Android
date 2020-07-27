package com.ub.pocketcares.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import com.ub.pocketcares.R;
import com.ub.pocketcares.utility.Utility;

public class HealthSummaryAdapter extends ArrayAdapter<HealthSummaryItem> {
    private Context context;
    private ArrayList<HealthSummaryItem> healthSummaryItems;
    private static final String CDC_URL = "https://www.cdc.gov/coronavirus/2019-ncov/index.html";

    HealthSummaryAdapter(@NonNull Context context, int resource, @NonNull List<HealthSummaryItem> items) {
        super(context, resource, items);
        this.context = context;
        this.healthSummaryItems = (ArrayList<HealthSummaryItem>) items;
    }

    @SuppressLint("ViewHolder")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout row = (LinearLayout) inflater.inflate(R.layout.health_summary_row, parent, false);
        TextView date = row.findViewById(R.id.date_value);
        TextView report = row.findViewById(R.id.report_value);
        TextView recommendation = row.findViewById(R.id.recommendation_value);

        date.setText(healthSummaryItems.get(position).getDate());
        report.setText(healthSummaryItems.get(position).getResponse());
        String recommendationString = healthSummaryItems.get(position).getRecommendation();
        if (recommendationString.contains("CDC")) {
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Utility.launchInAppLink(context.getResources().getColor(R.color.ub), context, CDC_URL);
                }
            };
            recommendation.setText(Utility.getSpannableHyperlink(recommendationString, clickableSpan,
                    recommendationString.indexOf("CDC"), recommendationString.length()));
            recommendation.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            recommendation.setText(recommendationString);
        }
        return row;
    }
}
