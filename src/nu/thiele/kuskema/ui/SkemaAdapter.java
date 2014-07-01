package nu.thiele.kuskema.ui;

import java.util.ArrayList;
import nu.thiele.kuskema.R;
import android.app.Activity;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SkemaAdapter extends BaseAdapter{
	private SkemaAdapter.Entry[] data;
	private Activity context;
	public SkemaAdapter(Activity context, SkemaAdapter.Entry[] values) {
		this.context = context;
		this.data = values;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View row = inflater.inflate(R.layout.skema, null, false);
		TextView dag = (TextView) row.findViewById(R.id.skemaDag);
		ArrayList<Time> timer = this.data[position].timer;
		LinearLayout kurser = (LinearLayout) row.findViewById(R.id.skemaKurser);
		kurser.setBackgroundDrawable(this.context.getResources().getDrawable(R.drawable.baggrund_skema));
		//Indsæt timerne
		for(Time t : timer){
			TextView add = new TextView(this.context);
			add.setText(t.tid+"\t");
			
			//Og sæt farver
			int col = Color.parseColor("#555555");
			if(t.type.equals("Forelæsning")) col = Color.parseColor("#FF0000");
			else if(t.type.equals("Øvelse")) col = Color.parseColor("#0000FF");
			add.append(t.kursus);
			Spannable sl = (Spannable) add.getText();
			sl.setSpan(new ForegroundColorSpan(col), add.getText().toString().indexOf(t.kursus), add.getText().length(), 0);
			
			
			kurser.addView(add);
		}
		row.setBackgroundDrawable(this.context.getResources().getDrawable(R.drawable.baggrund_graa));
		dag.setText(this.data[position].dag);
		return row;
	}
	
	public Entry[] getEntries(){
		return this.data;
	}

	@Override
	public int getCount() {
		return this.data.length;
	}

	@Override
	public Object getItem(int position) {
		return this.data[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	/**
	 * 
	 * Til at holde fag+parent
	 *
	 */
	public static class Entry{
		public String dag;
		public ArrayList<Time> timer;
		public Entry(String dag, ArrayList<Time> timer){
			this.dag = dag;
			this.timer = timer;
		}
		
		public String toString(){
			return "Dag: "+this.dag;
		}
	}
}