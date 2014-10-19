package nu.thiele.kuskemaapp.ui;

import nu.thiele.kuskemaapp.R;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Class used to ensure the same behaviour throughout app
 * @author Andreas
 *
 */
public class Dialogs {
	public static void alert(Context c, String text, String title){
		alert(c,text, title,c.getString(R.string.dialog_alert_default_button));
	}
	
	public static void alert(Context c, String text, String title, String button){
		AlertDialog.Builder builder = createAlertDialogBuilder(c);
		builder.setTitle(title);
		builder.setMessage(text);
		builder.setNeutralButton(button, null);
		showAlertDialog(builder.create());
	}
	
	public static void confirm(Context c, String title, String text, String yes, String no, DialogInterface.OnClickListener onclick){
		AlertDialog.Builder builder = createAlertDialogBuilder(c);
		builder.setTitle(title);
		builder.setMessage(text);
		builder.setNegativeButton(no, onclick);
		builder.setPositiveButton(yes, onclick);
		showAlertDialog(builder.create());
	}
	
	private static AlertDialog.Builder createAlertDialogBuilder(Context c){
		AlertDialog.Builder builder = new AlertDialog.Builder(c);
		return builder;
	}
	
	public static void customInput(Context c, String title, View customView, String ok, String cancel, DialogInterface.OnClickListener onclick){
		AlertDialog.Builder builder = createAlertDialogBuilder(c);
		builder.setTitle(title);

		builder.setView(customView);
		builder.setPositiveButton(ok, onclick);
		builder.setNegativeButton(cancel, null);

		showAlertDialog(builder.create());
	}
	
	public static void multichoice(Context c, String msg, String[] options, OnClickListener onclick){
		AlertDialog.Builder builder = createAlertDialogBuilder(c);
		builder.setTitle(msg);
		builder.setItems(options, onclick);
		showAlertDialog(builder.create());
	}
	
	public static void notice(Context c, String note){
		Toast.makeText(c, note, Toast.LENGTH_SHORT).show();
	}
	
	public static ProgressDialog progress(Context c, String msg, boolean cancellable, OnCancelListener listener){
		ProgressDialog prog = new ProgressDialog(c);
		prog.setMessage(msg);
		if(cancellable){
			prog.setCancelable(true);
			prog.setOnCancelListener(listener);
		}
		return prog;
	}
	
	public static ProgressDialog progress(Context c, String msg){
		return progress(c, msg, false, null);
	}
	
	public static void progressHide(ProgressDialog dialog){
		dialog.dismiss();
	}
	
	public static void progressShow(ProgressDialog dialog){
		dialog.show();
	}

	private static void showAlertDialog(AlertDialog dialog){
		/*
		 * Everything inside this function is hacks.
		 * Bad, but seems like one of the simplest ways in Android to style dialogs
		 */
		dialog.show();
		
		//Try setting colour of things
		try{
			//Set text colour
			int textViewId = dialog.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
			TextView tv = (TextView) dialog.findViewById(textViewId);
			tv.setTextColor(dialog.getContext().getResources().getColor(R.color.dialog_text_color));
			 
			//Set divider colour
			int dividerId = dialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
			View divider = dialog.findViewById(dividerId);
			divider.setBackgroundColor(dialog.getContext().getResources().getColor(R.color.highlight_color));
		}
		catch(Exception e){}
	}
}
