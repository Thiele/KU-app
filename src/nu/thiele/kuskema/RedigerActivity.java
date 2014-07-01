package nu.thiele.kuskema;

import java.util.ArrayList;
import java.util.Collections;

import nu.thiele.kuskema.ui.RedigerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;;

public class RedigerActivity extends Activity{
	ArrayList<RedigerAdapter.Entry> data = new ArrayList<RedigerAdapter.Entry>();
	ListView timer;
	SQLiteDatabase database;
	EditText institut,navn;
    
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.rediger);
        //Init
        ArrayList<RedigerAdapter.Entry> old = (ArrayList<RedigerAdapter.Entry>) getLastNonConfigurationInstance();
        this.database = this.openOrCreateDatabase(this.getString(R.string.databasenavn), Context.MODE_PRIVATE, null);
        this.institut = (EditText) findViewById(R.id.redigerInstitut);
        this.navn = (EditText) findViewById(R.id.redigerNavn);
        this.navn.setText(this.getIntent().getExtras().getString("alias"));
        this.timer = (ListView) findViewById(R.id.redigerTimer);
        if(old == null){
            hentinstitut();
            henttimer();	
        }//Restore gammel session
        else{
        	this.data = old;
        	opdater();
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        return this.data;
    }
    
    private void alert(String m){
    	AlertDialog.Builder b = new AlertDialog.Builder(this);
    	b.setMessage(m);
    	b.setNeutralButton("Ok", null);
    	b.create().show();
    }
    
    private void hentinstitut(){
    	if(this.getIntent().getBooleanExtra("nytkursus", false)){
    		this.institut.setText("Navn på institut");
    		return;
    	}
    	Cursor c = this.database.rawQuery("SELECT institut FROM fag WHERE kursus='"+this.getIntent().getStringExtra("navn")+"'", null);
    	c.moveToFirst();
    	this.institut.setText(c.getString(c.getColumnIndex("institut")));
    	c.close();
    }
    
    private void henttimer(){
    	this.data.clear(); //Tøm...
    	if(this.getIntent().getBooleanExtra("nytkursus", false)) return;
    	Cursor c = this.database.rawQuery("SELECT * FROM skema WHERE kursus='"+this.getIntent().getStringExtra("navn")+"'", null);
    	while(c.moveToNext()){
    		String dag = c.getString(c.getColumnIndex("dag"));
    		dag = (dag.charAt(0)+"").toUpperCase()+dag.substring(1);
    		String tidspunkt = c.getString(c.getColumnIndex("tidspunkt"));
    		String type = c.getString(c.getColumnIndex("type"));
    		this.data.add(new RedigerAdapter.Entry(type, dag, tidspunkt));
    	}
    	c.close();
    	opdater();
    }
    
    private void opdater(){
    	this.timer.setAdapter(null); //Tøm
    	Collections.sort(this.data);
    	final RedigerAdapter.Entry[] data = new RedigerAdapter.Entry[this.data.size()];
    	int i = 0;
    	for(RedigerAdapter.Entry e : this.data){
    		data[i] = e;
    		i++;
    	}
    	this.timer.setAdapter(new RedigerAdapter(this, data));
    	this.timer.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2, long arg3) {
				AlertDialog.Builder builder = new AlertDialog.Builder(RedigerActivity.this);
				builder.setTitle(data[arg2].dag+" - "+data[arg2].tidspunkt);
				builder.setItems(new String[]{"Skift aktivitetstype","Slet"}, new OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which == 0){
							AlertDialog.Builder typevalg = new AlertDialog.Builder(RedigerActivity.this);
							typevalg.setTitle("Vælg type");
							typevalg.setItems(new String[]{"Forelæsning","Øvelsestime"}, new OnClickListener(){
								@Override
								public void onClick(DialogInterface arg0, int arg1) {
									if(arg1 == 0) data[arg2].type = "Forelæsning";
									else if(arg1 == 1) data[arg2].type = "Øvelse";
									opdater();
								}
							});
							typevalg.show();
						}
						if(which == 1) slet(data[arg2].dag.toLowerCase(), data[arg2].tidspunkt, data[arg2].type);	
					}
				});	
				builder.show();
			}
    	});
    	
    }
    
    private RedigerAdapter.Entry findentry(String type, String dag, String tidspunkt){
    	for(RedigerAdapter.Entry ent : this.data){
    		if(ent.type.equals(type) && ent.dag.toLowerCase().equals(dag.toLowerCase()) && ent.tidspunkt.equals(tidspunkt)) return ent;
    	}
    	return null;
    }
    
    public void gem(View v){
    	String nytalias = this.navn.getText().toString().replaceAll("'", ""); //Sikr mod sql injections
    	String gammeltnavn = this.getIntent().getExtras().getString("navn");
		String institut = this.institut.getText().toString();
		//Hvis nyt kursus, tjek om navn findes
		if(this.getIntent().getBooleanExtra("nytkursus", false)){
    		Cursor c = this.database.rawQuery("SELECT count(*) FROM fag WHERE kursus='"+nytalias+"'", null);
    		c.moveToFirst();
    		int count = c.getInt(0);
    		c.close();
    		if(count > 0){
    			alert("Kursus med det navn findes allerede");
    			return;
    		}
    	}
    	//Først opdater hele skemaet til det nye
    	this.database.execSQL("DELETE FROM skema WHERE kursus='"+gammeltnavn+"'");
    	for(RedigerAdapter.Entry ent : this.data){
    		String sql = "INSERT INTO skema(kursus,tidspunkt,dag,type) VALUES ('"+gammeltnavn+"','"+ent.tidspunkt+"','"+ent.dag.toLowerCase()+"','"+ent.type+"')";
    		this.database.execSQL(sql);
    	}
    	//Opdater eventuelle omdøbninger og ændringer i fakultet
    	if(this.getIntent().getBooleanExtra("nytkursus", false)){ //Hvis nyt kursus
    		String intetinstitut = "Intet institut fundet for kurset";
    		this.database.execSQL("INSERT INTO fag(kursus,alias,institut,link) VALUES('"+nytalias+"','"+nytalias+"','"+(institut.equals("") ? intetinstitut : institut)+"','')");
    		this.database.close();
    		//Retur
    		Intent retur = new Intent();
     		setResult(RESULT_OK,retur);
     		finish();
    	}
    	else{ //Hvis der redigeres
    		database.execSQL("UPDATE fag SET alias='"+nytalias+"' WHERE kursus='"+gammeltnavn+"'");
    		database.execSQL("UPDATE fag SET institut='"+institut+"' WHERE kursus='"+gammeltnavn+"'");
    		this.database.close();
    		//Retur
    		Intent retur = new Intent();
    		setResult(RESULT_OK,retur);
    		finish();
    	}
    }
    
    private void slet(String dag, String tidspunkt, String type){
    	RedigerAdapter.Entry e = findentry(type,dag,tidspunkt);
    	if(e == null) return;
    	this.data.remove(e);
    	opdater();
    }
    
    public void tilfojny(View v){
    	final RedigerAdapter.Entry ny = new RedigerAdapter.Entry(null, null, null);
    	
    	//Find type
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Vælg aktivitetstype");
		builder.setItems(new String[]{"Forelæsning","Øvelse"}, new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0) ny.type = "Forelæsning";
				else ny.type = "Øvelse";
				//Find tidspunkt
				AlertDialog.Builder a = new AlertDialog.Builder(RedigerActivity.this);
				a.setTitle("Vælg dag");
				a.setItems(new String[]{"Mandag", "Tirsdag","Onsdag","Torsdag","Fredag"}, new OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which == 0) ny.dag = "Mandag";
						else if(which == 1) ny.dag = "Tirsdag";
						else if(which == 2) ny.dag = "Onsdag";
						else if(which == 3) ny.dag = "Torsdag";
						else if(which == 4) ny.dag = "Fredag";
						else ny.dag = "Mandag";
						
						
						//Find tidspunkt
						AlertDialog.Builder b = new AlertDialog.Builder(RedigerActivity.this);
						b.setTitle("Vælg starttidspunkt");
						final TimePicker start = new TimePicker(RedigerActivity.this);
						start.setIs24HourView(true);
						start.setAddStatesFromChildren(true);
						b.setView(start);
						b.setNeutralButton("OK", new OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which) {
								AlertDialog.Builder c = new AlertDialog.Builder(RedigerActivity.this);
								c.setTitle("Vælg sluttidspunkt");
								final TimePicker slut = new TimePicker(RedigerActivity.this);
								slut.setAddStatesFromChildren(true);
								slut.setIs24HourView(true);
								c.setView(slut);
								c.setNeutralButton("OK", new OnClickListener(){
									@Override
									public void onClick(DialogInterface dialog, int which) {
										start.clearFocus();
										slut.clearFocus();
										ny.tidspunkt = start.getCurrentHour()+":"+(start.getCurrentMinute() < 10 ? "0"+start.getCurrentMinute() : start.getCurrentMinute())+"-"+slut.getCurrentHour()+":"+(slut.getCurrentMinute() < 10 ? "0"+slut.getCurrentMinute() : slut.getCurrentMinute());
										data.add(ny);
										opdater();
									}
								});
								c.show();
							}
						});
						b.show();
					}
				});
				a.show();
			}
		});	
		builder.show();
    }
}
