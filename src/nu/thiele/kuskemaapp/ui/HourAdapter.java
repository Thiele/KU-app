package nu.thiele.kuskemaapp.ui;

import java.util.List;

import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHour;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHourType;
import nu.thiele.kuskemaapp.utils.Utils;
import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HourAdapter extends BaseAdapter{
	private List<ClassHour> hours;
	private FragmentActivity context;
	public HourAdapter(FragmentActivity context, List<ClassHour> hours) {
		this.context = context;
		this.hours = hours;
	}
	
	@Override
	public int getCount() {
		if(this.hours == null) return 0;
		return this.hours.size();
	}

	@Override
	public Object getItem(int position) {
		return this.hours.get(position);
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
		
		List<ClassHour> classesToday = (List<ClassHour>) this.getItem(useDay);
		
		//Set day name
		day.setText(Utils.dayNameByIndex(this.context,useDay));
		//Just a click listener for the things
		OnClickListener click = new OnClickListener(){
			@Override
			public void onClick(View v) {
				Dialogs.notice(context, "Hej!");	
			}
		};
		//If there are no classes, add that view
		if(classesToday == null || classesToday.size() == 0){
			View toAdd = inflater.inflate(R.layout.schedule_no_hours, parent, false); //No attaching to daddy
			courseList.addView(toAdd);
		}
		else{
			for(ClassHour c : classesToday){
				View toAdd = inflater.inflate(R.layout.schedule_hour, parent, false); //No attaching to daddy
				toAdd.setOnClickListener(click);
				TextView details = (TextView) toAdd.findViewById(R.id.schedule_details);
				TextView headline = (TextView) toAdd.findViewById(R.id.schedule_headline);
				TextView time = (TextView) toAdd.findViewById(R.id.schedule_time);
				TextView type = (TextView) toAdd.findViewById(R.id.schedule_type);
				
				//set values
				ClassHourType t = c.getHourType();
				details.setText(c.getRoom().trim());
				headline.setText(c.getParent().getName().trim());
				time.setText(Utils.timeIntervalFormatter(c.getStart(), c.getEnd()).trim());
				String typeVal = "";
				switch(t){
				case CLASS:
					typeVal = this.context.getString(R.string.class_class);
					break;
				case LECTURE:
					typeVal = this.context.getString(R.string.class_lecture);
					break;
				default:
				case UNKNOWN:
					typeVal = this.context.getString(R.string.class_unknown);
					break;
				}
				type.setText(typeVal);
				courseList.addView(toAdd); 
			}
		}
		return row;
	}
}
