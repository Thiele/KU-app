package nu.thiele.kuskemaapp.utils;

import java.util.Calendar;

import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHourType;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.inputmethod.InputMethodManager;

public class Utils {
	public static String classTypeStringByIndex(Context c, ClassHourType t){
		switch(t){
		case CLASS:
			return c.getString(R.string.class_class);
		case LECTURE:
			return c.getString(R.string.class_lecture);
		default:
		case UNKNOWN:
			return c.getString(R.string.class_unknown);
		}
	}
	/**
	 * Make the number look nice.
	 * @param num
	 * @return number in format hh or mm instead of 0h or 0m
	 */
	public static String clockify(int num){
		if(num < 10) return "0"+num;
		else return Integer.toString(num);
	}
	public static String dayNameByIndex(Context c, int index){
		switch(index){
		case 0: return c.getString(R.string.monday);
		case 1: return c.getString(R.string.tuesday);
		case 2: return c.getString(R.string.wednesday);
		case 3: return c.getString(R.string.thursday);
		case 4: return c.getString(R.string.friday);
		case 5: return c.getString(R.string.saturday);
		default: //'cause we like sundays
		case 6: return c.getString(R.string.sunday);
		}
	}
	
	public static void hideSoftKeyboard(Activity activity) {
	    InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
	    inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}
	
	public static int hourFromClockifiedString(String s){
		if(s == null || !s.contains(":")) return -1;
		return Integer.parseInt(s.substring(0, s.indexOf(":")));
	}
	
	public static int minutesFromClockifiedString(String s){
		if(s == null || !s.contains(":")) return -1;
		return Integer.parseInt(s.substring(s.indexOf(":")+1));
	}
	
	public static boolean isInteger(Double d){
		System.out.println(d);
		return d - Math.floor(d) < 0.00000005; //0.00...5 since we may have rounding errors
	}
	
	public static String longestCommonPrefix(String[] vals){
		if(vals == null || vals.length == 0) return "";
		String prefix = vals[0];
		for(int i = 1; i < vals.length; i++){
			if(vals[i] == null) return ""; //Don't give null input
			StringBuilder sb = new StringBuilder();
			for(int j = 0; j < prefix.length() && j < vals[i].length() && prefix.charAt(j) == vals[i].charAt(j); j++){
				sb.append(prefix.charAt(j));
			}
			prefix = sb.toString();
		}
		return prefix;
	}
	
	public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }
	
	public static int todaysDayIndex(){
		Calendar c = Calendar.getInstance();
		switch(c.get(Calendar.DAY_OF_WEEK)){
		case Calendar.MONDAY: return 0;
		case Calendar.TUESDAY: return 1;
		case Calendar.WEDNESDAY: return 2;
		case Calendar.THURSDAY: return 3;
		case Calendar.FRIDAY: return 4;
		case Calendar.SATURDAY: return 5;
		case Calendar.SUNDAY: return 6;
		}
		return 0;
	}
	
	public static String timeIntervalFormatter(String start, String end){
		return start+" - "+end;
	}
	public static String timeIntervalFormatter(String hour1, String minutes1, String hour2, String minutes2){
		return timeIntervalFormatter(hour1+":"+minutes1,hour2+":"+minutes2);
	}
}
