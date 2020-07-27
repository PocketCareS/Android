package com.ub.pocketcares.settings;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingSummary implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public boolean b_service = true;
	public boolean b_bt = true;
	public boolean b_wifi = true;
	public boolean b_gps = true;
	public boolean b_actrec = true;
	
	public boolean b_nightmode = false;
	
	public int frq_bt = 5;
	public int frq_wifi = 5;
	public int frq_gps = 10;
	public int frq_act = 5;
	
	public String toJsonString () {
		JSONObject settings = new JSONObject();
		try {
			settings.put("service", b_service);
			settings.put("bt", b_bt);
			settings.put("wifi", b_wifi);
			settings.put("gps", b_gps);
			settings.put("activityRec", b_actrec);
			
			settings.put("nightmode", b_nightmode);
			
			settings.put("frq bt", frq_bt);
			settings.put("frq wifi", frq_wifi);
			settings.put("frq gps", frq_gps);
			settings.put("frq activity", frq_act);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return settings.toString();
	}
	
}
