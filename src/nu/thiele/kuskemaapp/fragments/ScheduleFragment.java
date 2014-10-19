package nu.thiele.kuskemaapp.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.data.Course;
import nu.thiele.kuskemaapp.storage.DatabaseContract;
import nu.thiele.kuskemaapp.storage.DatabaseHelper;
import nu.thiele.kuskemaapp.ui.DayAdapter;
import nu.thiele.kuskemaapp.ui.Dialogs;
import nu.thiele.kuskemaapp.ui.OnManualCourseAdditionRequest;
import nu.thiele.kuskemaapp.utils.CourseLoader;
import nu.thiele.kuskemaapp.utils.Utils;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHour;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHourType;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

public class ScheduleFragment extends Fragment {
	private static ScheduleFragment instance;
	private ListView dayList;
	private OnManualCourseAdditionRequest manualAdditionCallback;
	public ScheduleFragment(){}
	
	public static ScheduleFragment getInstance(){
		if(instance == null) instance = new ScheduleFragment();
		return instance;
	} 
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        this.manualAdditionCallback = (OnManualCourseAdditionRequest) activity;
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
    	//Initialise sooo many things
    	ViewGroup v =(ViewGroup) inflater.inflate(R.layout.fragment_schedule, container, false);
    	this.dayList = (ListView) v.findViewById(R.id.schedule_day_list);
    	this.updateCourses();
        return v;
    } 
    
    public void updateCourses(){
    	//Load different courses first
    	DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
    	SQLiteDatabase db = dbHelper.getReadableDatabase();
    	
    	HashMap<Integer,Course> courses = new HashMap<Integer,Course>();
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
    		int id = c.getInt(c.getColumnIndex(BaseColumns._ID));
    		if(id > 0){ //Really should be. But just in case
    			course.setId(id);
        		courses.put(id, course);
    		} 
    	}
    	c.close();
    	
    	//And now, load all of the hours
    	Map<Integer,List<ClassHour>> coursesHours = new HashMap<Integer,List<ClassHour>>();
    	//public ClassHour(Course parent, int day, String start, String end, String room, String teacher, ClassHourType type){
    	c = db.rawQuery(DatabaseContract.CourseClassEntry.SELECT_ALL_CLASS_FOR_ALL_COURSES, null);
    	while(c.moveToNext()){
    		int courseId = c.getInt(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_ID));
    		int hourId = c.getInt(c.getColumnIndex(BaseColumns._ID));
    		if(courses.containsKey(courseId)){
    			String chType = c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_TYPE));
    			ClassHourType actualType = ClassHourType.UNKNOWN;
    			try{
    				actualType = ClassHourType.valueOf(chType);
    			}
    			catch(Exception e){
    				e.printStackTrace();
    			};
    			ClassHour ch = new ClassHour(
    					courses.get(courseId),
    					c.getInt(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_DAY)),
    					c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_START_TIME)),
    					c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_END_TIME)),
    					c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_PLACE)),
    					c.getString(c.getColumnIndex(DatabaseContract.CourseClassEntry.COURSE_TEACHER)),
    					actualType
    					);
    			ch.setId(hourId);
    			//And add it
    			List<ClassHour> daysHours = coursesHours.get(ch.getDay());
    			if(daysHours == null) daysHours = new LinkedList<ClassHour>();
    			daysHours.add(ch);
    			coursesHours.put(ch.getDay(), daysHours);
    		}
    	}
    	c.close();
    	db.close();
    	dbHelper.close();
    	
    	//And do stuff with it
    	//Make sure every days exists, and sort them
    	for(int i = 0; i <= 6; i++){ //Make sure all days are in there
    		if(!coursesHours.containsKey(i)) coursesHours.put(i, new LinkedList<ClassHour>());
    		Collections.sort(coursesHours.get(i), new ClassHour.StartEndComparator());
    	}
    	this.dayList.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					final int position, long id) {
					int numOptionsPerCourse = 1;
					String dayName = Utils.dayNameByIndex(getActivity(), position);
					DayAdapter adapt = (DayAdapter) dayList.getAdapter();
					List<ClassHour> hours = (List<ClassHour>) adapt.getItem(position);
					final ArrayList<Course> courses = new ArrayList<Course>(); //Find all different courses for the day
					for(ClassHour ch : hours){
						if(!courses.contains(ch.getParent())) courses.add(ch.getParent());
					}
					//Sort it
					Collections.sort(courses, new Course.NameComparator());
					String[] opts = new String[1+courses.size()*numOptionsPerCourse];
					opts[0] = getActivity().getString(R.string.edit_classes);
					int i = 1;
					for(Course c : courses){
						opts[i] = c.getName();
						i += numOptionsPerCourse;
					}
					Dialogs.multichoice(getActivity(), dayName, opts, new OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(which == 0) manualAdditionCallback.manualCourseAdditionRequest(position);
							else{ //Some course pressed
								final Course picked = courses.get(which-1);
								String[] courseOpts = { //If a course page is present
										getActivity().getString(R.string.rename_course),
										getActivity().getString(R.string.remove_course)
									};
								if(picked.getCourseLink() != null && !picked.getCourseLink().isEmpty()){
									courseOpts = new String[]{
											getActivity().getString(R.string.open_course_page),
											getActivity().getString(R.string.rename_course),
											getActivity().getString(R.string.remove_course)
										};
								}
								final String[] finalCourseOpts = courseOpts;
								Dialogs.multichoice(getActivity(), picked.getName(), finalCourseOpts, new OnClickListener(){
									@Override
									public void onClick(DialogInterface dialog, int which) {
										if(which == 0 && finalCourseOpts.length == 3){ //Open course page
											String url = CourseLoader.makeFullCoursePageUrl(picked.getCourseLink());
											Intent i = new Intent(Intent.ACTION_VIEW);
											i.setData(Uri.parse(url));
											startActivity(i);
										}
										else if(which == 1 && finalCourseOpts.length == 3 || which == 0 && finalCourseOpts.length == 2){
											final EditText input = new EditText(getActivity());
											input.setText(picked.getName());
											input.setInputType(InputType.TYPE_CLASS_TEXT);											
											Dialogs.customInput(getActivity(), getString(R.string.enter_new_name), input, getActivity().getString(R.string.ok), getActivity().getString(R.string.cancel), new OnClickListener(){
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
													db.update(DatabaseContract.CourseEntry.TABLE_NAME, cv, BaseColumns._ID+"="+picked.getId(), null);
													Dialogs.notice(getActivity(), getActivity().getString(R.string.course_got_updated));
													db.close();
													dbHelper.close();
													
													//Renew courses
													updateCourses();
												}
											});
										}
										else if(which == 2 && finalCourseOpts.length == 3 || which == 1 && finalCourseOpts.length == 2){
											Dialogs.confirm(
													getActivity(),
													getActivity().getString(R.string.confirm),
													getActivity().getString(R.string.confirm_course_delete), 
													getActivity().getString(R.string.yes),
													getActivity().getString(R.string.no),
													new OnClickListener(){
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
															if(which == AlertDialog.BUTTON_POSITIVE){ //Oh yeah, delete!
																DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
														    	SQLiteDatabase db = dbHelper.getWritableDatabase();
														    	db.execSQL(DatabaseContract.CourseClassEntry.DELETE_ALL_COURSE_CLASSES(picked.getId()));
														    	db.execSQL(DatabaseContract.CourseEntry.DELETE_COURSE(picked.getId()));
														    	db.close();
														    	dbHelper.close();
														    	//Notify user
														    	Dialogs.notice(getActivity(), getActivity().getString(R.string.course_was_deleted));
														    	//And update the view
														    	updateCourses();
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
    	});

    	this.dayList.setAdapter(new DayAdapter(this.getActivity(), coursesHours));
    }
}
