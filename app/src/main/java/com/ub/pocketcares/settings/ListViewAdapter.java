package com.ub.pocketcares.settings;

import com.ub.pocketcares.R;
import com.ub.pocketcares.utility.Utility;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class ListViewAdapter extends BaseAdapter {

    private SettingTabFragment m_settingtab;
    public Context m_context;

    ListViewAdapter(SettingTabFragment stf) {
        this.m_settingtab = stf;
        m_context = m_settingtab.getActivity();
    }

    @Override
    public int getCount() {
        Log.d("COUNTING", String.valueOf(SettingStatic.ITEMLIST.length));
        return SettingStatic.ITEMLIST.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (SettingStatic.LISTTYPE_DIVIDER == getListType(position)) {
            View header = convertView;
            if (null == convertView || !isListItemDivider(convertView)) {
                header = LayoutInflater.from(m_settingtab.getActivity()).inflate(R.layout.lv_divider_layout, parent, false);
                header.setTag(position);
            }
            return header;
        }

        View item;
        item = LayoutInflater.from(m_settingtab.getActivity()).inflate(R.layout.lv_setting, parent, false);
        item.setTag(position);

        ImageButton icon = item.findViewById(R.id.lv_item_icon);
        setIcon(icon, position);
        TextView itemstr = item.findViewById(R.id.lv_item_tv);
        String settingName = SettingStatic.ITEMLIST[position % getCount()];
        itemstr.setText(settingName);
        itemstr.setTag(position);
        Log.d("TEXT", SettingStatic.ITEMLIST[position % getCount()]);
        ImageButton itemdetail = item.findViewById(R.id.lv_item_imagedetail);
        itemdetail.setTag(position);
        itemdetail.setOnClickListener(m_settingtab);

        if (position + 1 < getCount()) {
            if (null != (SettingStatic.ITEMLIST[(position + 1) % getCount()])) {
                View botdivider = item.findViewById(R.id.lv_item_separator_bottom);
                botdivider.setVisibility(View.INVISIBLE);
            }
        }

        item.setOnClickListener(m_settingtab);
        return item;
    }

    public void setIcon(ImageButton ib, int position) {
        String strResource = m_context.getPackageName();
        String settingCon = SettingStatic.ITEMLIST[position];
        if (null == settingCon)
            return;

        else if (0 == settingCon.compareTo(SettingStatic.STR_INTERFACE))
            strResource += ":drawable/preferences";
        else if (0 == settingCon.compareTo(SettingStatic.STR_TERMS))
            strResource += ":drawable/ic_stab_term";
        else if (0 == settingCon.compareTo(SettingStatic.STR_CONTACT))
            strResource += ":drawable/ic_stab_contact";
        else if (0 == settingCon.compareTo(SettingStatic.STR_REFER))
            strResource += ":drawable/ic_stab_refer";
        else
            strResource += ":drawable/ic_stab_ginfo";

        int RID = m_context.getResources().getIdentifier(strResource, null, null);
        Drawable draw = m_context.getResources().getDrawable(RID);
        ib.setImageDrawable(draw);
    }

    public int getListType(int position) {
        if (null == SettingStatic.ITEMLIST[position % getCount()])
            return SettingStatic.LISTTYPE_DIVIDER;
        else
            return SettingStatic.LISTTYPE_ITEM;
    }

    public boolean isListItemDivider(View v) {
        int position = (Integer) v.getTag();
        if (SettingStatic.LISTTYPE_DIVIDER == getListType(position))
            return true;
        else
            return false;
    }

}
