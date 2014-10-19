package nu.thiele.kuskemaapp.ui;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHour;
import nu.thiele.kuskemaapp.utils.Utils;
import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DayAdapter extends BaseAdapter{
	private Map<Integer,List<ClassHour>> days;
	private Activity context;
	public DayAdapter(Activity context, Map<Integer,List<ClassHour>> days) {
		this.context = context;
		this.days = days;
	}
	
	@Override
	public int getCount() {
		if(this.days == null) return 0;
		return this.days.size();
	}

	@Override
	public Object getItem(int position) {
		return this.days.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View row = inflater.inflate(R.layout.adapter_day, parent, false);
		TextView day = (TextView) row.findViewById(R.id.schedule_day_name);
		LinearLayout courseList = (LinearLayout) row.findViewById(R.id.schedule_courses);
		//Shows today first. Calculate that day
		int useDay = position;//(Utils.todaysDayIndex()+position)%7;
		
		if(Utils.todaysDayIndex() == position){
			day.setTextColor(this.context.getResources().getColor(R.color.highlight_color)); 
		}
		
		List<ClassHour> classesToday = (List<ClassHour>) this.getItem(useDay);
		
		//Set day name
		day.setText(Utils.dayNameByIndex(this.context,useDay));
		//If there are no classes, add that view
		if(classesToday == null || classesToday.size() == 0){
			View toAdd = inflater.inflate(R.layout.schedule_no_hours, parent, false); //No attaching to daddy
			courseList.addView(toAdd);
		}
		else{
			boolean afterEnd = false;
			boolean dividerAdded = false;
			Calendar cal = Calendar.getInstance();
			for(ClassHour c : classesToday){
				View toAdd = inflater.inflate(R.layout.schedule_hour, parent, false); //No attaching to daddy
				TextView details = (TextView) toAdd.findViewById(R.id.schedule_details);
				TextView headline = (TextView) toAdd.findViewById(R.id.schedule_headline);
				TextView time = (TextView) toAdd.findViewById(R.id.schedule_time);
				TextView type = (TextView) toAdd.findViewById(R.id.schedule_type);
				/*		OUT-COMMENTED CODE FOR TIME OF DAY INDICATOR
				int hoursEnd = Utils.hourFromClockifiedString(c.getEnd());
				int hoursStart = Utils.hourFromClockifiedString(c.getStart());
				int minutesEnd = Utils.minutesFromClockifiedString(c.getEnd());
				int minutesStart = Utils.minutesFromClockifiedString(c.getStart());
				
				//Check if current time marker should be added now
				if(afterEnd && !dividerAdded
						&& (
								cal.get(Calendar.HOUR_OF_DAY) < hoursStart
								|| cal.get(Calendar.HOUR_OF_DAY) == hoursStart && cal.get(Calendar.MINUTE) < minutesStart
								)){
					View spacer = inflater.inflate(R.layout.today_indicator, parent, false);
					courseList.addView(spacer);
				}
				
				//Check if current time is after class end
				if(cal.get(Calendar.HOUR_OF_DAY) > hoursEnd ||
						cal.get(Calendar.HOUR_OF_DAY) == hoursEnd && cal.get(Calendar.MINUTE) > minutesEnd){
					afterEnd = true;
				}
				*/
				//set values
				details.setText(c.getRoom().trim());
				headline.setText(c.getParent().getName().trim());
				time.setText(Utils.timeIntervalFormatter(c.getStart(), c.getEnd()).trim());
				String typeVal = Utils.classTypeStringByIndex(context, c.getHourType());
				type.setText(typeVal);
				courseList.addView(toAdd); 
			}
		}
		return row;
	}
}
