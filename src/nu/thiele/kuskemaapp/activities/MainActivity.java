package nu.thiele.kuskemaapp.activities;


import java.util.List;
import java.util.Locale;

import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.data.Course;
import nu.thiele.kuskemaapp.fragments.AddCourseFragment;
import nu.thiele.kuskemaapp.fragments.ScheduleFragment;
import nu.thiele.kuskemaapp.fragments.AddCourseFragment.OnCourseLoadedListener;
import nu.thiele.kuskemaapp.storage.DatabaseContract;
import nu.thiele.kuskemaapp.storage.DatabaseHelper;
import nu.thiele.kuskemaapp.ui.OnManualCourseAdditionRequest;
import nu.thiele.kuskemaapp.utils.Utils;
import nu.thiele.kuskemaapp.utils.CourseLoader.ClassHour;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends ActionBarActivity implements
		ActionBar.TabListener, OnCourseLoadedListener, OnManualCourseAdditionRequest{
	private static final int ADD_EDIT_RESULT = 1;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a {@link FragmentPagerAdapter}
	 * derivative, which will keep every loaded fragment in memory. If this
	 * becomes too memory intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, requestCode, data);
	    if (requestCode == ADD_EDIT_RESULT) {
	        //Update no matter what
	    	this.loadScheduleFragmentContent();
	    }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getCount()-1); //Risky, but with only 3 it's fine
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		//Make sure keyboard is gone
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		Utils.hideSoftKeyboard(this); //Hide keyboard if open from user writing in edittext
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		private Fragment[] fragments;
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
			this.fragments = new Fragment[2];
		}

		@Override
		public Fragment getItem(int position) {
			switch(position){
				default:
					return PlaceholderFragment.newInstance(position);
				case 0:
					return ScheduleFragment.getInstance();
				case 1:
					return AddCourseFragment.getInstance();
			}
		}

		@Override
		public int getCount() {
			return this.fragments.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment(sectionNumber);
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment(int num) {
			
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
 			return rootView;
		}
	}

	@Override
	public void onCourseLoaded(SparseArray<List<ClassHour>> schedule, Course parent) {
		DatabaseHelper dbHelper = new DatabaseHelper(this);
    	SQLiteDatabase db = dbHelper.getWritableDatabase();
    	
    	//Store course info
    	ContentValues values = new ContentValues();
        values.put(DatabaseContract.CourseEntry.COURSE_ECTS, parent.getECTS());
        values.put(DatabaseContract.CourseEntry.COURSE_LANGUAGE, parent.getLanguage());
        values.put(DatabaseContract.CourseEntry.COURSE_LINK, parent.getCourseLink());
        values.put(DatabaseContract.CourseEntry.COURSE_STUDY_NAEVN, parent.getStudieNaevn());
        values.put(DatabaseContract.CourseEntry.COURSE_TITLE, parent.getName());

        long newRowId = db.insert(DatabaseContract.CourseEntry.TABLE_NAME,null,values);
        //Second, store class schedule
    	for(int i = 0; i < schedule.size(); i++){
    		for(ClassHour ch : schedule.get(i)){
    	        values = new ContentValues();
    	        values.put(DatabaseContract.CourseClassEntry.COURSE_DAY, ch.getDay());
    	        values.put(DatabaseContract.CourseClassEntry.COURSE_END_TIME, ch.getEnd());
    	        values.put(DatabaseContract.CourseClassEntry.COURSE_ID, newRowId);
    	        values.put(DatabaseContract.CourseClassEntry.COURSE_PLACE, ch.getRoom());
    	        values.put(DatabaseContract.CourseClassEntry.COURSE_START_TIME, ch.getStart());
    	        values.put(DatabaseContract.CourseClassEntry.COURSE_TEACHER, ch.getTeacher());
    	        values.put(DatabaseContract.CourseClassEntry.COURSE_TYPE, ch.getHourType().toString());
    	        db.insert(DatabaseContract.CourseClassEntry.TABLE_NAME,null,values);
    		}
    	}
    	db.close();
    	dbHelper.close();
		
		//Update schedule with new courses
		this.loadScheduleFragmentContent();
	}
	
	private void loadScheduleFragmentContent(){ 
		for(Fragment f : this.getSupportFragmentManager().getFragments()){
			if((f instanceof ScheduleFragment)){
				ScheduleFragment sf = (ScheduleFragment) f;
				sf.updateCourses();
				break;
			}
		}
	}

	@Override
	public void manualCourseAdditionRequest() {
		this.manualCourseAdditionRequest(0, ""); //No day given. Start on monday
	}

	@Override
	public void manualCourseAdditionRequest(int day) {
		this.manualCourseAdditionRequest(day, "");
	}

	@Override
	public void manualCourseAdditionRequest(String courseName) {
		this.manualCourseAdditionRequest(0, courseName);
	}

	@Override
	public void manualCourseAdditionRequest(int day, String courseName) {
		if(day < 0 || day > 6) return;
		if(courseName != null && !courseName.isEmpty()){ //Save the new one
			DatabaseHelper dbHelper = new DatabaseHelper(this);
	    	SQLiteDatabase db = dbHelper.getWritableDatabase();
	    	ContentValues cv = new ContentValues();
	    	cv.put(DatabaseContract.CourseEntry.COURSE_TITLE, courseName);
	    	db.insert(DatabaseContract.CourseEntry.TABLE_NAME, null, cv);
	    	db.close();
	    	dbHelper.close();
		}
		Intent i = new Intent(this, AddEditActivity.class);
		i.putExtra(AddEditActivity.INITIAL_DAY, day);
		this.startActivityForResult(i, ADD_EDIT_RESULT);
	}
}
