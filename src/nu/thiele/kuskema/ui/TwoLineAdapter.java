package nu.thiele.kuskema.ui;

import nu.thiele.kuskema.R;
import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TwoLineAdapter extends BaseAdapter{
	private TwoLineAdapter.Entry[] data;
	private Activity context;
	public TwoLineAdapter(Activity context, Entry[] values) {
		this.context = context;
		this.data = values;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View row = inflater.inflate(R.layout.egnefag, null, false);
		TextView fag = (TextView) row.findViewById(R.id.egneFag);
		TextView fagParent = (TextView) row.findViewById(R.id.egneFagParent);
		row.setBackgroundDrawable(this.context.getResources().getDrawable(R.drawable.baggrund_graa));
		fagParent.setTextColor(Color.LTGRAY);
		fag.setText(this.data[position].alias);
		fagParent.setText(this.data[position].parent);
		return row;
	}
	
	public Entry[] getEntries(){
		return this.data;
	}
	
	/**
	 * 
	 * Til at holde fag+parent
	 *
	 */
	public static class Entry implements Comparable<Entry>{
		public String alias;
		public String fag;
		public String link;
		public String parent;
		public Entry(String alias, String fag, String link, String parent){
			this.alias = alias;
			this.fag = fag;
			this.link = link;
			this.parent = parent;
		}
		public String toString(){
			return "Fag: "+this.fag+". Alias: "+this.alias+" Parent: "+this.parent;
		}
		@Override
		public int compareTo(Entry another) {
			//Først alias
			if(!alias.equals(another.alias)) return alias.compareTo(another.alias);
			//Sammenlign først navn
			if(!fag.equals(another.fag)) return fag.compareTo(another.fag);
			//Dernæst link
			return this.parent.compareTo(another.parent);
		}
		
		public boolean equals(Entry e){
			return compareTo(e) == 0;
		}
		
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
}