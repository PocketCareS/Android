package com.ub.pocketcares.survey;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import com.ub.pocketcares.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class CheckboxAdapter extends ArrayAdapter<String> 
	implements OnCheckedChangeListener {
		
	public Context mContext;
	public LayoutInflater mInflater = null;
	public ArrayList<String> mlist;
	public HashSet<Integer> mCheckIndex = null;
	
	public CheckboxAdapter ( Context context, ArrayList<String> list ) {
		super(context, R.layout.dialog_health_check, list);
		this.mContext = context;
		this.mlist = list;
		this.mCheckIndex = new HashSet<Integer>();
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mlist.size();
	}
	
	@Override
	public String getItem(int position) {
		// TODO Auto-generated method stub
		return mlist.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return mlist.indexOf(getItem(position));
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		CheckBox cb_day;
		if ( null == convertView ) {
			convertView = mInflater.inflate(R.layout.dialog_health_check, null);
			cb_day = (CheckBox) convertView.findViewById(0);
			convertView.setTag(cb_day);
		}
		else
			cb_day = (CheckBox) convertView.getTag();
		
		cb_day.setTag(position);
		cb_day.setOnCheckedChangeListener(this);
		cb_day.setText(mlist.get(position));
//		cb_day.setText(FormatCalendarString( mlist.get(position) ));
		
		if ( mCheckIndex.contains(position) )
			cb_day.setChecked(true);
		else
			cb_day.setChecked(false);
		
		return convertView;
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		int position = (Integer) buttonView.getTag();
		if ( isChecked )
			mCheckIndex.add(position);
		else
			mCheckIndex.remove(position);
//		notifyDataSetChanged();
	}
	
	public static String FormatCalendarString ( Calendar c ) {
		return String.format("%1$tA, %1$tb %1$td", c);
	}
}
