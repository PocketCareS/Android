package com.ub.pocketcares.survey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import com.ub.pocketcares.backend.HealthStatus;
import com.ub.pocketcares.R;
import com.ub.pocketcares.utility.LogTags;
import com.ub.pocketcares.utility.Utility;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;

@SuppressLint({"UseSparseArrays", "InflateParams"})
public class HealthReportAdapter extends BaseExpandableListAdapter
        implements OnCheckedChangeListener {

    public static final int TYPE_GROUP = 0;
    public static final int TYPE_SYMPTOM = 1;

    public Context m_context;
    public ExpandableListView m_listview;
    public LayoutInflater m_inflater;

    public HashMap<String, CheckBox> m_viewMap;
    public HashMap<Integer, HealthViewHolder> m_viewSymptomMap;
    public ArrayList<String> m_grouplist;
    public ArrayList<String> m_symptomlist;

    public HashSet<String> m_checkedString;

    private static final String SYMPTOMS_URL = "https://www.cdc.gov/coronavirus/2019-ncov/symptoms-testing/symptoms.html";

    public HealthReportAdapter(Context context, ExpandableListView lv) {
        m_context = context;
        m_listview = lv;
        m_inflater = (LayoutInflater) m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        String[] glist = m_context.getResources().getStringArray(R.array.health_report_group);
        m_grouplist = new ArrayList<String>();
        m_grouplist.addAll(Arrays.asList(glist));
        m_symptomlist = HealthStatus.SYMPTOM_LIST;
        m_viewMap = new HashMap<String, CheckBox>();
        m_viewSymptomMap = new HashMap<Integer, HealthReportAdapter.HealthViewHolder>();

        m_checkedString = new HashSet<String>();
    }

    @Override
    public int getGroupCount() {
        return m_grouplist.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return m_symptomlist.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return m_grouplist.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return m_symptomlist.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        HealthViewHolder vholder;
//		if ( null == convertView ) {
        convertView = m_inflater.inflate(R.layout.dialog_health_check, null);
        vholder = new HealthViewHolder();
        vholder.type = TYPE_GROUP;
        vholder.position = groupPosition;
        vholder.checkbox = (CheckBox) convertView.findViewById(R.id.dialog_health_checkbox);

        vholder.checkbox.setChecked(m_checkedString.contains(m_grouplist.get(groupPosition)));
        String healthOptionText = m_grouplist.get(groupPosition);
        if (healthOptionText.contains("symptoms")) {
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Utility.launchInAppLink(m_context.getResources().getColor(R.color.ub), m_context, SYMPTOMS_URL);
                }
            };
            vholder.checkbox.setText(Utility.getSpannableHyperlink(healthOptionText, clickableSpan,
                    healthOptionText.indexOf("symptoms"), healthOptionText.indexOf(":") + 1));
            vholder.checkbox.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            vholder.checkbox.setText(m_grouplist.get(groupPosition));
        }

        convertView.setTag(vholder);
        m_viewMap.put(m_grouplist.get(groupPosition), vholder.checkbox);
//		}
        String content = m_grouplist.get(groupPosition);

        vholder.checkbox.setTag(content);
//		Log.e(LogTags.TEST, groupPosition+"|"+vholder.toString());
//        vholder.checkbox.setTextSize(Utility.dpToInt(6));
        vholder.checkbox.setOnCheckedChangeListener(this);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        HealthViewHolder symHolder;
        if (null == convertView) {
            convertView = m_inflater.inflate(R.layout.dialog_health_subcheck, null);
            symHolder = new HealthViewHolder();
            symHolder.type = TYPE_SYMPTOM;
            symHolder.position = childPosition;
            symHolder.checkbox = (CheckBox) convertView.findViewById(R.id.dialog_health_sub_checkbox);

            convertView.setTag(symHolder);
            m_viewSymptomMap.put(childPosition, symHolder);
        } else
            symHolder = (HealthViewHolder) convertView.getTag();

        String symptom = m_symptomlist.get(childPosition);
        symHolder.checkbox.setText(symptom);
        symHolder.checkbox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        symHolder.checkbox.setChecked(m_checkedString.contains(symptom));
        symHolder.checkbox.setOnCheckedChangeListener(this);
        symHolder.checkbox.setTag(symptom);

        return convertView;
    }

    @Override
    public void onGroupExpanded(final int groupPosition) {
        super.onGroupExpanded(groupPosition);
        m_listview.setSelectedGroup(groupPosition);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String content = (String) buttonView.getTag();
        if (buttonView.getText().toString().compareTo(m_grouplist.get(0)) == 0) {
            if (isChecked) {
//				m_listview.expandGroup(0, true);
                m_listview.collapseGroup(1);
                clearSymptom();
                m_checkedString.remove(m_grouplist.get(1));
                m_viewMap.get(m_grouplist.get(1)).setChecked(false);
            } else {
                m_listview.expandGroup(1);
                clearSymptom();
                m_checkedString.add(m_grouplist.get(1));
                m_viewMap.get(m_grouplist.get(1)).setChecked(true);
            }
        } else if (buttonView.getText().toString().compareTo(m_grouplist.get(1)) == 0) {
            if (isChecked) {
//                m_listview.collapseGroup(0);
                m_listview.expandGroup(1, true);
                clearSymptom();
                m_checkedString.remove(m_grouplist.get(0));
                m_viewMap.get(m_grouplist.get(0)).setChecked(false);
            } else {
//                m_listview.expandGroup(0, true);
                m_listview.collapseGroup(1);
                m_checkedString.add(m_grouplist.get(0));
                m_viewMap.get(m_grouplist.get(0)).setChecked(true);
            }
        }

        if (isChecked)
            m_checkedString.add(content);
        else
            m_checkedString.remove(content);

//		notifyDataSetChanged();
//		for ( int i=0;i<m_viewMap.size();i++ )
//			Log.d(LogTags.TEST, m_viewMap.get(m_grouplist.get(i)).getText().toString());
//		Log.e(LogTags.TEST, content + " | " + buttonView.getText() + " | " + isChecked);
        Log.e(LogTags.TEST, getHealthString());
    }

    public void clearSymptom() {
        for (int i = 0; i < m_symptomlist.size(); i++)
            m_checkedString.remove(m_symptomlist.get(i));
    }

    public HashSet<String> getSelectedSymptoms() {
        return m_checkedString;
    }

    public String getHealthString() {
        String hstr = "";
        for (int i = 0; i < m_grouplist.size(); i++)
            if (m_checkedString.contains(m_grouplist.get(i)))
                hstr += "1";
            else
                hstr += "0";

        hstr += HealthStatus.DIVIDER;
        for (int i = 0; i < m_symptomlist.size(); i++)
            if (m_checkedString.contains(m_symptomlist.get(i)))
                hstr += "1";
            else
                hstr += "0";
        return hstr;
    }

    public boolean isHealthReportEmpty(String hstr) {
        String empty = "";
        for (int i = 0; i < m_grouplist.size(); i++)
            empty += "0";
        empty += HealthStatus.DIVIDER;
        for (int i = 0; i < m_symptomlist.size(); i++)
            empty += "0";

        return hstr.compareTo(empty) == 0;
    }

    private class HealthViewHolder {
        public int position;
        public CheckBox checkbox;
        public int type;

        public String toString() {
            return type + "|" + position + "|" + checkbox.getText();
        }
    }
}
