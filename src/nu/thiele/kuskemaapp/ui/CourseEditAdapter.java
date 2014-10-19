package nu.thiele.kuskemaapp.ui;

import java.util.ArrayList;
import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.utils.Utils;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHour;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHourType;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CourseEditAdapter extends BaseAdapter{
	private ArrayList<ClassHour> data;
	private Activity context;
	public CourseEditAdapter(Activity context, ArrayList<ClassHour> values) {
		this.context = context;
		this.data = values;
	}
	
	@Override
	public int getCount() {
		if(this.data == null) return 0;
		return this.data.size()+1;
	}

	@Override
	public Object getItem(int position) {
		return this.data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		if(position == this.data.size()){ //Last one reached. Add the new one
			View row = inflater.inflate(R.layout.adapter_add_edit_add_new, parent, false);
			return row;
		}
		else{
			ClassHour c = this.data.get(position);
			View row = null;
			if(c.isHighlighted()) row = inflater.inflate(R.layout.adapter_add_edit_course_highlight, parent, false);
			else row = inflater.inflate(R.layout.adapter_add_edit_course, parent, false);
			TextView details = (TextView) row.findViewById(R.id.schedule_details);
			TextView headline = (TextView) row.findViewById(R.id.schedule_headline);
			TextView time = (TextView) row.findViewById(R.id.schedule_time);
			TextView type = (TextView) row.findViewById(R.id.schedule_type);
			
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
			return row;
		}
	}
	
	public boolean isCourseHour(int pos){
		return pos < this.data.size();
	}
}
