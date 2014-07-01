package nu.thiele.kuskema.ui;


import nu.thiele.kuskema.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class RedigerAdapter extends BaseAdapter{
	private RedigerAdapter.Entry[] data;
	private Activity context;
	public RedigerAdapter(Activity context, RedigerAdapter.Entry[] data) {
		this.context = context;
		this.data = data;
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View row = inflater.inflate(R.layout.baggrund_time, null, false);
		TextView top = (TextView) row.findViewById(R.id.baggrund_time_top);
		TextView content = (TextView) row.findViewById(R.id.baggrund_time_indhold);
		top.setText(this.data[position].dag+ " - "+this.data[position].tidspunkt);
		content.setText(this.data[position].type);
		return row;
	}
	
	public static class Entry implements Comparable<Entry>{
		public String type;
		public String dag;
		public String tidspunkt;
		public Entry(String type, String dag, String tidspunkt){
			this.dag = dag;
			this.tidspunkt = tidspunkt;
			this.type = type;
		}
		@Override
		public int compareTo(Entry another) {
			if(dag.equals(another.dag)){ //Sammenlign tidspunkt
				int thisstart = tidspunktToInt(this.tidspunkt.substring(0, this.tidspunkt.indexOf("-")));
				int thatstart = tidspunktToInt(another.tidspunkt.substring(0, another.tidspunkt.indexOf("-")));
				if(thisstart < thatstart) return -1;
				else if(thatstart > thisstart) return 1;
				else{ //Hvis starter samtidig...
					int thisslut = tidspunktToInt(this.tidspunkt.substring(this.tidspunkt.indexOf("-")+1));
					int thatslut = tidspunktToInt(another.tidspunkt.substring(another.tidspunkt.indexOf("-")+1));
					if(thisslut < thatslut) return -1;
					else return 1;
				}
			}
			else{
				if(dagToInt(dag) < dagToInt(another.dag)) return -1;
				else return 1;
			}
		}
		
		public static int dagToInt(String dag){
			dag = dag.toLowerCase();
			if(dag.equals("mandag")) return 1;
			if(dag.equals("tirsdag")) return 2;
			if(dag.equals("onsdag")) return 3;
			if(dag.equals("torsdag")) return 4;
			if(dag.equals("fredag")) return 5;
			if(dag.equals("lørdag")) return 6;
			if(dag.equals("søndag")) return 7;
			return 0;
		}
		
		private static int tidspunktToInt(String tidspunkt){
			return 60*Integer.parseInt(tidspunkt.substring(0, tidspunkt.indexOf(":")))+Integer.parseInt(tidspunkt.substring(tidspunkt.indexOf(":")+1));
		}
	}
}
