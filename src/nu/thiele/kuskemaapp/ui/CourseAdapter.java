package nu.thiele.kuskemaapp.ui;

import java.util.ArrayList;

import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.data.Course;
import nu.thiele.kuskemaapp.utils.Utils;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CourseAdapter extends BaseAdapter{
	private ArrayList<Course> data;
	private Activity context;
	public CourseAdapter(Activity context, ArrayList<Course> values) {
		this.context = context;
		this.data = values;
	}
	
	@Override
	public int getCount() {
		if(this.data == null) return 0;
		return this.data.size();
	}

	@Override
	public Object getItem(int position) {
		return this.data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public int getPositionByName(String name){
		int i = 0;
		for(Course c : this.data){
			if(c.getName().equals(name)) return i;
			i++;
		}
		return -1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View row = inflater.inflate(R.layout.adapter_course, parent, false); 
		TextView firstLine = (TextView) row.findViewById(R.id.firstLine);
		TextView secondLine = (TextView) row.findViewById(R.id.secondLine);
		Course c = (Course)this.getItem(position);

		//Find out if ECTS value is proper to display. Empty in some cases
		boolean okEcts = true; 
    	try{
    		String ectsStr = c.getECTS().replace(',', '.'); //comma is used instead of full stop
    		Double.parseDouble(ectsStr);
    	}
    	catch(Exception e){okEcts = false;}
		String ectsVal = okEcts ? c.getECTS() : "?"; //? if unparsable
		firstLine.setText(c.getName());
		secondLine.setText(c.getLevel()+" - "+ectsVal+" "+this.context.getString(R.string.ects));
		return row;
	}
	
	
}