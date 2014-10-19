package nu.thiele.kuskemaapp.activities;

import java.util.Locale;

import nu.thiele.kuskemaapp.R;
import nu.thiele.kuskemaapp.fragments.AddEditCourseFragment;
import nu.thiele.kuskemaapp.fragments.AddEditCourseFragment.OnEditListener;
import nu.thiele.kuskemaapp.utils.Utils;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

public class AddEditActivity extends ActionBarActivity implements
	ActionBar.TabListener, OnEditListener{
	
	public static final String INITIAL_DAY = "INITIAL_DAY"; 

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
		mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getCount()-1);
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
		//Also set to initial if it is the one
		int initial = 0;
		if(this.getIntent().getExtras().containsKey(INITIAL_DAY)
				&& this.getIntent().getExtras().getInt(INITIAL_DAY) >= 0
				&& this.getIntent().getExtras().getInt(INITIAL_DAY) < this.mSectionsPagerAdapter.getCount()){
			initial = this.getIntent().getExtras().getInt(INITIAL_DAY);
		}
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this), i == initial);
		}		
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
		FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}
	
	@Override
	public void onTabUnselected(ActionBar.Tab tab,
		FragmentTransaction fragmentTransaction) {
		Utils.hideSoftKeyboard(this); //Hide keyboard if open
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
			this.fragments = new Fragment[7];
		}
		
		@Override
		public Fragment getItem(int position) {
			return AddEditCourseFragment.newInstance(position);
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
				return getString(R.string.monday).toUpperCase(l);
			case 1:
				return getString(R.string.tuesday).toUpperCase(l);
			case 2:
				return getString(R.string.wednesday).toUpperCase(l);
			case 3:
				return getString(R.string.thursday).toUpperCase(l);
			case 4:
				return getString(R.string.friday).toUpperCase(l);
			case 5:
				return getString(R.string.saturday).toUpperCase(l);
			case 6:
				return getString(R.string.sunday).toUpperCase(l);
			}
			return null;
		}
	}
	@Override
	public void editFinished() { 
		this.finish();
	}

	@Override
	public void newEdit() {
		for(Fragment f : this.getSupportFragmentManager().getFragments()){
			if((f instanceof AddEditCourseFragment)){
				AddEditCourseFragment sf = (AddEditCourseFragment) f;
				sf.loadCourses();
				sf.load();
			}
		}
	}
}
