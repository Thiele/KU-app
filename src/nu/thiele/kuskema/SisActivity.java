package nu.thiele.kuskema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import nu.thiele.kuskema.ui.SisAdapter;
import nu.thiele.kuskema.ui.SkemaAdapter;
import nu.thiele.kuskema.ui.Time;
import nu.thiele.kuskema.ui.Tree;
import nu.thiele.kuskema.ui.TwoLineAdapter;
import nu.thiele.kuskema.ui.Tree.Node;
import nu.thiele.kuskema.ui.TwoLineAdapter.Entry;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SisActivity extends Activity{
	private boolean fagfoldet = true, skemafoldet = false, sisfoldet = true;
	private ImageButton foldfag, foldskema, foldsis;
	private ListView fag, skema, sis;
	private ProgressDialog loaddialog;
	private SisCrawler siscrawler;
	private SQLiteDatabase database;
	private static final int Rediger = 1;
	private static final String intetnetværk = "Intet netværk tilgængeligt.";
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        //Initialiser
        this.database = this.openOrCreateDatabase(this.getString(R.string.databasenavn), Context.MODE_PRIVATE, null);
        this.fag = (ListView) findViewById(R.id.mainFagList);
        this.loaddialog = new ProgressDialog(this);
        this.sis = (ListView) findViewById(R.id.mainSisList);
        this.skema = (ListView) findViewById(R.id.mainSkemaList);
        this.foldfag = (ImageButton) findViewById(R.id.mainFoldFag);
        this.foldskema = (ImageButton) findViewById(R.id.mainFoldSkema);
        this.foldsis = (ImageButton) findViewById(R.id.mainFoldSis);
        
        //Skriv hvis manglende
        database.execSQL("CREATE TABLE IF NOT EXISTS fag (kursus varchar(50), alias varchar(50), institut varchar(50), link varchar(100))");
        database.execSQL("CREATE TABLE IF NOT EXISTS skema (kursus varchar(50), tidspunkt varchar(11), dag varchar(7), type varchar(50))");
        
        //Hent egne fag
        hentEgneFag();
        //Indlæs skema
        hentSkema();
        
        //Få styr på siscrawleren
        this.siscrawler = (SisCrawler) getLastNonConfigurationInstance();
        //Hvis den ikke er gemt tidligere, eller den nuværende er toppen
        if(this.siscrawler == null || this.siscrawler.isTop() && this.siscrawler.getCurrent().children.size() == 1)	this.siscrawler = new SisCrawler();
        else fill(this.siscrawler.getCurrent().children); //Hvis der bliver ændret orientering
        
        //Hvis det er topnoden
        if(this.siscrawler.getCurrent().children.size() == 0 && this.siscrawler.isTop()){
        	//Opsæt listener til sis
            this.sis.setAdapter(new SisAdapter(this,new String[]{getString(R.string.loadCourses)}));
            this.sis.setOnItemClickListener(new OnItemClickListener(){
    			@Override
    			public void onItemClick(AdapterView<?> arg0, final View arg1, int arg2,
    					long arg3) {
    				String tekst = ((TextView) arg1).getText().toString();
    				if(tekst.equals(getString(R.string.loadCourses))){
    					new Henter().execute((Node) null);
    				}
    			}
            });
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	if(resultCode == RESULT_OK){
			switch(requestCode){
			case Rediger: //Sørg for at opdatere
				hentEgneFag();//
				hentSkema();
				opdaterWidget();
				break;
			}
		}
    }
    
    //Sørger for at stoppe program og sørger for at gå opad i crawleren om 
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
          if(this.siscrawler.isTop()){ //Slut hvis det er toppen
        	  this.database.close();
        	  this.finish();
          }
          else tilbage();
          break;
        case KeyEvent.KEYCODE_HOME:
        	this.database.close();
        	this.finish();
        	break;
        }
        return true;
      }
        
    @Override
    public Object onRetainNonConfigurationInstance() {
        return this.siscrawler;
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putBoolean("foldetfag", this.fagfoldet);
      savedInstanceState.putBoolean("foldetsis", this.sisfoldet);
      savedInstanceState.putBoolean("foldetskema", this.skemafoldet);
    }
    
    private void alert(String m){
    	AlertDialog.Builder b = new AlertDialog.Builder(this);
    	b.setMessage(m);
    	b.setNeutralButton("Ok", null);
    	b.create().show();
    }
    
    private boolean erNetværkTilgængeligt() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }
    
    private void fagpopup(final Entry ent){
    	final String[] items = {this.getString(R.string.VisKursusside), this.getString(R.string.SletKursus), getString(R.string.rediger)};
    	Arrays.sort(items);
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setItems(items, new OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int which) {
				String valg = items[which];
				if(valg.equals(getString(R.string.VisKursusside))){
					viskursus(ent.link);
				}
				else if(valg.equals(getString(R.string.SletKursus))){
					database.execSQL("DELETE FROM fag WHERE kursus='"+ent.fag+"' AND institut='"+ent.parent+"' AND link='"+ent.link+"' AND alias='"+ent.alias+"'");
					database.execSQL("DELETE FROM skema WHERE kursus='"+ent.fag+"'");
					hentEgneFag();
					hentSkema();
					opdaterWidget();
				}
				else if(valg.equals(getString(R.string.rediger))){
					rediger(false, ent.fag, ent.alias);
				}
			}
    	});
		builder.setTitle("Vælg handling for "+ent.fag);
		builder.create().show();
    }
    
    private boolean findeskursus(String kursus){
    	Cursor c = this.database.rawQuery("SELECT * FROM fag WHERE kursus='"+kursus+"'", null);
    	boolean r = c.getCount() > 0;
    	c.close();
    	return r;
    }
    
    private void rediger(boolean nyt, String kursus, String alias){
    	Intent i = new Intent(this, RedigerActivity.class);
		i.putExtra("alias", alias);
		i.putExtra("navn", kursus);
		if(nyt) i.putExtra("nytkursus", true);
		startActivityForResult(i, Rediger);
    }
    
    private void fill(final ArrayList<Node> liste){
    	if(liste == null || liste.size() == 0) return;
    	Collections.sort(liste);
    	int i = 1;
    	int s = liste.size()+1;
    	if(this.siscrawler.isTop()){
    		s--;
    		i--;
    	}
    	String[] data = new String[s];
    	data[0] = getString(R.string.goBack); //Bliver overskrevet ellers
    	for(Node n : liste){
    		data[i] = n.id;
    		i++;
    	}
    	this.sis.setAdapter(new SisAdapter(this,data));
    	this.sis.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				TextView view = (TextView) arg1;
				String tekst = view.getText().toString();
				if(tekst.equals(getString(R.string.goBack))){
					tilbage();
				}
				else{
					//Find rigtige node
					Node r = null;
					for(Node n : liste){
						if(n.id.equals(tekst)){
							r = n;
							break;
						}
					}
					if(r.type.equals(Tree.Node.kursus)){
						kursuspopup(r);
					}
					else if(r.type.equals(Tree.Node.link)){
						new Henter().execute(r);
					}
				}
			}
    	});
    }
    
    private void bevarsisregler(RelativeLayout.LayoutParams sisparams){
    	//Opstil sis
    	RelativeLayout sislayout = (RelativeLayout) findViewById(R.id.mainSis);
    	sisparams.setMargins(0, pixelToDP(6), 0, 0);
    	sisparams.addRule(RelativeLayout.BELOW, R.id.mainFag);
    	sislayout.setLayoutParams(sisparams);
    }
    
    private void bevarskemaregler(RelativeLayout.LayoutParams skemaparams){
    	//Opstil skema
    	RelativeLayout skemalayout = (RelativeLayout) findViewById(R.id.mainSkema);
    	skemaparams.setMargins(0, pixelToDP(6), 0, 0);
    	skemaparams.addRule(RelativeLayout.BELOW, R.id.mainSis);
    	skemalayout.setLayoutParams(skemaparams);
    }
    
    public void foldFag(View v){
    	boolean foldud = this.fagfoldet;
    	//Hvis der skal foldes sammen
    	if(!foldud){
        	RelativeLayout faglayout = (RelativeLayout) findViewById(R.id.mainFag);
        	RelativeLayout fagtekstlayout = (RelativeLayout) findViewById(R.id.mainFagTekstLayout);
        	RelativeLayout.LayoutParams fagP = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,fagtekstlayout.getHeight());
        	
        	//opdater
        	this.foldfag.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.fold_out));
        	faglayout.setLayoutParams(fagP);
        	this.fagfoldet = true;
    		return;
    	}
    	if(!foldud) return;
    	//Hvis der skal foldes ud
    	RelativeLayout faglayout = (RelativeLayout) findViewById(R.id.mainFag);
    	RelativeLayout.LayoutParams fagP = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
    	faglayout.setLayoutParams(fagP);
    	
    	this.foldfag.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.fold));
    	this.fagfoldet = false;
    }
    
    public void foldSkema(View v){
    	boolean foldud = this.skemafoldet;
    	if(!foldud){
        	RelativeLayout skematekstlayout = (RelativeLayout) findViewById(R.id.mainSkemaTekstLayout);
        	RelativeLayout.LayoutParams skemaP = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,skematekstlayout.getHeight());
        	
        	//Opdater
        	bevarskemaregler(skemaP);
        	this.foldskema.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.fold_out));
        	this.skemafoldet = true;
    		return;
    	}
    	//Hvis der skal foldes ud. Bevar reglerne
    	RelativeLayout.LayoutParams skemaP = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
    	bevarskemaregler(skemaP);
    	
    	//Opdater
    	this.foldskema.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.fold));
    	this.skemafoldet = false;
    }
    
    public void foldSis(View v){
    	boolean foldud = this.sisfoldet;
    	if(!foldud){
        	RelativeLayout sistekstlayout = (RelativeLayout) findViewById(R.id.mainSisTekstLayout);
        	RelativeLayout.LayoutParams sisP = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,sistekstlayout.getHeight());
        	
        	//Opdater
        	bevarsisregler(sisP);
        	this.foldsis.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.fold_out));
        	this.sisfoldet = true;
    		return;
    	}
    	//Hvis der skal foldes ud. Bevar reglerne
    	RelativeLayout.LayoutParams sisP = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
    	bevarsisregler(sisP);
    	
    	//Opdater
    	this.foldsis.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.fold));
    	this.sisfoldet = false;
    }
    
    private void hentEgneFag(){
    	//Fjern listener...
    	this.fag.setOnItemClickListener(null);
    	//Og opsæt liste
		Cursor c = database.rawQuery("SELECT * FROM fag ORDER BY alias", null);
		c.moveToFirst();
		int antal = c.getCount();
		TwoLineAdapter.Entry[] data = new TwoLineAdapter.Entry[antal];
		for(int i = 0; !c.isAfterLast(); i++){
			String alias = c.getString(c.getColumnIndex("alias"));
			String kursus = c.getString(c.getColumnIndex("kursus"));
			String institut = c.getString(c.getColumnIndex("institut"));
			String link = c.getString(c.getColumnIndex("link"));
			data[i] = new TwoLineAdapter.Entry(alias,kursus, link, institut);
			c.moveToNext();
		}	
		c.close();
		if(antal == 0) data = new TwoLineAdapter.Entry[]{new TwoLineAdapter.Entry(getString(R.string.IngenFag), getString(R.string.IngenFag), "", "")};
    	Arrays.sort(data);
    	this.fag.setAdapter(new TwoLineAdapter(this,data));
    	if(antal == 0) return;
    	//Tilføj listener hvis nogen fag
    	this.fag.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Entry ent = ((TwoLineAdapter)fag.getAdapter()).getEntries()[arg2];
				fagpopup(ent);
			}
    	});
    }
    
    private void hentSkema(){
    	SkemaAdapter.Entry[] dage = new SkemaAdapter.Entry[5];
    	for(int i = 1; i <= 5; i++){
    		String dag = getString(R.string.friday);
    		if(i == 1) dag = getString(R.string.monday);
    		else if(i == 2) dag = getString(R.string.tuesday);
    		else if(i == 3) dag = getString(R.string.wednesday); 
    		else if(i == 4) dag = getString(R.string.thursday);
    		else dag = getString(R.string.friday);
    		
    		
    		//Find timerne
    		ArrayList<Time> timer = new ArrayList<Time>();
    		Cursor c = this.database.rawQuery("select * from skema where dag='"+dag.toLowerCase()+"'", null);
			while(c.moveToNext()){
				if(c.getCount() == 0) break; //Just in case
				String kursus = c.getString(c.getColumnIndex("kursus"));
				Cursor cd = this.database.rawQuery("SELECT alias FROM fag WHERE kursus='"+kursus+"'", null);
				if(cd.getCount() == 0) break; //Just in case
				cd.moveToFirst();
				String alias = cd.getString(0);
				timer.add(new Time(c.getString(c.getColumnIndex("tidspunkt")), alias, c.getString(c.getColumnIndex("type"))));
				cd.close();
			}
			c.close();
			Collections.sort(timer);
    		dage[i-1] = new SkemaAdapter.Entry(dag, timer);
    	}
    	this.skema.setAdapter(new SkemaAdapter(this,dage));    	
    }
    
    private ArrayList<Node> hentFag(final Node needle) throws Exception{
    	ArrayList<Node> data = null;
    	try{
    		data = this.siscrawler.hent(needle);
    	}
    	catch(Exception e){
    		throw new Exception("Uventet fejl... Nærmere beskrivelse af fejl: "+e.getMessage());
    	}
    	return data;
    }
    
    private void kursuspopup(final Node n){
    	final String[] items = {this.getString(R.string.TilfojTilMineFag),this.getString(R.string.VisKursusside)};
    	Arrays.sort(items);
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setItems(items, new OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int which) {
				String valg = items[which];
				if(valg.equals(getString(R.string.TilfojTilMineFag))){
					new KursusHenter().execute(n);
				}
				else if(valg.equals(getString(R.string.VisKursusside))){
					viskursus(n);
				}
			}
    	});
		builder.setTitle("Vælg handling for "+n.id);
		builder.create().show();
    }
    
    public void nytKursus(View v){
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Indtast kursusnavn");
		final EditText navn = new EditText(this);
		builder.setView(navn);
		builder.setNeutralButton("Ok", new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(findeskursus(navn.getText().toString())){
					alert("Der findes allerede et kursus, der oprindeligt havde samme navn. Vælg nyt.");
					return;
				}
				rediger(true, navn.getText().toString(),  navn.getText().toString());
			}
		});
		builder.create().show();
    }
    
    public int pixelToDP(int p){
    	float d = getResources().getDisplayMetrics().density;
    	return (int)(p * d);
    }
    
    private void opdaterWidget(){
    	WidgetProvider.opdaterWidget(this.getApplicationContext());
    }
        
    private void tilbage(){
    	try{
    		ArrayList<Node> parent = this.siscrawler.tilbage();
        	fill(parent);	
    	}
    	catch(Exception e){
    		alert(e.getMessage());
    	}
   }
    
    private void tilfojfag(TreeMap<String,String> data, Node n, String fag){
    	fag = fag.replaceAll("'", "");
    	n.id = n.id.replaceAll("'", "");
    	String institut = data.get("institut").replaceAll("'", "");
    	//Indsæt alle fagene
    	data.remove("institut"); //Fjern først institut...
    	for(String key : data.keySet()){
    		String[] v = data.get(key).split("#");
    		this.database.execSQL("INSERT INTO skema(kursus,tidspunkt,dag,type) VALUES ('"+fag+"','"+v[2]+"-"+v[3]+"','"+v[1]+"','"+v[0]+"')");
    	}
    	//Først hvis dette går godt, indsæt i fag
    	this.database.execSQL("INSERT INTO fag(kursus,alias,institut,link) VALUES('"+n.id+"','"+n.id+"','"+institut+"','"+n.value+"')");
    	hentEgneFag();
    	hentSkema();
    	opdaterWidget();
    	Toast.makeText(getApplicationContext(), "Kursus blev tilføjet. Husk at fjerne øvelsestimer for de hold, du ikke er på", Toast.LENGTH_LONG).show();
    }
    
    private void viskursus(Node n){
    	viskursus(n.value);
    }
    
    public void skjulloaddialog(){
    	this.loaddialog.hide();
    	this.loaddialog.setTitle("");
    }
    
    public void visloaddialog(String titel){
    	this.loaddialog.setCancelable(true);
    	this.loaddialog.setCanceledOnTouchOutside(false);
    	this.loaddialog.setMessage(titel);
    	this.loaddialog.show();
    }
    
    private void viskursus(String s){
    	if(s.equals("")){
    		alert("Der fandtes ingen kursusside til faget.");
    		return;
    	}
    	Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SisCrawler.root+s));
		startActivity(browserIntent);
    }
    
    private class Henter extends AsyncTask<Node, Integer, ArrayList<Node>>{
    	Exception error;
    	
    	@Override
    	protected void onPreExecute() {
    		if(!erNetværkTilgængeligt()){
    			alert(intetnetværk);
    			this.cancel(true);
    			return;
    		}
    		visloaddialog("Henter 1 side");
    	}

		@Override
		protected ArrayList<Node> doInBackground(Node... params) {
			try{
				return hentFag(params[0]);	
			}
			catch(Exception e){
				e.printStackTrace();
				this.error = e;
				System.out.println(e);
				return null;
			}
		}
    	
		@Override
		protected void onPostExecute(ArrayList<Node> param){
			if(this.error != null){ //Hvis der er sket en fejl i hentningen
				skjulloaddialog();
				alert(error.getMessage());
				return;
			}
			fill(param);
			skjulloaddialog();
		}
    } 
    
    private class KursusHenter extends AsyncTask<Node, Integer, TreeMap<String,String>>{
    	Node n;
    	Exception error;
    	
    	@Override
    	protected void onPreExecute(){
    		if(!erNetværkTilgængeligt()){
    			alert(intetnetværk);
    			this.cancel(true);
    			return;
    		}
    		visloaddialog("Henter 2 sider");
    	}
    	
		@Override
		protected TreeMap<String,String> doInBackground(Node... params) {
			try{
				this.n = params[0];
				if(findeskursus(this.n.id)){
		    		this.error = new Exception("Kursus findes allerede");
		    		return null;
		    	}
			}
			catch(Exception e){
				this.error = e;
				return null;
			}

			TreeMap<String,String> data = null;
			try{
				data = siscrawler.hentKursusInfo(this.n.value);
			}
			catch(Exception e){
				e.printStackTrace();
				this.error = e;
				return null;
			}
			return data;
		}
		
		@Override
		protected void onPostExecute(TreeMap<String,String> params){
			if(error != null){ //Hvis der før er sket en fejl
				skjulloaddialog();
				alert(error.getMessage());
				return;
			}
			tilfojfag(params, this.n, this.n.id);
			skjulloaddialog();
		}
    }
}