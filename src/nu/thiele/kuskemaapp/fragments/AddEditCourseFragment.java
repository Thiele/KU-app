package nu.thiele.kuskemaapp.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.data.Course;
import nu.thiele.kuskemaapp.storage.DatabaseContract;
import nu.thiele.kuskemaapp.storage.DatabaseHelper;
import nu.thiele.kuskemaapp.ui.CourseEditAdapter;
import nu.thiele.kuskemaapp.ui.Dialogs;
import nu.thiele.kuskemaapp.utils.Utils;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHour;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHourType;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemClickListener;

public class AddEditCourseFragment extends Fragment{
	private Button finishButton;
	private ListView hoursList;
	private SparseArray<Course> allCourses;
	private OnEditListener callback;
	private int day;
	public AddEditCourseFragment(){
		this(0);
	}
	public AddEditCourseFragment(int d){
		this.day = d;
	}
	
	public static AddEditCourseFragment newInstance(int d){
		AddEditCourseFragment fragment = new AddEditCourseFragment(d);
		return fragment;
	} 
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.callback = (OnEditListener) activity;
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
    	//Initialise sooo many things
    	ViewGroup v =(ViewGroup) inflater.inflate(R.layout.fragment_add_edit_course, container, false);
    	
		//Init stuff
		this.allCourses = new SparseArray<Course>();
		this.finishButton = (Button) v.findViewById(R.id.add_edit_save_courses);
		this.hoursList = (ListView) v.findViewById(R.id.add_edit_hours_list);
		        
        //Find all courses
        this.loadCourses();
		
		this.finishButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				finishActivity();
			}
		});
		//And load the day
		this.load();

    	
        return v;
    } 
    
    private void finishActivity(){
    	this.callback.editFinished();
    }
    
	public void loadCourses(){
		this.allCourses.clear();
		DatabaseHelper dbHelper = new DatabaseHelper(this.getActivity());
    	SQLiteDatabase db = dbHelper.getReadableDatabase();
    	Cursor c = db.rawQuery(DatabaseContract.CourseEntry.SELECT_ALL_COURSES, null);
    	while(c.moveToNext()){
    		Course course = new Course(
    				c.getString(c.getColumnIndex(DatabaseContract.CourseEntry.COURSE_TITLE)),
    				c.getString(c.getColumnIndex(DatabaseContract.CourseEntry.COURSE_LINK)),
    				c.getString(c.getColumnIndex(DatabaseContract.CourseEntry.COURSE_STUDY_NAEVN)),
    				c.getString(c.getColumnIndex(DatabaseContract.CourseEntry.COURSE_ECTS)),
    				c.getString(c.getColumnIndex(DatabaseContract.CourseEntry.COURSE_LEVEL)),
    				c.getString(c.getColumnIndex(DatabaseContract.CourseEntry.COURSE_LANGUAGE)),
    				c.getString(c.getColumnIndex(DatabaseContract.CourseEntry.COURSE_YEAR))
    				);
    		course.setId(c.getInt(c.getColumnIndex(BaseColumns._ID)));
    		this.allCourses.put(course.getId(), course);
    	}
    	c.close();
    	db.close();
    	dbHelper.close();
	}
	
	public void load(){
    	ArrayList<ClassHour> todaysHours = new ArrayList<ClassHour>();
		DatabaseHelper dbHelper = new DatabaseHelper(this.getActivity());
    	SQLiteDatabase db = dbHelper.getReadableDatabase();
    	Cursor c = db.rawQuery(DatabaseContract.CourseClassEntry.SELECT_ALL_CLASS_FOR_ALL_COURSES(day), null);
    	while(c.moveToNext()){
    		int courseId = c.getInt(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_ID));
    		int hourId = c.getInt(c.getColumnIndex(BaseColumns._ID));
    		String chType = c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_TYPE));
			ClassHourType actualType = ClassHourType.UNKNOWN;
			try{
				actualType = ClassHourType.valueOf(chType);
			}
			catch(Exception e){
				e.printStackTrace();
			};
			if(this.allCourses.get(courseId) != null){
				ClassHour ch = new ClassHour(
						this.allCourses.get(courseId),
						c.getInt(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_DAY)),
						c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_START_TIME)),
						c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_END_TIME)),
						c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_PLACE)),
						c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_TEACHER)),
						actualType
						);
				boolean highlight = ((int)c.getInt(c.getColumnIndex(DatabaseContract.CourseClassEntry.IS_READY))) != 0;
				ch.setId(hourId);
				ch.setHighlight(highlight);
				todaysHours.add(ch);
			}
    	}
    	c.close();
    	db.close();
    	dbHelper.close();
    	
    	//Sort the data
    	Collections.sort(todaysHours, new ClassHour.StartEndComparator());
    	//And set it
    	this.hoursList.setAdapter(new CourseEditAdapter(this.getActivity(), todaysHours));
    	this.hoursList.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				CourseEditAdapter adapter = (CourseEditAdapter) hoursList.getAdapter();
				if(!adapter.isCourseHour(position)){
					//Room for new courses
					final String[] opts = new String[allCourses.size()+1];
					final LinkedList<Course> c = new LinkedList<Course>();
					for(int i = 0; i < allCourses.size(); i++){
						c.add(allCourses.get(allCourses.keyAt(i)));
					}
					Collections.sort(c, new Course.NameComparator());
					int i = 0;
					for(Course course : c){
						opts[i] = course.getName();
						i++;
					}
					opts[i] = getActivity().getString(R.string.add_new_course); //Last spot for new course
					Dialogs.multichoice(getActivity(), getActivity().getString(R.string.addition_of_hours), opts, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(which == opts.length-1){ //Completely new course
								final EditText input = new EditText(getActivity());
								input.setHint(R.string.enter_course_name_hint);
								input.setInputType(InputType.TYPE_CLASS_TEXT);
								Dialogs.customInput(getActivity(), getString(R.string.enter_course_name), input, getActivity().getString(R.string.ok), getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener(){
									@Override
									public void onClick(
											DialogInterface dialog,
											int which) {
										String newCourseName = input.getText().toString();
										if(newCourseName == null || newCourseName.isEmpty()){
											Dialogs.alert(getActivity(), getActivity().getString(R.string.course_name_cannot_be_empty), getActivity().getString(R.string.error));
											return;
										}
										//Insert new course
										ContentValues cv = new ContentValues();
										cv.put(DatabaseContract.CourseEntry.COURSE_TITLE, newCourseName);
										DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
										SQLiteDatabase db = dbHelper.getWritableDatabase();
										long courseId = db.insert(DatabaseContract.CourseEntry.TABLE_NAME,null,cv);
										//Insert new default hour
										ContentValues values = getDefaultClassHourContentValues(courseId);
						    	        db.insert(DatabaseContract.CourseClassEntry.TABLE_NAME,null,values);
										db.close();
										dbHelper.close();
										loadCourses();
										load();
									}
								});
							}
							else{ //Use course name
								//We are going to need these
								DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
						    	final SQLiteDatabase db = dbHelper.getWritableDatabase();
								Course course = c.get(which);
							    ContentValues values = getDefaultClassHourContentValues(course.getId());
				    	        db.insert(DatabaseContract.CourseClassEntry.TABLE_NAME,null,values);
				    	        db.close();
								dbHelper.close();
								//And reload views
								load();
							}
						}
					});
				}
				else{
					final ClassHour ch = (ClassHour) adapter.getItem(position);
					String[] opts = {
							getString(R.string.change_course_type),
							getString(R.string.change_course_time),
							getString(R.string.change_course_place),
							getString(R.string.remove_class_hour),
							getString(R.string.rename_course),
							getString(R.string.remove_course)
					};
					Dialogs.multichoice(getActivity(), ch.getStart()+": "+ch.getParent().getName(), opts, new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(which == 0){
								final String[] types = new String[ClassHourType.values().length];
								for(int i = 0; i < ClassHourType.values().length; i++){
									types[i] = Utils.classTypeStringByIndex(getActivity(), ClassHourType.values()[i]);
								}
								Dialogs.multichoice(getActivity(), getActivity().getString(R.string.choose_new_course_type), types, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										ContentValues cv = new ContentValues();
										cv.put(DatabaseContract.CourseClassEntry.COURSE_TYPE, ClassHourType.values()[which].toString());		
										cv.put(DatabaseContract.CourseClassEntry.IS_READY, "0");
										DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
										SQLiteDatabase db = dbHelper.getWritableDatabase();
										db.update(DatabaseContract.CourseClassEntry.TABLE_NAME, cv, BaseColumns._ID+"="+ch.getId(), null);
										db.close();
										dbHelper.close();
										load();
									}
								});
							}
							else if(which == 1){ //Changing time
								final TimePicker inputStart = new TimePicker(getActivity());
								inputStart.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
								String start = "";
								if(ch.getStart() != null) start = ch.getStart();
								try{
									String[] split = start.split(":");
									int hours = Integer.parseInt(split[0]);
									int minutes = Integer.parseInt(split[1]);
									inputStart.setCurrentHour(hours);
									inputStart.setCurrentMinute(minutes);
								}
								catch(Exception e){}
								Dialogs.customInput(getActivity(),
										getString(R.string.choose_start_time),
										inputStart,
										getActivity().getString(R.string.ok),
										getActivity().getString(R.string.cancel),
										new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface dialog,int which) {
										final TimePicker inputEnd = new TimePicker(getActivity());
										inputEnd.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
										String end = "";
										if(ch.getEnd() != null) end = ch.getEnd();
										try{
											String[] split = end.split(":");
											int hours = Integer.parseInt(split[0]);
											int minutes = Integer.parseInt(split[1]);
											inputEnd.setCurrentHour(hours);
											inputEnd.setCurrentMinute(minutes);
										}
										catch(Exception e){}
										Dialogs.customInput(getActivity(), getString(R.string.choose_end_time), inputEnd, getActivity().getString(R.string.ok), getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener(){
											@Override
											public void onClick(DialogInterface dialog,int which) {
												//Check if end is earlier than start
												int hoursStart = inputStart.getCurrentHour();
												int hoursEnd = inputEnd.getCurrentHour();
												int minutesStart = inputStart.getCurrentMinute();
												int minutesEnd = inputEnd.getCurrentMinute();
												if(hoursEnd < hoursStart || hoursStart == hoursEnd && minutesStart < minutesEnd){
													Dialogs.notice(getActivity(), "Det skal starte, før det slutter..."); //MOVE TO XML
													return;
												}
												else{
													String newStart = Utils.clockify(hoursStart)+":"+Utils.clockify(minutesStart);
													String newEnd = Utils.clockify(hoursEnd)+":"+Utils.clockify(minutesEnd);
													ContentValues cv = new ContentValues();
													//Remove highlight if it's properly entered
													cv.put(DatabaseContract.CourseClassEntry.IS_READY, "0");
													cv.put(DatabaseContract.CourseClassEntry.COURSE_START_TIME, newStart);
													cv.put(DatabaseContract.CourseClassEntry.COURSE_END_TIME, newEnd);
													DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
													SQLiteDatabase db = dbHelper.getWritableDatabase();
													db.update(DatabaseContract.CourseClassEntry.TABLE_NAME, cv, BaseColumns._ID+"="+ch.getId(), null);
													db.close();
													dbHelper.close();
													load();													
												}
											}
										});
									}
								});
							}
							else if(which == 2){ //Changing place
								final EditText input = new EditText(getActivity());
								String place = "";
								if(ch.getRoom() != null) place = ch.getRoom();
								input.setHint(getString(R.string.default_room)); 
								input.setText(place);
								input.setInputType(InputType.TYPE_CLASS_TEXT);
								Dialogs.customInput(getActivity(), getString(R.string.enter_new_place), input, getActivity().getString(R.string.ok), getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener(){
									@Override
									public void onClick(
											DialogInterface dialog,
											int which) {
										String newName = input.getText().toString();
										//And update
										ContentValues cv = new ContentValues();
										cv.put(DatabaseContract.CourseClassEntry.COURSE_PLACE, newName);
										cv.put(DatabaseContract.CourseClassEntry.IS_READY, "0");
										DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
										SQLiteDatabase db = dbHelper.getWritableDatabase();
										db.update(DatabaseContract.CourseClassEntry.TABLE_NAME, cv, BaseColumns._ID+"="+ch.getId(), null);
										db.close();
										dbHelper.close();
										load();
									}
								});
							}
							else if(which == 3){ //Deleting single class
								DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
								SQLiteDatabase db = dbHelper.getWritableDatabase();
								db.execSQL(DatabaseContract.CourseClassEntry.DELETE_COURSE_CLASS(ch.getId()));
								db.execSQL(DatabaseContract.CourseEntry.DELETE_LEFTOVER_COURSES); //If no classes left, removes course. Bonus: Removes any residual classes, which there should not be any of
								db.close();
								dbHelper.close();
								load();
							}
							else if(which == 4){ //Renaming
								final EditText input = new EditText(getActivity());
								input.setText(ch.getParent().getName());
								input.setInputType(InputType.TYPE_CLASS_TEXT);
								Dialogs.customInput(getActivity(), getString(R.string.enter_new_name), input, getActivity().getString(R.string.ok), getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener(){
									@Override
									public void onClick(
											DialogInterface dialog,
											int which) {
										String newName = input.getText().toString();
										if(newName == null || newName.isEmpty()){
											Dialogs.alert(getActivity(), getActivity().getString(R.string.course_name_cannot_be_empty), getActivity().getString(R.string.error));
											return;
										}
										
										//And update
										ContentValues cv = new ContentValues();
										cv.put(DatabaseContract.CourseEntry.COURSE_TITLE, newName);
										
										DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
										SQLiteDatabase db = dbHelper.getWritableDatabase();
										db.update(DatabaseContract.CourseEntry.TABLE_NAME, cv, BaseColumns._ID+"="+ch.getParent().getId(), null);
										db.close();
										dbHelper.close();
										loadCourses();
										load();
										callback.newEdit(); //Remember to do it for all
									}
								});
							}
							else if(which == 5){ //Deleting it
								Dialogs.confirm(
										getActivity(),
										getActivity().getString(R.string.confirm),
										getActivity().getString(R.string.confirm_course_delete), 
										getActivity().getString(R.string.yes),
										getActivity().getString(R.string.no),
										new DialogInterface.OnClickListener(){
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												if(which == AlertDialog.BUTTON_POSITIVE){ //Oh yeah, delete!
													DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
											    	SQLiteDatabase db = dbHelper.getWritableDatabase();
											    	db.execSQL(DatabaseContract.CourseClassEntry.DELETE_ALL_COURSE_CLASSES(ch.getParent().getId()));
											    	db.execSQL(DatabaseContract.CourseEntry.DELETE_COURSE(ch.getParent().getId()));
											    	db.close();
											    	dbHelper.close();
											    	//And update
											    	loadCourses();
											    	load();
											    	callback.newEdit();
												}
											}
										});
							}
						}
					});
				}
			} 
    	});
	}
	
	private ContentValues getDefaultClassHourContentValues(long id){
		ContentValues values = new ContentValues();
	    values.put(DatabaseContract.CourseClassEntry.COURSE_DAY, this.day);
	    values.put(DatabaseContract.CourseClassEntry.IS_READY, "1");
        values.put(DatabaseContract.CourseClassEntry.COURSE_END_TIME, "00:00");
        values.put(DatabaseContract.CourseClassEntry.COURSE_PLACE, "");
        values.put(DatabaseContract.CourseClassEntry.COURSE_START_TIME, "00:00");
        values.put(DatabaseContract.CourseClassEntry.COURSE_TEACHER, "");
        values.put(DatabaseContract.CourseClassEntry.COURSE_TYPE, ClassHourType.UNKNOWN.toString());
        values.put(DatabaseContract.CourseClassEntry.COURSE_ID, id);
        return values;
	}
	
	public interface OnEditListener {
        public void editFinished();
        public void newEdit();
    }
}
