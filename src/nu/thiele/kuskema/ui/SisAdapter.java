package nu.thiele.kuskema.ui;

import nu.thiele.kuskema.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SisAdapter extends BaseAdapter{
	private String[] data;
	private Activity context;
	public SisAdapter(Activity context, String[] values) {
		this.context = context;
		this.data = values;
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
		View row = inflater.inflate(R.layout.sis, null, false);
		TextView tekst = (TextView) row.findViewById(R.id.sisTekst);
		tekst.setBackgroundDrawable(this.context.getResources().getDrawable(R.drawable.baggrund_graa));
		tekst.setText(this.data[position]);
		return row;
	}
	
	
}
