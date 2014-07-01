package nu.thiele.kuskema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import nu.thiele.kuskema.ui.Time;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {
	private static String dag = "mandag";
	private static final String BEFORE = "BEFORE";
	private static final String NEXT = "NEXT";
	private static final String OPEN = "OPEN";
	
	@Override
	public void onEnabled(Context context){
		WidgetProvider.opdaterWidget(context);
	}
	
	public void onReceive(Context context, Intent intent){
		super.onReceive(context, intent); 
		String action = intent.getAction();
		if(action.equals(NEXT)){
			opdaterWidget(context,intToDag(dagToInt(dag)%5+1));
		}
		else if(action.equals(BEFORE)){
			if(dag.equals("mandag")) opdaterWidget(context,"fredag");
			else opdaterWidget(context,intToDag(dagToInt(dag)-1));
		}
	}
	
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		opdaterWidget(context);
    }
    
	public static void opdaterWidget(Context context, String dag){
		WidgetProvider.dag = dag;
    	//Klar gør data
    	String tekst = "";
    	ArrayList<Time> timer = getTimes(dag, context);
    	if(timer == null || timer.size() == 0) tekst = "Skemafri";
    	else{
    		Collections.sort(timer);
    		for(Time t : timer){
    			tekst = tekst+t.tid+":\n"+t.kursus+"\n"+t.type+"\n\n";
    		}
    	}
    	//Og opdater...
    	ComponentName thisWidget = new ComponentName(context,WidgetProvider.class);
    	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    	int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
    	RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		remoteViews.setTextViewText(R.id.widgetSkema, tekst);
		remoteViews.setTextViewText(R.id.widgetOverskrift, dag.substring(0,1).toUpperCase()+dag.substring(1));
		
		//Sæt intents
		Intent op = new Intent(context,SisActivity.class);
		op.setAction(OPEN);
		op.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pop = PendingIntent.getActivity(context, 0, op, 0);
		
		Intent b = new Intent(context, WidgetProvider.class);
		Intent n = new Intent(context, WidgetProvider.class);
		b.setAction(BEFORE);
		n.setAction(NEXT);
		PendingIntent pb = PendingIntent.getBroadcast(context, 0, b, 0);
		PendingIntent pn = PendingIntent.getBroadcast(context, 0, n, 0);
		remoteViews.setOnClickPendingIntent(R.id.widgetVenstre, pb);
		remoteViews.setOnClickPendingIntent(R.id.widgetHojre, pn);
		remoteViews.setOnClickPendingIntent(R.id.widgetLayout, pop);
		appWidgetManager.updateAppWidget(allWidgetIds, remoteViews);
	}
	
    @SuppressWarnings("deprecation")
	public static void opdaterWidget(Context context){
    	Date d = new Date(System.currentTimeMillis());
    	int dint = d.getDay();
    	if(dint > 5) dint = 1;
    	String dag = intToDag(dint);
    	opdaterWidget(context,dag);
    }
    
    private static ArrayList<Time> getTimes(String dag, Context context){
    	ArrayList<Time> retur = new ArrayList<Time>();
    	SQLiteDatabase db = context.openOrCreateDatabase(context.getString(R.string.databasenavn), Context.MODE_PRIVATE, null);
    	Cursor c = db.rawQuery("SELECT * FROM skema WHERE dag='"+dag+"' ORDER BY tidspunkt", null); 
    	while(c.moveToNext()){
    		if(c.getCount() == 0) break; //Just in case
			String kursus = c.getString(c.getColumnIndex("kursus"));
			String tidspunkt = c.getString(c.getColumnIndex("tidspunkt"));
			String type = c.getString(c.getColumnIndex("type"));
			Cursor cd = db.rawQuery("SELECT alias FROM fag WHERE kursus='"+kursus+"'", null);
			if(cd.getCount() == 0) break; //Just in case
			cd.moveToFirst();
			String alias = cd.getString(0);
			cd.close();
			Time t = new Time(tidspunkt,alias,type);
			retur.add(t);
		}
    	c.close();
    	db.close();
    	return retur;
    }
    
    private static int dagToInt(String dag){
    	dag = dag.toLowerCase();
    	if(dag.equals("mandag")) return 1;
    	if(dag.equals("tirsdag")) return 2;
    	if(dag.equals("onsdag")) return 3;
    	if(dag.equals("torsdag")) return 4;
    	return 5;
    	
    }
    
    private static String intToDag(int i){
    	if(i == 1) return "mandag";
    	if(i == 2) return "tirsdag";
    	if(i == 3) return "onsdag";
    	if(i == 4) return "torsdag";
    	if(i == 5) return "fredag";
    	return "mandag";
    }
}