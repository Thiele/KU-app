package nu.thiele.kuskemaapp.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.data.Course;
import nu.thiele.kuskemaapp.data.HtmlOption;
import nu.thiele.kuskemaapp.storage.DatabaseContract;
import nu.thiele.kuskemaapp.storage.DatabaseHelper;
import nu.thiele.kuskemaapp.ui.CourseAdapter;
import nu.thiele.kuskemaapp.ui.Dialogs;
import nu.thiele.kuskemaapp.ui.OnManualCourseAdditionRequest;
import nu.thiele.kuskemaapp.utils.CourseLoader;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHour;
import nu.thiele.kuskemaapp.utils.CourseLoader.ScheduleClassLink;
import nu.thiele.kuskemaapp.utils.Utils;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

public class AddCourseFragment extends Fragment {
	private static AddCourseFragment instance;
	private Button addManually, search;
	private CourseLoader loader;
	private EditText freetext;
	private ListView courseList;
	private Map<String,String> periodValueLookupMap, facultyValueLookupMap;
	private Spinner facultySpinner, periodSpinner;
	private OnCourseLoadedListener mCallback;
	private OnManualCourseAdditionRequest manualAdditionCallback;
	private static final String PREFERENCE_BLOCK_VS_SEMESTER = "BLOCK_OR_SEMESTER",
			PREFERENCE_FACULTY = "FACULTY";
	public AddCourseFragment(){
		loader = new CourseLoader();
	}
	
	private void addCourse(Course c){
		CourseClassesLinksFetcher fetcher = new CourseClassesLinksFetcher();
		fetcher.execute(c);
	}
	
	public static AddCourseFragment getInstance(){
		if(instance == null) instance = new AddCourseFragment();
		return instance;
	}
	
	private void doSearch(){
		String faculty = facultyValueLookupMap.get(facultySpinner.getItemAtPosition(facultySpinner.getSelectedItemPosition()).toString());
		String period = periodValueLookupMap.get(periodSpinner.getItemAtPosition(periodSpinner.getSelectedItemPosition()).toString());
		String text = freetext.getText().toString();
		if(faculty == null) faculty = "";
		if(period == null) period = "";
		if(text == null) text = "";
		final String fFaculty = faculty;
		final String fPeriod = period;
		final String fText = text;
		//Ask user if nothing entered. It will be heavy on data otherwise
		if(faculty.isEmpty() && period.isEmpty() && text.isEmpty()){
			Dialogs.confirm(getActivity(), getActivity().getString(R.string.confirm), getActivity().getString(R.string.confirm_heavy_load_warning), getActivity().getString(R.string.yes), getActivity().getString(R.string.no), new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(which == Dialog.BUTTON_POSITIVE){
						//First store user information
						storePreference(PREFERENCE_FACULTY, fFaculty);
						storePreference(PREFERENCE_BLOCK_VS_SEMESTER, fPeriod);
						
						SearchHandler searcher = new SearchHandler(fText, fPeriod, fFaculty);
						searcher.execute();
					}
				}

			});
		}
		else{
			//First store user information
			storePreference(PREFERENCE_FACULTY, fFaculty);
			storePreference(PREFERENCE_BLOCK_VS_SEMESTER, fPeriod);

			//And search
			SearchHandler searcher = new SearchHandler(text, period, faculty);
			searcher.execute();
		}
	}
	
	private long storePreference(String name, String value){
		DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
    	SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues vals = new ContentValues();
        vals.put(DatabaseContract.UserPreferenceEntry.PREFERENCE_NAME, name);
        vals.put(DatabaseContract.UserPreferenceEntry.PREFERENCE_VALUE, value);
        long retval = db.insert(DatabaseContract.UserPreferenceEntry.TABLE_NAME, null, vals);
        db.execSQL(DatabaseContract.UserPreferenceEntry.DELETE_OBSELETE_PREFERENCES(name, 5));
        db.close();
        dbHelper.close();
        return retval;
	}
	
	private void loadForm(){
		SearchFormLoader formLoader = new SearchFormLoader();
		formLoader.execute();
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        this.mCallback = (OnCourseLoadedListener) activity;
        this.manualAdditionCallback = (OnManualCourseAdditionRequest) activity;
        
        //Load basic form now, so no waiting when it's needed
      	this.loadForm();
    }
	
	public interface OnCourseLoadedListener {
        public void onCourseLoaded(SparseArray<List<ClassHour>> result, Course parent);
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
    	ViewGroup v =(ViewGroup) inflater.inflate(R.layout.fragment_add_courses, container, false);
    	//Find buttons and other init stuff
    	this.addManually = (Button) v.findViewById(R.id.add_course_button_add_manually);
    	this.courseList = (ListView) v.findViewById(R.id.add_courses_list_view);
    	this.freetext = (EditText) v.findViewById(R.id.add_courses_freetext);
    	this.search = (Button) v.findViewById(R.id.add_course_button_search);
    	
    	//Load spinners with default info
    	this.freetext = (EditText) v.findViewById(R.id.add_courses_freetext);
    	this.facultySpinner = (Spinner) v.findViewById(R.id.add_courses_spinner_faculty);
    	this.periodSpinner = (Spinner) v.findViewById(R.id.add_courses_spinner_period);
    	
    	List<HtmlOption> facultyValues = new LinkedList<HtmlOption>();
    	List<HtmlOption> periodValues = new LinkedList<HtmlOption>();

    	//Prepare values. Default ones. The newest ones are loaded from internet, but these are backups
    	facultyValues.add(new HtmlOption("",this.getString(R.string.all_faculties)));
    	facultyValues.add(new HtmlOption("FACULTY_0001",this.getString(R.string.faculty_value_health)));
    	facultyValues.add(new HtmlOption("FACULTY_0004",this.getString(R.string.faculty_value_humaniora)));
    	facultyValues.add(new HtmlOption("FACULTY_0006",this.getString(R.string.faculty_value_law)));
    	facultyValues.add(new HtmlOption("FACULTY_0005",this.getString(R.string.faculty_value_nat_bio)));
    	facultyValues.add(new HtmlOption("FACULTY_0002",this.getString(R.string.faculty_value_social_studies)));
    	facultyValues.add(new HtmlOption("FACULTY_0003",this.getString(R.string.faculty_value_theology)));
    	
    	periodValues.add(new HtmlOption("",this.getString(R.string.all_periods)));
    	periodValues.add(new HtmlOption("Block1",this.getString(R.string.period_value_b1)));
    	periodValues.add(new HtmlOption("Block2",this.getString(R.string.period_value_b2)));
    	periodValues.add(new HtmlOption("Block3",this.getString(R.string.period_value_b3)));
    	periodValues.add(new HtmlOption("Block4",this.getString(R.string.period_value_b4)));
    	periodValues.add(new HtmlOption("Autumn,Block1,Block2",this.getString(R.string.period_value_s1)));
    	periodValues.add(new HtmlOption("Spring,Block3,Block4",this.getString(R.string.period_value_s2)));
    	periodValues.add(new HtmlOption("Block5",this.getString(R.string.period_value_summer)));
    	
    	//Handle faculties spinner
    	this.updateFaculties(facultyValues);
    	
    	//Handle periods spinner
    	this.updatePeriods(periodValues);
        
        //Onclicklisteners
        this.addManually.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				manualAdditionCallback.manualCourseAdditionRequest();
			}
        });
        this.search.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
					Utils.hideSoftKeyboard(getActivity());
				}
				return false;
			}
        });
        this.search.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				doSearch();
				Utils.hideSoftKeyboard(getActivity()); //Hide keyboard, as it is probably open
			}
        });
        
        return v;
    }
    
    private void updateCourses(List<Course> courses){
    	if(courses == null || courses.size() == 0) return;
    	Collections.sort(courses, new Course.NameComparator());
    	this.courseList.setAdapter(new CourseAdapter(this.getActivity(),new ArrayList<Course>(courses)));
    	this.courseList.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				final Course c = (Course) courseList.getItemAtPosition(position);
				final String[] options = {getActivity().getString(R.string.add_course),getActivity().getString(R.string.open_course_page)};
				Dialogs.multichoice(getActivity(), c.getName(), options, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which == 0) addCourse(c);
						else if(which == 1){
							String url = CourseLoader.makeFullCoursePageUrl(c.getCourseLink());
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setData(Uri.parse(url));
							startActivity(i);
						}
					}
				});
			}
    	});
    }
    
    private void updateFaculties(List<HtmlOption> faculties){
    	//First find favorite
    	String favorite = this.getFavoriteFaculty();
    	String favName = "";
    	int selection = -1;
    	this.facultyValueLookupMap = new HashMap<String,String>();
    	List<String> texts = new LinkedList<String>();
    	for(HtmlOption opt : faculties){
    		if(opt.getValue().trim().equalsIgnoreCase(favorite.trim())) favName = opt.getText();
    		facultyValueLookupMap.put(opt.getText(), opt.getValue());
    		texts.add(opt.getText());
    	}
    	Collections.sort(texts);
    	ArrayAdapter<String> facultyAdapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_item,texts);
    	facultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	this.facultySpinner.setAdapter(facultyAdapter);
    	selection = facultyAdapter.getPosition(favName);
    	if(selection >= 0) this.facultySpinner.setSelection(selection);
        this.facultySpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
        });
        if(selection >= 0) this.facultySpinner.setSelection(selection);
    }
    
    private void updatePeriods(List<HtmlOption> periods){
    	this.periodValueLookupMap = new HashMap<String,String>();
    	HashMap<String,String> invMap = new HashMap<String,String>();
    	List<String> texts = new LinkedList<String>();
    	for(HtmlOption opt : periods){
    		invMap.put(opt.getValue(), opt.getText());
    		periodValueLookupMap.put(opt.getText(), opt.getValue());
    		texts.add(opt.getText());
    	}
    	Collections.sort(texts);
    	ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_item,texts);
    	dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	this.periodSpinner.setAdapter(dataAdapter);
        this.periodSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
        });
        //Figure out the most probable period, based on current time
        Calendar cal = Calendar.getInstance();
        String val = null;
        switch(cal.get(Calendar.MONTH)){
        case Calendar.JANUARY:
        case Calendar.FEBRUARY:
        case Calendar.MARCH:
        	if(this.usesBlocks()) val = "Block3";
        	else val = "Spring,Block3,Block4";
        case Calendar.APRIL:
        case Calendar.MAY:
        	if(this.usesBlocks()) val = "Block4";
        	else val = "Spring,Block3,Block4";
        case Calendar.JUNE:
        case Calendar.JULY:
        case Calendar.AUGUST:
        	val = "Block5";
        case Calendar.SEPTEMBER:
        case Calendar.OCTOBER:
        	if(this.usesBlocks()) val = "Block1";
        	else val = "Autumn,Block1,Block2";
        	break;
        case Calendar.NOVEMBER:
        case Calendar.DECEMBER:
        	if(this.usesBlocks()) val = "Block2";
        	else val = "Autumn,Block1,Block2";
        	break;
        }
        if(val != null && invMap.containsKey(val)){
        	ArrayAdapter<String> courseAdapter = (ArrayAdapter<String>) periodSpinner.getAdapter();
        	try{
        		int index = courseAdapter.getPosition(invMap.get(val));
        		if(index >= 0) periodSpinner.setSelection(index);
        	}
        	catch(Exception e){}
        }
    }
    
    private String getFavoriteFaculty(){
    	DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
    	SQLiteDatabase db = dbHelper.getReadableDatabase();
    	Cursor c = db.rawQuery(DatabaseContract.UserPreferenceEntry.SELECT_PREFERENCE_VALUES(PREFERENCE_FACULTY), null);
    	HashMap<String,Integer> vals = new HashMap<String,Integer>();
    	while(c.moveToNext()){
    		String val = c.getString(c.getColumnIndex(DatabaseContract.UserPreferenceEntry.PREFERENCE_VALUE)).toLowerCase(Locale.getDefault()).toLowerCase(Locale.getDefault());
    		if(vals.containsKey(val)) vals.put(val, vals.get(val)+1);
    		else vals.put(val, 1);
    	}
    	c.close();
    	db.close();
    	dbHelper.close();
    	int best = -1;
    	String favorite = "";
    	for(String s : vals.keySet()){
    		if(vals.get(s) > best){
    			best = vals.get(s);
    			favorite = s;
    		}
    	}
    	return favorite;
    }
    
    private boolean usesBlocks(){
    	DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
    	SQLiteDatabase db = dbHelper.getReadableDatabase();
    	Cursor c = db.rawQuery(DatabaseContract.UserPreferenceEntry.SELECT_PREFERENCE_VALUES(PREFERENCE_BLOCK_VS_SEMESTER), null);
    	int block = 0;
    	int semester = 0;
    	while(c.moveToNext()){
    		String val = c.getString(c.getColumnIndex(DatabaseContract.UserPreferenceEntry.PREFERENCE_VALUE)).toLowerCase(Locale.getDefault()).toLowerCase(Locale.getDefault());
    		if(val.contains("autumn") || val.contains("spring")) semester++;
    		else if(val.contains("block")) block++;
    	}
    	c.close();
    	db.close();
    	dbHelper.close();
    	return block >= semester; // >= just to make a guess. > would have given semesters a greater chance
    }
    
    private class CourseClassesLinksFetcher extends AsyncTask<Course, Void, List<ScheduleClassLink>>{
    	private Course parent;
    	private Exception error;
    	private ProgressDialog progress;
    	@Override
        protected void onPreExecute() {
    		//Recheck a few times if network is available
    		boolean available = false;
    		for(int i = 1; i <= 3; i++){
    			available = Utils.isNetworkAvailable(getActivity());
    			if(available) break;
    		}
    		//Break if bad
            if(!available){
            	this.cancel(true);
            	Dialogs.alert(getActivity(), getActivity().getString(R.string.error_connecting_to_course_page), getActivity().getString(R.string.network_not_available));
            	return;
            }
            else{
            	this.progress = Dialogs.progress(getActivity(), getActivity().getString(R.string.progress_loading_course_page), true, new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						cancel(true);
					}
            	});
            	Dialogs.progressShow(progress);
            }
        }
    	
		@Override
		protected List<ScheduleClassLink> doInBackground(Course... params) {
			if(params == null || params.length == 0) return null;
			try {
				this.parent = params[0];
				return loader.getCourseClasses(params[0]);
			} catch (IOException e) {
				this.error = e;
			}
			return null;
		}
		
		@Override
	    protected void onPostExecute(final List<ScheduleClassLink> result) {
			Dialogs.progressHide(this.progress);
			if(this.error != null || result == null){
				Dialogs.alert(getActivity(), getActivity().getString(R.string.error_loading)+this.error.getMessage(), getActivity().getString(R.string.error_loading_title));
			}
			else if(result.size() == 0){
				Dialogs.confirm(getActivity(), getActivity().getString(R.string.error_loading_no_classes_found_title), getActivity().getString(R.string.error_loading_no_classes_found), getActivity().getString(R.string.yes),getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which == DialogInterface.BUTTON_POSITIVE){
							manualAdditionCallback.manualCourseAdditionRequest(parent.getName());	
						}
					}
				});
			}
			else{
				if(result.size() == 1){
					CourseClassesScheduleFetcher fetcher = new CourseClassesScheduleFetcher(result.get(0).getParent());
					fetcher.execute(result.get(0));
				}
				if(result.size() > 1){ //Ask for which one
					final String[] names = new String[result.size()];
					int i = 0;
					for(ScheduleClassLink lnk : result){
						names[i] = lnk.getText();
						i++; 
					}
					String prefix = Utils.longestCommonPrefix(names);
					if(prefix.length() >= 3){ //So little difference if less than 3. Don't bother 
						for(i = 0; i < names.length; i++){
							names[i] = names[i].substring(prefix.length());
						}
					}
					Dialogs.multichoice(getActivity(), getActivity().getString(R.string.pick_class_title), names,
							new DialogInterface.OnClickListener(){
								@Override
								public void onClick(DialogInterface dialog, int which) {
									CourseClassesScheduleFetcher fetcher = new CourseClassesScheduleFetcher(result.get(which).getParent());
									fetcher.execute(result.get(which));
								}
					});
				}
			}
	    }
    }
    
    private class CourseClassesScheduleFetcher extends AsyncTask<ScheduleClassLink, Void, SparseArray<List<ClassHour>>>{
    	private Course parent;
    	private Exception error;
    	private ProgressDialog progress;
    	public CourseClassesScheduleFetcher(Course c){
    		this.parent = c;
    	}
    	@Override
        protected void onPreExecute() {
    		//Recheck a few times if network is available
    		boolean available = false;
    		for(int i = 1; i <= 3; i++){
    			available = Utils.isNetworkAvailable(getActivity());
    			if(available) break;
    		}
    		//Break if bad
            if(!available){
            	this.cancel(true);
            	Dialogs.alert(getActivity(), getActivity().getString(R.string.error_connecting_to_course_page), getActivity().getString(R.string.network_not_available));
            	return;
            }
            else{
            	this.progress = Dialogs.progress(getActivity(), getActivity().getString(R.string.progress_loading_class_hours), true, new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						cancel(true);
					}
            	});
            	Dialogs.progressShow(progress);
            }
        }
    	
		@Override
		protected SparseArray<List<ClassHour>> doInBackground(ScheduleClassLink... params) {
			if(params == null || params.length == 0) return null;
			try {
				return loader.getCourseSchedule(params[0]);
			} catch (IOException e) {
				this.error = e;
			}
			return null;
		}
		
		@Override
	    protected void onPostExecute(SparseArray<List<ClassHour>> result) {
			Dialogs.progressHide(this.progress);
			if(this.error != null || result == null){
				Dialogs.alert(getActivity(), getActivity().getString(R.string.error_loading)+this.error.getMessage(), getActivity().getString(R.string.error_loading_title));
			}
			else{
				int num = 0;
				for(int i = 0; i < result.size(); i++){
					if(result.get(i).size() > 0){
						num += result.get(i).size();
						break;
					}
				}
				if(num == 0){ //No classes found for course
					Dialogs.notice(getActivity(), getActivity().getString(R.string.no_classes_found_for_course));
				}
				else{
					mCallback.onCourseLoaded(result, this.parent);
					Dialogs.notice(getActivity(), getActivity().getString(R.string.course_was_added));
				}
			}
	    }
    }
    
    private class SearchHandler extends AsyncTask<String, Void, List<Course>>{
    	private Exception error;
    	private String text, period, faculty;
    	private ProgressDialog progress;
    	public SearchHandler(String text, String period, String faculty){
    		this.text = text;
    		this.period = period;
    		this.faculty = faculty;
    	}
    	@Override
        protected void onPreExecute() {
    		//Recheck a few times if network is available
    		boolean available = false;
    		for(int i = 1; i <= 3; i++){
    			available = Utils.isNetworkAvailable(getActivity());
    			if(available) break;
    		}
    		//Break if bad
            if(!available){
            	this.cancel(true);
            	Dialogs.alert(getActivity(), getActivity().getString(R.string.error_connecting_to_search_page), getActivity().getString(R.string.network_not_available));
            	return;
            }
            else{
            	this.progress = Dialogs.progress(getActivity(), getActivity().getString(R.string.progress_searching_courses), true, new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						cancel(true);
					}
            	});
            	Dialogs.progressShow(progress);
            }
        }
    	
		@Override
		protected List<Course> doInBackground(String... params) {
			try {
				return loader.doSearch(this.text, this.faculty, this.period);
			} catch (IOException e) {
				this.error = e;
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
	    protected void onPostExecute(List<Course> result) {
			Dialogs.progressHide(this.progress);
			if(this.error != null || result == null){
				Dialogs.alert(getActivity(), getActivity().getString(R.string.error_loading)+this.error.getLocalizedMessage(), getActivity().getString(R.string.error_loading_title));
			}
			else{
				if(result.size() == 0){
					Dialogs.alert(getActivity(), getActivity().getString(R.string.no_courses_found), getActivity().getString(R.string.no_courses_found_title));
				}
				else updateCourses(result);
			}
	    }
    }

    private class SearchFormLoader extends AsyncTask<Void, Void, Map<String,List<HtmlOption>>>{
    	private IOException error = null;
    	private static final String KEY_FACULTIES = "FACULTIES", KEY_PERIODS = "PERIODS";
    	
    	@Override
        protected void onPreExecute() {}
    	
		@Override
		protected Map<String,List<HtmlOption>> doInBackground(Void... params) {
			for(int i = 1; i <= 3; i++){
				try {
					loader.initialise();
					Map<String,List<HtmlOption>> retval = new HashMap<String,List<HtmlOption>>();
					retval.put(KEY_FACULTIES, loader.getFaculties());
					retval.put(KEY_PERIODS, loader.getPeriods());
					return retval;
				} catch (IOException e) {
					this.error = e;
				}
			}
			return null; //Just not meant to be
		}  
    	
		@Override
	    protected void onPostExecute(Map<String,List<HtmlOption>> result) {
			if(this.error != null || result == null){}
			else{
				updateFaculties(result.get(KEY_FACULTIES));
				updatePeriods(result.get(KEY_PERIODS));
			}
	    }
    }
}
